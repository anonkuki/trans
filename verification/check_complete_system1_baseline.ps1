param(
    [Parameter()]
    [string]$RepositoryRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
    $RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}
$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$violations = [System.Collections.Generic.List[string]]::new()
$excludedDirectoryPattern = '\\(?:\.git|node_modules|target|dist|dist-prod|dist-test|logs|\.idea|\.vscode)(?:\\|$)'

function Get-RelativePath([string]$path) {
    return $path.Substring($root.Length).TrimStart('\').Replace('\', '/')
}

$files = @(Get-ChildItem -LiteralPath $root -File -Recurse -Force |
        Where-Object { $_.FullName -notmatch $excludedDirectoryPattern })

foreach ($file in $files) {
    $relativePath = Get-RelativePath $file.FullName
    if ($file.Name -in @('nacos.sql', 'svaai.sql', 'svaai_zs.sql')) {
        $violations.Add("raw-database-dump:$relativePath")
    }
    if (($file.Name -eq '.env' -or $file.Name -like '.env.*') -and $file.Name -ne '.env.example') {
        $violations.Add("local-environment-file:$relativePath")
    }
}

$nestedGit = @(Get-ChildItem -LiteralPath $root -Directory -Recurse -Force -Filter '.git' |
        Where-Object { $_.FullName -ne (Join-Path $root '.git') })
foreach ($directory in $nestedGit) {
    $violations.Add("nested-git-metadata:$(Get-RelativePath $directory.FullName)")
}

$textExtensions = @('.java', '.kt', '.xml', '.yaml', '.yml', '.properties', '.json', '.ts', '.tsx', '.js', '.vue', '.py', '.md', '.txt', '.ps1')
$literalProviderKeyPattern = '(?<![A-Za-z])sk-[A-Za-z0-9._-]{20,}'
foreach ($file in $files | Where-Object { $textExtensions -contains $_.Extension.ToLowerInvariant() }) {
    $content = [IO.File]::ReadAllText($file.FullName)
    if ($content -match $literalProviderKeyPattern) {
        $violations.Add("literal-provider-key:$(Get-RelativePath $file.FullName)")
    }
}

$literalJavaCredentialPatterns = @(
    '(?i)\.(?:apiKey|secretKey|appKey|accessToken|clientSecret|privateKey)\(\s*"([^"]+)"\s*\)',
    '(?i)String\s+(?:apiKey|secretKey|appKey|accessToken|clientSecret|privateKey)\s*=\s*"([^"]+)"'
)
foreach ($file in $files | Where-Object { $_.Extension.ToLowerInvariant() -eq '.java' }) {
    $content = [IO.File]::ReadAllText($file.FullName)
    foreach ($pattern in $literalJavaCredentialPatterns) {
        foreach ($match in [regex]::Matches($content, $pattern)) {
            $value = $match.Groups[1].Value.Trim()
            if ($value -notmatch '^(test|demo|xxx|xx|changeme|replace)\b') {
                $violations.Add("literal-java-credential:$(Get-RelativePath $file.FullName)")
                break
            }
        }
    }
}

$literalConfigSecretPattern = '(?im)^\s*(?:#\s*)?(password|api[-_]?key|app[-_]?key|secret[-_]?key|client[-_]?secret|private[-_]?key|access[-_]?token|tencent-lbs-key|secret)\s*[:=]\s*([^#\r\n]+)'
foreach ($file in $files | Where-Object {
        $_.Extension.ToLowerInvariant() -in @('.yaml', '.yml', '.properties') -and
        (Get-RelativePath $_.FullName) -notmatch '(^|/)src/test/'
}) {
    $content = [IO.File]::ReadAllText($file.FullName)
    foreach ($match in [regex]::Matches($content, $literalConfigSecretPattern)) {
        $value = $match.Groups[2].Value.Trim()
        if (-not [string]::IsNullOrWhiteSpace($value) -and
            -not $value.StartsWith('${') -and
            $value -notmatch '^(test|demo|xxx|changeme)\b') {
            $violations.Add("literal-config-secret:$(Get-RelativePath $file.FullName)")
            break
        }
    }
}

$platformRoot = Join-Path $root 'apps\platform'
if (Test-Path -LiteralPath $platformRoot) {
    $requiredPlatformPaths = @(
        'sva-framework\sva-spring-boot-starter-security\src\main\java\cn\iocoder\sva\framework\security\core\LoginUser.java',
        'sva-module-system\sva-module-system-server\src\main\java\cn\iocoder\sva\module\system\service\logger\LoginLogServiceImpl.java'
    )
    foreach ($relativePath in $requiredPlatformPaths) {
        if (-not (Test-Path -LiteralPath (Join-Path $platformRoot $relativePath) -PathType Leaf)) {
            $violations.Add("missing-platform-source:$($relativePath.Replace('\', '/'))")
        }
    }

    $unsafeGatewayPaths = @(
        'sva-gateway\src\main\java\cn\iocoder\sva\gateway\filter\logging\AccessLog.java',
        'sva-gateway\src\main\java\cn\iocoder\sva\gateway\filter\logging\AccessLogFilter.java'
    )
    foreach ($relativePath in $unsafeGatewayPaths) {
        if (Test-Path -LiteralPath (Join-Path $platformRoot $relativePath) -PathType Leaf) {
            $violations.Add("unsafe-gateway-payload-logger:$($relativePath.Replace('\', '/'))")
        }
    }

    $platformIgnore = Join-Path $platformRoot '.gitignore'
    if (Test-Path -LiteralPath $platformIgnore) {
        $ignoreLines = Get-Content -LiteralPath $platformIgnore
        if ($ignoreLines | Where-Object { $_.Trim() -eq 'LOG*' }) {
            $violations.Add('unsafe-ignore-rule:apps/platform/.gitignore:LOG*')
        }
    }
}

if ($violations.Count -gt 0) {
    $violations | Sort-Object -Unique | ForEach-Object { Write-Output "BASELINE_BOUNDARY_VIOLATION=$_" }
    Write-Output "BASELINE_BOUNDARY=FAIL COUNT=$($violations.Count)"
    exit 1
}

Write-Output 'BASELINE_BOUNDARY=PASS'
exit 0

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$scriptPath = Join-Path $repositoryRoot 'tools\translation-eval\prepare_samples.ps1'
$fixtureRoot = Join-Path 'D:\Temp' ('translation-eval-samples-' + [guid]::NewGuid().ToString('N'))
$sourceRoot = Join-Path $fixtureRoot 'source'
$outputRoot = Join-Path $fixtureRoot 'output'

$names = @(
    '水痘减毒活疫苗生产用硅胶管可提取物验证VD-OT-471-2025.pdf',
    '水痘减毒活疫苗生产用硅胶管可提取物验证VD-OT-471-2025-EN.docx',
    'EU GMP 附录1 无菌产品生产-2020版（中英文对照）.docx',
    'BJ-SOP-TM-0201-05_Standard Operating Procedure for Determination of Protein Content (EP).docx',
    '蛋白质含量检验标准操作规程（EP）BJ-SOP-TM-0201-05.docx',
    'Advancing institutional compliance and continuous improvement in vaccine manufacturing.pdf'
)

try {
    New-Item -ItemType Directory -Force -Path $sourceRoot | Out-Null
    foreach ($name in $names) {
        [IO.File]::WriteAllText((Join-Path $sourceRoot $name), "fixture:$name", [Text.UTF8Encoding]::new($false))
    }

    & pwsh -NoProfile -File $scriptPath -SourceDirectory $sourceRoot -OutputRoot $outputRoot
    if ($LASTEXITCODE -ne 0) {
        throw "prepare_samples.ps1 failed with exit code $LASTEXITCODE"
    }

    $inputFiles = @(Get-ChildItem -LiteralPath (Join-Path $outputRoot '00_原始样本') -File)
    if ($inputFiles.Count -ne 6) {
        throw "Expected 6 copied input files, got $($inputFiles.Count)"
    }

    $manifestPath = Join-Path $outputRoot 'manifest.json'
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.files.Count -ne 6) {
        throw "Expected 6 manifest files, got $($manifest.files.Count)"
    }
    if ($manifest.cases.Count -ne 5) {
        throw "Expected 5 evaluation cases, got $($manifest.cases.Count)"
    }
    foreach ($file in $manifest.files) {
        if ([string]::IsNullOrWhiteSpace($file.sha256) -or $file.sha256.Length -ne 64) {
            throw "Invalid SHA256 for $($file.name)"
        }
    }

    Write-Output 'PREPARE_SAMPLES_TEST=PASS'
} finally {
    if (Test-Path -LiteralPath $fixtureRoot) {
        Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
    }
}

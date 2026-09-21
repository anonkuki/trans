$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$checker = Join-Path $repositoryRoot 'verification\check_complete_system1_baseline.ps1'
$fixtureRoot = Join-Path 'D:\Temp' ('trans-boundary-fixture-' + [guid]::NewGuid().ToString('N'))

try {
    New-Item -ItemType Directory -Force -Path $fixtureRoot | Out-Null
    Set-Content -LiteralPath (Join-Path $fixtureRoot 'README.md') -Value '# safe fixture'

    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Expected a clean fixture to pass, exit code was $LASTEXITCODE"
    }

    Set-Content -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Value 'password:'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Expected an intentionally empty local password to pass, exit code was $LASTEXITCODE"
    }
    Remove-Item -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Force

    Set-Content -LiteralPath (Join-Path $fixtureRoot 'svaai.sql') -Value 'forbidden database dump'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a raw database dump fixture to fail'
    }

    Remove-Item -LiteralPath (Join-Path $fixtureRoot 'svaai.sql') -Force
    Set-Content -LiteralPath (Join-Path $fixtureRoot '.env.local') -Value 'SECRET=value'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a local environment fixture to fail'
    }

    Remove-Item -LiteralPath (Join-Path $fixtureRoot '.env.local') -Force
    Set-Content -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Value 'password: supersecret123'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a literal configuration secret fixture to fail'
    }

    Set-Content -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Value '# secret: commented-secret-is-still-a-secret'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a commented literal configuration secret fixture to fail'
    }

    Set-Content -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Value 'secretKey: camel-case-secret'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a camel-case literal configuration secret fixture to fail'
    }

    Remove-Item -LiteralPath (Join-Path $fixtureRoot 'application.yaml') -Force
    Set-Content -LiteralPath (Join-Path $fixtureRoot 'CredentialFixture.java') -Value '.apiKey("real-looking-provider-credential")'
    & powershell -NoProfile -ExecutionPolicy Bypass -File $checker -RepositoryRoot $fixtureRoot
    if ($LASTEXITCODE -eq 0) {
        throw 'Expected a Java literal provider credential fixture to fail'
    }

    Write-Output 'BOUNDARY_CHECK_TEST=PASS'
} finally {
    if (Test-Path -LiteralPath $fixtureRoot) {
        Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
    }
}

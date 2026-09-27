param(
    [Parameter()]
    [string]$SourceDirectory = 'C:\Users\lengj\Downloads',

    [Parameter()]
    [string]$OutputRoot = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path '测试文件夹')
)

$ErrorActionPreference = 'Stop'

$sampleNames = @(
    '水痘减毒活疫苗生产用硅胶管可提取物验证VD-OT-471-2025.pdf',
    '水痘减毒活疫苗生产用硅胶管可提取物验证VD-OT-471-2025-EN.docx',
    'EU GMP 附录1 无菌产品生产-2020版（中英文对照）.docx',
    'BJ-SOP-TM-0201-05_Standard Operating Procedure for Determination of Protein Content (EP).docx',
    '蛋白质含量检验标准操作规程（EP）BJ-SOP-TM-0201-05.docx',
    'Advancing institutional compliance and continuous improvement in vaccine manufacturing.pdf'
)

$referenceNames = @(
    '水痘减毒活疫苗生产用硅胶管可提取物验证VD-OT-471-2025-EN.docx',
    'BJ-SOP-TM-0201-05_Standard Operating Procedure for Determination of Protein Content (EP).docx',
    '蛋白质含量检验标准操作规程（EP）BJ-SOP-TM-0201-05.docx'
)

$directories = @(
    '00_原始样本',
    '01_参考译文',
    '02_基线提取',
    '03_运行结果\质量测试',
    '03_运行结果\并发测试',
    '04_逐页渲染',
    '05_评估报告',
    '06_运行日志'
)

foreach ($relativeDirectory in $directories) {
    New-Item -ItemType Directory -Force -Path (Join-Path $OutputRoot $relativeDirectory) | Out-Null
}

$files = @()
foreach ($name in $sampleNames) {
    $sourcePath = Join-Path $SourceDirectory $name
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing required sample: $sourcePath"
    }

    $destinationPath = Join-Path (Join-Path $OutputRoot '00_原始样本') $name
    Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
    $item = Get-Item -LiteralPath $destinationPath
    $hash = Get-FileHash -LiteralPath $destinationPath -Algorithm SHA256
    $files += [ordered]@{
        name = $name
        sourcePath = (Resolve-Path -LiteralPath $sourcePath).Path
        copiedPath = "00_原始样本/$name"
        bytes = $item.Length
        sha256 = $hash.Hash.ToLowerInvariant()
    }
}

foreach ($name in $referenceNames) {
    $sourcePath = Join-Path $SourceDirectory $name
    $destinationPath = Join-Path (Join-Path $OutputRoot '01_参考译文') $name
    Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
}

$cases = @(
    [ordered]@{
        id = 'S1'
        source = $sampleNames[0]
        targetLanguage = 'English'
        reference = $sampleNames[1]
        requiresOcr = $true
    },
    [ordered]@{
        id = 'S2'
        source = $sampleNames[3]
        targetLanguage = 'Chinese'
        reference = $sampleNames[4]
        requiresOcr = $false
    },
    [ordered]@{
        id = 'S3'
        source = $sampleNames[4]
        targetLanguage = 'English'
        reference = $sampleNames[3]
        requiresOcr = $false
    },
    [ordered]@{
        id = 'S4'
        source = $sampleNames[2]
        targetLanguage = 'structure-dependent'
        reference = $sampleNames[2]
        requiresOcr = $false
    },
    [ordered]@{
        id = 'S5'
        source = $sampleNames[5]
        targetLanguage = 'Chinese'
        reference = $null
        requiresOcr = $true
    }
)

$manifest = [ordered]@{
    schemaVersion = 1
    createdAtUtc = [DateTime]::UtcNow.ToString('o')
    sourceDirectory = (Resolve-Path -LiteralPath $SourceDirectory).Path
    outputRoot = (Resolve-Path -LiteralPath $OutputRoot).Path
    files = $files
    cases = $cases
}

$manifestPath = Join-Path $OutputRoot 'manifest.json'
$json = $manifest | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($manifestPath, $json, [Text.UTF8Encoding]::new($false))

Write-Output "SAMPLE_MANIFEST=$manifestPath"
Write-Output "SAMPLE_COUNT=$($files.Count)"

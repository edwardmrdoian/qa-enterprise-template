$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
# An adopted project has already completed initialization; its normal tests remain applicable.
$originalPom = Get-Content (Join-Path $root 'pom.xml') -Raw
if ($originalPom -notmatch '<artifactId>qa-enterprise-template</artifactId>' -or
    $originalPom -notmatch '<groupId>com.company.qa</groupId>') {
    Write-Host 'Already initialized; template rename verification is not applicable.'
    exit 0
}
$sandbox = Join-Path ([System.IO.Path]::GetTempPath()) ("qa-template-check-" + [guid]::NewGuid())
[void][System.IO.Directory]::CreateDirectory($sandbox)
try {
    foreach ($entry in @('pom.xml', 'README.md', 'src', 'docs', 'scripts', '.mvn', 'mvnw', 'mvnw.cmd')) {
        Copy-Item (Join-Path $root $entry) $sandbox -Recurse
    }
    Push-Location $sandbox
    try {
        & java scripts/NewProject.java 'not..valid' 'invalid-name' *> $null
        if ($LASTEXITCODE -eq 0) { throw 'Initializer accepted an invalid package' }
        if ((Get-Content pom.xml -Raw) -notmatch '<groupId>com.company.qa</groupId>') {
            throw 'Invalid input changed the project'
        }
        & java scripts/NewProject.java 'org.example.automation' 'sample-qa-tests' 'Sample QA Tests'
        if ($LASTEXITCODE -ne 0) { throw 'Initialization failed' }
        $pom = Get-Content pom.xml -Raw
        if ($pom -notmatch '<name>Sample QA Tests</name>' -or $pom -notmatch '<groupId>org.example.automation</groupId>') {
            throw 'Project metadata was not renamed'
        }
        $leftovers = Get-ChildItem src -Recurse -File | Select-String -SimpleMatch 'com.company.qa'
        if ($leftovers) { throw 'Template package remains in source/resources' }
        if ($IsWindows -or $env:OS -eq 'Windows_NT') {
            & .\mvnw.cmd -B -q test '-Dtest.groups=framework & !ui'
        } else {
            & bash ./mvnw -B -q test '-Dtest.groups=framework & !ui'
        }
        if ($LASTEXITCODE -ne 0) { throw 'Renamed project did not compile and pass framework checks' }
        & java scripts/NewProject.java 'org.example.other' 'second-name' *> $null
        if ($LASTEXITCODE -eq 0) { throw 'Initializer unexpectedly allowed a second initialization' }
    } finally {
        Pop-Location
    }
} finally {
    $resolved = (Resolve-Path $sandbox).Path
    if ((Split-Path $resolved -Leaf) -notlike 'qa-template-check-*') { throw 'Unexpected cleanup path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
exit 0

$ErrorActionPreference = 'Stop'
$reports = @(Get-ChildItem (Join-Path $PSScriptRoot '..\target\surefire-reports\TEST-*.xml') -ErrorAction Stop)
if ($reports.Count -eq 0) { throw 'No Surefire reports were produced' }
$tests = 0
$skipped = 0
$failures = 0
foreach ($report in $reports) {
    [xml]$xml = Get-Content $report.FullName -Raw
    $tests += [int]$xml.testsuite.tests
    $skipped += [int]$xml.testsuite.skipped
    $failures += [int]$xml.testsuite.failures + [int]$xml.testsuite.errors
}
Write-Host "Selected tests: $tests; skipped: $skipped; failures/errors: $failures"
if ($tests -eq 0 -or $skipped -ne 0 -or $failures -ne 0) {
    throw 'The selected suite must execute tests with no skips, failures, or errors'
}

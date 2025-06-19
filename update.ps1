# PowerShell script to check latest versions of JavaParser artifacts
param(
    [string]$PomPath = "pom.xml",
    [switch]$UpdatePom = $false
)

Write-Host "🔍 Checking JavaParser artifacts on mvnrepository.com..." -ForegroundColor Cyan
Write-Host ""

# List of JavaParser artifacts to check
$artifacts = @(
    "javaparser-core",
    "javaparser-core-generators",
    "javaparser-core-generators-jbehave",
    "javaparser-core-metamodel-generator",
    "javaparser-core-serialization",
    "javaparser-symbol-solver-core",
    "javaparser-symbol-solver-model",
    "javaparser-symbol-solver-logic"
)

$groupId = "com.github.javaparser"
$versionInfo = @{}

foreach ($artifact in $artifacts) {
    Write-Host "📦 Checking $artifact..." -NoNewline

    $url = "https://mvnrepository.com/artifact/$groupId/$artifact"

    try {
        # Fetch the page
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing

        # Parse HTML to find version
        # Look for the version link pattern
        if ($response.Content -match '<a[^>]+class="vbtn[^"]*"[^>]*>([0-9]+\.[0-9]+\.[0-9]+)</a>') {
            $latestVersion = $matches[1]
            $versionInfo[$artifact] = $latestVersion
            Write-Host " ✅ " -ForegroundColor Green -NoNewline
            Write-Host $latestVersion -ForegroundColor Yellow
        }
        else {
            Write-Host " ⚠️ Could not parse version" -ForegroundColor Red
        }

        # Be nice to the server
        Start-Sleep -Milliseconds 500
    }
    catch {
        Write-Host " ❌ Error: $_" -ForegroundColor Red
    }
}

# Display summary
Write-Host "`n📊 Version Summary:" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Gray

$uniqueVersions = $versionInfo.Values | Select-Object -Unique

if ($uniqueVersions.Count -eq 1) {
    Write-Host "✅ All artifacts have the same version: " -NoNewline -ForegroundColor Green
    Write-Host $uniqueVersions[0] -ForegroundColor Yellow
} else {
    Write-Host "⚠️  Different versions found:" -ForegroundColor Yellow
    foreach ($artifact in $versionInfo.Keys | Sort-Object) {
        Write-Host "   $artifact : " -NoNewline
        Write-Host $versionInfo[$artifact] -ForegroundColor Yellow
    }
}

# Generate Maven XML
Write-Host "`n📝 Maven Dependencies XML:" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Gray

$xml = @"
<!-- JavaParser Dependencies - Latest Versions -->
<dependencies>
"@

foreach ($artifact in $versionInfo.Keys | Sort-Object) {
    $xml += @"

    <dependency>
        <groupId>$groupId</groupId>
        <artifactId>$artifact</artifactId>
        <version>$($versionInfo[$artifact])</version>
    </dependency>
"@
}

$xml += @"

</dependencies>
"@

Write-Host $xml -ForegroundColor DarkGray

# Optionally update the pom.xml
if ($UpdatePom -and (Test-Path $PomPath)) {
    Write-Host "`n🔧 Updating $PomPath..." -ForegroundColor Cyan

    # Backup original
    $backupPath = "$PomPath.backup"
    Copy-Item -Path $PomPath -Destination $backupPath
    Write-Host "💾 Backup created: $backupPath" -ForegroundColor Gray

    # Read pom.xml
    $pomContent = Get-Content $PomPath -Raw

    # Update each artifact version
    foreach ($artifact in $versionInfo.Keys) {
        $pattern = "(<artifactId>$artifact</artifactId>\s*<version>)[^<]+(</version>)"
        $replacement = "`${1}$($versionInfo[$artifact])`${2}"
        $pomContent = $pomContent -replace $pattern, $replacement
    }

    # Save updated pom.xml
    $pomContent | Set-Content $PomPath
    Write-Host "✅ Updated $PomPath with latest versions!" -ForegroundColor Green
}

# Save version info to file
$reportPath = "javaparser-versions-$(Get-Date -Format 'yyyy-MM-dd-HHmmss').txt"
$versionInfo | ConvertTo-Json -Depth 10 | Out-File $reportPath
Write-Host "`n📄 Version report saved to: $reportPath" -ForegroundColor Gray
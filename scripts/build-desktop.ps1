# MuhasebePlus — Desktop Build Script
# Kullanim: .\scripts\build-desktop.ps1
# Gereksinimler: Java 21 JDK (JAVA_HOME), Maven, Node.js

param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipJre,
    [switch]$SkipPackage
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent

Write-Host "`n=== MuhasebePlus Desktop Build ===" -ForegroundColor Cyan

# --- 1. Backend JAR ---
if (-not $SkipBackend) {
    Write-Host "`n[1/4] Backend JAR olusturuluyor..." -ForegroundColor Yellow
    Push-Location "$Root\Backend"
    & mvn clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Maven build basarisiz!" }
    Pop-Location
    Copy-Item "$Root\Backend\target\demo-0.0.1-SNAPSHOT.jar" -Destination "$Root\backend.jar" -Force
    Write-Host "    backend.jar hazir." -ForegroundColor Green
} else {
    Write-Host "[1/4] Backend atlandi." -ForegroundColor Gray
}

# --- 2. Minimal JRE (jlink) ---
if (-not $SkipJre) {
    Write-Host "`n[2/4] Minimal JRE olusturuluyor (jlink)..." -ForegroundColor Yellow

    if (-not $env:JAVA_HOME) { throw "JAVA_HOME tanimlanmamis!" }
    if (-not (Test-Path "$Root\backend.jar")) { throw "backend.jar bulunamadi. Once backend build'i calistirin." }

    # Gerekli Java modullerini tespit et
    $ModulesRaw = & "$env:JAVA_HOME\bin\jdeps" --print-module-deps --ignore-missing-deps -q --multi-release 21 "$Root\backend.jar" 2>$null
    # Spring Boot icin ek moduller ekle
    $ExtraModules = "jdk.crypto.ec,jdk.crypto.mscapi,jdk.unsupported,jdk.management,jdk.zipfs"
    $AllModules = if ($ModulesRaw) { "$ModulesRaw,$ExtraModules" } else { $ExtraModules }

    if (Test-Path "$Root\jre") { Remove-Item "$Root\jre" -Recurse -Force }

    & "$env:JAVA_HOME\bin\jlink" `
        --module-path "$env:JAVA_HOME\jmods" `
        --add-modules $AllModules `
        --strip-debug `
        --no-man-pages `
        --no-header-files `
        --compress=2 `
        --output "$Root\jre"

    if ($LASTEXITCODE -ne 0) { throw "jlink basarisiz!" }
    $JreSize = [math]::Round((Get-ChildItem "$Root\jre" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 1)
    Write-Host "    JRE hazir (${JreSize} MB)." -ForegroundColor Green
} else {
    Write-Host "[2/4] JRE atlandi." -ForegroundColor Gray
}

# --- 3. Frontend Electron build ---
if (-not $SkipFrontend) {
    Write-Host "`n[3/4] Frontend Electron build yapiliyor..." -ForegroundColor Yellow
    Push-Location "$Root\Frontend"
    & npm run build:electron
    if ($LASTEXITCODE -ne 0) { throw "Frontend build basarisiz!" }
    Pop-Location

    # dist'i Electron/frontend'e kopyala
    if (Test-Path "$Root\Electron\frontend") { Remove-Item "$Root\Electron\frontend" -Recurse -Force }
    Copy-Item "$Root\Frontend\dist" -Destination "$Root\Electron\frontend" -Recurse -Force
    Write-Host "    Frontend hazir." -ForegroundColor Green
} else {
    Write-Host "[3/4] Frontend atlandi." -ForegroundColor Gray
}

# --- 4. Electron installer ---
if (-not $SkipPackage) {
    Write-Host "`n[4/4] Electron installer olusturuluyor..." -ForegroundColor Yellow
    Push-Location "$Root\Electron"

    if (-not (Test-Path "node_modules")) {
        Write-Host "    npm install calistiriliyor..."
        & npm install --legacy-peer-deps
        if ($LASTEXITCODE -ne 0) { throw "npm install basarisiz!" }
    }

    & npm run dist:win
    if ($LASTEXITCODE -ne 0) { throw "electron-builder basarisiz!" }
    Pop-Location

    $InstallerPath = Get-ChildItem "$Root\Electron\dist\*.exe" | Select-Object -First 1
    if ($InstallerPath) {
        $SizeMB = [math]::Round($InstallerPath.Length / 1MB, 1)
        Write-Host "    Installer hazir: $($InstallerPath.Name) (${SizeMB} MB)" -ForegroundColor Green
    }
} else {
    Write-Host "[4/4] Paketleme atlandi." -ForegroundColor Gray
}

Write-Host "`n=== Build tamamlandi! ===" -ForegroundColor Cyan
Write-Host "Installer: $Root\Electron\dist\" -ForegroundColor White

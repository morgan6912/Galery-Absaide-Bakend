Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Galery Absaide - Backend Server" -ForegroundColor Cyan  
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Find Java - check Android Studio JBR first (always present if AS is installed)
$javaExe = $null

$candidates = @(
    "C:\Program Files\Android\Android Studio\jbr\bin\java.exe",
    "C:\Program Files\Android\Android Studio\jre\bin\java.exe",
    "$env:JAVA_HOME\bin\java.exe",
    "java"
)

foreach ($candidate in $candidates) {
    if ($candidate -eq "java") {
        try { 
            $null = & java -version 2>&1
            $javaExe = "java"
            Write-Host "Usando Java del sistema" -ForegroundColor Green
            break
        } catch {}
    } elseif (Test-Path $candidate) {
        $javaExe = $candidate
        Write-Host "Usando Java: $candidate" -ForegroundColor Green
        break
    }
}

if (-not $javaExe) {
    Write-Host "ERROR: No se encontro Java." -ForegroundColor Red
    Write-Host "Instala JDK 17 desde: https://adoptium.net" -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"
    exit 1
}

# Download gradle wrapper jar if missing
$wrapperJar = "gradle\wrapper\gradle-wrapper.jar"
if ((Get-Item $wrapperJar).Length -lt 10000) {
    Write-Host "Descargando Gradle Wrapper..." -ForegroundColor Yellow
    try {
        $url = "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar -UseBasicParsing
        Write-Host "Gradle Wrapper descargado!" -ForegroundColor Green
    } catch {
        Write-Host "No se pudo descargar. Intentando con Chocolatey..." -ForegroundColor Yellow
    }
}

# Run with gradlew
Write-Host ""
Write-Host "Iniciando servidor en http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host ""

& $javaExe -classpath $wrapperJar org.gradle.wrapper.GradleWrapperMain run

Read-Host "Presiona Enter para salir"

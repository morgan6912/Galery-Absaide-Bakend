@echo off
echo ========================================
echo  Galery Absaide - Backend Server
echo ========================================
echo.

REM Check if Gradle is installed
where gradle >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo Usando Gradle instalado...
    gradle run
    goto end
)

REM Check if gradlew.bat exists and works
if exist gradlew.bat (
    echo Usando gradlew.bat...
    call gradlew.bat run
    goto end
)

REM Use Java directly with Android Studio JBR
set AS_JBR="C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
if exist %AS_JBR% (
    echo Usando JBR de Android Studio...
    REM Download gradle wrapper if missing
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo Descargando Gradle Wrapper...
        powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
    )
    %AS_JBR% -jar gradle\wrapper\gradle-wrapper.jar run
    goto end
)

echo ERROR: No se encontro Java ni Gradle.
echo Por favor instala JDK 17 desde: https://adoptium.net
pause

:end

@echo off
echo Detectando IP local...

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4" ^| findstr /v "169.254"') do (
    set IP=%%a
    goto :found
)

:found
set IP=%IP: =%
echo IP detectada: %IP%

set MOBILE=C:\Users\morgan\Downloads\GaleryAbsaide (5)\GaleryAbsaide\GaleryAbsaide-Mobile

echo Actualizando ApiConfig.kt...
powershell -Command "(Get-Content '%MOBILE%\composeApp\src\commonMain\kotlin\com\absaide\gallery\data\datasource\ApiConfig.kt') -replace 'http://[0-9.]+:8080', 'http://%IP%:8080' | Set-Content '%MOBILE%\composeApp\src\commonMain\kotlin\com\absaide\gallery\data\datasource\ApiConfig.kt'"

echo Actualizando UploadRoutes.kt...
powershell -Command "(Get-Content 'src\main\kotlin\com\absaide\server\routes\UploadRoutes.kt') -replace 'http://[0-9.]+:8080', 'http://%IP%:8080' | Set-Content 'src\main\kotlin\com\absaide\server\routes\UploadRoutes.kt'"

echo.
echo IP actualizada a: %IP%
echo Recuerda hacer Build en Android Studio despues de iniciar el backend
echo.

"C:\Users\morgan\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" run
@echo off
setlocal
set "GRADLE_VERSION=8.11.1"
set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "GRADLE_CACHE=%GRADLE_USER_HOME%\manual-distributions"
set "DIST_DIR=%GRADLE_CACHE%\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%DIST_DIR%\bin\gradle.bat"
set "ZIP_FILE=%GRADLE_CACHE%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_BIN%" (
  if not exist "%GRADLE_CACHE%" mkdir "%GRADLE_CACHE%"
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'; Expand-Archive -LiteralPath '%ZIP_FILE%' -DestinationPath '%GRADLE_CACHE%' -Force"
  if errorlevel 1 exit /b 1
)

pushd "%ROOT_DIR%"
call "%GRADLE_BIN%" wrapper --gradle-version %GRADLE_VERSION% --distribution-type bin
if errorlevel 1 (
  popd
  exit /b 1
)
echo.
echo Gradle Wrapper generated. Use gradlew.bat bundleRelease to build the Play bundle.
popd
endlocal

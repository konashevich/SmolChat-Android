@echo off
setlocal ENABLEDELAYEDEXPANSION
cd /d %~dp0

set TARGET_FLAVOR=crisisAI
set DEBUG_TASK=:app:assemble%TARGET_FLAVOR%Debug
set RELEASE_TASK=:app:assemble%TARGET_FLAVOR%Release

if "%JAVA_HOME%"=="" (
  echo INFO: JAVA_HOME not set. Attempting to continue with system java.
) else (
  echo Using JAVA_HOME=%JAVA_HOME%
)

java -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java not available on PATH. Please install JDK 17 and/or set JAVA_HOME.
  exit /b 1
)

echo.
echo === Cleaning previous build outputs (optional) ===
if exist app\build (rmdir /s /q app\build)

echo.
echo === Stopping existing Gradle daemons ===
call gradlew.bat --stop >nul 2>&1

echo.
echo === Building Crisis AI Debug APK ===
call gradlew.bat %DEBUG_TASK% --stacktrace --info
if errorlevel 1 goto :error

echo.
echo === Building Crisis AI Release APK ===
call gradlew.bat %RELEASE_TASK% --stacktrace --info
if errorlevel 1 goto :error

echo.
echo === Listing outputs ===
if exist app\build\outputs\apk\%TARGET_FLAVOR%\debug (
  echo Debug APKs:
  dir /b app\build\outputs\apk\%TARGET_FLAVOR%\debug
) else (
  echo WARNING: Debug output directory missing.
)

if exist app\build\outputs\apk\%TARGET_FLAVOR%\release (
  echo Release APKs:
  dir /b app\build\outputs\apk\%TARGET_FLAVOR%\release
) else (
  echo WARNING: Release output directory missing.
)

echo.
echo SUCCESS: Build complete.
echo Debug APK:   app\build\outputs\apk\%TARGET_FLAVOR%\debug\app-%TARGET_FLAVOR%-debug.apk
echo Release APK: app\build\outputs\apk\%TARGET_FLAVOR%\release\app-%TARGET_FLAVOR%-release.apk

exit /b 0

:error
echo.
echo BUILD FAILED (exit code %errorlevel%). Review the log above for the first error.
exit /b %errorlevel%


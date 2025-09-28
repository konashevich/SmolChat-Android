@echo off
setlocal
cd /d %~dp0

echo == Forcing clean build of Crisis AI Debug variant ==
if exist app\build (echo Removing previous build folder & rmdir /s /q app\build)

echo.
call gradlew.bat --stop >nul 2>&1
call gradlew.bat --refresh-dependencies assembleCrisisAIDebug --stacktrace --info
if errorlevel 1 goto :error

echo.
echo == Listing expected APK output directory ==
dir /b app\build\outputs\apk\crisisAI\debug || echo Directory missing.

echo.
if exist app\build\outputs\apk\crisisAI\debug\app-crisisAI-debug.apk (
  echo SUCCESS: APK generated at app\build\outputs\apk\crisisAI\debug\app-crisisAI-debug.apk
) else (
  echo WARNING: APK still not found. See suggestions in docs / troubleshooting section.
)
exit /b 0

:error
echo Build failed. Scroll up for details. Exit code: %errorlevel%
exit /b %errorlevel%

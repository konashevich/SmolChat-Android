@echo off
setlocal
cd /d %~dp0

echo == Building Crisis AI Debug APK ==
call gradlew.bat assembleCrisisAIDebug || goto :error

echo.
echo Build finished.
echo APK path: app\build\outputs\apk\crisisAI\debug\app-crisisAI-debug.apk
exit /b 0

:error
echo Build failed. See Gradle output above.
exit /b 1

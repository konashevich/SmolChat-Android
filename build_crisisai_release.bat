@echo off
setlocal
cd /d %~dp0

echo == Building Crisis AI Release APK ==
IF "%RELEASE_KEYSTORE_PASSWORD%"=="" (
  echo ERROR: RELEASE_KEYSTORE_PASSWORD not set
  exit /b 1
)
IF "%RELEASE_KEYSTORE_ALIAS%"=="" (
  echo ERROR: RELEASE_KEYSTORE_ALIAS not set
  exit /b 1
)
IF "%RELEASE_KEY_PASSWORD%"=="" (
  echo ERROR: RELEASE_KEY_PASSWORD not set
  exit /b 1
)
IF NOT EXIST keystore.jks (
  echo ERROR: keystore.jks not found in project root.
  exit /b 1
)

call gradlew.bat assembleCrisisAIRelease || goto :error

echo.
echo Build finished.
echo APK path: app\build\outputs\apk\crisisAI\release\app-crisisAI-release.apk
echo AAB path (if built): app\build\outputs\bundle\crisisAIRelease\app-crisisAI-release.aab
exit /b 0

:error
echo Build failed. See Gradle output above.
exit /b 1

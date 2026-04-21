@echo off
setlocal

set "PROJECT_JAVA_HOME=H:\java\Eclipse Adoptium\jdk-21.0.8.9-hotspot"

if not exist "%PROJECT_JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JDK 21 not found: %PROJECT_JAVA_HOME%
  exit /b 1
)

set "JAVA_HOME=%PROJECT_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0mvnw.cmd" %*
exit /b %ERRORLEVEL%

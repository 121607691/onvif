@echo off
REM 运行简化的设备发现程序

echo 正在编译 SimpleDiscovery.java...
javac SimpleDiscovery.java

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败
    pause
    exit /b 1
)

echo.
echo 正在运行设备发现...
java SimpleDiscovery

echo.
echo 按任意键退出...
pause >nul
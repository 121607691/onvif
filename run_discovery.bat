@echo off
REM 编译并运行设备发现测试

echo 正在编译 DiscoveryTest.java...
javac -cp "onvif-java/target/classes;onvif-ws-client/target/classes" DiscoveryTest.java

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败
    pause
    exit /b 1
)

echo 正在运行设备发现...
java -cp ".;onvif-java/target/classes;onvif-ws-client/target/classes" DiscoveryTest

pause
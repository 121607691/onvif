# ONVIF 设备发现指南

## 概述

本项目提供了两种方式来发现网络中的 ONVIF 设备（IPC 摄像头等）：

1. **简化版发现程序** (`SimpleDiscovery.java`) - 基于原始 WS-Discovery 协议实现
2. **完整版发现程序** (`DiscoveryTest.java`) - 使用 onvif 库的发现功能

## 快速开始

### 1. 使用简化版发现（推荐）

```bash
# 编译
javac SimpleDiscovery.java

# 运行
java SimpleDiscovery
```

### 2. 使用完整版发现

```bash
# 编译运行（需要先构建 onvif 库）
javac -cp "onvif-java/target/classes" DiscoveryTest.java
java -cp ".;onvif-java/target/classes" DiscoveryTest
```

## 运行结果示例

### 发现到设备的情况：
```
信息: 发现到 2 个 ONVIF 设备:
  - http://192.168.1.101/onvif/device_service
    设备信息: 设备服务响应正常
  - http://192.168.1.102/onvif/device_service
    设备信息: 响应代码: 200
```

### 未发现设备的情况：
```
信息: 没有发现到 ONVIF 设备
信息: 请确保：
1. IPC 设备已连接到网络
2. IPC 设备已启用 ONVIF 服务
3. 设备和电脑在同一网络段
4. 防火墙没有阻止 3702 端口
```

## 故障排除

### 如果没有发现到设备，请检查：

1. **网络连接**
   - IPC 设备是否已开机并连接到网络
   - 设备和电脑是否在同一网络段
   - 可以 `ping` 设备的 IP 地址确认连通性

2. **ONVIF 服务**
   - IPC 设备是否已启用 ONVIF 服务
   - 有些设备需要手动开启 ONVIF 功能

3. **防火墙**
   - 检查 Windows 防火墙是否阻止了 3702 端口
   - 检查其他安全软件
   - 临时关闭防火墙测试

4. **网络配置**
   - 某些设备需要配置用户名和密码才能被发现的
   - 检查设备是否配置了静态 IP 或 DHCP

### 手动测试设备是否在线

```bash
# 使用 telnet 测试设备服务端口
telnet <设备IP> 80

# 或者使用 curl
curl http://<设备IP>/onvif/device_service
```

## 获取更详细的设备信息

发现设备后，可以使用以下方法获取更多信息：

```java
// 创建 OnvifDevice 实例（需要已构建的 onvif 库）
de.onvif.soap.OnvifDevice device = new de.onvif.soap.OnvifDevice(deviceUrl);

// 获取设备信息
OnvifDeviceInfoSnapshot info = device.fetchDeviceInfo();
System.out.println("制造商: " + info.manufacturer);
System.out.println("型号: " + info.model);
System.out.println("固件版本: " + info.firmwareVersion);
```

## 常见 ONVIF 设备地址格式

- HikVision: `http://<IP>/onvif/device_service`
- Dahua: `http://<IP>/onvif/device_service`
- Axis: `http://<IP>/axis-media/media.amp`
- Canon: `http://<IP>/onvif/device_service`

## 下一步

1. 记录发现的设备地址
2. 测试连接到设备服务
3. 使用 onvif-consumer-template 中的适配器进行设备控制
4. 实现设备管理和媒体流功能

## 相关资源

- [ONVIF 官方网站](https://www.onvif.org/)
- [ONVIF 规范文档](https://www.onvif.org/Specs/)
- [WS-Discovery 规范](https://schemas.xmlsoap.org/ws/2004/08/discovery/)
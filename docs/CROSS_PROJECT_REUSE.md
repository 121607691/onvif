# ONVIF 跨项目复用指南

本文说明如何在**其他 Maven 项目**中依赖本仓库已封装的 ONVIF 能力（`onvif-java` + `onvif-ws-client`），以及如何发布构件、隔离业务边界与做最小联调验证。

## 1. 构件与版本


| 坐标                          | 说明                                                       |
| --------------------------- | -------------------------------------------------------- |
| `org.onvif:onvif-java`      | 对外主依赖：封装 `OnvifDevice`、发现等                               |
| `org.onvif:onvif-ws-client` | 由 `onvif-java` **传递依赖**，一般不必单独声明                         |
| 当前版本                        | 根 POM 中为 `1.0-SNAPSHOT`；对内发版建议改为 `1.0.0-internal` 等可追踪版本 |


**主 JAR 与 fat JAR**

- **默认主构件**：瘦 `onvif-java-*.jar`，适合作为 Maven 依赖（传递引入 CXF、WS 客户端等）。
- **附加构件**：`*-with-dependencies.jar`（classifier `with-dependencies`），适合 `java -jar` 或不便使用 Maven 依赖的场景。

## 2. 复用路径选型


| 方案            | 适用场景        | 要点                                                                                                           |
| ------------- | ----------- | ------------------------------------------------------------------------------------------------------------ |
| **A. 私服（推荐）** | 团队/CI 统一构建  | Nexus / Artifactory / GitHub Packages；`deploy` 到私服后在下游 `pom.xml` 配置 `<repository>`                           |
| **B. 本地仓库过渡** | 单机/临时联调     | `mvn install` 安装到 `~/.m2/repository`，或 `deploy` 到本仓库 `dist/mvn-repo`（见下文）                                    |
| **C. 网关服务**   | 非 Java 或多语言 | 自建 HTTP API，由 Java 服务调用 `OnvifDevice`；领域模型可与 [src/types/onvif-client.ts](../../src/types/onvif-client.ts) 对齐 |


## 3. 发布到统一目录 `dist/mvn-repo`（方案 B 的目录化产物）

根 [pom.xml](../pom.xml) 中 `distributionManagement` 指向：

`file://${maven.multiModuleProjectDirectory}/dist/mvn-repo`

在 `onvif` 目录执行（Windows 可用 [mvnw21.cmd](../mvnw21.cmd) 指定 JDK 21）：

```bash
mvnw21.cmd clean deploy -DskipTests
```

生成目录结构可被**复制到内网私服**或作为 `file://` 仓库路径在下游引用。

> **注意**：`deploy` 使用的 `<server>` id 为 `onvif-local-repo`。若需鉴权，在 `~/.m2/settings.xml` 中配置同名 `<server>`（file 协议通常无需用户名密码）。

## 4. 安装到本机 `~/.m2`（开发常用）

```bash
cd onvif
mvnw21.cmd clean install -DskipTests
```

下游只需依赖：

```xml
<dependency>
  <groupId>org.onvif</groupId>
  <artifactId>onvif-java</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## 5. 下游最小接入与适配层

### 5.1 直接使用库 API（最快）

核心类：

- `de.onvif.soap.OnvifDevice`：连接设备、取流地址/快照、Device/Media/PTZ 等（懒加载）。
- `de.onvif.discovery.OnvifDiscovery`：局域网发现 ONVIF 设备 URL。

示例构造：

```java
OnvifDevice device = new OnvifDevice("192.168.1.100:80", "user", "pass");
```

### 5.2 推荐：业务只依赖自有接口（隔离升级）

参考模板工程：[onvif-consumer-template](../../onvif-consumer-template/)：

- `OnvifClientPort`：业务侧接口（不暴露 `org.onvif.ver10.*`）。
- `OnvifDeviceAdapter`：将调用委托给 `OnvifDevice`。

### 5.3 运行发现/工具类 fat JAR

```bash
java -jar onvif-java/target/onvif-java-1.0-SNAPSHOT-with-dependencies.jar
```

（主类仍为 `de.onvif.discovery.DeviceDiscovery`。）

## 6. 集成验证清单（最小基线）

在每台**代表机型**上勾选（建议保留 Excel/表格记录厂商、固件版本）。


| 序号  | 检查项      | 通过标准                                                              |
| --- | -------- | ----------------------------------------------------------------- |
| 1   | 连接与能力    | `new OnvifDevice(host, user, pass)` 无异常，或异常可明确归因（网络/账号）           |
| 2   | 设备信息     | `getDeviceInfo()` 返回厂商/型号等非空                                      |
| 3   | Profile  | `getMedia().getProfiles()` 至少 1 个 profile（无媒体能力设备可标 N/A）          |
| 4   | 主码流 RTSP | `getStreamUri()` 或指定 token 的 `getStreamUri(token)` 返回非空 `rtsp://` |
| 5   | 快照       | `getSnapshotUri()` 返回 URL；若需鉴权，验证业务侧下载方式（勿把密码写进日志）                |
| 6   | PTZ（如有）  | `getPtz()` 可用且与硬件一致                                               |
| 7   | 发现（可选）   | `OnvifDiscovery.discoverOnvifURLs()` 在目标网段能发现设备                   |
| 8   | 升级回归     | 升级 `onvif-java` 后重复 1–5                                           |


## 7. 版本升级策略（建议）

- **补丁**：仅修复兼容性问题 → 回归清单 1–5。
- **小版本**：CXF/ONVIF 桩或依赖升级 → 全量清单 + 重点厂商抽测。
- **大版本**：API 变更 → 适配层编译失败即驱动业务改造，并更新 [src/types/onvif-client.ts](../../src/types/onvif-client.ts)（若存在 API 网关）。

## 8. 风险摘要

- 厂商 ONVIF 实现差异大，需「能力探测 + 降级」。
- 依赖链较大，避免在下游再二次 fat 打包同一套 CXF（除非明确需要 uber jar）。
- 凭据禁止写入日志；快照 URL 下载需按设备要求带认证头或 Session。


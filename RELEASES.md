# 发布与变更记录（Releases）

本文档汇总本仓库的**发布说明与重要变更**。当前 Maven 坐标为 `org.onvif:onvif:1.0-SNAPSHOT`，子模块版本与父 POM 一致；仓库尚未使用 Git 标签做版本发布，以下以**当前工作副本能力**与**近期提交**为主进行记录。

## 项目定位（摘自 README 并校对当前结构）

- **目标**：在 Java 中对接 ONVIF（网络视频设备互通标准），把与 SOAP/WS 的繁琐交互交给 **Apache CXF**，上层尽量保持简单、可维护、可扩展。
- **与 milg0/onvif-java-lib 的关系**：README 说明本项目意在改进该库的方向（Maven 化、模块化、由 CXF 生成桩代码等）。
- **当前远程**：`https://github.com/121607691/onvif.git`（以你本地 `.git/config` 为准）。

## 模块结构（与 README 的差异说明）

README 中仍写 `onvif-ws-tests`；**当前仓库实际为**：


| 模块                | 说明                                                                  |
| ----------------- | ------------------------------------------------------------------- |
| `onvif-ws-client` | WSDL 资源、`jax-ws-catalog.xml`、由 CXF 等工具链生成的 Web Service 客户端桩代码及相关依赖。 |
| `onvif-java`      | 面向使用者的封装（如 `de.onvif.soap.OnvifDevice`），依赖 `onvif-ws-client`。       |


根目录另有 `settings.gradle`，表示也支持 Gradle 多模块（`onvif-java`、`onvif-ws-client`），与 Maven 多模块并存。

## 技术栈（根 `pom.xml`）

- **Java**：21（`release` / source / target）
- **Apache CXF**：4.1.0（`cxf.version`）
- **代码检查**：Checkstyle + Google Checks（构建时执行 `check`）
- **构件发布**：`distributionManagement` 指向仓库根下 `dist/mvn-repo`（多模块统一输出，便于复制到私服或共享；详见 [docs/CROSS_PROJECT_REUSE.md](docs/CROSS_PROJECT_REUSE.md)）
- **onvif-java 构件**：主 JAR 为瘦包（Maven 依赖用）；附加 **`with-dependencies` classifier** 的 fat JAR 用于命令行/工具场景

## 维护者文档要点（README）

- **重建 WS 桩**：下载 ONVIF WSDL 到资源目录（文件名建议带规格版本后缀）、必要时更新 `de.onvif.utils.WSDLLocations`、`jax-ws-catalog.xml`、清理旧生成代码后，在对应模块启用 CXF codegen 并执行 `mvn clean install`。新服务接入可参考 `OnvifDevice.init()`。
- **已知 TODO**：更完整示例（事件订阅、I/O 等）、规范版本标签统一、WS-Discovery 示例修复、简单测试 UI、offline（local XML）模式修复等。

---

## 变更摘要（基于近期 Git 历史，无版本号标签）

以下为当前分支上可见的**近期主要提交方向**，便于跟踪能力演进（非严格语义化版本发布说明）：

- **服务初始化优化**：ONVIF 相关服务**懒加载与缓存**，降低内存占用（PR #40 相关分支合并）。
- **Pull Point / 事件**：增加 `PullPointTest` 等示例；增强断线恢复与错误恢复（PR #38 等）。
- **依赖与构建**：CXF 升级至 4.1.0、Checkstyle 插件升级、依赖整理、Maven Wrapper、WSDL 路径调整。
- **互操作性**：探测消息（Probe）等调整以改善部分厂商设备（如 Reolink 门铃）的发现响应；`URL` 构造向 `URI` 迁移等修正。
- **媒体 WSDL**：`media_2.6.wsdl` 曾遇 Gradle/wsdl2java 构建问题时有回退与单独升级计划说明（以提交说明为准）。

---

## 后续若正式发版建议

1. 在根 `pom.xml`（及子模块）将 `1.0-SNAPSHOT` 改为如 `1.0.0`。
2. 执行 `mvn -q -DskipTests package`（或完整测试）后打 Git 标签，例如 `v1.0.0`。
3. 在本文件顶部增加 **YYYY-MM-DD — vX.Y.Z** 一节，列出相对上一标签的变更与迁移注意点。

---

*文档生成日期：2026-03-26；若与 README 或 pom 不一致，以仓库内实际文件为准。*
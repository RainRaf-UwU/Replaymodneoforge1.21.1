# Replaymodneoforge1.21.1

ReplayMod 的 Minecraft 1.21.1 NeoForge 移植版本，包含录制、回放、自由相机、时间轴、相机路径、关键帧和视频渲染功能的源码，以及校订后的简体中文翻译。

## 目标环境

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.209；模组依赖范围为 `[21.1.209,21.2)` |
| Java | JDK 21 |
| Yarn mappings | 1.21.1+build.3 |
| NeoForge mappings patch | Architectury 1.21+build.6 |
| Gradle | 9.4.1，通过仓库自带 wrapper 使用 |

原 21.1.215 构建已调整为面向 21.1.209 重新编译，以兼容安装了 NeoForge 21.1.209 的游戏实例。21.1.209 以外的 NeoForge 补丁版本未逐一验证。

## 获取源码

```bash
git clone https://github.com/RainRaf-UwU/Replaymodneoforge1.21.1.git
cd Replaymodneoforge1.21.1
```

jGui、ReplayStudio 和语言资源已直接收录在仓库中，包含本移植所需的修改；无需额外初始化 Git 子模块。上游来源和基准提交见 [docs/UPSTREAM_SOURCES.md](docs/UPSTREAM_SOURCES.md)。

## 构建

安装 JDK 21，并确保 `JAVA_HOME` 指向它。

Windows：

```powershell
.\gradlew.bat build
```

Linux / macOS：

```bash
./gradlew build
```

默认构建 Minecraft 1.21.1 NeoForge 目标，成品会复制到根目录 `build/libs/`，也可在 `versions/1.21.1-neoforge/build/libs/` 找到。只编译并打包目标版本可以运行：

```powershell
.\gradlew.bat :1.21.1-neoforge:bundleJar :bundleJar
```

初次构建需要联网下载 Minecraft、NeoForge 和其他依赖。项目使用 Essential Loom 多版本预处理器，保留共享源码基线和历史版本转换节点；默认不构建完整历史版本矩阵。

## 启动开发客户端

```powershell
.\gradlew.bat :runClient
```

使用成品时，将 ReplayMod JAR 放入 Minecraft 1.21.1 NeoForge 实例的 `mods` 目录；更新时替换旧 JAR。

## 简体中文

游戏语言选择“简体中文”。翻译包含 273 条原始界面文本和 18 条现代键位名称，同时保留并同步旧格式语言文件：

- `src/main/resources/assets/replaymod/lang/zh_cn.json`
- `src/main/resources/assets/replaymod/lang/zh_CN.lang`

## 源码结构与记录

- `src/`：ReplayMod 共享源码、资源和测试。
- `versions/1.21.1-neoforge/`：NeoForge 平台适配、Mixin、模组元数据和构建版本配置。
- `jGui/`：已适配的 GUI 库源码。
- `libs/ReplayStudio/`：ReplayStudio 源码和构建入口。
- [PORTING_1.21.1.md](PORTING_1.21.1.md)：1.21.1 移植记录与此前 21.1.215 的运行验证范围。
- [NEOFORGE_21.1.209.md](NEOFORGE_21.1.209.md)：21.1.209 兼容修订与编译记录；此修订未重新运行游戏测试。

记录中的本地构建日志、录像、截图、缓存和下载的成品不随源码上传。`.porting/integration/java/` 保留可选开发验证驱动的源码，不会打包进发布模组；其本机输出路径需要开发者按环境调整。

## 许可证与上游

保留 ReplayMod 及依赖源码的版权声明和许可证。ReplayMod 使用 GPL-3.0-or-later，详见 [LICENSE.md](LICENSE.md)；jGui、ReplayStudio 和语言资源的许可证保留在各自目录中。原版构建说明留存在 [docs/README.upstream.md](docs/README.upstream.md)。

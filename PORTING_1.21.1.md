# ReplayModNeoForge：Minecraft 1.21.1 移植与验证记录

2026-10-01 后续修订：当前项目改为面向 NeoForge 21.1.209 构建，保留简体中文。新成品与本次编译范围见 [NEOFORGE_21.1.209.md](NEOFORGE_21.1.209.md)。下文保留 2026-09-30 在 21.1.215 上完成的移植与运行验证记录。

更新日期：2026-09-30。开发客户端与成品 JAR 的核心流程均已通过，包括真实录制/保存/回放、连续拖动时间轴、自由相机、关键帧、Camera Path、PNG/MP4 输出和 Mixin 全目标审计。未覆盖范围在文末单独列出。

## 最终版本与构建结构

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.215 |
| Java toolchain | 21；本机验证使用 Temurin 21.0.12.1 |
| Yarn mappings | 1.21.1+build.3，v2 |
| NeoForge Yarn patch | Architectury 1.21+build.6 |
| Essential Loom | 1.15.48，保留 |
| Essential multi-version toolkit | 0.7.0-alpha.4，保留 |
| Gradle wrapper | 9.4.1，保留 |
| Kotlin / Shadow | 2.3.20 / 9.4.1，保留 |
| Mixin | 0.8.7 |
| MixinExtras | 编译注解 0.3.6；NeoForge 提供运行时 0.5.0 |

项目使用 Essential Loom 和 ReplayMod 多版本预处理器，没有使用 Parchment、NeoGradle、ModDevGradle 或版本目录 `gradle/libs.versions.toml`。目标源码经 Yarn 编译，发布 JAR 转换到 NeoForge 的 Mojang 命名空间。ReplayStudio 使用 JDK 21 编译，但保留其 Java 8 库基线。

新增 `1.21.1` → `1.21.1-neoforge` 转换节点，保留 1.16.4 共享源码基线及历史节点。默认 build/client 任务选择 NeoForge 1.21.1；完整历史矩阵可用 `-Preplaymod.allVersions=true` 显式启用，其他版本未在本次移植中重新验收。

## 原始目录与文件清单

原始目录是源码压缩包，没有顶层 Git 仓库；未执行 reset/clean。既有源码修改先保存到 `.porting/originals`。源码包中的空子模块按上游 Git 树的精确提交恢复：

- jGui：`082c6357e4cff95ef5b0b0295975c3290580a499`
- ReplayStudio：`ca00f8101bec034efe77bdb938522aa3b800cf50`
- Translations：`ed16d95cd373d4b5ddd257ea4b30790d2fcff353`

源码包缺少 Git 历史时使用 `2.6.26-source` 版本。文件清单见 [PORTING_FILES_1.21.1.txt](PORTING_FILES_1.21.1.txt)。主要修改包括构建脚本、目标版本配置、NeoForge 后端、共享录制/回放/渲染分支、Mixin 配置、Camera/FOV/关键帧，以及 jGui 绘制和输入适配。

## 1.21 → 1.21.1 的实际变化

已下载并核对官方 Mojang 客户端 mappings 的 SHA-1，对比完整命名成员签名以及 Minecraft、GameRenderer、LevelRenderer、Camera、EntityRenderer、LivingEntityRenderer、ClientLevel、LocalPlayer、ClientPacketListener、Connection、RenderTarget、Window、Gui、Screen、MouseHandler、KeyboardHandler、SoundManager、ParticleEngine、DeltaTracker 等重点类。

这些重点类的 vanilla 命名成员签名没有变化。官方映射中变化主要集中在命令选择器上下文/解析方法，以及 BlockEntity 的方块状态验证辅助方法；项目没有直接依赖这些变更点。两个版本的协议均为 767，资源包格式均为 34，数据包格式均为 48，Java 均为 21。证据保存在 `.porting/validation/api-comparison.txt` 和 `api-all-changes.json`。已检查目标生成的 343 个 ReplayMod 与 130 个 jGui Java 文件；发布配置的 95 个静态 Mixin/accessor 类均存在。

实际移植故障主要来自 NeoForge 的补丁、平台分支与映射，而非 vanilla 全面改名：

- NeoForge 的 `getInventory`、`renderHotbar` 与 vanilla Yarn 名称发生冲突，使用官方 Architectury NeoForge Yarn patch。
- NeoForge 将屏幕渲染包装到 `ClientHooks.drawScreen`，GUI Mixin 改为该调用点。
- NeoForge 的粒子循环位于新增五参数 `ParticleEngine.render`，三参数方法成为包装器；生产命名中两个重载同名，必须精确选择签名。
- NeoForge 重建 HUD 层和字符输入回调，改用对应 GUI/输入事件保留行为。
- NeoForge 模块加载器要求开发环境正确组合模组资源根；发布包必须避免和 Minecraft 自带库形成 split package。
- 现代 Identifier/ResourceLocation 工厂已由版本帮助方法提供；目标生成源码没有直接构造 ResourceLocation/Identifier 的旧写法。

## NeoForge 与 Mixin 修复

- `ReplayModBackend` 使用客户端 `@Mod`、注入的 `IEventBus`/`ModContainer`，在 `RegisterKeyMappingsEvent` 初始化模块和键位。
- 使用 `AddPackFindersEvent` 提供翻译转换和开发 jGui 资源，包格式 34；配置界面通过 `IConfigScreenFactory` 注册。
- 在 configuration/play 阶段注册 restriction payload codec，保留 ReplaySender 对限制消息的实际处理。
- 使用 Screen accessor 将 Replay Viewer 按钮加入绘制与输入集合。
- jGui `MixinGameRenderer` 严格捕获 `ClientHooks.drawScreen` 的 GuiGraphics 参数，并在调用后发送回调。
- jGui `MixinKeyboardListener` 通过严格构造注入注册 NeoForge 屏幕键盘/字符输入事件。
- jGui `Mixin_RenderHudCallback` 通过严格构造注入注册 `RenderGuiEvent.Post`，保留 F1 可见性。
- `MixinParticleManager` 使用 `@Desc` 和参数 class literals 精确选中五参数 render；类参数随 JAR 重映射，粒子调用仍通过生产 refmap 转换。保留全向粒子朝向，并以 `Quaternionf.set(original)` 恢复摄像机旋转。
- 修正现代 NeoForge 录制、实体渲染、Camera、GUI 和 world-border 分支，避免落入旧 Forge API。
- world-border 移动内部类采用正确 binary name，开发与生产 Mixin audit 都已强制加载验证。
- 新增严格 `MixinCameraFov`，在 replay camera 的 GameRenderer FOV 返回处应用关键帧值。
- Iris/OptiFine 兼容 Mixin 仅在拥有目标类的可选模组存在时加入配置；实现仍保留。无相应模组时不出现 ClassNotFound 警告。
- 开发客户端使用 Yarn 注解直接匹配；仅开发 run 配置关闭 refmap。成品 JAR 保留并实际测试生产 refmap。
- 必需 Mixin 配置保持 required=true，所有目标配置均为 defaultRequire=1；可选 shader 兼容配置保留原有 required=false。未添加 require=0 规避注入失败。

## Replay、相机、关键帧、GUI 与渲染

1. 修复 ReplayFilesService 启动扫描与早期录制重叠的保存竞态：跳过被活动 Replay 持有的输出、`.mcpr.tmp` 与缓存目录，防止录制数据被移动后丢失。已在录制中主动重跑 initialScan 验证。
2. FullReplaySender 在 ReplayStudio 消费完 enabled_packs_data 注册表补充后，阻止该内部数据进入 NeoForge 的服务器 payload decoder；Replay 文件和 Studio 中的注册表数据保留。修复被跳过 packet 的 ByteBuf 释放。
3. 保留现代 ReplayTimer、网络包捕获/序列化、客户端世界重建、自由相机控制和渲染流程。
4. 新增目标 FOV property，支持编辑、插值、序列化、关键帧移动、undo/redo、旧路径省略 FOV，以及路径结束后恢复自由相机 FOV。保留 Roll、Position、Time 和 Camera Path。
5. 修复 HUD 之后的 jGui 深度、层次矩阵和 GuiGraphics flush；渐变顶点应用当前矩阵，解决关键帧弹窗文字透出、遮罩顺序错误。
6. 为 NeoForge 1.21.1 的回放时间轴补充左键拖动 Seek，Marker 点击/拖动维持独立分支。倒回重建世界会关闭输入 Screen，Seek 后恢复 Overlay 输入，保证同一次拖动可以继续。已通过实际 Screen 点击、向后拖动、再向前拖动的回归。
7. ReplayStudio 私有 Guava 的 `com.google.thirdparty` 一并重定位；发布包排除 Minecraft 已提供的 `javax.annotation`，消除生产模块 split package。

未删除录制、回放、网络、Camera Path、渲染或 Mixin 功能；未加入空实现或吞异常逻辑。

## 已执行的实际游戏测试

测试由 `.porting/integration` 的可选驱动在真实客户端内执行，使用正常单人服务器、网络、世界和 OpenGL 渲染，并输出实际 framebuffer 截图。驱动编译到独立目录，不包含在发布 JAR 中。原 Windows 窗口捕获在恢复尝试后仍失败，因此没有宣称完成手动键鼠测试。

| 检查 | 当前结果 |
| --- | --- |
| Gradle build | BUILD SUCCESSFUL，build-external-23.log，4m44s |
| 单元测试 | 45 通过，0 失败/错误/跳过：41 原有时间轴测试 + 4 FOV 测试 |
| runClient | BUILD SUCCESSFUL，integration-15.log，1m56s；含实际 Screen 输入、Mixin audit 与 PNG/MP4 |
| 发布 JAR | Mojang 命名空间、生产 refmap 启用、无源码模组根/Unprotect，最终 production-launch-08 客户端 exit 0 |
| 录制 | 真实单人世界约 35 秒，移动/跳跃/放块/破坏/物品切换/牛实体交互通过 |
| 保存与重开 | 自录 .mcpr 保存并重开；ZIP CRC、packet 长度边界与时间戳单调性通过 |
| 回放 | 区块、实体、已录玩家模型加载通过；截图目视确认正常世界 |
| 暂停/继续 | 暂停时间戳保持；实际 Overlay 播放/暂停按钮通过 |
| 倍速 | 0.5x/1x/2x/4x 的实际时间戳增量符合倍速 |
| Seek | 20s → 5s → 25s → 2s，目标时间准确，区块保留 |
| GUI 时间轴拖动 | 实际 Screen 输入：点击到 25306ms，向后拖到 5384ms，再向前拖到 21672ms；区块与输入 Screen 保留 |
| 自由相机 | 暂停时 W/S/A/D/Space/Shift 都产生位移；鼠标视角处理改变 yaw/pitch；滚轮控制器执行；不切回录制玩家 |
| Camera Path | 3 Position + 2 Time 关键帧，新增/删除/移动/播放通过 |
| FOV/Roll | 路径中插值与播放结束恢复通过；关键帧编辑器保存 FOV 通过 |
| GUI | Replay Viewer、Replay Settings、Keyframe Editor、Rendering GUI、Replay Overlay 实际显示并检查截图 |
| PNG | 320×180、5fps、4 秒、20 帧，实际 VideoRenderer 输出通过 |
| MP4 | 同一 Camera Path 经真实 FFmpeg/libx264 编码；4 秒/20 帧 H.264，重新完整解码 exit 0 |
| Mixin 全目标 audit | 开发/生产均通过；强制加载剩余 world-border 与 player-public-key 内部类 |

最后一轮完整录制的证据：`production-client-05-success.log`。回放文件为 `E:/NeoForgeReplay-porting/integration/recordings-1790776940783/2026_09_30_22_02_34.mcpr`，共 2,976 packets，末包时间 36,209ms，协议 767、MCPR 格式 14。该单人回放的 metadata players 列表为空是原有自身玩家记录方式；已通过实际客户端确认自身玩家模型存在。

最新通过的启动日志没有 ERROR/FATAL、InvalidMixin、InjectionError、NoSuchMethod、NoSuchField 或 ClassNotFound。日志仍包含 vanilla 资源/着色器提示及 NeoForge 回放配置重新同步的 WARN；未隐藏这些日志。

## 构建、产物与本机验证环境

在项目根目录设置 Java 21 后，可使用正常任务：

```powershell
$env:JAVA_HOME='C:/path/to/jdk-21'
.\gradlew.bat build
.\gradlew.bat :runClient
```

产物位于 [build/libs/replaymod-1.21.1-neoforge-2.6.26-source.jar](build/libs/replaymod-1.21.1-neoforge-2.6.26-source.jar)，14,747,604 bytes。发布包包含 Java 21 主类、jGui、ReplayStudio、翻译及生产 refmap；不包含测试驱动。ZIP CRC 通过，且与最后一次生产验证实际加载的 JAR 逐字节一致。

SHA-256：`f9af85a378f0ae00f4858dc9e7b510a40c9ba1f16a2765109032b0bbd6a93e2c`，另存于同目录 `.jar.sha256` 文件。

本项目内保留最终证据：[build 日志](.porting/validation/build-final.log)、[runClient 日志](.porting/validation/runClient-final.log)、[成品 JAR 回归日志](.porting/validation/production-client.log)、[完整录制日志](.porting/validation/production-client-05-success.log) 和 [结构化结果](.porting/validation/validation-results.json)。最终 MP4 为 `E:/NeoForgeReplay-porting/integration/render-video-1790778822601.mp4`，347,904 bytes，完整重新解码为 20 帧/4 秒，exit 0。

本机 C 盘空间不足，构建/缓存/测试输出放在 `E:/NeoForgeReplay-porting`。本机 JVM HTTPS 下载曾反复中断，验证时使用 `.porting/maven-transport.init.gradle` 的校验缓存桥接和本机代理；项目公开仓库 URL 保留。这些是环境辅助文件，不是运行模组所需依赖。

实际验证构建使用 JDK 21、`--project-cache-dir E:/NeoForgeReplay-porting/project-cache`、`.porting/external-build.init.gradle` 和上述 transport init。完整 build/client 日志保存于 E 盘。可选测试参数为 `-Preplaymod.integration=true`；测试生成唯一名称的平坦世界和回放，录制设置只在内存启用。

本机 FFmpeg 验证工具位于 `E:/NeoForgeReplay-porting/ffmpeg.exe`；测试显式指定此路径，没有写入系统 PATH 或新增模组依赖。正常 GUI 导出仍需要可用的 FFmpeg 路径。

## 尚未覆盖与兼容性风险

- 没有通过独立零售启动器新安装实例进行手动验收。生产验证使用真实 Mojang 命名游戏、FML dev launch 与成品 JAR，不依赖源码根或 Unprotect；此范围比普通 Yarn runClient 更接近发布运行环境。
- 未完成物理键盘/鼠标输入和拖放的手动测试；相机与 Screen 输入通过客户端真实输入处理方法验证。
- 未装 Iris/Sodium/OptiFine 或其他大型整合包进行组合测试；可选兼容代码保留，兼容性尚未确认。
- 未测试远程模组服务器、跨维度录制、长时间/大回放、所有 replay restrictions、EXR/Blend/360°/立体/ODS 输出、音频导出和每种 FFmpeg preset。
- 已测试 NeoForge 21.1.215；元数据允许同分支更新，但不能据此声称所有后续 21.1.x 或其他显卡驱动均已验证。
- 截至已通过的流程，未发现阻塞核心录制/保存/回放/时间轴拖动/相机路径/普通视频输出的未解决故障。

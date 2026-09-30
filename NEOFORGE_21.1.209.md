# NeoForge 21.1.209 兼容修订

2026-10-01：用户环境为 Minecraft 1.21.1 / NeoForge 21.1.209，原成品声明最低 NeoForge 21.1.215，导致加载器在启动前拒绝加载。

ReplayMod 和 jGui 的 `versions/1.21.1-neoforge/gradle.properties` 均改为依赖 NeoForge 21.1.209。目标 `META-INF/neoforge.mods.toml` 的版本范围改为 `[21.1.209,21.2)`。Minecraft 1.21.1、Java 21、mappings 和简体中文翻译保持原有配置。

使用 21.1.209 的实际 Minecraft 补丁、映射和 FML 4.0.41 重新编译、重映射并打包，`BUILD SUCCESSFUL`，耗时 7 分 58 秒。执行的是 `:1.21.1-neoforge:bundleJar :bundleJar`；按用户要求未重新运行单元测试或游戏测试。

对比两版发布依赖，录制、回放、相机、渲染和 GUI 涉及的 21 个重点 Minecraft 类字节一致。重新构建后的成品与此前中文成品相比，唯一内容变化为 `META-INF/neoforge.mods.toml`；所有类、Mixin 配置、refmap 和语言资源内容保持一致。简体中文 JSON 包含 273 条原始文本与 18 条现代键位名称，旧格式语言文件同步保留。

成品：[replaymod-1.21.1-neoforge-2.6.26-source-neo21.1.209-zh_cn.jar](build/libs/replaymod-1.21.1-neoforge-2.6.26-source-neo21.1.209-zh_cn.jar)，14,753,032 bytes。

SHA-256：`063484cfd3ef7a2e639aa021a518c090b173e490a9711475fea3c59d669e7fc4`。

构建日志：[bundle-neo209.log](.porting/validation/bundle-neo209.log)；静态差异与产物记录：[neo209-compatibility.json](.porting/validation/neo209-compatibility.json)。之前 21.1.215 的运行验证记录保留在 `PORTING_1.21.1.md`，不作为本次 21.1.209 的运行测试结论。

安装时将游戏 `mods` 目录中的旧 ReplayMod JAR 替换为本次成品。

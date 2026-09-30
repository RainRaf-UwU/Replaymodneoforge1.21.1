# 上游源码来源

本仓库直接收录以下源码及本次移植修改，避免克隆时缺失原有 Git 子模块。原始版权声明和许可证保持不变。

| 目录 | 上游 | 基准提交 |
| --- | --- | --- |
| `jGui/` | https://github.com/mattHmm/jGui | `082c6357e4cff95ef5b0b0295975c3290580a499` |
| `libs/ReplayStudio/` | https://github.com/ReplayMod/ReplayStudio | `ca00f8101bec034efe77bdb938522aa3b800cf50` |
| `src/main/resources/assets/replaymod/lang/` | https://github.com/ReplayMod/Translations | `ed16d95cd373d4b5ddd257ea4b30790d2fcff353` |

ReplayMod 主项目来自此次移植使用的 `NeoForgeReplay-mc-1.21` 源码压缩包，压缩包未包含顶层 Git 历史。Minecraft 1.21.1 / NeoForge 21.1.209 的平台适配、GUI 和相机相关修改，以及简体中文修订已与这些源码一起提交。

本仓库无需运行 `git submodule update`。此仓库的提交历史记录本次源码上传后的变更；各上游项目的历史可通过上表所列仓库查阅。

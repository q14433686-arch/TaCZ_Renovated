# MC 百科发布文案（TaCZ: Renovated）

> 供 MC 百科词条编辑使用。正文采用 MC 百科 BBCode；版本范围与功能边界已按仓库
> `README.md`、`CHANGELOG.md`、各版本分支及 GitHub Releases 于 2026-08-22 同步。
> 后续更新时按本文末尾的维护清单核对，不要把单个文件的更新日志堆进词条正文。

## 词条名称

- 中文名：`永恒枪械工坊：零：焕新`
- 英文名：`TaCZ: Renovated`
- 缩写建议：`TaCZR-NF`（如与现有词条简称冲突，以站方意见为准）

## 简介

```text
《永恒枪械工坊：零：焕新》（TaCZ: Renovated）是《永恒枪械工坊：零》
（Timeless & Classics Guns: Zero，简称 TaCZ）的非官方 NeoForge 社区移植，
当前提供 Minecraft 26.2、26.1.2 与 1.21.11 构建。
```

## 正文（复制到 MC 百科）

```bbcode
[h1=非官方移植说明]

TaCZ: Renovated 是《永恒枪械工坊：零》（Timeless & Classics Guns: Zero，简称 TaCZ）的非官方 NeoForge 社区移植，当前提供 Minecraft 26.2、26.1.2 与 1.21.11 构建。

项目基于 [url=https://github.com/MCModderAnchor/TACZ]MCModderAnchor / TACZ[/url]、[url=https://github.com/Sh1roCu/TACZ-Refabricated]Sh1roCu / TACZ-Refabricated[/url] 与 [url=https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial]TaCZ Refabricated Unofficial[/url] 的公开源码和 TaCZ 1.1.8-hotfix，重点维护新版 Minecraft 的 NeoForge 运行适配、枪包兼容和扩展接口。它不是 TACZ Dev Team 的官方发布，也未经其审阅或背书。

[h1=主要内容]

• 保留 TaCZ 的枪械、配件、瞄具、弹药、改装台和数据驱动枪包体系，并附带上游默认枪包。

• 针对各版本改写 NeoForge 注册、事件、网络、资源加载、GUI 与渲染接线；不同版本采用各自的瞄具渲染实现。

• 支持现代枪包直接加载，并提供旧布局枪包转换功能。

• 内置部分 LRTactical 兼容框架，覆盖近战、消耗品、引爆器及多类投掷物的基础数据与运行路径。

• 对 JEI / REI 提供内置弹药查询和工作台类别，并在服务端枪包同步后刷新配方数据；部分版本另有 Carry On 工作台兼容。准确状态以对应版本的兼容矩阵和 Release 说明为准。

项目重点是移植与兼容性维护，不以额外增加枪械内容为目标。第三方枪包、战术装备包、可选模组和光影包仍需按具体版本逐项验证。

[h1=支持环境]

加载器：NeoForge

目前提供的 Minecraft 版本：26.2、26.1.2、1.21.11

必需前置：无。LuaJ 与 Commons Math 已随模组文件打包；本项目使用 NeoForge 原生配置，不需要 Fabric API 或 Forge Config API Port。

Java：

• Minecraft 26.2 / 26.1.2：Java 25 或更高版本

• Minecraft 1.21.11：Java 21 或更高版本

不同 Minecraft 版本的模组文件不能混用。NeoForge 的准确版本要求请以对应下载文件及 [url=https://github.com/q14433686-arch/TaCZ_Renovated/releases]Release 说明[/url]为准。本项目不提供 Forge / Fabric 构建；Fabric 构建请使用下方姊妹项目。

[h1=枪包安装]

现代枪包放入：

.minecraft/tacz/

zip 可以直接放入，无需解压；也支持解压后的文件夹。无论采用哪种形式，gunpack.meta.json 都必须位于枪包根目录，zip 外层不要再多套一层文件夹。

旧布局枪包应放入：

.minecraft/tacz_backup/

进入游戏后执行：

/tacz convert

转换前请保留原文件。转换器会处理受支持的旧目录和数据格式，但不能保证自动修复所有资源、配方或脚本差异。是否需要转换应以枪包内部结构为准，而不是只看其标注的 Minecraft 版本。

联机时应在服务端和客户端安装相同枪包：服务端提供数值与配方等逻辑数据，客户端提供模型、贴图、动画、音效和语言资源。服务端新增枪包后执行 /tacz reload；客户端新增枪包后按 F3+T 重载资源。

[h1=LRTactical 与 Arcana]

本项目内置的是 LRTactical 的部分兼容框架，并非其完整 NeoForge 发行版：不包含原作完整美术资源，flash_shield 尚未移植，也不能保证所有相关内容包直接兼容。内容包仍须自行携带其获准分发的模型、贴图、动画与音效。

本项目不包含 [url=https://www.curseforge.com/minecraft/mc-mods/tacz-arcana-timeless-and-classics-guns]TacZ:Arcana[/url]，也未实现其 API 或受保护资产加载流程。明确要求 Arcana 的内容包目前不属于受支持范围。紫黑贴图或模型缺失并不能单独证明枪包依赖 Arcana，也可能由目录层级、资源路径、版本约束或文件不完整造成。

[h1=已知边界]

• 不保证所有第三方枪包、LRTactical 内容包、可选模组或光影包兼容。

• 瞄具渲染按不同版本分别适配；不同图形后端和 shader pack 的最终效果可能不同。26.2 的 OpenGL、Iris 与 Vulkan 状态须以该版本 Release 说明和兼容矩阵为准。

• 枪包能够被扫描或显示条目，不代表其模型、动画、配方与脚本已经完全兼容。

• 面板服、代理网络和 NeoForge 与 Bukkit 混合服务端不属于统一保证范围；相关问题应先在原生 NeoForge 专用服务器复现。

遇到问题时，请先使用“本模组”的最小 NeoForge 环境复现，并保留完整 latest.log 或崩溃报告。联机问题应同时提供服务端和客户端日志。

[h1=相关项目、来源与反馈]

• [url=https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated]本项目 CurseForge 页面（Project ID 1663324）[/url]

• [url=https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated]姊妹项目：TaCZ Refabricated Unofficial（Fabric）[/url]

• [url=https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial]姊妹项目源码（Fabric）[/url]

• [url=https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero]原始 TaCZ 发布页（CurseForge）[/url]

• [url=https://github.com/MCModderAnchor/TACZ]原始项目源码：MCModderAnchor / TACZ[/url]

• [url=https://github.com/Sh1roCu/TACZ-Refabricated]直接上游：Sh1roCu / TACZ-Refabricated[/url]

• [url=https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements]LRTactical 原项目源码[/url]

• [url=https://github.com/q14433686-arch/TaCZ_Renovated]本移植源码[/url]

• [url=https://github.com/q14433686-arch/TaCZ_Renovated/releases]下载与版本说明[/url]

• [url=https://github.com/q14433686-arch/TaCZ_Renovated/issues]问题反馈[/url]

为兼容既有枪包依赖和存档数据，本移植继续使用 mod ID“tacz”。这不代表其为官方版本。

本仓库不同部分可能采用不同许可：TaCZ、本移植及移入的 LRTactical 代码部分使用 GPL-3.0；默认枪包资源依其 gunpack_info.json 使用 CC BY-NC-ND 4.0；随包使用的 LuaJ 为 MIT；Commons Math 为 Apache-2.0；其他第三方代码、资源和内容包以各自许可为准。代码许可不会自动覆盖模型、贴图、动画、音效等资源，完整信息请查看仓库中的 [url=https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSE]LICENSE[/url] 与 [url=https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSES.md]LICENSES.md[/url]。

本移植产生的问题请反馈至本项目，不要提交给 TaCZ、TaCZ Refabricated 或 LRTactical 原作者。
```

## 词条属性建议

| 字段 | 内容 |
|---|---|
| 运行方式 | 客户端需装、服务端需装 |
| 支持平台 | Java 版 |
| 加载器 | NeoForge |
| 支持版本 | 26.2、26.1.2、1.21.11 |
| 许可 | GPL-3.0-only（资源另见词条正文） |
| Mod ID | `tacz`（兼容性需要，并非官方身份声明） |
| 源码 | https://github.com/q14433686-arch/TaCZ_Renovated |
| 下载 | https://github.com/q14433686-arch/TaCZ_Renovated/releases |
| 问题反馈 | https://github.com/q14433686-arch/TaCZ_Renovated/issues |
| CurseForge | https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated（Project ID 1663324） |
| 姊妹项目 | https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated |

## 后续更新维护清单

1. 新增或停止支持 Minecraft 版本时，同步“简介”“支持环境”“词条属性”。
2. Java、NeoForge 或必需前置变化时，以各分支 `gradle.properties` 和 Release 为准。
3. 功能状态变化时先更新对应分支 README / CHANGELOG / 兼容矩阵，再改本页概括；未实测内容不得写成“支持”。
4. 每个构建的修复列表只写在文件更新日志或 GitHub Release，不累积进百科正文。
5. 链接变更时同时核对姊妹项目、原始项目、直接上游、源码、下载、Issues、LICENSE 与 LICENSES。

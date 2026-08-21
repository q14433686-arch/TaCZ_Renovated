# 移植工单：NeoForge 26.1.2 → 26.2（交接给执行 AGENT）

> **文档性质**：二期移植的工作合同附件。执行 AGENT 开工前必须先完整读
> [`../CHARTER.md`](../CHARTER.md)（其全部红线在本工单内继续有效，本文只做修订与补充），
> 再读本文。
>
> **本文的差异清单是不完整的**（调研 AI 于 2026-08-21 联网核实的快照 + 本仓库 records 沉淀）。
> 凡本文未列出的差异，以第 3 节的官方情报源与 26.2 未混淆游戏源码为准——
> 执行 AGENT 应自行查证、javap、读反编译源，**而不是依赖本文穷举**。
> 凭训练数据记忆写 API = 打回，照旧。

---

## 0. 一句话目标

把本仓库（TaCZ NeoForge **26.1.2**，基线 Beta-1）**前滚**到
**Minecraft 26.2 + NeoForge 26.2.0.x（release 通道）**。
是前滚，不是重写：26.1.2 代码库是唯一的出发点。

## 1. 前置闸门（执行第一天先核，全部有证据才动工）

| 检查项 | 调研快照（2026-08-21） | 执行时动作 |
|---|---|---|
| NeoForge 26.2 转 release | ✅ 大概率已转：neoforged/NeoForge 提交历史含 **"26.2.0 Stable"**，仓库默认分支已是 `26.2.x`（[projects.neoforged.net](https://projects.neoforged.net/neoforged/neoforge) 页面提交列表） | 在 projects 页 / Maven metadata 确认最新 `26.2.0.<build>`（**无 `-beta` 后缀**），写入 `gradle.properties`，证据入 records |
| 官方 MDK | 26.1.2 用的是 MDK-26.1.2-ModDevGradle | 找 `NeoForgeMDKs/MDK-26.2-ModDevGradle`，逐项对齐构建骨架（Gradle wrapper、ModDevGradle 版本、**toolchain JDK——26.2 是否仍 JDK 25 以 MDK 为准**、模板文件集） |
| 光影生态 | Iris 已有 26.2 版（[Modrinth](https://modrinth.com/mod/iris/versions?g=26.2)，OpenGL 后端）；**Aperture（Iris 的 Vulkan 继任）截至 2026-07 仍在私有开发**（社区多方证实） | 光影兼容仍走 Iris/GL 路线，`ShaderCompat` 后端**不换**；Aperture 留待其发布 |
| 上游语义权威 | refab `26.2(main)` 活跃（最后提交 2026-08-18，257 commits） | 见第 2 节 |

## 2. 权威与参考边界（对宪章第 2 节的修订）

| 仓库 | 二期角色 | 变化 |
|---|---|---|
| [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) **`26.2(main)` 分支** | **游戏语义唯一权威**（升格） | 原 26.1.2 分支降为回溯参考。该仓库有自己的 `AGENTS.md` 与 `docs/`（含 26.2↔26.1.2 语义差异沉淀），**先通读再动手**。仍然禁止照抄其 Fabric API 表面 |
| 本仓库 `26.1.2` 基线（Beta-1） | 代码出发点 + 加载器习语权威 | 26.1.2 的 NeoForge 写法（PayloadRegistrar / DeferredRegister / 事件面）大体平移，逐个对 26.2 sources 重验 |
| MUKSC/TACZ-1.21.1 | 原则上**不再需要** | 渲染禁抄条款继续有效 |
| CurseForge `tacz-port` jar | **洁净室红线原文有效**（宪章 §2.1） | 不下载、不反编译、不参考 |

## 3. 官方情报源（按优先级）

1. **26.2 未混淆游戏源码 / javap**（① 级，唯一真理）。
2. **官方迁移 primer**：https://docs.neoforged.net/primer/docs/26.2/
   （GitHub 源：https://github.com/neoforged/.github/blob/main/primers/26.2/index.md ）。
   **超长文档（网页抓取约 24 个分块），必须通读**，本工单第 4 节只映射了与本仓库直接相关的部分。
   primer 采用 CC-BY-4.0，引用时留链接。
3. **NeoForge 26.2 sources jar**（loader/事件面变化）+ neoforged.net/news 的 26.2 发布博文
   + Discord Dev Announcements 频道（破坏性变更公告）。
4. 数据包/资源包层面变更：https://misode.github.io/versions/?id=26.2&tab=changelog
5. 概览（定方向用）：Fabric 26.2 博文 https://fabricmc.net/2026/06/15/262.html
   ——26.2 是**以渲染与注册为主**的小版本，外加实验性 Vulkan 后端。
6. refab `26.2(main)` 的 docs/ 与提交历史（③ 级：上游踩坑沉淀）。

## 4. 已核实差异 → 本仓库 touchpoint 映射

> 标注【硬】= 不改必炸（编译或启动）；【验】= 需逐一对 26.2 重验，可能存活；
> 【策】= 需要先做技术决策。文件路径均相对 `src/main/java/com/tacz/guns/`。

### A.【策】Vulkan 双后端 —— 瞄具 depth-aperture 的存亡问题

- 26.2 原版新增 Vulkan 图形后端（选项菜单可切），`com.mojang.blaze3d.opengl` 下的类
  普遍有 `com.mojang.blaze3d.vulkan` 平行版（primer "Vulkan" 节）。
- **touchpoint**：`mixin/client/GlCommandEncoderScopeDepthCopyMixin.java`
  ——`@Mixin(targets="com.mojang.blaze3d.opengl.GlCommandEncoder")`，注 `drawFromBuffers`。
  这是瞄具 depth-aperture 的心脏。用户切到 Vulkan 后端时该 mixin **不应用**：
  预期表现是瞄具效果静默失效（而非崩溃），但必须实测确认失效模式。
- 配套：`client/render/scope/ScopeDepthCopyState.java`（depth 拷贝状态机）。
- **决策预案（供发起人确认）**：首发 **GL-only**——26.2 默认后端仍是 OpenGL，Vulkan 是
  实验选项；运行时检测图形后端（primer 提及 `Options#preferredGraphicsBackend`），
  Vulkan 下瞄具走无 depth 效果的降级路径 + 日志说明；Vulkan 平行 mixin 留三期。
- refab 26.2 线的瞄具方案是宪章点名"高危"的 **Iris GL 掩码桥**。执行 AGENT 需读其实现后
  独立评估：是沿用我们的 depth-aperture 前滚，还是采纳上游方案。**默认倾向前者**，
  除非 26.2 的管线变更（见 B）使 depth 拷贝不再可行。

### B.【硬】管线构建 API 大改

primer：`SourceFactor`/`DestFactor` 合并为 `BlendFactor`（+`BlendOp`）；`TextureFormat` 与
顶点元素格式统一为 `GpuFormat` 枚举；**`VertexFormat` 重写为 builder/attribute 动态模型**
（`IndexType`/`PrimitiveTopology` 独立枚举）；新增 `BindGroupLayout`（sampler/uniform 声明
从 pipeline 拆出）；`RenderPipeline` 支持多 `ColorTargetState`。

- **touchpoint**：`client/render/scope/ScopeRenderTypes.java`
  （`RenderPipeline.builder()` 段，import 了 `ColorTargetState`/`DepthStencilState`/
  `CompareOp`/`MeshData`/`VertexFormat`——全在震区）。
- `client/model/bedrock/BedrockModel.java` 及全部自建 RenderType/pipeline 处同查。

### C.【硬】AT 条目逐条重验

`src/main/resources/META-INF/accesstransformer.cfg` 现有 5 条，其中两条在渲染震中：
`RenderType <init>(String, RenderSetup)` 与 `RenderPipelines#register`。
**AT 签名失配是启动即炸且报错难读的故障**——对 26.2 jar 逐条 javap 重验（类名、
包名、参数签名一个都不能想当然），其余三条（`MultiPlayerGameMode#ensureHasSentCarriedItem`、
`Minecraft#startUseItem`、`LivingEntity#jumping`）同样过一遍。

### D.【硬】Feature Rendering 完全接管 / MultiBufferSource 移除

primer："The Takeover"——`MultiBufferSource` 与原版 chunk 渲染之外的直接顶点上传
被彻底替换；新增 `SubmitNodeStorage` / `FeatureRenderDispatcher#prepareFrame` /
自定义 `SubmitNode`+`FeatureRenderPhase`+`FeatureRenderer` 扩展点。

- **touchpoint**：`client/model/bedrock/BedrockModel.java:357`（代码内已有注释
  "26.2: Minecraft.renderBuffers() removed"——refab 语义早已预告，顺着这条注释改）。
- 第一人称渲染已在 SubmitNodeCollector 体系上（26.1.2 WP⑤ 的架构红利），预期低改动，
  但 `submitCustomGeometry` 一类入口的签名需重验。
- 自定义相位需求（HAND_TRANSLUCENT 分类）评估是否改走官方 `FeatureRenderPhase` 扩展点。

### E.【硬】PictureInPicture 签名变化

primer：PiP 的 `prepare` 改收 `FeatureRenderDispatcher`，`renderToTexture` 改收
`SubmitNodeCollector`，`Context#bufferSource()` 移除。

- **touchpoint**：`client/gui/preview/GunPreviewRenderer.java`（javadoc 里就引用着
  `Context#bufferSource()`）、`GunPreviewRenderState.java`、`GunSmithTableScreen.java`、
  `client/init/ClientSetupEvent.java`（注册处）。

### F.【验】RenderSystem 瘦身

primer 点名移除/迁移：`flipFrame`、`renderBuffers`、`getMainRenderTarget`→
`GameRenderer#mainRenderTarget`、`getModelViewMatrix`→`getModelViewMatrixCopy` 等。
本仓库 12 处使用点，逐一判死活：

- `GunPreviewRenderState.java`：`getModelViewStack` ×2、`enableScissor`
- `GunSmithTableScreen.java`：`enableScissor` ×2
- overlay 组：`HeatBarOverlay`/`GunHudOverlay` 的 `setShaderColor`、
  `KillAmountOverlay` 的 `enableBlend`、`TaczImageButton` 的 `enableDepthTest`
  ——这类全局状态 setter 在 feature 接管后大概率死，正解是换 RenderType/pipeline 表达
- `ScopeDepthCopyState.java`：`assertOnRenderThread` ×2

### G.【硬】Gui 重组

primer：`Screen`/`ChatListener` 等移入 `Gui`（`Minecraft.getInstance().gui.screen()`），
HUD 拆出独立 `Hud` 类，`Options.hideGui`→`Hud#isHidden`。

- **touchpoint**：全仓库 `Minecraft.getInstance().screen` / `setScreen` 约 **39 处**
  （`RefitTransform`、`PreventsHotbarEvent`、`RenderCrosshairEvent`、`GunRefitScreen`、
  cloth 兼容层等）——机械替换，但量大，建议单独一个提交。
- `hideGui`/`setOverlayMessage` 本仓库未用（已核）。
- **连带风险**：HUD 拆分后 NeoForge 侧 `RegisterGuiLayersEvent`/`GuiLayer` 的事件面
  可能随之改名换签名——本仓库全部 overlay 的注册路径，查 NF 26.2 sources 定论。

### H.【验】文本与杂项渲染

- `ChatFormatting` 被掏空改 `Style#withColor/withBold/...`：本仓库仅
  `client/gui/overlay/InteractKeyTextOverlay.java` 1 处。
- `Font` 直绘方法全移除（改 prepareText/GlyphVisitor）：本仓库无直绘（已核，0 处），
  但所有经 `GuiGraphics`/submitText 的 HUD 文本路径顺手验一遍。
- Gizmos 改走 `GizmoFeatureRenderer`：`client/event/RenderHeadShotAABB.java`
  （26.1.2 手写 `RenderTypes.lines()` + `setLineWidth(2.5F)`，见 records/WP05）——
  评估直接换官方 gizmo 提交，可能反而删代码。
- `ParticleFeatureRenderer`→`QuadParticleFeatureRenderer` 等改名：
  `init/ModParticles.java`（自定义 `BulletHoleOption`）与粒子渲染注册
  （`registerSpecial`）重验；另注意 records/WP07 坑 #9：**`SimpleParticleType`
  构造器 26.2 变 protected**（refab 已注明，需工厂/匿名子类）。

### I.【硬】transfer API removal 落地

26.1.2 基线编译带 **19 条 `@Deprecated(forRemoval)` transfer API 警告**
（records/WP05 记录在案）。26.2 是 removal 最可能落地的版本——这 19 条在 bump 后
即成编译错误。**建议作为前置卫生在 26.1.2 基线上先清零**（成果可直接发 26.1.2 Beta-2）。
同类：`LoadingModList.get()` 已 forRemoval（records/WP06，本仓库已用替代路径，验一遍）。

### J.【验】非渲染杂项

- primer "Minor Migrations" 有一长串可见性/签名微调（通读，逐条 grep 本仓库）。
- 26.2 **注册相关变化**被 Fabric 博文点名为两大主题之一——`init/` 全目录
  （DeferredRegister 路径、`ModItems`/`ModBlocks`/`ModEntities`/`ModRecipe`）对 26.2 重验。
- 社区实证：26.2 改变了**方块破坏事件的触发方式**（26.2 beta 期已致第三方 mod 崩溃）
  ——本仓库的方块交互 / `LivingDamageEvent` / tick 事件链全部实测。
- 版本元数据：`minecraft_version_range=[26.2,)` 或 `[26.2.0]` 以 MDK 写法为准；
  枪包版本检查语义（`>=1.1.8`）回归测试。

## 5. 建议的工作包切分

沿用一期"每包一份 records 证据文档 + 专用服务端冒烟"的节奏，命名 `WP-262-x`：

| 包 | 内容 | 验收 |
|---|---|---|
| WP-262-0 前置卫生（**仍在 26.1.2 上做**） | 清零 19 条 forRemoval 警告；顺手删 `gui/GunPackProgressScreen` 死代码（records/WP04 遗留） | 26.1.2 构建零 removal 警告，可发 Beta-2 |
| WP-262-1 构建骨架 bump | gradle.properties 三版本号、MDK-26.2 对齐、`mod_version=1.1.8+neoforge.26.2.0.r0` | `runServer` Mod List 可见 `tacz` |
| WP-262-2 非渲染编译修复 | 第 4 节 I/J/G(非渲染面)/AT 重验(C) | 专用服务端 `Done`，枪包装载数字与 26.1.2 一致 |
| WP-262-3 渲染层 | B/D/E/F/H，GL-only；瞄具决策 A 在本包开头定案 | `compileJava` 过 + 有 GPU 环境实机矩阵 |
| WP-262-4 兼容矩阵重验 | Cloth/PAL/Controllable/SSR/JEI/REI/Iris/CarryOn/FirstPerson **逐个查 26.2 构建是否存在**，坐标全部重钉（一期的 CurseMaven file id 全部失效）；重写 `COMPATIBILITY.md` | 逐行用户 PASS 或明确标注未实测 |
| WP-262-5 发布 | CHANGELOG 新条目、README 版本信息、分支策略执行 | 发布 jar + 对应源码 |

## 6. 版本号与分支（红线）

- 项目显示名已于 2026-08-21 更名 **TaCZ: Renovated**（modId 仍为 `tacz`，
  见 `records/NAMING_DECISION.md`）；26.2 发布物料直接用新名，README 版本导航表
  在 WP-262-5 加 26.2 行。
- `mod_version` = `1.1.8+neoforge.26.2.0.r0` 起步。**`+` 后是 build metadata；
  禁止 `-`**（pre-release 会让枪包 `>=1.1.8` 检查静默失败，宪章 §7.4）。
- 改版本号必须同步 README/CHANGELOG 并通过
  `scripts/check_release_consistency.sh --strict`（AGENTS.md §1）。
- 26.1.2 线转维护分支还是就地冻结，由发起人在 WP-262-1 前定案；工单默认：
  为 26.2 开新分支，26.1.2 分支只收 bugfix。

## 7. 纪律复述（不重复宪章全文，只列最易犯的）

1. **洁净室**：`tacz-port` jar 碰一下，整段代码作废。
2. **证据**：每条非平凡 API 在 records 文档指认 `类#方法(签名)` + 来源层级。
   26.2 未混淆，没有任何借口。
3. **禁抄边界**：refab 抄语义不抄 Fabric API 表面；MUKSC 渲染一行不碰。
4. **不跨包顺手改**；每包结束更新 CHANGELOG 草稿，**不在 README 记进度**。

## 8. 已知未知（执行时优先消除的情报缺口）

- NeoForge 26.2 自身（loader/事件面）的破坏性变更全貌——本工单只核了 vanilla primer，
  NF 侧要读 26.2 sources + 发布博文。
- `RegisterGuiLayersEvent`/`GuiLayer` 在 Hud 拆分后的形态（G 节连带风险）。
- depth-aperture 的 depth 拷贝在 26.2 GL 管线（RenderPass/render area 改动后）是否原样可行。
- refab 26.2 "Iris GL 掩码桥"的实现细节与风险面（读上游源码后给评估结论，入 records）。
- Aperture 发布时间表（影响三期 Vulkan 光影规划，不阻塞本期）。

---

*调研快照：2026-08-21，由一期文档整理 AI 联网核实。链接内容以执行当日实况为准。*

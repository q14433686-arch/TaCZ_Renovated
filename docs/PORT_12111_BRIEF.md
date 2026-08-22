# 移植工单：NeoForge 26.1.2 → 1.21.11（回移植，交接给执行 AGENT）

> **文档性质**：回移植的工作合同附件。执行 AGENT 开工前必须先完整读
> [`../CHARTER.md`](../CHARTER.md)（其全部红线在本工单内继续有效，本文只做修订与补充），
> 再读本文。
>
> **本文的差异清单是不完整的**（调研快照 2026-08-22：本仓库全树 grep + 姊妹项目
> `1.21.11` 分支 docs 逐份通读 + NeoForge 官方 21.11 发布页 + MUKSC
> `neoforge/1.21.1` 构建文件联网核实）。凡本文未列出的差异，以第 3 节的官方情报源
> 与 **1.21.11 混淆 jar（javap 反查官方映射）**为准——执行 AGENT 应自行查证，
> **而不是依赖本文穷举**。凭训练数据记忆写 API = 打回，照旧。

---

## 0. 一句话目标

把本仓库（TaCZ: Renovated，NeoForge **26.1.2**，基线 **R1**（含 LRTactical 内置层））
**回移植**到 **Minecraft 1.21.11 + NeoForge 21.11.x（release 通道）**。
是回移植，不是重写：本仓库 R1 代码是唯一的出发点，姊妹项目
`TaCZ_Refabricated_Unofficial` 的 **`1.21.11` 分支**是游戏语义权威（它已完成同一条
路线的 Fabric 版，含全部踩坑台账）。

背景：宪章 §1 把 1.21.11 列为「⏸ 暂缓、视需求决定是否回移」——发起人现已决定回移，
本工单即该决定的执行文件。

---

## 1. 前置闸门（执行第一天先核，全部有证据才动工）

| 检查项 | 调研快照（2026-08-22） | 执行时动作 |
|---|---|---|
| NeoForge 21.11.x release 构建 | 官方发布页 [NeoForge 21.11 for Minecraft 1.21.11](https://neoforged.net/news/21.11release/)（2025-12-09）确认 21.11 线；版本对照表给出 **21.11.44 release**（[mcreference](https://mcreference.com/guides/neoforge-version-numbers-explained)） | 在 [projects.neoforged.net](https://projects.neoforged.net/neoforged/neoforge) / Maven metadata 确认最新 **21.11.\<build\>（无 `-beta`）**，写入 `gradle.properties`，证据入 records |
| 官方 MDK | **`NeoForgeMDKs/MDK-1.21.11-ModDevGradle` 存在**（GitHub 已核） | 拉取，逐项对齐构建骨架：ModDevGradle 版本、toolchain（JDK **21**，1.21.11 是 Java 21 纪元）、模板文件集（尤其 `neoforge.mods.toml` 键位——26.x MDK 把 `license` 放在顶层，1.21.x 放 `[[mods]]` 内，以 MDK 模板为准） |
| 混淆与映射 | 1.21.11 是**混淆版本**（NeoForge 官方发布页明确「NeoForge 21.11 continues to be built on top of an obfuscated Minecraft」；去混淆自 26.1 起）。姊妹项目实测：需官方映射 + remap 工具链 | MDG 侧启用官方 Mojang 映射（+ Parchment，**以 MDK 写法为准**）；MUKSC 1.21.1 用 parchment `2024.11.17`，1.21.11 需**重新解析** 2025-11 之后的 parchment 构建（1.21.11 发布于 2025-11，旧 parchment 不含它）。mixin AP / refmap 回归（26.1.2 因无映射而省略——姊妹项目阶段 1 第 4、5 步同款） |
| 本仓库基线状态 | R1 = `1.1.8+neoforge.26.1.2.R1`，800 java / 约 78k 行 / 25 个已注册 mixin+Accessor（4 配置，另 1 个未注册 HumanoidModelMixin）/ 6 条 AT / 3754 资源 | 开工前把基线 commit 钉进 records（姊妹项目把 1.21.11 分支起点钉为 `1bf91c0` 的做法） |
| 上游语义权威 | 姊妹项目 `1.21.11` 分支活跃（最新提交 2026-08-22，R2-hotfix 已收），相对其 26.1.2 分支 **41 ahead / 15 behind** | 见第 2 节；其 docs 清单见第 9 节 |
| 沙箱/开发机约束 | 26.1.2 构建沿用 MDG `disableRecompilation = true`（沙箱内存适配） | 1.21.11 是混淆版，**需要 neoform 反编译产物做 javap 核验**；确认 MDG 2.x 在 1.21.x 上的等价产物路径（runtime/joined jar），内存参数以实机为准 |

---

## 2. 权威与参考边界（对宪章第 2 节的修订）

| 仓库 | 回移植角色 | 变化 |
|---|---|---|
| [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) **`1.21.11` 分支** | **游戏语义唯一权威（升格）** | 它的 1.21.11 分支已把同一条 26.1.2→1.21.11 路线在 Fabric 侧走完：`docs/PORT_1_21_11_PHASE1/2.md`（构建层 + 11 个错误族收敛全表）、`docs/CHANGELOG_1_21_11.md`（R1→R2-hotfix 全部修复）、`docs/EXPLICIT_GAPS_AUDIT_R12.md`、`docs/CARRYON_COMPAT.md`、`docs/verify_mixin_targets.py` / `docs/verify_shader_imports.py` / `docs/migrate_*.py`（迁移脚本）。**这些是直接可用的资产**。仍然禁止照抄其 Fabric API 表面（事件、注册、网络写法）——NeoForge 侧全部重写 |
| 本仓库 26.1.2 基线（R1） | 代码出发点 + 26.x 语义的自我权威 | 回移时所有 26.1.2 NeoForge 习语（PayloadRegistrar / DeferredRegister / 事件面 / jarJar）**不能想当然平移**，逐一对 21.11 sources 重验（§4-N） |
| [MUKSC/TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)（分支 `neoforge/1.21.1`） | **加载器习语唯一权威（重新升格）** | 1.21.11 回到旧纪元，MUKSC 的 1.21.x NeoForge 写法（`build.gradle.kts` + ModDevGradle 2.0.74 + parchment + `neoforge.mods.toml` 键位 + NeoForge 21.1.233 依赖声明）重新成为第一参照。**渲染代码一行都不能抄**的条款继续有效（1.21.x 是旧渲染纪元：无 Feature Rendering）。注意 MUKSC 是 1.21.**1**，与本目标的 1.21.11 隔着 10 个小版本——vanilla API 以姊妹项目 javap 证据为准，MUKSC 只用于加载器骨架 |
| CurseForge `tacz-port` jar | **洁净室红线原文有效**（宪章 §2.1） | 不下载、不反编译、不参考 |

---

## 3. 官方情报源（按优先级）

1. **1.21.11 混淆游戏 jar（javap，官方映射反查）**（① 级，唯一真理）。姊妹项目阶段 1 的原话照抄：「产物 named 命名空间 jar 可直接用 javap 逐符号核验 1.21.11 API，后续阶段应当用它」。
2. **官方迁移 primer**：https://github.com/neoforged/.github/blob/main/primers/1.21.11/index.md
   （NeoForge 21.11 发布页点名；CC-BY-4.0，引用时留链接）。必须通读。
3. **NeoForge 21.11 官方发布页**：https://neoforged.net/news/21.11release/ ——已核实要点：
   `ResourceLocation→Identifier`（**本仓库 26.1.2 基线已在 Identifier 纪年，276 文件不受影响**）、
   JSpecify 空注解切换、`RenderType` vanilla 常量搬入 **`RenderTypes`** 类、`BakedQuad` int[] 编码废除、
   model JSON `block_light/sky_light` → `light_emission`。另注意 21.9 线的「Transfer Rework」（#2663）
   与 21.11 的增量——本仓库 26.1.2 基线带 **19 条 transfer API `forRemoval` 警告**，这些调用在
   21.11 上属于 rework **之后**、removal **之前**的形态，**必须逐条对 21.11 sources 重验**。
4. 数据包/资源包变更：https://misode.github.io/versions/?id=1.21.11&tab=changelog （本仓库 3754 个资源文件）。
5. 姊妹项目 `1.21.11` 分支的 docs/ 与提交历史（③ 级：同路线踩坑沉淀，见第 9 节清单）。

---

## 4. 已核实差异 → 本仓库 touchpoint 映射

> 标注【硬】= 不改必炸（编译或启动）；【验】= 需逐一对 1.21.11 重验，可能存活；
> 【策】= 需要先做技术决策。vanilla 侧差异依据姊妹项目 javap 台账（其 Fabric 与我们的
> NeoForge 共享全部 vanilla 类），本仓库计数为本次调研实测。文件路径均相对 `src/main/`。

### A.【硬】构建骨架整体回退（姊妹项目阶段 1 的 NeoForge 版）

- `gradle.properties`：`minecraft_version=1.21.11`、`minecraft_version_range=[1.21.11]`、
  `neo_version=21.11.<最新 release build>`、`mod_version=1.1.8+neoforge.1.21.11.R0`（§6）。
- `build.gradle`：JDK toolchain **25 → 21**；启用官方映射 + parchment；mixin AP/refmap 接线
  （26.1.2 注释掉的那套回归）；`neoforge.mods.toml` 模板对 MDK-1.21.11 键位逐项对齐
  （现模板注释写着「Do not add mappings/parchment anywhere in this project: 26.1+ is
  unobfuscated」——这句随回移作废）。
- mixin 配置 `compatibilityLevel`：`tacz`/`tacz.iris` 已是 `JAVA_21` ✓；
  **`tacz.carryon` 是 `JAVA_25` → 必须降到 `JAVA_21`**（1.21.11 的 sponge-mixin 无 JAVA_25）；
  `lrtactical` 是 `JAVA_17` ✓。
- **vendored jar 字节码版本核验**：`libs/commons-math3-3.6.1.jar`、`libs/luaj-jse-3.0.1.jar`
  是 26.1.2 纪元随迁的本地文件——**先查 major version 是否 ≤ 61（Java 21）**，超标则换
  Java 21 兼容构建（MUKSC 同款钉 `commons-math3 3.6.1` / `luaj 3.0.8-figura` / `bcel 6.6.1`）。
- Java 25 专属语法审计（模块 import、新 pattern 特性等）：`compileJava` 会兜底，但首轮错误日志
  里要能认出这类，别混进 vanilla API 族。

### B.【硬】GUI 家族回退（姊妹项目错误族 1/4/7）

26.x 把 `GuiGraphics` 改名 `GuiGraphicsExtractor` 并把方法名改短；1.21.11 是
`GuiGraphics` + `drawString`/`drawCenteredString`/`renderItem`/`hLine`/`renderTooltip` 等
（完整对照表在姊妹项目 `docs/migrate_family1.py`，直接借用）。

- **本仓库计数：35 个文件引用 `GuiGraphicsExtractor`**（姊妹项目是 32 文件/86 错误，量级一致）。
- `extract*` 方法调用 2 处 + GUI 覆写族（`Screen`/`AbstractButton`/列表 Entry 的
  `extract*`→`render*` 覆写）——姊妹项目警告过 **过度改名误伤**（`EntityRenderer#extractRenderState`、
  `SingleQuadParticle#extract`、`IItemHandler#extractItem` 等是真实存在的，不许动），照抄其排除清单。
- **LR 层连带**：`me/xjqsh/lrtactical/mixin/client/GuiGraphicsExtractorMixin.java` 的
  **mixin 目标类名**随回退改为 `GuiGraphics`。

### C.【验】Feature Rendering 层 —— 无需回退（工单调研更正，2026-08-22 执行中核实）

**更正**：1.21.11 原生就有 Feature Rendering——姊妹 1.21.11 定稿文件仍 import
`SubmitNodeCollector` / `RenderTypes`（如 `TargetMinecartRenderer`、`ScopeRenderTypes`），
其 `SpecialModelRenderer#submit(..., SubmitNodeCollector, ...)` 签名也在。26.1 只是
在它之上又发明了 `RenderPipeline` 状态对象。**本仓库 33 个引用
`SubmitNodeCollector`/`FeatureRenderDispatcher` 的文件不需要结构性回退**。

- 需**逐文件验**的 1.21.11 差异：`RenderTypes` 常量类取代 `RenderType` 常量（NeoForge
  21.11 发布页点名）；`submitHitbox`/`renderLineBox` **不存在**（F 的 Gizmo 修复）；
  `submitCustomGeometry` 存在（F3+B 崩溃正是经由它发生）。
- `BedrockModel.java` 的「26.2: Minecraft.renderBuffers() removed」注释与 1.21.11 无关。

### D.【硬】包迁移（姊妹项目错误族 2/3/5/8/9）

| 26.1.2 | 1.21.11 | 本仓库计数 |
|---|---|---:|
| `renderer.state.level.*`（`CameraRenderState` 等） | `renderer.state.*` | **7 文件** |
| `resources.model.cuboid.*`（`ItemTransform(s)` 等） | `renderer.block.model.*` | **7 文件** |
| `renderer.state.gui.pip.PictureInPictureRenderState` | `client.gui.render.state.pip.*` | `client/gui/preview/GunPreviewRenderer.java` + `GunSmithTableScreen.java` |
| `resources.model.sprite.TextureSlots` | `renderer.block.model.TextureSlots` | 待 compile 首轮确认 |
| `ItemStackTemplate` | **1.21.11 确认不存在** → `SlotDisplay.ItemStackSlotDisplay(ItemStack)` 直接收 ItemStack | `crafting/PartialNbtIngredient.java` |
| `LightCoordsUtil.pack` | `LightTexture.pack` | `client/model/functional/TextShowRender.java` |
| `Player#sendSystemMessage` | `Player#displayClientMessage(Component, boolean)`（注意 `ServerPlayer`/`CommandSourceStack` 上旧名仍存在，勿误改） | `api/util/LuaEntityAccessor.java`、`client/event/PlayerEnterWorld.java`（共 4 处，均客户端路径） |

### E.【硬】瞄具管线重写（执行中修正版，2026-08-22）

- 1.21.11 **有** `RenderPipeline`/`RenderPipelines`/`DepthTestFunction`/`BlendFunction`
  基础 API（姊妹 1.21.11 定稿 `ScopeRenderTypes` 的 import 实证）；26.1-only 的只有
  `ColorTargetState`/`DepthStencilState`/`CompareOp` 那套状态对象。
- **方案：整体采纳姊妹项目 1.21.11 分支的 scope 包**（`ScopeRenderTypes` 844 行 +
  `ScopeDepthCopyState`/`ScopeNodeSet` + Reticle 渲染器组 + Iris late/final overlay 状态），
  其 `DepthTestFunction.NO_DEPTH_TEST` + `GlCommandEncoderScopeDepthCopyMixin` 里的
  `_enableDepthTest()+_depthFunc(GL_ALWAYS)` 方案就是遗留问题 #1 的定案（**该 mixin
  已在本回合移植完毕**）。配套采纳其 1.21.11 版的 `IrisCompat`（legacy/newly 分层）+
  2 个 Iris mixin + scope 消费方（`BedrockGunModel`/`BedrockAttachmentModel`/
  `AttachmentRender`/`MuzzleFlashRender`/`GunItemRendererWrapper`/`ShaderCompat`/
  `GunModClient`）——这是 WP-12111-3 的主体，**NeoForge 面逐文件改写，Fabric 面不抄**。
- 原 26.1.2 `ScopeRenderTypes` 用 `CompareOp.ALWAYS_PASS` ×3（316/334/351 行）随替换消失。
- 验收不变：GPU 实机矩阵 + 准星缺失/闪烁专测（正解是补 depthFunc，不是换枚举）。

### F.【策】爆头判定盒 F3+B 冲突（姊妹项目 R2-hotfix 同款病）

`client/event/RenderHeadShotAABB.java` 经 `submitCustomGeometry(RenderTypes.lines(), …)`
+ `setLineWidth(2.5F)` 画判定盒——1.21.11 无该入口，且与 F3+B 同时开启会崩
（姊妹项目 R2-hotfix 已实证并修复：改在原版 `EntityHitboxDebugRenderer#showHitboxes` 的
GizmoCollector 里发黄色 `Gizmos.cuboid`）。**默认直接采用其修复方案**，决策只需确认
进 R0 还是作为首个 hotfix。

### G.【硬】mixin 目标与注入签名全量重验（姊妹项目「编译通过 ≠ 运行期安全」五次前科）

25 个已注册 mixin/Accessor / 4 配置，vanilla 目标是混淆纪元——**移植姊妹项目
`docs/verify_mixin_targets.py` 到本仓库**（4 类检查：方法名 / 描述符 / @At 目标 /
handler 形参前缀匹配，沿超类链查找；javap 数据源换成 MDG 的 1.21.11 映射 jar）。
姊妹项目该脚本已抓出 2 处「编译期只 warning、启动即炸」的真实错误，其中：

- **`CameraMixin` 需要重构，不是改名**：26.1 把 FOV 计算搬进 `Camera`；1.21.11 的
  `Camera` 没有 `calculateFov`/`calculateHudFov`/`update`——FOV 在
  `GameRenderer#getFov(Camera, float, boolean)`（true=世界、false=手部），相机角度入口是
  `Camera#setup(Level, Entity, boolean, boolean, float)`。影响 ADS 缩放与后坐/晃动（核心手感）。
- `GameRendererMixin` 的 `renderItemInHand`/`bobHurt`/`bobView` 形参表已变
  （姊妹项目已给出三方法的 1.21.11 javap 签名，照其表核）。
- 其余已知目标：`SoundManager$Preparations`（26.x 名）、`SoundEngine`（LR 耳鸣）、
  `MouseHandler`/`AbstractButton`/`Screen`（GUI 族 B 连带）、`ItemInHandRenderer`/
  `ItemInHandLayer`/`PlayerModel`/`HumanoidModel`（**后者未注册、永久废弃，保持不注册**）、
  `ServerGamePacketListenerImpl`、`StairBlockAccessor`、`LanguageMixin`——全部进脚本核对。
- mixin AP 对**不带描述符**的注入目标只发 warning 不报错——姊妹项目原话：「名字写错照样编译
  通过，直到启动才炸」。**每改一次 mixin 跑一次核验脚本**。

### H.【硬】AT 逐条重验（6 条）

| 26.1.2 条目 | 1.21.11 状态（姊妹项目 AW 已 javap 核实同一批目标） |
|---|---|
| `RenderType <init>(String, RenderSetup)` | ✅ **描述符完全一致，保留** |
| `MultiPlayerGameMode#ensureHasSentCarriedItem` | ✅ 存在 |
| `Minecraft#startUseItem` | ✅ 存在 |
| `LivingEntity#jumping` | ✅ 存在 |
| `Player#canCriticalAttack` | ✅ 存在（且仍是 private，AW/AT 依然必要） |
| `RenderPipelines#register` | ❌ **26.x 专属，1.21.11 无此类**——随瞄具管线重写（E）删条目 |

姊妹项目 AW 的 5 个目标与本仓库 AT 前 5 条**同名同签名**，回移时照表执行即可；
**AT 签名失配是启动即炸且报错难读的故障**，仍要对 1.21.11 jar 逐条 javap 复验（别直接抄表）。

### I.【验】shader 资源核验（姊妹项目黑屏事故同类项）

- 本仓库 `assets/tacz/shaders/core/` 三个 fsh 的 `#moj_import` 只有
  `minecraft:fog.glsl` 与 `minecraft:dynamictransforms.glsl`——**两者都在 1.21.11 的
  8 个 include 文件清单内**（animation_sprite/chunksection/dynamictransforms/fog/globals/
  light/matrix/projection），无姊妹项目黑屏事故里的 `sample_lightmap` 悬空 import。
  但 GLSL 正文（attribute/uniform 命名、矩阵来源）是 26.1.2 纪元写的，**必须**逐行对
  1.21.11 原版 shader 核验（借用其 `docs/verify_shader_imports.py` 的思路）。
- model JSON 层：NeoForge 发布页点名 `neoforge_data` 的 `block_light`/`sky_light` 键在
  1.21.11 失效、改认 `light_emission`——**全树 3754 资源 grep 一遍这两个键**。

### J.【硬/验】JSpecify 空注解迁移（102 文件）

NeoForge 21.11 发布页把 JSpecify 切换列为 1.21.11 的迁移项。本仓库 26.1.2 基线有
**102 个文件用 `javax.annotation.Nullable`**、0 个 jspecify。执行时先确认 1.21.11 编译
classpath 上 `javax.annotation` 是否仍在（在 → 可缓迁；不在 → 机械替换
`org.jspecify.annotations.Nullable`，注意 type-use 位点：`Map.@Nullable Entry`、
数组前/后置语义不同——发布页给了三个例子，照抄到 records）。无论哪种，**先写一条决策记录**。

### K.【验】数据组件 / 配方 / 注册层

- `DataComponents` 常量族：姊妹项目全程未出现组件类错误族——同一纪年（1.20.5 起）推定存活，
  compile 首轮兜底，抽查 `init/` 与 item 层。
- 配方层：姊妹项目族 11 点名 `Recipe#assemble` 补回 `HolderLookup.Provider` 形参、
  `RecipeSerializer` 在 1.21.11 是**接口**（`new` 改匿名实现）——本仓库
  `crafting/GunSmithTableSerializer.java` + `RecipeCompat.java` 对表核。
- `SlotDisplayContext.fromLevel`：1.21.11 存在 ✓（姊妹项目 REI 适配用的正是它）；
  本仓库 `GunSmithTableScreen` / `GunSmithTableCategory` 两处不用动。

### L.【策】Iris 兼容（默认关闭，同姊妹项目）

`tacz.iris.mixins.json` 的 `IrisDepthRestoreShaderMixin` 注入 26.x Iris 内部
`ShaderCreator`——1.21.11 的 Iris（NeoForge 侧）内部不同，且姊妹项目 1.21.11 的定案就是
**关闭 Iris 模块**（sourceSet 排除 + mixin 配置清空，`IrisCompat` 门面保留编译）。
本仓库默认同案：关闭 + 门面保留；若发起人要求开，需先核 Iris 1.21.11 NeoForge 构建的
内部符号再定（高风险，不推荐放 R0）。

### M.【策】可选兼容矩阵重新钉版（姊妹项目阶段 1 的 NeoForge 版）

26.1.2 的全部 CurseMaven file id 作废。以下为姊妹项目 1.21.11 已核的**参考值**，
NeoForge 1.21.11 变体需执行时对活动仓库逐项重解析（拿到 200 才算数）：

| 集成 | 26.1.2 现状 | 1.21.11 参考（Fabric 值仅作版本线参考，NeoForge 重钉） | 风险 |
|---|---|---|---|
| JEI | 29.29.0.77 | 27.x 线（姊妹项目 27.23.0.71）→ NeoForge 变体 | 版本线回退，API 重新核 |
| REI | curse 8271478 | 21.11.816 → NeoForge 变体 | 同上 |
| Cloth | 26.1.154 | 21.11.153 → NeoForge 变体 | API 相对稳定 |
| PAL | curse 8454167（1.2.5+26.1） | **1.1.9（向下跨版本，API delta）** → `compat/playeranimator/**` 重新核符号 | 高，姊妹项目已标注 |
| Controllable | curse 7943194（0.26.0） | **0.25.7 线（向下跨版本）** → NeoForge 1.21.11 构建重钉 | 高，API 很可能不同 |
| Shoulder Surfing | curse 8596489（5.0.10） | 1.21.11-5.0.10 → NeoForge 变体 | 中 |
| Iris | 见 L | 关闭（默认） | — |
| CarryOn | 3 个 mixin（`required:false` + plugin） | 姊妹项目 R2 做过完整兼容（`docs/CARRYON_COMPAT.md`） | 【策】默认 R0 沿用 26.1.2 现状（plugin 开关），其 R2 兼容工作留二期回哺 |
| 附：Zoomify / AR / KubeJS 等 | 26.1.2 即关闭/禁用 | 保持关闭 | 文案照 AGENTS §2 口径 |

### N.【验】NeoForge 加载器习语平移清单（MUKSC 骨架 × 21.11 sources）

- `PayloadRegistrar` / `CustomPacketPayload` / `StreamCodec`：**39 文件**在 payload 体系上；
  21.1.x 有 PayloadRegistrar（MUKSC 同代），逐条对 21.11 sources 重验。
- `DeferredRegister` / 事件面 / `GuiLayer`：17 文件 DeferredRegister——1.21.x 纪元存在，
  但 **21.9 的 Transfer Rework**（官方 changelog #2663）与本仓库 19 条 transfer
  `forRemoval` 调用（26.1.2 记录在案）必须逐个对 21.11 sources 重验。
- `neoforge.mods.toml`：对 MDK-1.21.11 键位（§1）。
- jarJar：`commons-math3` / `luaj-jse` 两个本地 jar 的流程保留，字节码版本见 A。

### O.【策】LRTactical 内置层回移（基线 R1 已含，不砍）

- 2 个 mixin：`GuiGraphicsExtractorMixin`（目标改名，B）、`SoundEngineMixin`（G）。
- 网络 5 条消息无 ItemStack 字段（EMPTY 纪律天然过）；payload 通道 `lr1` 平移（N）。
- **已知缺口（姊妹项目 R12 已修复、本仓库未修）**：爆炸数据 `screen_shake_time/amplitude`
  在本仓库只有数据（`GrenadeEntity`/`ExplodeThrowableData` 等）**没有生效载荷与相机回调**——
  姊妹项目 R12 用范围限定 S2C payload + 客户端 tick 衰减 + 视觉层补齐。**默认列为回移后的
  首个 R 序列候选**（决策：R0 带上或 R1 补）；`destroy_multiplier` 仍按 R12 结论不粗暴实现。
- `SimpleParticleType` 构造器问题（26.2 才变 protected）**不适用 1.21.11**，LR 粒子不动。

---

## 5. 建议的工作包切分

沿用「每包一份 records 证据文档 + 专用服务端冒烟 + 每包 CHANGELOG 草稿」的节奏，
命名 `WP-12111-x`（与姊妹项目阶段 1/2 的方法论对齐）：

| 包 | 内容 | 验收 |
|---|---|---|
| WP-12111-0 前置闸门与证据 | §1 全部闸门：NeoForge 21.11 release 构建、MDK 对齐、parchment/mixin AP 方案、vendored jar 字节码、兼容矩阵可解析性预检（各取 200）、基线 commit 钉死；`docs/records/PORT_12111_GATES.md` | 每项有证据记录，无代码改动 |
| WP-12111-1 构建骨架 bump | `gradle.properties`/`build.gradle`/mods.toml/Java 21/compatLevel/AT 首轮/映射接线；`mod_version=1.1.8+neoforge.1.21.11.R0` | `./gradlew help` 过；`runServer` Mod List 可见 `tacz`；映射 jar 可 javap（姊妹项目阶段 1 的 NeoForge 版） |
| WP-12111-2 非渲染编译修复 | §4 B/D/J/K/N（GUI 族、包迁移、jspecify、配方单点、payload/事件面）+ mixin 核验脚本移植并首跑 | `compileJava` 0 error；refmap 产出；**专用服务端 L0-L2**（`docs/DEDICATED_SERVER_TEST.md`）`Done` 且枪包扫描数 = 26.1.2 基线 |
| WP-12111-3 渲染层 | §4 C/E/F/G/I（Feature Rendering 逐文件验 + `RenderTypes` 常量、scope 包整体采纳 + ALWAYS 深度方案（已移植 mixin）、爆头盒 Gizmo 修复、shader/GLSL 核验、渲染类 mixin 收尾） | 有 GPU 实机矩阵：开镜/准星/裁剪/黑屏专项 + 单机 L2.5；**瞄具专测不能省**（姊妹项目遗留问题 #1） |
| WP-12111-4 兼容矩阵重验 | §4 L/M：Iris/CarryOn 决策执行、JEI/REI/Cloth/PAL/Controllable/SSR 重新钉版 + 符号核验、重写 `docs/COMPATIBILITY.md` | 逐行用户 PASS 或明确标注「未实测」；全矩阵回归 |
| WP-12111-5 发布 | CHANGELOG 新条目（**新建 `docs/CHANGELOG_1_21_11.md`，仿姊妹项目分支级 changelog 惯例**）、README 版本导航表加 1.21.11 行 + AGENTS §1 全部同步点、`scripts/check_release_consistency.sh --strict`、发布 jar + 源码 | `--strict` 0 退出；枪包 `>=1.1.8` 语义回归 |

姊妹项目实机阶段（其「阶段 5 起」：runClient + mixin 逐族放开 + 工作台/瞄具/LR/Iris/兼容）
全部落入 WP-12111-2/3/4 的验收；**专服 L0-L3 是本仓库基线 R1 的既有纪律，回移验收不得降级**。

---

## 6. 版本号与分支（红线）

- `mod_version` = **`1.1.8+neoforge.1.21.11.R0`** 起步，基于 R1 代码。**`+` 后是 build
  metadata；禁止 `-`**（pre-release 会让枪包 `>=1.1.8` 检查静默失败，宪章 §7.4）。
  姊妹项目 1.21.11 用的是 `1.1.8+fabric.1.21.11.R2-hotfix`——`hotfix` 放 `+` 内的写法照搬。
- 分支策略：为回移**新建分支 `1.21.11`**（与姊妹项目分支命名对齐），26.1.2 基线转
  维护线（只收 bugfix）或就地冻结，由发起人在 WP-12111-1 前定案。
- README 同步点：顶部版本行、§1 环境表、§5 版本约束示例、**「选择你的版本」导航表加
  1.21.11/NeoForge 行**（当前该表 26.1.2/26.2 两行都指向本仓库）；AGENTS.md §0
  「当前单分支」段落需发起人批准后更新（规则文件改动权限）。
- 改版本号跑 `bash scripts/check_release_consistency.sh --strict`；发布前
  `--all` 语义随新分支出现而扩展（脚本本次实测不含硬编码分支清单，执行时确认其
  远端分支枚举行为，必要时先补脚本再发布）。
- CHANGELOG 措辞红线（AGENTS §2）全文有效：禁用 ≠ 修复；未实机复现的崩溃只能写
  「加固」，不能写「修复了崩溃」（姊妹项目 REFAB_BACKPORT_PLAN 同名条目的 NeoForge 版）。

---

## 7. 纪律复述（不重复宪章全文，只列最易犯的）

1. **洁净室**：`tacz-port` jar 碰一下，整段代码作废。
2. **证据**：每条非平凡 API 在 records 文档指认 `类#方法(签名)` + 来源层级。
   1.21.11 是混淆版，javap 反查官方映射就是证据（姊妹项目全程这么做）。
3. **禁抄边界**：姊妹项目抄语义不抄 Fabric API 表面；MUKSC 渲染一行不碰。
4. **「编译通过 ≠ 运行期安全」**：姊妹项目 1.21.11 线有五次前科（mixin 警告掩盖崩溃、
   形参不匹配、shader 黑屏、F3+B 崩、getName 专服崩）——mixin 核验脚本 + 专服 L0-L3 +
   GPU 实机矩阵三件套缺一不可。
5. **不跨包顺手改**；每包结束更新 CHANGELOG 草稿；**不在 README 记进度**。
6. **跨分支复制代码/README 段落逐句核版本号与分支名**（姊妹项目历史事故 ×2：README 自相矛盾、
   「26.x 端口」字样忘改）。

---

## 8. 已知未知（执行时优先消除的情报缺口）

- NeoForge 21.11 自身（loader/事件面）相对 26.1.2 的破坏性变更全貌——尤其 21.9 Transfer
  Rework 之后的 transfer API 形态与本仓库 19 条调用的对应关系；读 21.11 sources + primer 定论。
- `GuiLayer` / `RegisterGuiLayersEvent` 在 21.11 的形态（MUKSC 1.21.1 已证存在，但 1.21.1↔
  1.21.11 之间有 10 个小版本，overlay 注册路径仍要对 21.11 重验）。
- PiP（工作台预览）在 **NeoForge** 21.11 的注册与上下文 API——姊妹项目走的是 vanilla +
  FAPI 的 `SpecialGuiElementRegistry`，我们侧要的是 vanilla 注册表 + NeoForge 事件面，
  没有现成对照，需 javap + vanilla 用法搜索（发布页给的迁移方法论）。
- `javax.annotation` 在 1.21.11 编译 classpath 是否存活（决定 J 的迁移方式）。
- parchment 2025-11 后构建的可用性（§1）。
- 瞄具 `GREATER + _depthFunc(GL_ALWAYS)` 方案在 NeoForge 侧的 mixin 注入点是否与 Fabric 侧
  同构（`GlCommandEncoderScopeDepthCopyMixin` 已在，但 21.11 的 `GlCommandEncoder` 形态要重验）。
- Controllable 0.25.7 / PAL 1.1.9 的 NeoForge 1.21.11 构建是否存在、API delta 多大。
- Iris 1.21.11 NeoForge 构建存在性（仅当发起人推翻 L 的关闭决策时才需要）。

---

## 9. 姊妹项目 1.21.11 分支资产清单（回移时直接消费）

全部在 `q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `1.21.11` 分支（2026-08-22 联网核对）：

| 资产 | 用途 |
|---|---|
| `docs/PORT_1_21_11_PHASE1.md` | 构建层回退 + 146 错误基线 + 11 错误族总表 + 依赖钉版方法 |
| `docs/PORT_1_21_11_PHASE2.md` | 源码编译打通全过程 + 瞄具遗留问题 #1 + 两次 mixin 启动崩溃修复 + shader 黑屏修复 |
| `docs/port-1.21.11-error-families.json` / `docs/migrate_*.py` | 机器可读错误族 + 迁移脚本（GUI 族等） |
| `docs/verify_mixin_targets.py` / `docs/verify_shader_imports.py` | 移植到本仓库的两道启动前闸门 |
| `docs/CHANGELOG_1_21_11.md` | R1→R2-hotfix 全部修复（getName 双端化、F3+B、CarryOn、屏幕震动、弹药查询 API…）——本仓库 R 序列候选清单 |
| `docs/EXPLICIT_GAPS_AUDIT_R12.md` | 空实现审计方法论 + LR 缺口定论 |
| `docs/CARRYON_COMPAT.md`、`docs/AMMO_SOURCE_API.md` | 二期回哺候选 |

**fix-flow 约定（双向）**：姊妹项目 1.21.11 线已修的 vanilla 级缺陷（尤其
`Item#getName` 双端化——本仓库已在 26.1.2 基线修复并专服实证，姊妹项目 2026-08-22 刚修），
回移完成后按「每分支改版本号 → README 同步 → 一致性脚本」纪律持续互相回哺；
跨分支复制时逐句核版本号与加载器（本仓库 AGENTS §5 既有条目）。

---

*调研快照：2026-08-22，由回移调研 AI 联网核实（姊妹项目三条分支文件、NeoForge 官方
发布页、MUKSC 构建文件、NeoForge Maven 版本线）+ 本仓库全树 grep 实测。链接内容与
钉版数字以执行当日实况为准。*

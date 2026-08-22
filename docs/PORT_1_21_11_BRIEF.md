# 移植工单：NeoForge 26.1.2 → 1.21.11（回移植，交接给执行 AGENT）

> **文档性质**：三期移植的工作合同附件。执行 AGENT 开工前必须先完整读
> [`../CHARTER.md`](../CHARTER.md) 与本仓库 [`AGENTS.md`](../AGENTS.md)
> （其全部红线在本工单内继续有效，本文只做修订与补充），再读本文。
> 同时**必须通读姊妹项目 1.21.11 分支的两份执行报告**：
> [`PORT_1_21_11_PHASE1.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/1.21.11/docs/PORT_1_21_11_PHASE1.md) 与
> [`PORT_1_21_11_PHASE2.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/1.21.11/docs/PORT_1_21_11_PHASE2.md)。
>
> **本文的差异清单是不完整的**（调研快照 2026-08-22 + 姊妹项目已实证的踩坑沉淀）。
> 凡本文未列出的差异，以 1.21.11 官方映射 jar（named 命名空间）与 NeoForge 21.11 sources
> 为准——执行 AGENT 应自行 javap、读反编译源，**而不是依赖本文穷举**。
> 凭训练数据记忆写 API = 打回，照旧。

---

## 0. 一句话目标

把本仓库（TaCZ: Renovated，NeoForge **26.1.2**，基线 **R1**——含 LRTactical 内置层，
已过单机 + 专用服务器双重验收）**回移植**到 **Minecraft 1.21.11 + NeoForge 21.11.x
（release 通道）**。是回移植，不是重写：26.1.2 代码库是唯一出发点。
产出 `1.1.8+neoforge.1.21.11.R1`。

姊妹项目（TaCZ_Refabricated_Unofficial，Fabric）**已经把这条路完整走过一遍**
（2026-08-13 完成构建层 + 编译打通，随后发布了 `1.1.8+fabric.1.21.11.R2` 与
`R2-hotfix`）。本工单的核心策略：**复用其经验与错误族，平移其脚本，
自担 NeoForge 侧差异，补上它没跑完的实机验收，并按回哺纪律把结果回馈给它。**

## 1. 现状核对（2026-08-22 已联网核实，开工无需重查）

| 事实 | 结论 |
|---|---|
| 本仓库 `1.21.11` 分支 = `26.1.2` 分支 = commit `d312ae6`，`gradle.properties` 仍是 `minecraft_version=26.1.2` | **分支名已是 1.21.11，内容仍是 26.1.2**。与姊妹 Phase1 起始状态完全同构（其报告第一节原话同款）。无需重新拉分支，直接在现有分支上做 |
| NeoForge maven `21.11` 线已转 release | `21.11.42 / 21.11.44 / 21.11.45` 三个无 `-beta` 版本（**21.11.43 不存在**）。开工日再确认最新无 beta 版本号 |
| MC 1.21.11 是**混淆**版本（26.1+ 才未混淆） | mixin refmap 回归；AT 逐条重验；JDK toolchain **25 → 21** |
| 本仓库 R1 已含 `getName` 走 common 索引修复（`3b19477`）+ 创造标签重建段（`ClientPacketHandlers.onSyncGunPack`） | 姊妹 1.21.11 线的 R3 待办（REFAB_BACKPORT_PLAN 第二节）**在本仓库不需要再做**，验收时直接覆盖 |
| 姊妹 1.21.11 线已发 R2/R2-hotfix，但沙箱 2GB 无显示设备，**`runClient`/`runServer` 一次没跑过** | 它留了两处"必须实机验证"的遗留问题（瞄具深度测试等价性、专服行为）。本线是 NeoForge，**实机验收不能省，结果回哺姊妹** |

## 2. 权威与参考边界

| 源 | 角色 |
|---|---|
| 姊妹项目 [`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) **`1.21.11` 分支** | **本线游戏语义权威 + 移植经验权威**。其 docs 与提交历史是踩坑沉淀，先通读再动手。仍然禁止照抄其 Fabric API 表面 |
| 本仓库 `26.1.2` 分支（R1） | 代码出发点 + 加载器习语权威。NeoForge 写法（PayloadRegistrar / DeferredRegister / 事件面 / jarJar）大体平移，逐个对 1.21.11 sources 重验 |
| 本仓库 `26.2` 分支 | 版本无关修复的上游（可选回哺，见 WP-11211-8） |
| MUKSC/TACZ-1.21.1 | 只参考加载器习语，**渲染一行不抄** |
| CurseForge `tacz-port` jar | **洁净室红线原文有效**：不下载、不反编译、不参考 |

### 姊妹项目可直接复用的资产（全部在 `refab@1.21.11`）

| 资产 | 用途 |
|---|---|
| `docs/PORT_1_21_11_PHASE1.md` | 构建层改造清单 + 依赖钉版表（Fabric 侧；NeoForge 侧做同构核对） |
| `docs/PORT_1_21_11_PHASE2.md` | 11 个错误族的收敛过程、改名对照表、误伤排除名单 |
| `docs/port-1.21.11-error-families.json` + `docs/classify_errors.py` | 错误族分类器，改路径后直接用于本仓库首轮编译日志 |
| `docs/migrate_family1.py` / `migrate_gui_overrides.py` / `migrate_dynamic_item_model.py` | 机械迁移脚本。**必须先按本仓库包结构适配、先跑 dry-run 再落地**（姊妹第 06→07 轮就是在修自己第一版脚本对 6 个 renderer 类的误改） |
| `docs/verify_mixin_targets.py` / `verify_shader_imports.py` | mixin 注点 / shader 引用核对脚本 |
| `docs/CHANGELOG_1_21_11.md` | 1.21.11 线独有的上线后 bug（爆头盒崩溃等），本线**直接预防** |
| `docs/CARRYON_COMPAT.md` | Carry On 适配细节（Fabric 2.9.2；NeoForge 版版本号需自查） |

## 3. 官方情报源（按优先级）

1. **1.21.11 官方映射 jar / javap**（① 级，唯一真理）。MDG 处理混淆版时产物在
   `~/.gradle/` 缓存下（named 命名空间，可直接 javap，类比姊妹 Phase1 第五节给的 Loom 缓存路径）。
2. **NeoForge 21.11.x sources jar**（loader/事件面）+ maven metadata 确认最新 release build。
3. 姊妹项目 `1.21.11` 分支 docs + 提交历史（③ 级：上游踩坑沉淀）。
4. 本仓库既有 records（WP01–WP07、SERVER_TEST_*、LR2_INVENTORY）——验收判据的出处。

## 4. 已核实差异 → 本仓库 touchpoint 映射

> 标注【硬】= 不改必炸（编译或启动）；【验】= 需逐一对 1.21.11 重验，可能存活；
> 【策】= 需要先做技术决策。文件路径相对 `src/main/java/`。

### 4.0 构建层（姊妹 Phase1 已实证的坑，NeoForge 侧对应物）【硬】

| 姊妹 Fabric 侧改动 | NeoForge 侧对应 |
|---|---|
| Loom `-remap` 插件 id + 官方 Mojang 映射回归 | MDG 2.0.144 对混淆版自动处理官方映射与重混淆（产物需确认含 `tacz.refmap.json`，26.1+ 分支没有）。`enable.version = 21.11.45`（开工日核最新）；`disableRecompilation = true` 在 2GB 沙箱维持（本仓库惯例） |
| AW 头 `official` → `named` | 本仓库 AT 文件是官方名书写（1.20.5+ 惯例），但**每一条**都要对 1.21.11 jar 重验（见 4.2/4.3，已有一条必删） |
| Java 21 钉死（非"不足才提升"） | `java.toolchain.languageVersion = 21`；`mods.toml` 若有 java 版本声明同步 |
| mod 依赖改 `mod*` 配置 | 本仓库 `compileOnly` + CurseMaven/Architectury/Shedaniel 坐标**全部重钉**（见 4.5）；jarJar 的两个 vendored 库（commons-math3/luaj）加载器无关，不动 |
| mixin AP / refmap 回归 | MDG 自动接线；验证构建产物 manifest/refmap 存在 |

### 4.1 错误族平移（姊妹 Fabric 实测 146 错 / 11 有效族 → 本仓库预期）【硬】

姊妹 Phase1 的 11 族中，**F1–F5、F7–F9、F11 是 vanilla 层面差异，对 NeoForge 同样成立**；
F6/F10 是 Fabric API 专属，NeoForge 侧按 21.11 sources 自查对应入口。

| 族 | 根因（26.1 → 1.21.11） | 本仓库 touchpoint |
|---|---|---|
| F1 | `GuiGraphicsExtractor`（26.x 对 `GuiGraphics` 的改名）不存在 | LR 的 `me/xjqsh/lrtactical/mixin/client/GuiGraphicsExtractorMixin.java` + GUI 引用。方法级改名照姊妹 Phase2 族1 表（`text`→`drawString`、`centeredText`→`drawCenteredString`、`item`→`renderItem`、`outline`→`renderOutline`、`horizontalLine`→`hLine` 等）；mixin 注点 `itemCooldown` → 1.21.11 的私有 `renderItemCooldown(ItemStack,int,int)`（描述符同，可见性变） |
| F2/F3 | `renderer.state.level.*` 包不存在 | `CameraRenderState` / `QuadParticleRenderState` → `net.minecraft.client.renderer.state.*`（枪口粒子、相机取景相关引用） |
| F9 | `state.gui.pip` 路径变 | `PictureInPictureRenderState` → `net.minecraft.client.gui.render.state.pip.*`（工作台预览 `GunPreviewRenderer` 一带） |
| F4/F5/F8 | `resources.model.cuboid`/`resources.model.sprite` 包不存在 | `ItemTransform(s)`/`TextureSlots` → `net.minecraft.client.renderer.block.model.*` |
| F7 | `BlockModelRenderState` 不存在 | 矿车物品渲染 `submitMinecartContents` 第二参 → 裸 `BlockState`（本仓库用到才改） |
| F6/F10 | Fabric 专属（menu.v1 / FAPI 入口） | 不适用。NeoForge 对应面（菜单/提示组件/粒子注册）对 21.11 sources 自查 |
| F11 | 单点：`ItemStackTemplate` 不存在；`Recipe#assemble` 补 `HolderLookup.Provider` 形参；`RecipeSerializer` 1.21.11 是接口；`LightCoordsUtil.pack`→`LightTexture.pack`；`ItemStack#typeHolder`→`getItemHolder`；`Camera#getCameraEntityPartialTicks`→`getPartialTickTime`；`Player#sendSystemMessage`→`displayClientMessage`（ServerPlayer/CommandSourceStack 上的不动）；`AbstractContainerScreen` 构造器无宽高 + `renderBg` 抽象 | 全仓 grep 对应符号逐条核 |

**姊妹实测"不要改"清单（本仓库同样遵守）**：

- `Identifier`（`net/minecraft/resources/Identifier`）1.21.11 存在——不做全仓替换；
- **保留 SubmitNodeCollector 架构**——1.21.11 有该类；
- `network/**`、payload、StreamCodec **不重写**——姊妹首轮错误文件列表里零出现；
- 机械改名前先过姊妹 Phase2 族4 的**误伤排除名单**：`EntityRenderer#extractRenderState`、
  `SingleQuadParticle#extract`、`SpecialModelRenderer#extractArgument`、`IItemHandler#extractItem`、
  `AccessorSparseUtils#extractIndices` 等名字带 extract 但 1.21.11 真实存在/自有 API 的方法**不动**。

**姊妹 1.21.11 上线后才爆的坑（本线直接预防）**：

- 1.21.11 已删除 `SubmitNodeCollector#submitHitbox` / `ShapeRenderer#renderLineBox`——
  爆头判定盒**改走 `Gizmos`**（其 R2-hotfix 记录在案；对应本仓库
  `client/event/RenderHeadShotAABB.java`，26.1.2 手写 `RenderTypes.lines()`）；
- `Minecraft#pausePartialTick` 在 1.21.11 不存在——若本仓库有对应 accessor 直接删
  （姊妹删的是 `MinecraftAccessor`）。

### 4.2 瞄具 depth-aperture【策】——本线最大的技术决策

26.1.2 基线的瞄具实现整套构建在 26.x 渲染管线上：

- `client/render/scope/ScopeRenderTypes.java`：`RenderPipeline.builder()` +
  `ColorTargetState`/`DepthStencilState`/`CompareOp.ALWAYS_PASS`——**这些类在 1.21.11 全部不存在**；
- `client/mixin/client/GlCommandEncoderScopeDepthCopyMixin.java`：注 `GlCommandEncoder#drawFromBuffers`；
- AT 条目 `RenderPipelines#register`——**1.21.11 无此类，必删**。

姊妹 Phase2 已对 1.21.11 GL 字节码定性（逐字节码核实 `GlCommandEncoder`/`GlConst`）：

- 1.21.11 的 `DepthTestFunction` 只有 `NO_DEPTH_TEST / EQUAL / LEQUAL / LESS / GREATER`，
  **没有 ALWAYS**；`GL_ALWAYS` 只出现在"关深度测试"那一支，而 OpenGL 关深度测试时深度写入一并丢弃
  →「恒通过 + 写深度」无法直接表达；
- 姊妹暂用 `GREATER_DEPTH_TEST`：**depth-cleanup 语义等价**（把更远世界深度写回手部深度之上），
  但**刻线（etched）/可见准星不等价**——位于已写镜面深度之前的准星像素会被丢弃。

**决策（发起人已定案，2026-08-22）：方案 A —— 沿用姊妹 `GREATER_DEPTH_TEST` 思路**：

- 1.21.11 的 `RenderType`/`DepthTestFunction` 模型重写 `ScopeRenderTypes`，
  depth-cleanup 用 `GREATER`，刻线/可见准星退化程度**实机量化**（WP-11211-3 验收判据）；
- 方案 B（自研变通）、方案 C（首发降级）**否决**；若 WP-11211-3 实机量化显示退化不可接受，
  回报发起人重开决策。
- 连带：`GlCommandEncoderScopeDepthCopyMixin` 注点对 1.21.11 javap 重验（类存在但内部方法名需核）。

### 4.3 mixin / AT 全量重验【硬】——混淆版回归

混淆版本上 refmap 是运行期生效的，**目标写错 = 启动即炸或静默失效**。全部 4 份
mixin json（`tacz` / `tacz.carryon` / `tacz.iris` / `lrtactical`）的目标逐一
javap 重验，表格入 records。预期：

| 组 | 1.21.11 预期 |
|---|---|
| 客户端组（GameRenderer/Camera/LocalPlayer/MouseHandler/HumanoidModel/PlayerModel/ItemInHandLayer/ItemInHandRenderer/AbstractButton/Language/SoundManagerPreparations/ScreenAccessor/StairBlockAccessor） | 目标类均存在，但 26.1↔1.21.11 之间 vanilla 有重构，**方法名/描述符逐条核** |
| 公共组（Entity/LivingEntity/Player/ServerGamePacketListenerImpl/ServerPlayNetHandler/ServerPlayer） | 同上 |
| CarryOn 组 | 先钉 CarryOn 1.21.11 NeoForge 构建版本（姊妹 Fabric 用 2.9.2），符号按 `CARRYON_COMPAT.md` 精神适配 |
| Iris 组（IrisDepthRestoreShaderMixin） | Iris 1.10.7 内部 `ShaderCreator` 不存在（姊妹已证）——**保持禁用**至 WP-11211-5 |
| LR 组（GuiGraphicsExtractorMixin / SoundEngineMixin） | 前者随族1 改类名+注点；`SoundEngine` 1.21.11 存在，注点重验 |
| 自有 accessor（LuaEntityAccessor 等） | 不涉 vanilla，编译期兜底 |

AT 六条处置（姊妹已对 vanilla 1.21.11 jar 核过五条）：

- `RenderType <init>(String;RenderSetup)V` ✅ 描述符完全一致，保留；
- `MultiPlayerGameMode.ensureHasSentCarriedItem()V` / `Minecraft.startUseItem()V` /
  `LivingEntity.jumping`（public-f）✅ 存在，保留；
- `Player.canCriticalAttack(Entity)Z` ✅ 1.21.11 是 private，LR 依赖它，保留；
- `RenderPipelines.register(...)` ❌ **删**（随 4.2 方案一并处理）。

### 4.4 NeoForge API 面：21.11 vs 26.1.2【验】——执行时对 21.11 sources 逐条核

- 大概率平移：`DeferredRegister` / `RegisterEvent` 注册窗口 / `PayloadRegistrar`
  （1.20.5 起存在）/ `AddServerReloadListenersEvent#addListener(Identifier, listener)` /
  原生 `ConfigurationScreen` / 本仓库的 `BuiltinItemRendererRegistry` 用法。
- 需重验：GUI layer 注册事件（`RegisterGuiLayersEvent` 等 1.21.x 形态与 26.x 不同名）、
  物品渲染注册路径、`ClientSetupEvent` 类目——以编译错误 + 21.11 sources 为准。
- 26.2 才做的"去 legacy item handler"**不回移**（21.11 两者并存，无需）。

### 4.5 兼容矩阵重钉【硬】——姊妹 Fabric 版已钉，NeoForge 版执行首日对活动仓库逐项核

> **口径（发起人已定案，2026-08-22）：先查后定。** 每项依赖先查 1.21.11 NeoForge 官方构建；
> 无官方构建再查衍生/替代项目；**官方与衍生都没有才挂起**——挂起 = 摘除对应 compileOnly +
> 排除对应源码编译 + `COMPATIBILITY.md` 如实标注「无 1.21.11 NeoForge 构建，已挂起」，
> 不得写成「兼容」（AGENTS §2）。

| 依赖 | 26.1.2 现值 | 1.21.11 预期（姊妹 Fabric 参照） | NeoForge 侧动作 |
|---|---|---|---|
| JEI | `jei-26.1.2-neoforge:29.29.0.77` | 姊妹 Fabric 用 `27.23.0.71` | 查 `jei-1.21.11-neoforge` 实际版本 |
| REI | curse 8271478 | 姊妹用 `21.11.816` | 查 1.21.11 NeoForge file id |
| Architectury | `architectury-neoforge:20.0.6` | 姊妹用 19.0.1 | 查 1.21.11 NeoForge 构建；**若无则移除该 compileOnly** |
| Cloth Config | `cloth-config-neoforge:26.1.154` | 姊妹 Fabric 用 21.11.153 | 查 1.21.11 NeoForge 变体 |
| PAL | curse 8454167（1.2.5） | **1.1.9+1.21.11** merged jar | 已确认 API 差异：`get3DTransform` 1.1.9 返回 `PlayerAnimBone`（1.2.5 返回 void）→ `compat/playeranimator/**` 适配 |
| Controllable | curse 7943194（0.26.0） | 姊妹 Fabric 最新 **0.25.7** | 查 1.21.11 NeoForge 构建；无则**挂起该兼容并如实标注**（AGENTS §2 口径） |
| Shoulder Surfing | curse 8596489（5.0.10） | 姊妹用 `1.21.11-5.0.10` | 查 NeoForge 变体 |
| Carry On / Iris / Sodium | — | 姊妹：2.9.2 / 1.10.7+mc1.21.11 / 0.8.13+mc1.21.11 | 查 1.21.11 NeoForge 版 |
| Zoomify / ImmediatelyFast | no-op（NeoForge 无此 mod / 26.x 无需） | — | 维持 no-op 现状，不新增 |

每个结论写证据（仓库 / file id / 返回码）入 `docs/records/`；不可用项在
`COMPATIBILITY.md` 标"不可用/未实测"，**不得**写成"兼容"。

### 4.6 版本元数据与发布【硬】

```properties
minecraft_version=1.21.11
minecraft_version_range=[1.21.11]      # 精确区间，与 26.1.2 分支同款纪律
neo_version=21.11.45                    # 已核无 -beta；注意 21.11.43 不存在
mod_version=1.1.8+neoforge.1.21.11.R1   # R1 发布版本
```

- **`+` 铁律、禁止 `-`**（pre-release 会让枪包 `>=1.1.8` 检查静默失败，宪章 §7.4）；
- `mod_id` 永远 `tacz`；显示名 `TaCZ: Renovated`；
- README 版本导航表加 1.21.11 行；AGENTS §1 三处 + CHANGELOG 同步；
  `bash scripts/check_release_consistency.sh --strict` 通过才许发布。

## 5. 建议的工作包切分

沿用"每包一份 records 证据文档 + 专用服务端冒烟"的节奏，命名 `WP-11211-x`：

| 包 | 内容 | 验收 |
|---|---|---|
| **WP-11211-0 情报闸门与依赖钉版** | §4.5 全表实测 + MDG/JDK/neo 版本核 + 姊妹 Phase1/2 文档与脚本通读；错误族分类器按本仓库路径适配并跑 dry-run | 证据表入 `docs/records/PORT_11211_DEPS.md`；首轮编译日志可用分类器归族 |
| **WP-11211-1 构建骨架 bump** | gradle.properties 三版本号、toolchain 21、build.gradle 依赖重钉、mods.toml、refmap 确认 | `runServer` 启动，Mod List 可见 `tacz`（1.21.11） |
| **WP-11211-2 编译收敛** | §4.1 错误族逐族平移（先收大头族1，一次一族，每族后重编）；复用姊妹脚本做改名回退检查 | `compileJava` 0 error；构建产物含 refmap |
| **WP-11211-3 瞄具决策与重写** | §4.2 决策（发起人定案）+ `ScopeRenderTypes` 按 1.21.11 `RenderType`/`DepthTestFunction` 重写 + `GlCommandEncoderScopeDepthCopyMixin` 注点重验 + AT 清理 | GPU 实机：变焦/刻线/可见准星/depth-cleanup；决策记录入 records |
| **WP-11211-4 mixin/AT 全量重验** | §4.3 表逐条 javap + 4 份 mixin json 目标核对 | 客户端/专服启动无 mixin 注入失败 |
| **WP-11211-5 兼容矩阵** | §4.5 实施 + PAL 1.1.9 API 适配 + `COMPATIBILITY.md` 重写 | 逐项用户 PASS 或明确标注未实测（AGENTS §2 口径） |
| **WP-11211-6 专服验收 + LAN + 回哺** | `docs/DEDICATED_SERVER_TEST.md` L0–L3 + L2.5 全跑（混淆版专服有五连前科，**编译通过≠运行期安全**）；`/give` 四类物品 + LR 物品；枪包 F3+T 热载；创造标签重建；姊妹 Phase2 遗留的两处实机验证在本线补上 | 记录入 records；**回哺清单发姊妹仓库 1.21.11 分支**（REFAB_BACKPORT_PLAN 惯例） |
| **WP-11211-7 发布** | R1 收版：版本号 + README/CHANGELOG + `--strict` + 三平台发布文案 | 发布 jar + 对应源码 |

**WP-11211-8（26.2 版本无关修复回哺）：发起人已定案不包含（2026-08-22）。**
姊妹仓库有 `BACKPORT_FROM_26_2_APPLIED.md` 成例（补丁 01–07 + 两处符号级适配），
日后若需要，照该模式单独立项。

## 6. 版本号与分支（红线）

- **分支策略**：直接在现有 `1.21.11` 分支上做——它已是从 26.1.2 R1 切出的正确起点
  （与姊妹 Phase1 第一节结论一致）。`26.1.2` 只收 bugfix；`26.2` 不受影响。
- 项目显示名 `TaCZ: Renovated`（modId `tacz` 不变）。
- `mod_version` = `1.1.8+neoforge.1.21.11.R1`，R1 收版。**`+` 后是 build metadata；
  禁止 `-`**。改版本号必须同步 README/CHANGELOG 并通过 `--strict`。
- 该线首发版本号建议 `R1`（与 26.1.2 线语义对齐：含 LR、含联机修复、含品牌物料），
  与姊妹线的 R2-hotfix 内容差异在各自 CHANGELOG 说明。

## 7. 纪律复述（不重复宪章全文，只列最易犯的）

1. **洁净室**：`tacz-port` jar 碰一下，整段代码作废。
2. **证据**：每条非平凡 API 在 records 文档指认 `类#方法(签名)` + 来源层级。
   1.21.11 混淆版要用 named jar javap（缓存路径类比姊妹 Phase1 第五节），不是借口也不是障碍。
3. **禁抄边界**：姊妹抄语义与错误族，**不抄 Fabric API 表面**；MUKSC 渲染一行不碰。
4. **机械迁移先 dry-run**：姊妹的迁移脚本在它自己仓库都误伤过 6 个类（Phase2 第 06→07 轮），
   本仓库适配后先出 diff 清单再落地。
5. **不跨包顺手改**；每包结束更新 CHANGELOG 草稿，**不在 README 记进度**。
6. **回哺纪律**：本线发现的版本无关 bug，同步评估姊妹 1.21.11 / 26.1.2 / 26.2 三线
   （本仓库已有 `docs/records/REFAB_BACKPORT_PLAN.md` 成例）。

## 8. 已知未知（执行时优先消除的情报缺口）

- NeoForge 21.11 面（loader/事件）与 26.1.2 的差异全貌——§4.4 只列了预期，
  以编译错误 + 21.11 sources 为准。
- 瞄具 GL 方案在 NeoForge 1.21.11 的具体可行性（§4.2 决策依赖）。
- 兼容依赖的 1.21.11 NeoForge 构建存在性（Controllable / SSR / CarryOn / Iris /
  architectury 等，首日实测）。
- `GlCommandEncoderScopeDepthCopyMixin` 注点在 1.21.11 的形态。
- 26.1→1.21.11 之间 vanilla 内部重构对 mixin 目标方法名的连带总量（§4.3 重验表的实际规模）。
- MDG 2.0.144 对 NeoForge 21.11（混淆版）的实际行为差异（refmap 接线、AT 处理），
  若异常则按 MDK 对应版本对齐。

---

*调研快照：2026-08-22，联网核实（GitHub API / maven.neoforged.net / 两仓库 raw 文档）。
链接与版本以执行当日实况为准。*

# 更新日志

版本号格式：`1.1.8+neoforge.<mc>.<标签>`。`+` 之后是 SemVer build metadata，
因此枪包的 `tacz >= 1.1.8` 依赖检查照常通过（**禁止**改用 `-`，那是 pre-release，会静默不满足 `>=1.1.8`）。

## 1.1.8+neoforge.1.21.11.R1-hotfix — 2026-09-01 姊妹线同步（版本号未变）

> 姊妹项目 `TaCZ_Refabricated_Unofficial` 的 `arena/01a05db2` 分支 2026-08-30 ~ 09-01 的新增内容，
> 逐 commit 核对后按本线（NeoForge 21.11.45 / 原生 ModConfigSpec / 无 PIP / 无 meshloader）等价移植。
> 完整对照与「不搬清单」见 `docs/records/SYNC_SIBLING_0105DB2_20260901.md`。
> **证据级别：源码级静态闭环 + 姊妹线 CI/javap 旁证；本线 CI 编译门与实机均未跑，按 AGENTS §2 不宣称已修。**
> 姊妹线给本线的同步清单 `SYNC_CHECKLIST_1211_NEOFORGE_SISTER_20260901.md` 的 §2（P0-a）、§3（P0-b）、
> §4（P1）三条已全部落地。

### 镜内 `text_show` 文本（MK5/MK5HD 弹药计数）三连修（P0-a / P0-b / P1）

- **P0-a 补回被绕开的 functionalTasks flush**（姊妹线 `1cfa42b` + `cb39564`）：
  `BedrockAttachmentModel#submit` 的孔径路径自己重放几何、没走 `super.submit(...)`，
  于是快照里冻结的 `submitText` 任务无人 flush，镜内文字一帧都不画。
  `BedrockRenderSnapshot` 加只读 `functionalTasks()`；瞄具序列在 depth-cleanup 之后对
  `bodySnapshot`、每个 `ocularSnapshots`、`ocularRingSnapshot` 各 flush 一次（非延迟走
  collector 默认 order(0)，落在 cleanup(-1) 与准星(1) 之间）；`deferReticleToIrisFinalOverlay`
  时经 `ScopeFinalOverlayState.queueFunctionalTask` 与准星/镜框同族推迟、在 reticle 之前用
  `task.submit(submitNodes)` 提交（`OrderedSubmitNodeCollector` 不是 `SubmitNodeCollector`）。
  非镜内序列的 flush 放在 `!bodySnapshot.isEmpty()` 门**外**（`isEmpty()` 只看几何），
  `ocularRingSnapshot` 的任务也一并兜底 —— 这两处是姊妹线相对 26.1.2 那版补丁的差别，照收。
  另加每局一次的日志判据（`Flushed N in-lens text task(s) ... scopeMask=...`）。
- **P0-b 纯查表**（姊妹线 `c9b8ba1`，对齐 26.2 `ec51f556`）：
  `PapiManager#getTextShow` 与 `ClientAttachmentItemTooltip` / `ClientBlockItemTooltip` 不再走
  `I18n.get`（那是「查表 + `String.format`」的格式化接口，枪包把 `textKey` 写成含 `%` 的内联串时
  返回 `"Format error: ..."`），改用 `Language.getInstance().getOrDefault(...)` 纯查表。
- **P1 镜内文字按目镜孔径掩码裁剪**（姊妹线 `d076cf5`，等价 26.1.2 `e1c550ee`）：
  新增 `ScopeTextSubmitter`（`Font#prepareText → PreparedText#visit` 按 `TextRenderable#textureView()`
  分组，每组一次 `submitCustomGeometry` 用新的 `ScopeRenderTypes.maskedText(pageId)`，字体页用
  `AbstractTexture` 壳纹理 + 每帧刷新指向）+ 新着色器 `shaders/core/scope_text_final.fsh`
  （本代 `rendertype_text.fsh` 克隆 + `tacz_ScopeMaskMode` 孔径比较，故意不含 fog）。
  `ScopeDepthCopyState` 只加 `isMaskCycleValid()`；`TextShowRender` 加 `clipToScopeMask` 旗与四参构造
  （三参重载保留，枪包注册面不变）；瞄具侧传 `true`、枪身侧显式 `false`。
  失败语义：掩码不可用 ⇒ 回退 vanilla `submitText`，不丢字、不画错，最差回到「贴边溢出」。

### 同步的其余实质改动

- **检视打断动画两修**（姊妹线 `1c765c6`，逐字移植 26.2 `4aa8d7b` + `12d6f3c`；本线三份动画文件
  与姊妹线改前基线逐字节相同，移植为零差异）：`stopAnimation(track)` 连坐停在 `transitionTo` 上的
  runner（修「开镜检视不可打断」）；`AnimationStateMachine#trigger` 用全局出生序号快照豁免
  「本次 trigger 刚启动的后继动画」（修第一修引入的「检视中换弹被误杀」回归）。
- **`tacz:nbt` 跨包材料**（姊妹线 `e1aad10` 第 1 件，按本线原生 NeoForge Ingredient API 等价改写）：
  枪包升级工具把旧配方批量转成上游 1.21.1 的 `tacz:nbt`（`items` 常写成单个字符串、`partial` 布尔），
  本线此前不认该类型、整条配方解析失败。`RecipeCompat.normalizeCustomIngredient` 现在把 `tacz:nbt`
  改写为已注册的 `neoforge:ingredient_type=tacz:partial_nbt`（`strict = !partial`，缺省 strict），
  `items` 字符串→数组；旧的无 type `{item+nbt}` 写法不再**静默丢弃 nbt**，改写为 partial 语义；
  解析失败的 WARN 现在带上规范化形态 + 原文，并catch `LinkageError`。
  （姊妹线第 2 件「开镜 mesh 距离闸门按角尺寸补偿」依赖其 meshloader/PIP，本线无此项，不搬。）
- **`/tacz overwrite` 落盘**（等价姊妹线 `cd14a2a` 第 4 项）：命令行开关绕过了 Cloth 面板的
  savingRunnable，改完重启回默认；现在 `PreLoadConfig.spec.save()` 显式落盘
  （本线原生 NeoForge `ModConfigSpec#save` = `loadedConfig.save()`，姊妹线自建的 ConfigPersist
  原子合并写那套是 FCAP 专用，本线不需要）。
- **lang 补齐**（对应姊妹线「恢复完整 lang」）：补 `attribute.name.tacz.bullet_resistance` 与
  `commands.tacz.arguments.enum.invalid` 两条（前者被 `ModAttributes` 实际引用，此前属性名显示原始键）。

### 明确不搬（对照姊妹线，理由见 records 文档）

- 全部 `ScopePip*`（PIP 二次渲染/重渲染/时域隔离/显示阈/倍率渐变/窄遍守卫等）：本线 1.21.11 无 PIP；
- 全部 meshloader / TML GPU / poly_mesh 项（含法线矩阵读取时刻、纹理懒加载、EMISSIVE 降级等）；
- 镜内裁手（`8ddde34`/`3918049`）：依赖 PIP 镜内区域与 `ScopePipMinMagnification`；
- FCAP 配置落盘整族（`7f3cfd5`/`cd14a2a` 的 1-3 项/`630b0a0` 的 ModMenu 出口）：本线无 FCAP/ModMenu，
  且 Cloth 面板早已接 `setSavingRunnable`；
- 掩码周期帧戳（`8d28e57` 的 `onClientFrameStart`/`hasMaskCycleThisFrame`）：其消费者是 mesh 手部
  裁剪闸与 PIP 合成闸，本线均无；镜内文字按姊妹线口径保留跨帧 `isMaskCycleValid` 语义；
- Iris 二次渲染的 Sodium/Voxy 通道（Fabric 专属 mod）；姊妹线 `.github/workflows` 与 CI 探针（TEMP）。
- 版本号**未动**（仍 `1.1.8+neoforge.1.21.11.R1-hotfix`）⇒ README 无需跟改。

## 1.1.8+neoforge.1.21.11.R1-hotfix — 2026-08-27

### 长按右键的「幽灵使用」与耳鸣资源（同步姊妹项目 2026-08-27 跟进）

- **长按右键不松手时进度条再读一次 / 姿势定格**：根因是原版输入循环在「使用结束」后
  的下一 tick 自动重新 `startUseItem`（对原版食物是特性，对 LR 有使用时长的物品是 bug）。
  新增 `UsePressGate` + `MinecraftUseRestartMixin`：一次按压只消耗一次使用，
  仅在「右键仍按着、刚用完的是 LR 物品、手里还是同一件物品」三个条件同时成立时
  拦下自动重开；松手即解锁，不影响连点投掷。纯客户端、无反射。
- **`use()` 两端都查冷却**：`ThrowableItem#use` / `ConsumableItem#use` 不再只查服务端，
  改用 `ModCapabilities#coolDowns` 按端返回的 `SERVER_COOL_DOWNS` / `CLIENT_COOL_DOWNS`
  两端各查一次，修掉「服务端在冷却、客户端却 startUsingItem → 读了个空条」的分叉。
  服务端仍是唯一权威（真正投出仍由 `releaseUsing` 服务端判定）。
- **`StuckUseRecovery` 兜底**：客户端若陷进服务端不存在的使用状态，越过
  「最长预燃 + 20 tick 延迟余量」就本地 `stopUsingItem()`（不是 `releaseUsingItem()`，
  那会真的把手雷扔出去）。只处理可预燃且 `life_time > 0` 的投掷物。
- **耳鸣声资源补齐**（此前三条 NeoForge 线全缺、效果图标为紫黑块）：
  - 新建 `assets/lrtactical/sounds.json`（顶层无反序列化注解键），
    音源 `sounds/stun_ringing.ogg`、效果图标 `textures/mob_effect/deafened.png` /
    `blinded.png`；
  - `DeafenState#tick` 接住 `SoundManager#play` 的 `PlayResult`，非 `STARTED` 时
    WARN 一次并把三个已知坑写进消息，避免「耳鸣声听不见但日志一无所有」。
- 本线**未改**耳鸣消声注入点（`SoundEngineMixin` 仍注入
  `SoundEngine#calculateVolume(SoundInstance)`）：用户实测 1.21.11 消声生效，
  与 26.x 的引擎行为不同，不应照搬 `AbstractSoundInstance#getVolume()` 改动。
- 新增 `scripts/verify_lr_assets.py` / `scripts/gen_effect_icons.py`，
  可用 `python3 scripts/verify_lr_assets.py --strict` 自检资源。
- **未实机**：本环境无 JDK/MC，上述均为源码级闭环，须按共用核心 §6 实机清单回归。

### 兼容性与修复（同步 26.2 最新提交）

- 安装 Punchy! 时右手脱离枪身、枪+手臂整体摆幅过大：按姊妹项目语义接入可选 mixin，
  持枪期间让 Punchy 的独立手臂与位移矩阵让出给 TACZ 第一人称状态机。
- 投掷物静止拉栓反复抖动：官方手雷脚本用字面量 `idle` 表示取消拔销，移植层却把近战
  专用的 `INPUT_IDLE` 每 tick 打给投掷物，两者撞名。位移 tick 改回只驱动近战。
- 跟官方 0.4.3 能跟的契约：烟雾粒子改采环境光（邻格回退、最低 2，不再全亮
  `0xF000F0`）；可预燃投掷物在手上炸改为 `prepare + 完整 lifeTime`；display
  增加 `display_offset` / `entity_transform`；消耗品补 `ConsumableItemRenderer`
  与 display 通道。tooltip 自定义描述本仓已有，未改。未实机。
- 可预燃满进度后 `life` 被夹到 0：实体 tick 改为 `life >= 0` 才超时引爆，
  `0` 当帧炸，C4 `-1` 仍不超时。未再被用户打回。
- 跨仓审计与负结果入档：`docs/records/SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md`、
  `docs/records/LR_043_FOLLOWUP_20260826.md`。

## 1.1.8+neoforge.1.21.11.R1 — 2026-08-22

### WP-11211-5a 光影下准星被云/粒子覆盖的修复（用户实机反馈）

- **修复**：开启 Iris 光影时准星被云、雾与药水粒子覆盖——根因是准星颜色在
  HAND_TRANSLUCENT 阶段烘焙进 gbuffer，而 shaderpack 更晚的 composite/final
  阶段会重画这些元素盖在其上。平移姊妹 1.21.11 分支的 R8/R9/R11 机制：准星/镜框
  快照延迟到 Iris 全部 composite/final pass 之后绘制（Iris 1.10.7 走
  `IrisRenderingPipeline#finalizeLevelRendering` TAIL 的 final-overlay，其余版本回退
  HAND_TRANSLUCENT 晚交），配套 6 条新管线、2 个新 Iris mixin（注点已对 1.10.7 jar
  javap 复核）、no-fog final shader 与专用世界深度副本；
- 镜框/准星绘制顺序修正为上游「先准星后镜框」（修复准星溢出镜框贴边的隐患）；
- 无光影 / 无 Iris / 非 1.10.7 时全部失效为原版即时路径，行为不变。

### WP-11211-4 客户端 mixin 注点审计（用户实机崩溃驱动修复）

- **修复**：`GameRendererMixin` 三处注点签名漂移——1.21.11 的
  `renderItemInHand(float, boolean, Matrix4f)` / `bobHurt(PoseStack, float)` /
  `bobView(PoseStack, float)`（26.1.2 是带 CameraRenderState/Matrix4fc 的旧签名）；
- **修复**：`CameraMixin` 注点 `update(DeltaTracker)` → `setup(Level, Entity, boolean,
  boolean, float)`（1.21.11 无 update 方法，javap 实证）；
- 全部 4 份 mixin 配置逐条 javap 审计（`docs/records/PORT_11211_MIXIN_AUDIT.txt` +
  COMPILE_RECORD 第六节），其余注点全部兼容；Iris 1.10.7 上 iris mixin 惰性安全、
  IrisCompat 反射全程 fail-safe；Carry On 未装时三 mixin 静默跳过。

26.1.2 R1 → Minecraft 1.21.11 回移植，进行中。工作包 WP-11211-x，工单
`docs/PORT_1_21_11_BRIEF.md`，执行台账 `docs/records/PORT_11211_DEPS.md` 起。

### WP-11211-1 构建骨架

- gradle.properties：bump 至 MC 1.21.11 + NeoForge 21.11.45（与官方
  MDK-1.21.11-ModDevGradle 钉版一致，maven metadata 实证 21.11.43 不存在）；
- JDK toolchain 25 → 21（1.21.11 随游戏发行 Java 21）；
- 依赖重钉：JEI 27.30.0.76、REI 21.11.816（转 maven.shedaniel.me 坐标）、
  Architectury 19.0.1、Cloth Config 21.11.153；PAL 1.1.9 / Controllable 0.25.8 /
  Shoulder Surfing 5.0.10 的 1.21.11 NeoForge 构建入 `libs/`（escape hatch，
  缺失即构建失败并给出下载指引）；
- mods.toml 展示元数据改 1.21.11；mixin compatibilityLevel：carryon
  JAVA_25 → JAVA_21（1.21.11 运行于 Java 21）。
- 沙箱适配：NeoForm 外部工具 JVM 需 `JAVA_TOOL_OPTIONS` 封顶（cgroup v1 OOM，
  gradle.properties 已注释）；run 配置堆 448M。

### WP-11211-2 编译收敛（完成）

- 6 轮收敛 100 → 0 错误：GuiGraphicsExtractor 改名族、GUI 覆写族
  （extract*→render*，含 renderer 误改回退）、包迁移族、动态物品模型三处
  接口差异、RecipeSerializer 接口化、瞄具管线改写（决策 A：GREATER_DEPTH_TEST +
  NO_DEPTH_TEST reticle + encoder mixin 强制 GL_ALWAYS）等；
- 专服冒烟：MC 1.21.11 + NeoForge 21.11.45 `runServer` Done (0.848s)，
  tacz/LR 注册、payloads、枪包导出、mixin/AT 运行期生效；
- 证据：`docs/records/PORT_11211_COMPILE_RECORD.md`、`PORT_11211_DEPS.md`。
- 已知遗留（下包）：瞄具 GPU 实机量化（WP-11211-3）、剩余 mixin 注点逐条复核
  （WP-11211-4）、兼容矩阵实施与 COMPATIBILITY.md 重写（WP-11211-5）。

## Unreleased

### 兼容性与修复（同步 26.2 最新提交）

- 安装 Punchy! 时右手脱离枪身、枪+手臂整体摆幅过大：按姊妹项目语义接入可选 mixin，
  持枪期间让 Punchy 的独立手臂与位移矩阵让出给 TACZ 第一人称状态机。
- 投掷物静止拉栓反复抖动：官方手雷脚本用字面量 `idle` 表示取消拔销，移植层却把近战
  专用的 `INPUT_IDLE` 每 tick 打给投掷物，两者撞名。位移 tick 改回只驱动近战。
- 跟官方 0.4.3 能跟的契约：烟雾粒子改采环境光（邻格回退、最低 2，不再全亮
  `0xF000F0`）；可预燃投掷物在手上炸改为 `prepare + 完整 lifeTime`；display
  增加 `display_offset` / `entity_transform`；消耗品补 `ConsumableItemRenderer`
  与 display 通道。tooltip 自定义描述本仓已有，未改。未实机。
- 可预燃满进度后 `life` 被夹到 0：实体 tick 改为 `life >= 0` 才超时引爆，
  `0` 当帧炸，C4 `-1` 仍不超时。未再被用户打回。
- 跨仓审计与负结果入档：`docs/records/SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md`、
  `docs/records/LR_043_FOLLOWUP_20260826.md`。

### 品牌

- 新增本仓库原创 `icon.png` / `logo.png`（青色四段瞄具环 + 铜色 R），并写入
  `neoforge.mods.toml` 的 `logoFile`。未使用官方 TaCZ 图标（CC BY-NC-ND 4.0
  禁止再创作），也与 Fabric 姊妹项目那套官方原图区分开。

## 1.1.8+neoforge.26.1.2.R1 — 2026-08-22

首个发布版。三条战线在同一版本收口，全部经用户实机验收
（LAN 双轮 + 专用服务器 L2/L3 + 枪包专项 + LR 单机/专服专项，
records/SERVER_TEST_*、records/LR2_INVENTORY.md 全程台账）。

### 更名

- **项目更名：TaCZ: Renovated**（原"TaCZ NeoForge 26.1.2（非官方移植）"）。
  只改显示名，**modId 仍为 `tacz`**，枪包兼容不受影响。
  决策记录：`docs/records/NAMING_DECISION.md`。

### 新增：LRTactical 内置层（WP-LR2）

- throwable/melee/detonator/consumable 四类基础物品、五类投掷行为
  （explode/sticky/smoke/stun/effect-cloud）、数据装载与网络同步
  （独立载荷通道 `lr1`）、反馈层（三类 tooltip / 使用进度 HUD / 分类冷却遮罩）。
- 依赖 `lrtactical` 的内容包**完整可用**。
- 范围界定：flash_shield 未含（独立子系统 + ARR 美术，与姊妹项目同边界）；
  原作美术零打包，道具模型/贴图由内容包提供；无内容包时显示原版占位模型。
- 实现要点：LR init 包全量 DeferredRegister 重写（根治 WP07 A 类注册时序坑，
  当年 r30 的未定位启动崩溃未复现——E-13 闭案）；本仓补齐 NeoForge 路径必需的
  `items/*.json` 与 `particles/smoke_cloud.json`（本仓实现所需，非上游缺陷）。

### 修复（联机战役，Beta-1 → R1）

- **专服致命**：四个物品类（枪/弹药/配件/工作台）的 `getName` 覆写调用 client 索引，
  `/give` 等服务端路径触发即 `NoClassDefFoundError` 崩服（26.1 起 NeoForge 不再按
  `@OnlyIn` 剥离成员，上游祖传写法失效）。改走 common 索引。
- **联机致命**：`ServerMessageGunDraw` 空 ItemStack 编码崩溃——加入/空手切枪把视野内
  所有玩家踢下线。改 `ItemStack.OPTIONAL_STREAM_CODEC`（上游 1.21.1 与 refab 同款）。
- **联机功能**：RECIPE_FILTER 与 ATTACHMENT_TAGS 漏出网络同步包——联机客户端方块索引
  全部解析失败（工作台不可用）、配件允装判断静默失效。接回 `registerNetwork`。
- **Iris**：Iris 1.11.3 已自动分类 entity 管线时不再误报 WARN，保留 Iris 分类。
- **构建**：mods.toml 模板注释中的字面量 dollar-brace 炸毁 `generateModMetadata`。

### 文档

- 文档体系对齐姊妹项目规范：README 重写、`AGENTS.md`、一致性自检脚本、
  专用服务器测试预案（`docs/DEDICATED_SERVER_TEST.md`，L0-L4 + L2.5 枪包专项）。
- 联机枪包指引：双端安装职责、服务端 `/tacz reload`、客户端新增包按 F3+T 重载。

### 已知事项

- Fabric 姊妹项目的 getName 模式待其作者顺手核查（NeoForge 侧已实证，Fabric 未实测）。
- 面板服/代理网络/混合服未测试（L4 矩阵在案，非阻塞项）。

## 1.1.8+neoforge.26.1.2.Beta-1 — 2026-08-21

首个稳定基线。此前的 r0–r30 为开发迭代，历史见 git log，各阶段验收证据见 `docs/records/`。

### 包含

- 完整枪械玩法：物品/方块/配方注册、网络同步、弹道、枪包装载
  （默认枪包：枪械 54、弹药 24、配件 99、方块 4、配方 182）
- 渲染：depth-aperture 瞄具、第一人称 Feature Rendering、工作台 BER
- 可选 Iris 光影兼容（无 Iris 时不加载任何相关代码）
- 可选 mod 兼容：Cloth Config、Player Animation Library、Controllable、
  Shoulder Surfing Reloaded、JEI / REI、Carry On、FirstPerson Model
  （矩阵见 `docs/COMPATIBILITY.md`）

### 相对 r30 的变更

- **移除**：LRTactical 内置框架（r26 立项，三轮修复后仍有未定位的启动崩溃，撤回；
  决策与踩坑记录见 `docs/records/WP07_LRTACTICAL_PLAN.md`）。
  枪包依赖检查对 `lrtactical` 的软放行保留。

### 已知问题

- 依赖 `lrtactical` 的枪包：枪械可用，LR 道具（近战/投掷/引爆器/消耗品）不可用。
- 启动日志中原版 `RecipeManager` 对工作台配方有 `empty ingredients` 警告——无害，
  实际合成走 mod 内部管线。

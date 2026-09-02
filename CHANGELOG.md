# 更新日志

版本号格式：`1.1.8+neoforge.<mc>.<标签>`。`+` 之后是 SemVer build metadata，
因此枪包的 `tacz >= 1.1.8` 依赖检查照常通过（**禁止**改用 `-`，那是 pre-release，会静默不满足 `>=1.1.8`）。

## 1.1.8+neoforge.1.21.11.R2 — 2026-09-02

> R2 = R1-hotfix 之后回传姊妹 1.21.11 线（`arena/01a05db2`）08-30~09-02 的全部修复，
> 并对照 26.1.2 / 26.2 两线的后续修复轮补齐（维护者指正轮）。范围：
> 镜内 `text_show` 三连修（P0-a/P0-b/P1）、检视打断动画两修、`tacz:nbt` 跨包材料、
> ScopePip 全族（镜内画中画 / 二次渲染 / 时域隔离）、内置 TacZ Mesh Loader（poly_mesh +
> GPU 烘焙，光影两键默认开）、镜内裁手与低倍率豁免、掩码周期帧戳、Sodium/Voxy 通道。
> **其中 TML / PIP / 镜内裁切 / 光影下 GPU 烘焙尚未实机验证**（仅 CI 编译门绿），
> 见 [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md) 与
> [`docs/records/SYNC_SIBLING_0105DB2_20260901.md`](docs/records/SYNC_SIBLING_0105DB2_20260901.md)。
> 配套文档：`docs/MESH_LOADER.md`（总览 / 配置键默认值 / A10 / 待实机清单）与三篇 TML 深潜
> （`TML_GPU_STEP2_HANDFLUSH_20260831`、`TML_GPU_FEASIBILITY_1211_20260831`、
> `REVIEW_UPSTREAM_TML_GPU_262_20260831`）。

### 2026-09-02 第二轮修正（维护者指正）

> 上轮移植只对了 1.21.11 线（`arena/01a05db2`）的 tip，而该线本身落后于 26.1.2 / 26.2 线的
> 后续修复轮次。本轮按维护者指正逐项核对三线差异后补齐（证据：26.1.2 线
> `SYNC_REPLY_TO_1211_20260902.md` §5 的默认值裁定表、26.2 线 `bb6fcb61`（下游审查 A10/A6）、
> 26.1.2 线 `091dd5ec`/`be054bc7`/`d3f0fdc2`；逐 commit 读 diff 核实）。
> 对照表：`docs/records/SYNC_SIBLING_0105DB2_20260901.md` §6。

#### 光影下烘焙两键默认开（维护者裁定，对齐 26.2 R3 定稿与 26.1.2）

- `MeshGpuUnderShaders` / `MeshGpuWorldUnderShaders` 默认 `true`（原随 1.21.11 线旧 A/B 为 false）。
  理由与 26.2 R3 终态一致：常驻 VBO 在光影下的收益胜过每帧 CPU 重变换；
  「高模枪遮挡太阳/月亮处继承天体亮度」是已知、可观测、可整键关闭的取舍。
  MeshyConfig 注释与中英语言描述同步更新。

#### 法线修复（下游审查 A10 —— 绕序×法线自洽，此前 1.21.11 线以默认关回避、未真修）

- `PolyMesh` 换用 26.2 线 `bb6fcb61` 采纳后的现代烘焙形态：镜像（奇数次轴翻转）时
  **反转发射绕序**（`MeshPolyMirrorReverseWinding` 默认 **true**），使变换后绕序叉积与
  烘焙法线一致 —— 修复光影包 `normal *= gl_FrontFacing` 写法把高光贴到错误一侧的病症；
  退化面优先退回枪包法线、再退化为确定方向（(0,1,0)，**绝不写零向量**，杜绝 NaN 高光）；
  `MeshPolyPreferPackNormals` 逐顶点消费枪包平滑法线。
- **配套修复当年逼退默认值的回归根因**：poly 的 collector 路径原走 `entityCutout`
  （1.21.11 该管线默认剔除背面，绕序反转后被剔的正是朝外面 → 整枪近乎全黑）。
  四个 poly 模型（枪/配件/弹药/方块）的 collector 提交改走 `entityCutoutNoCull`；
  GPU 路径管线本就 `withCull(false)`。两条路径都不再因反转吞面。

#### 烘焙额度与 LRU 容量解耦（下游审查 A6）

- 新增 `MeshGpuBakeBudgetPerFrame`（默认 4，1-64）：每帧烘焙额度独立于
  `MeshGpuLightCacheSize`（显存语义），不再 `Math.max(4, 容量)` 一个旋钮当两个用；
  额度耗尽时 log-once INFO（`[TacZMeshLoader] World bake budget ...`），溢出枪当帧回 collector。
  Cloth 面板条目 + en/zh 语言键同步。

### 2026-09-02 第四轮同步（姊妹线 09-02 新增两笔）

> 对照姊妹项目 [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
> `1.21.11` 分支（tip `6db3af93`）与 `26.1.2` 分支，逐 commit 比对 `src/` 的实质性改动：
> 上一轮之后姊妹线只新增两笔代码改动，其余为 docs / CI 日志。两笔均已等价移植。
> 记录：[`docs/records/SYNC_SIBLING_20260902_R4.md`](docs/records/SYNC_SIBLING_20260902_R4.md)。

#### 收枪（put-away）动画恢复 —— 移植姊妹 1.21.11 线 `b8041ab9`（源自 26.2 线 `ffe45485`+`32af4025`）

- 症状：切枪时旧枪的 `put_away` 动画看不到 —— 1.21.11 的 `ItemInHandRenderer` 在切枪当帧
  立刻换成新的主手物，状态机虽已触发 `INPUT_PUT_AWAY`，却没有任何视模可画。
- 根因：上游 `KeepingItemRenderer#keep` 的两处调用点在本代码库里都是注释状态，
  收枪窗口从未开启。
- 修法（与姊妹线同形）：
  - `LocalPlayerDraw#doPutAway` 成为 **唯一** 的 `keep()` 调用点，且只在
    `AnimateGeoItemRenderer#hasInitializedStateMachine(lastItem)` 成立时调用
    （对齐上游把 `keep()` 写在 `isInitialized()` 之内的语义，避免开出「旧枪静止一瞬」的空窗口）；
  - 新增 `AnimateGeoItemRenderer#hasInitializedStateMachine(ItemStack)`，把 `tryExit`
    内部同源的判定暴露给调用点；
  - `AnimateGeoItemRenderer#tryExit` / `GunItemRendererWrapper#tryExit` 里那两行注释
    **保持注释**，并加注说明「只能有一个调用点」；
  - `ItemInHandRendererMixin#keep` 的守卫由「窗口未过期就 return」改为
    **最新一次收枪接管**（仅当同一把枪且新请求不会延长窗口时才不动它）——
    原守卫会让连续快速切枪时第二把枪的 `put_away` 一帧都画不出来。
- 证据级别：静态等价移植（姊妹线同名文件逐行对照，本线仅 loader 侧 import 差异）。
  **实机未跑。**

#### Cloth 面板语言键跟随 toml 键蛇形 —— 移植姊妹 26.1.2 线 `ca083b5d`

- `config.tacz.client.render.mesh_gpu_bake_budget[.desc]` 改名为
  `…mesh_gpu_bake_budget_per_frame[.desc]`，与 toml 键 `MeshGpuBakeBudgetPerFrame` 的蛇形一致。
- 纯改名：字段绑定（`GPU_BAKE_BUDGET_PER_FRAME`、默认 4、区间 1-64）与显示文本一字未动；
  Cloth 落盘用的是 toml 键，**已存配置零影响**。

### 2026-09-02 第三轮修正（用户报告：光影下 VBO 世界路径枪体不可见）

- 用户报告：开光影且 `MeshGpuWorldUnderShaders` 默认开后，第三人称 / 掉落物 / 展示台的
  高模 mesh 枪**不显示**。
- 根因：本线 `IrisCompat#isRenderShadow` 是 `return false` 空壳（姊妹 1.21.11 线的
  legacy/newly 双桥均反射
  `ShadowRenderingState.areShadowsCurrentlyBeingRendered()`）。Iris 1.10.x 的阴影 pass
  经 `MixinGlCommandEncoder` 拦截 `glBindFramebuffer` 切 FBO（不走
  `outputColorTextureOverride`），于是阴影 pass 的 `renderAllFeatures` 调用点把世界 GPU
  表消费进阴影贴图并标记 `worldConsumedFrame` ⇒ 主画面那遍跳过 ⇒ 主画面不可见。
  （注：本仓 26.1.2 分支同名方法也是空壳，但该线实机 PASS、无此症状——维护者
  2026-09-02 实测——空壳致命是本线 1.21.11 消费拓扑特有的，26.1.2 不随本轮改动。）
- 修法：`isRenderShadow()` 按姊妹线同款反射实现（证据：Iris 上游 `1.21.11-unobf` 分支
  common 模块直读源码）。同步受益：`PolyRenderPolicy` 的阴影提交闸、`ShellRender` /
  `MuzzleFlashRender` 的阴影闸一并恢复真实信号。
- 证据级别：静态（读码 + gh api 直读 Iris 源码）+ **维护者实机 PASS**（2026-09-02：
  开光影 + 默认开关下第三人称 / 掉落物 / 展示台高模枪恢复显示）。次要项（阴影贴图
  枪影形状、shell/muzzle 光影下表现）未回报，仍在待实机清单 `docs/MESH_LOADER.md` §7。

#### 本轮的核对结论（其余项）

- 法线矩阵读取时刻修复（MV 栈 per-draw 压/弹）：1.21.11 线 `014f4b0` 已带、26.1.2 线
  `SYNC_REPLY_TO_1211` §4 明确判定两线形状一致且正确 —— **已在，无需再动**。
- Voxy ESC 崩溃三处封堵（allChanged 预热窗口取消 / `isMainStackBoundTo` / 释放拒绝熔断）、
  A1（输出目标覆写防御）、A2/A3（分表禁用、不回写配置、catch LinkageError）、纹理视图
  pass 外解析、镜内文字 log-once：核对后**均已在**（随 1.21.11 tip 移植）。
- A5（ENTITY stride 逐帧哨兵）为 26.2 线独有防御、26.1.2 未采纳；本线同 26.1.2 不搬。
- scope 五件套与 26.1.2 线的其余差异为 API 形状（CameraRenderState / ColorTargetState /
  FeatureRenderDispatcher 构造器参数等），语义无缺；`captureSceneAfterIrisFinal` 补了
  26.1.2 的 `getDeltaTracker()==null` 防御守卫。

### 2026-09-02 姊妹线渲染线全量移植

> 应项目要求把姊妹线 `arena/01a05db2` 的<b>全部实质性改动</b>等价移植到本线（NeoForge 1.21.11）：
> ScopePip 全族、meshloader/TML/GPU 全族、镜内裁手、掩码周期帧戳、Iris 时域隔离、
> Sodium/Voxy 通道。只排除本线不需要的 FCAP 配置落盘（本线原生 ModConfigSpec + Cloth
> savingRunnable 已闭环）。对照与适配记录：`docs/records/SYNC_SIBLING_0105DB2_20260901.md`。
> **证据级别：静态移植 + 姊妹线 javap/实机旁证 + 本线 CI 编译门绿（compile-check success）；实机未跑。**

#### Scope PIP（镜内画中画 / 二次渲染）

- 新增 `ScopePipRenderState`（屏幕空间重投影合成、双深度掩码、显示阈 `0.35`、重投影倍率渐变、
  `captureSceneFromMain`、按参数缓存的合成管线）与 `ScopePipDepthDebug`（品红透镜诊断）；
- 新增 `ScopePipRerender`（窄 FOV 二次渲染、隔帧 `ScopePipRerenderInterval`、画布代次守卫、
  `worldZoomForcedToOne` 只在本帧窄遍真会跑时压 1×、预热/空闲释放按 20 帧节流）;
- `CameraSetupEvent` 的 PIP FOV 让位分支（`WorldZoomShare` 拆分、同 partial-tick 判据）；
- `BedrockAttachmentModel` 的 `pipDefersReticle` 延迟闸；`ScopeFinalOverlayState.discardPendingOverlays`
  + 裸镜框捕获 + `hasPendingOverlay`；`IrisFinalScopeOverlayMixin` 窄遍守卫 +
  `captureSceneAfterIrisFinal/compositeAfterIrisFinal`；
- `GameRendererMixin`：`renderItemInHand` HEAD/RETURN 的 capture/composite/overlay flush、
  `renderLevel` Redirect 注入二次渲染窄遍、`render` HEAD 帧戳+预热；
- 光影时域隔离：`IrisScopePipelineCompat`（按维度管线 + 阴影降采样 + 预热/空闲释放，
  全程反射）+ `IrisScopeDimensionMixin`/`IrisShadowResolutionMixin`；
- Sodium 通道：`SodiumCompat` 反射同步其地形投影快照与区块 uniform 每帧一闸；
  Voxy 通道：`VoxyCompat`/`VoxyScopePipelineCompat`（纯反射）+ 3 个 `@Pseudo` mixin +
  `VoxyCompatMixinPlugin`（NeoForge 无 Voxy 发行，插件拒载 = 通道惰性无害，语义保留）；
- 配置：`RenderConfig` 14 个 `ScopePip*` 键 + Cloth 面板条目 + lang 56 键（en/zh）。
  功能默认全部关闭/旧行为。

#### TacZ Mesh Loader（TML/GPU，`cn.sh1rocu.tacz.compat.meshloader`，21 文件）

- poly_mesh 枪/配件/弹药/方块模型 + 解析缓存 + 顶点预算闸 + `MeshyConfig` 18 键
  （挂 `ClientConfig`，Cloth 面板全量暴露）；
- GPU 静态烘焙：`PolyMeshGpuRenderer` 常驻 VBO 手部/世界两表，绘制点分别为
  `ItemInHandRendererMixin`（`renderHandsWithItems` RETURN，flush 之后）与
  `FeatureRenderDispatcherMixin`（`renderAllFeatures` RETURN）；`GameRendererMixin` 圈定
  in-hand-pass / level-render 语境；法线矩阵读取时刻修复、pass 体内无纹理懒加载、
  首帧判据日志移出 pass 体、EMISSIVE 永久降级修复、光影两键默认关、绕序默认关——
  后续修复全量携带；
- 镜内 mesh 枪身目镜裁剪：`mesh_entity_scope_clip.fsh` + LIT 管线；
- 开镜 mesh 距离闸门角尺寸补偿：`ScopePipRenderState.currentDetailZoom` ×
  `PolyRenderPolicy` 两道闸；
- 4 个 self-mixin（`tacz.mesh.mixins.json`，`remap=false`）目标方法与本线签名逐一核对一致；
- `TaczMeshyIntegration` 挂 `GunModClient` enqueueWork（注册 `model_type=mesh` 构造器）+ 缓存
  失效监听器挂 `AddClientReloadListenersEvent#addListener`（1.21.11 双参形）；
  `ScreenRenderTracker`/`ShaderStateTracker` 分别改挂 `ScreenEvent.Render.Pre/Post` 与
  `RenderFrameEvent.Pre`。

#### 掩码周期帧戳与镜内裁手

- `ScopeDepthCopyState`：`onClientFrameStart`/`hasMaskCycleThisFrame`/`beginExternalMaskOutsideDraw`/
  `DepthHandle`（`worldDepthTarget`/`apertureDepthTarget`）；BACKUP 世界拷贝闸并入
  `ScopePipRenderState.needsIrisWorldDepthCopy`；
- 镜内裁手：`ScopeRenderTypes.shouldClipViewmodel`（`ScopePipMinMagnification` 低倍门禁）+
  `armClipped` 掩码手臂类型 + `RenderHelper` collector 代理（identity 替换
  `entityTranslucent(skin)`）；`MuzzleFlashRender` 同闸。

#### 明确不搬（本线不需要）

- FCAP 配置落盘整族（`ConfigPersist`/`ForgeConfigSpecAccessor`/ModMenu 入口）：本线原生
  NeoForge `ModConfigSpec`（`save()` = `loadedConfig.save()`），Cloth 面板早已接
  `setSavingRunnable`；`/tacz overwrite` 落盘上一轮已补。
- `fabric.mod.json` 的注册面由 `neoforge.mods.toml` [[mixins]] 等价承接；
  `TaczNbtIngredient` 的 Fabric `CustomIngredient` 注册以 `RecipeCompat` 改写等价承接（上轮）。
- 本条目提交时版本号未动（仍 `R1-hotfix`）；随 `1.1.8+neoforge.1.21.11.R2` 一并发布。

### 2026-09-01 姊妹线同步

> 姊妹项目 `TaCZ_Refabricated_Unofficial` 的 `arena/01a05db2` 分支 2026-08-30 ~ 09-01 的新增内容，
> 逐 commit 核对后按本线（NeoForge 21.11.45 / 原生 ModConfigSpec / 无 PIP / 无 meshloader）等价移植。
> 完整对照与「不搬清单」见 `docs/records/SYNC_SIBLING_0105DB2_20260901.md`。
> **证据级别：源码级静态闭环 + 姊妹线 CI/javap 旁证；本线 CI 编译门与实机均未跑，按 AGENTS §2 不宣称已修。**
> 姊妹线给本线的同步清单 `SYNC_CHECKLIST_1211_NEOFORGE_SISTER_20260901.md` 的 §2（P0-a）、§3（P0-b）、
> §4（P1）三条已全部落地。

#### 镜内 `text_show` 文本（MK5/MK5HD 弹药计数）三连修（P0-a / P0-b / P1）

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

#### 同步的其余实质改动

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

#### 明确不搬（对照姊妹线，理由见 records 文档）

- 全部 `ScopePip*`（PIP 二次渲染/重渲染/时域隔离/显示阈/倍率渐变/窄遍守卫等）：本线 1.21.11 无 PIP；
- 全部 meshloader / TML GPU / poly_mesh 项（含法线矩阵读取时刻、纹理懒加载、EMISSIVE 降级等）；
- 镜内裁手（`8ddde34`/`3918049`）：依赖 PIP 镜内区域与 `ScopePipMinMagnification`；
- FCAP 配置落盘整族（`7f3cfd5`/`cd14a2a` 的 1-3 项/`630b0a0` 的 ModMenu 出口）：本线无 FCAP/ModMenu，
  且 Cloth 面板早已接 `setSavingRunnable`；
- 掩码周期帧戳（`8d28e57` 的 `onClientFrameStart`/`hasMaskCycleThisFrame`）：其消费者是 mesh 手部
  裁剪闸与 PIP 合成闸，本线均无；镜内文字按姊妹线口径保留跨帧 `isMaskCycleValid` 语义；
- Iris 二次渲染的 Sodium/Voxy 通道（Fabric 专属 mod）；姊妹线 `.github/workflows` 与 CI 探针（TEMP）。
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

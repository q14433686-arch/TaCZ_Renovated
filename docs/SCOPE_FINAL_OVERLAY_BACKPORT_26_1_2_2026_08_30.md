# 26.1.2 ← 1.21.11：scope「延后重画」机制移植记录（2026-08-30，NeoForge 同步版）

> 任务来源：26.2 分支修遮光环时的跨分支取证（Fabric 姊妹仓对应记录引用
> `docs/records/SCOPE_RING_IRIS_OVERLAY_20260830.md`，本仓四分支均无该文件，未随搬）。
> 对象：本仓 `26.1.2` 分支（NeoForge，depth-aperture 架构）。
> 同步来源：Fabric 姊妹仓（TaCZ_Refabricated_Unofficial）`arena/01a05170` 分支提交
> `9a4e71fb`（fix(scope): redraw reticle/rim after Iris composite, backport from 1.21.11）。
> 语义来源：1.21.11 分支同源提交 `6f2528c`（defer Iris reticle to late hand pass）、
> `828ba10`（preserve late reticle depth for fog）、`2710c7c`（render reticle after Iris final
> composite）、`189a1bd`（remap GlRenderPipeline info accessor）。
> 按「只抄语义不抄代码」执行；姊妹已按 26.1.2 形态落底，本仓再按 NeoForge 加载器差异适配。

## 1. 结论

26.1.2 上的症状与 1.21.11 相同：有物理目镜框（`ocular_ring`，案例⑨ 地基已在），却没有
「最终覆盖」机制——开光影包开镜时，目镜内圈黑边和/或准星会被光影包的 composite/final pass
盖掉或糊掉。本次把 1.21.11 的整套「延后重画」机制移植了过来：

- **R8/R9 路径**：`HAND_SOLID` 冻结快照 → Iris 强制一次较晚的 `HAND_TRANSLUCENT` 手部 pass 提交；
- **R11 路径（经审计的 Iris 26.1 / 1.11.x）**：同一份快照在
  `IrisRenderingPipeline#finalizeLevelRendering()` TAIL（全部 composite/final 之后）用无雾
  vanilla 片元重画到主输出。

**状态：源码级同步完成，未编译、未实机。** 本沙箱无 JDK（网络仅可达 github.com，
Maven/Adoptium/Azul 全部不可达），无法 `./gradlew build`。符号正确性由姊妹侧对官方未混淆
26.1.2 jar 的逐符号核对（Fabric loom 缓存，`minecraft-merged-0d09a28b48-26.1.2.jar`）完成；
本仓（NeoForge 26.1.2）与姊妹共享同一份 vanilla 未混淆符号（neoforge moddev 亦用官方名），
故该结论可继承。本仓侧另做了源码级复核：Iris 26.1 分支（commit f4c0697）HandRenderer /
IrisRenderingPipeline / ItemInHandInterface 逐符号重新读过，与姊妹记录一致；
`RenderSystem.outputColorTextureOverride`/`outputDepthTextureOverride` 在本仓所属的
26.1.2 由第三方 Firmament（Fabric 同 MC 版本）直接赋值反证存在。**不要写 PASS；等用户实机
反馈再收口。**

## 2. 四个版本差异核对点（动手前取证结果）

| # | 核对点 | 取证方法 | 结果 |
|---|---|---|---|
| 1 | `RenderPipeline` 的 info 访问器（姊妹 189a1bd 修的） | 常量池核对 + API 形态对比 | **26.1.2 不需要该机制**。189a1bd 的 FORCE_ALWAYS 白名单是因为 1.21.11 的 `DepthTestFunction` 枚举没有 ALWAYS；26.1.2 的 `CompareOp` 直接有 `ALWAYS_PASS`，且 `DepthStencilState(CompareOp, boolean)` 两参构造器本分支已在用。新的 late/final 管线在管线层直接声明 `ALWAYS_PASS` + 期望的写深度标志，`GlCommandEncoderScopeDepthCopyMixin` **零改动**。 |
| 2 | `GlCommandEncoder` 的 depth-copy 注入点 | 对比两分支 `GlCommandEncoderScopeDepthCopyMixin` | 两分支同形：`drawFromBuffers` HEAD、可取消。final-overlay 不新增注入点，全部状态逻辑都在 `ScopeDepthCopyState.prepareMaskDraw` 内（该文件与 1.21.11 现已逐字节一致）。 |
| 3 | `ScopeRenderTypes` 的管线配方常量 | 常量池核对 | 26.1.2 是聚合对象形态：`ColorTargetState`（混合+颜色写）与 `DepthStencilState`（测试+写+bias）；1.21.11 是扁平 setter（`withColorWrite/withDepthTestFunction/...`）。新管线按 26.1.2 形态书写：late = `new DepthStencilState(ALWAYS_PASS, true)`，final = `new DepthStencilState(ALWAYS_PASS, false)`。 |
| 4 | `RenderSystem` 投影/模型视图方法名 | 常量池核对（RenderSystem.class） | **26.1.2 仍是 `getModelViewMatrix()`**（返回 `Matrix4f`）；`getModelViewStack()` 返回 `Matrix4fStack`；`getProjectionMatrixBuffer()`/`getProjectionType()`/`setProjectionMatrix(GpuBufferSlice, ProjectionType)`、公开静态字段 `outputColorTextureOverride`/`outputDepthTextureOverride`（`GpuTextureView`）全部存在。`getModelViewMatrixCopy()` 是 26.2 才有的改名，26.1.2 不可用。 |

### 2.1 附加核对（ScopeFinalOverlayState 用到的全部符号）

- `FeatureRenderDispatcher` 构造器是**八参**：
  `(SubmitNodeStorage, ModelManager, MultiBufferSource.BufferSource, AtlasManager, OutlineBufferSource, MultiBufferSource.BufferSource crumbling, Font, GameRenderState)`
  —— 第二参 1.21.11 是 `BlockRenderer`（`minecraft.getBlockRenderer()`，26.1.2 无此方法），
  26.1.2 是 `ModelManager`（`minecraft.getModelManager()`），且末尾多一个
  `GameRenderState`（`minecraft.gameRenderer.getGameRenderState()`）。已按 26.1.2 形态适配；
  该八参形态与 Iris 26.1 分支 `HandRenderer` 构造 dispatcher 的写法逐字一致（本仓复核：
  Iris 26.1 `HandRenderer.java` 第 54 行），属于 vanilla 认可的「私有 dispatcher +
  renderAllFeatures」模式（26.1.2 的 `ItemInHandRenderer` 自己也这么干）。
- `RenderBuffers(int)`、`bufferSource()`、`outlineBufferSource()`、`crumblingBufferSource()`
  在 `net.minecraft.client.renderer.RenderBuffers` ✓（Iris 26.1 `HandRenderer.java` 第 44 行
  同款构造交叉印证）。
- `SubmitNodeStorage`：无参构造 ✓、`implements SubmitNodeCollector` ✓、`order(int)` →
  `OrderedSubmitNodeCollector` ✓、`endFrame()` ✓。
- `RenderTarget.getColorTextureView()/getDepthTextureView()`、`Minecraft.getMainRenderTarget()/
  getModelManager()/getAtlasManager()`、公开字段 `font`、`gameRenderer` ✓
  （`getColorTextureView`/`getDepthTextureView` 另由 Firmament 26.1.2 直接调用反证）。
- `Minecraft.font` 为公开字段 ✓（1.21.11 相同）。

### 2.2 附加核对（Iris 侧，对照 IrisShaders/Iris **26.1 分支**，commit `f4c06978f3a1c64869e40cd5cc7c8ed383085cc0`）

Iris 26.1（1.11.x，MC 26.1.2 的实机对照版本）与 1.21.11 的 Iris 1.10.7 有两处内部差异，
均已按 26.1 形态适配：

1. **translucent 门的所在类变了**：1.10.7 是 `HandRenderer.isAnyHandTranslucent()`；
   26.1 是 `gameRenderer.itemInHandRenderer.iris$isAnyHandTranslucent()`——方法本体在
   `net.irisshaders.iris.mixinterface.ItemInHandInterface`（`MixinItemInHandRenderer implements
   ItemInHandInterface`，接口注入进 MC 的 `ItemInHandRenderer`），编译产物里调用点 owner 是
   `ItemInHandRenderer`。`IrisHandRendererReticlePassMixin` 以 owner=`ItemInHandRenderer`
   为主目标，另挂一个 owner=`ItemInHandInterface` 的后备处理器（两个 target 互斥、都
   `require=0`，谁匹配谁生效）。本仓源码复核：`HandRenderer.java` 第 134 行、
   `ItemInHandInterface.java` 第 14 行、`MixinItemInHandRenderer.java` 第 28/71 行。
2. **手部收集器的冲刷点**：1.10.7 靠 Iris 自己的 ItemInHandRenderer endBatch 包装；
   26.1 的 `HandRenderer.renderTranslucent` 在方法内直接 `submitNodeCollector.endFrame()`。
   两版共同点是「setPhase(HAND_TRANSLUCENT) 之后、冲刷之前」的窗口都存在，注入点
   （setPhase ordinal=0 AFTER）不变；`renderTranslucent` 内该窗口只有一次 setPhase 调用。
3. `HandRenderer.submitNodeCollector : SubmitNodeStorage` 字段名/类型两版一致（@Shadow 可用，
   本仓复核 `HandRenderer.java` 第 48 行）；`IrisRenderingPipeline#finalizeLevelRendering()`
   存在（第 1088 行），方法体先 `isRenderingWorld=false` 再 `compositeRenderer.renderAll()` +
   `finalPassRenderer.renderFinalPass()`，TAIL 语义与 1.21.11 相同；
   `WorldRenderingPipeline.setPhase(WorldRenderingPhase)` 接口方法一致。
4. **版本闸**：`IrisCompat.supportsFinalScopeOverlay()` 放行 Iris 版本号以 **`1.11`** 开头的构建
   （= 上面审计过的 26.1 分支线；1.21.11 上对应的是 `1.10.7` 精确前缀）。其他 Iris 构建
   （如旧日志里出现过的 1.10.9）两个 defer 标志都不成立，保持移植前的 solid-pass 行为——
   宁可维持已知的「被后处理盖掉」，不赌未审计的内部时序得到一颗隐形准星。

## 3. 同步文件清单（相对姊妹 9a4e71fb 的本仓差异）

共 15 个路径。**12 个与本仓逐字节一致**（`git hash-object` 与姊妹终版比对通过），仅
下面 2 个因加载器/构建差异需要适配，另有 1 个为同步记录：

| 路径 | 与姊妹终版关系 |
|---|---|
| `client/model/BedrockAttachmentModel.java` `client/render/scope/{ScopeDepthCopyState,ScopeRenderTypes,EtchedReticleRenderer,IlluminatedReticleRenderer,IReticleRenderer}.java` `client/render/scope/{ScopeLateReticleState,ScopeFinalOverlayState}.java`（新） `mixin/client/iris/{IrisHandRendererReticlePassMixin,IrisFinalScopeOverlayMixin}.java`（新） `assets/tacz/shaders/core/{scope_reticle_final,scope_ring_final}.fsh`（新） | **逐字节一致**（姊妹 9a4e71fb = 本仓工作树） |
| `compat/iris/IrisCompat.java` | 语义一致、加载器适配：`FabricLoader.getModContainer(...).getMetadata().getVersion().getFriendlyString().startsWith("1.11")` → `ModList.get().getModContainerById(...).map(c -> c.getModInfo().getVersion().toString().startsWith("1.11"))`（与 `GunHudOverlay.java:208` 同款 NeoForge 用法）；`isRenderingSolidHandPass()` 的 `isModLoaded` 检查换成 `ModList.get().isLoaded`。其余（反射 HandRenderer.INSTANCE/isActive/isRenderingSolid）两版相同 |
| `resources/tacz.iris.mixins.json` | 注册两个新 mixin 一致；**compatibilityLevel 维持本仓原状 `JAVA_21`**（姊妹为 JAVA_17；本仓基座就是 JAVA_21，新 mixin 未提高约束，向下兼容） |
| `docs/SCOPE_FINAL_OVERLAY_BACKPORT_26_1_2_2026_08_30.md` | 本记录（姊妹版微调的 NeoForge 说明版） |

其余（IrisCompat 之外的 `IReticleRenderer`、`EtchedReticleRenderer`、`IlluminatedReticleRenderer`、
`ScopeRenderTypes`、`ScopeDepthCopyState`、`BedrockAttachmentModel` 及两个新 State/两个新 mixin/
两个 fsh）均与 1.21.11 语义等价、与姊妹布局一致，详见姊妹记录对应章节（内容不重复抄录）。

## 4. 有意偏差（与 1.21.11 不同之处）

1. **不搬 `189a1bd` 的 FORCE_ALWAYS 白名单**（理由见 §2 表第 1 行）。`GlRenderPipeline.info()`
   的 remap 问题是 Loom 混淆分支独有的，26.1.2 未混淆，整类问题不存在。
2. **不搬绘制顺序对调**（`SCOPE_RETICLE_ORDER=1` / `SCOPE_OCULAR_RING_ORDER=2`）。1.21.11 在
   2026-08-13 依实机反馈把顺序从「框 1 / 准星 2」换成「准星 1 / 框 2」以修准星溢出镜框；
   该修复在差距清单之外，且会改变 vanilla 路径的绘制顺序，与验收项 1（不开光影逐位一致）
   冲突。**26.1.2 大概率存在同一溢出问题**（掩码判据 `APERTURE_TARGET` 同样在 body 边界
   快照、不含镜框信息），待实机确认后再单独立项。
3. **版本闸前缀从 `1.10.7` 换成 `1.11`**（§2.2 第 4 条），且**两条 defer 路径都受它约束**
   （1.21.11 只闸 final 路径）。因为本分支的 late-pass mixin 目标只对 Iris 26.1 线做过审计，
   对 26.1.2 上可能并存的其他 Iris 构建没有证据。
4. 普通蚀刻/发光准星管线维持 26.1.2 现状（`ALWAYS_PASS + writeDepth=true`、etched 归
   `HAND`、visible 归 `HAND_TRANSLUCENT`）——1.21.11 在 828ba10 里对普通管线深度语义的
   改写与本分支的 encoder 机制绑定，不随本次搬运。

## 5. 未验证项

- **编译**：本沙箱无 JDK 且 Maven 不可达，`compileJava` 未执行。姊妹侧对官方未混淆
  26.1.2 jar 的符号核对已覆盖新代码引用的全部 MC API；本仓侧以 Iris 26.1 分支源码逐符号
  复核了 mixin 注入目标与 `FeatureRenderDispatcher`/`RenderBuffers` 构造（见 §2.1/§2.2），
  但**未对 Iris 26.1.2 NeoForge 版 jar 字节码直接验证**（Iris 对 NeoForge 与 Fabric 共用同一
  份 common 源码，类名/签名一致，属合理推定而非实证）。
- **NeoForge 运行时特有面**：`RenderSystem.outputColorTextureOverride`/`outputDepthTextureOverride`
  的存在性由 Fabric 侧 26.1.2（Firmament）反证；NeoForge 是否对 RenderSystem 做同形保留
  未实证（NeoForge 自 1.20.4+ 不混淆 vanilla，字段应原样可见，属合理推定）。
- **全部实机项**：见 §6。历史教训（AGENTS.md §3）：该类改动每次都「编译通过但运行期崩」，
  编译（将来 CI 补跑）绝不构成收口依据。
- `tacz.iris.mixins.json` 维持本仓 `JAVA_21` compatibilityLevel（同步前现状，未改变）。

## 6. 复测清单（等实机）

环境矩阵：vanilla OpenGL（无 Iris）/ Iris 1.11.x（NeoForge 版）+ Sodium（关 shader pack）/
Iris 1.11.x + Sodium + 任一常用 shader pack（至少覆盖一个会用 post-composite 雾的包，如
Complementary）。Iris 版本另测一个 **非 1.11** 构建（如有）确认闸生效（行为应与移植前一致）。

1. **不开光影开镜：与改动前逐位一致（回归）**。蚀刻/发光准星、遮光环、镜内透明、枪口火光
   反向裁剪全部与移植前构建对比。
2. **开光影开镜**：目镜框与准星都在，且在光影雾效之上（不被糊）。日志应出现
   `Queued reticle for Iris post-composite overlay.` 与 `Rendered reticle and ocular rim after
   Iris final composite.`（各一次，之后不再刷）。
3. **含 `ocular_ring` 的镜（默认枪包 14 个）**：scope_1873_6x、scope_98k、scope_acog_ta31、
   scope_aug_default、scope_contender、scope_elcan_4x、scope_hamr、scope_lpvo_1_6、
   scope_mk5hd、scope_qmk152、scope_retro_2x、scope_scout、scope_standard_8x、scope_vudu
   —— 逐个看黑边完整。
4. **无准星帧**（开镜淡入淡出中 / 无 division 的镜）：镜框走 solid-pass 兜底分支，不应出现
   「为一次空的延后 pass 整帧无镜框」。
5. **第三人称 / 腰射 / GUI 预览（工作台 PIP）/ 第一人称不开镜**：不受影响（延后机制只在
   第一人称开镜 + Iris + 版本闸通过时排队）。
6. **重复开合镜 / 切枪 / 切组合镜 view**：无残留几何、无逐帧泄漏（`endFrame` 清理路径）。
7. **resize / 全屏切换**（Iris 下）：无 FBO incomplete、无显存持续增长（私有深度副本路径）。
8. **日志无 `[TACZ Scope] Post-composite reticle overlay failed` 刷屏**（出现即说明主输出
   状态恢复路径有缺陷）。

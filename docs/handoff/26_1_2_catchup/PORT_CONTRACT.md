# 26.1.2 ← 1.21.11「最终覆盖」移植契约

日期：2026-08-30
取证方式：`git show` / `git grep` / `git ls-tree` 直读 `origin/26.1.2` 与 `origin/1.21.11`
（tip `a3d3241` / `e3d9dd5`），**不是**靠记忆或推断。
蓝本：[`final_overlay_62532f1.diff`](final_overlay_62532f1.diff)（`62532f1` 的 15 路径子集）。

> 约定：本文里「✔ 已有」= 在本仓该分支源码里 **grep 到了实际使用**；
> 「未引用」= 本仓源码里 0 处，**不代表 MC 里没有**，必须对着
> `minecraft-merged-0d09a28b48-26.1.2.jar`（26.1.2 分支 `GunPreviewRenderer` 的注释里
> 点名的那份反编译 jar）核符号后再写。

---

## 1. 逐文件契约

### 1.1 全新文件（26.1.2 上不存在，按蓝本新建）

| 文件 | 行数 | 说明 / 移植注意 |
|---|:--:|---|
| `client/render/scope/ScopeFinalOverlayState.java` | 205 | 核心。见 §1.3 的 API 依赖表 |
| `client/render/scope/ScopeLateReticleState.java` | 143 | HAND_TRANSLUCENT 兜底路径（旧 Iris 版本） |
| `mixin/client/iris/IrisFinalScopeOverlayMixin.java` | 23 | `@Mixin(targets="net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap=false)`，注入 `finalizeLevelRendering` 的 TAIL |
| `mixin/client/iris/IrisHandRendererReticlePassMixin.java` | 53 | `@Mixin(targets="net.irisshaders.iris.pathways.HandRenderer", remap=false)`；`@Shadow private SubmitNodeStorage submitNodeCollector`；两处 `require = 0` |
| `resources/assets/tacz/shaders/core/scope_reticle_final.fsh` | 98 | 无雾版准星片元着色器 |
| `resources/assets/tacz/shaders/core/scope_ring_final.fsh` | 66 | 无雾版目镜框片元着色器 |

> **必须先证实的两件事**（做不到就整条路走不通，别硬写）：
> 1. 26.1.2 上装的那个 Iris 版本里，`IrisRenderingPipeline#finalizeLevelRendering()`
>    和 `net.irisshaders.iris.pathways.HandRenderer#renderTranslucent()`
>    两个目标是否还存在、签名是否没变。1.21.11 只审计过 Iris **1.10.7**
>    （`IrisCompat.supportsFinalScopeOverlay()` 就是拿版本号字符串 `startsWith("1.10.7")` 卡的）。
>    **26.1.2 上的 Iris 版本不一定是 1.10.7**，这个 gate 要重新定。
> 2. `HandRenderer` 里那个字段名是不是还叫 `submitNodeCollector`、类型是不是
>    `SubmitNodeStorage`（`@Shadow` 写错会在 mixin apply 阶段炸）。

### 1.2 需要改动的文件

| 文件 | 26.1.2 现状 | 1.21.11 目标态 |
|---|---|---|
| `client/render/scope/IReticleRenderer.java` | `Context` 是 **9 分量** record，止于 `maskActive`；唯一构造点 `BedrockAttachmentModel:660` | **11 分量**，末尾加 `deferToIrisTranslucent`、`deferToIrisFinalOverlay` |
| `client/model/BedrockAttachmentModel.java` | `:632-635` 目镜框**行内**提交；`:660` 构造 9 参数 Context | `:622-629` 算两个 defer 布尔量；`:663` 目镜框改成三分支；`:686-713` reticle RenderType 三选一 + 11 参数 Context；`:717-736` 目镜框延后排队 |
| `client/render/scope/ScopeRenderTypes.java` | 6 条管线：`DEPTH_APERTURE` / `DEPTH_CLEANUP` / `ETCHED_RETICLE` / `VISIBLE_RETICLE` / `VIEWMODEL_CUTOUT` / `FLASH_*`；**没有** `FORCE_ALWAYS_DEPTH_PIPELINES` / `needsForcedAlwaysDepth` | +6 条：`LATE_ETCHED_RETICLE` / `LATE_VISIBLE_RETICLE` / `LATE_OCULAR_RING` / `FINAL_ETCHED_RETICLE` / `FINAL_VISIBLE_RETICLE` / `FINAL_OCULAR_RING`；+`FORCE_ALWAYS_DEPTH_PIPELINES` 集合与 `needsForcedAlwaysDepth` |
| `client/render/scope/ScopeDepthCopyState.java` | 有 `MASK_MODE_UNIFORM`，无 final-overlay 分支 | +`FINAL_OVERLAY_UNIFORM = "tacz_ScopeFinalOverlay"`；Iris 分支里「有待重画准星时」多拷一份私有世界深度；MASK 分支按 `finalOverlay` 跳过目标一致性检查 |
| `compat/iris/IrisCompat.java` | 有 `isHandRendererActive()`、`shouldRenderInCurrentHandPhase(ItemStack)`（内部已反射 `isActive` + `isRenderingSolid`） | +`isRenderingSolidHandPass()`（`isActive() && isRenderingSolid()`）、+`supportsFinalScopeOverlay()`（版本号 gate） |
| `mixin/client/GlCommandEncoderScopeDepthCopyMixin.java` | 形参是 `@Coerce Object glRenderPipeline`，**没有** force-always 那段 | 形参改回 `GlRenderPipeline`，新增 `tacz$forceAlwaysDepthIfNeeded(...)` |
| `resources/tacz.iris.mixins.json` | `client` 只有 `IrisDepthRestoreShaderMixin` | 加两个新 mixin（1.21.11 那份顺带在 `_comment` 里写清了理由，值得照抄） |

### 1.3 `ScopeFinalOverlayState` 的 API 依赖（26.1.2 上逐条核过）

| 符号 | 1.21.11 怎么用 | 26.1.2 本仓源码 | 结论 |
|---|---|:--:|---|
| `com.mojang.blaze3d.ProjectionType` | `getProjectionType()` / `setProjectionMatrix(slice, type)` | 未引用 | 核 jar |
| `RenderSystem.getProjectionMatrixBuffer()` | 取/存 `GpuBufferSlice` | 未引用（本仓只用过 `setShaderColor`、`getModelViewStack`） | 核 jar |
| `RenderSystem.getModelViewMatrix()` | 冻结模型视图 | 未引用 | **核 jar，注意后缀**：26.2 上它叫 `getModelViewMatrixCopy()`，26.1.2 上叫什么必须先看清再写（这是我在 26.2 上翻过车的地方） |
| `RenderSystem.outputColorTextureOverride` / `outputDepthTextureOverride` | 把输出重定向回主 RT | 未引用 | 核 jar（1.21.6 加入，1.21.11 有） |
| `new RenderBuffers(availableProcessors)` + `bufferSource()` | 自建一套 | 未引用 | 核 jar；**或**考虑改用 26.1.2 已有的现成实例（见下） |
| `new SubmitNodeStorage()` + `.order(int)` + `.endFrame()` | 自建一套 | `SubmitNodeStorage` ✔ 用到，但都是**取现成的**；`endFrame` 未引用 | 核 jar；`new` 与 `endFrame` 都要验 |
| `new FeatureRenderDispatcher(nodes, blockRenderer, bufferSource, atlasManager, outlineBufferSource, crumblingBufferSource, font)` | 自建一套 | ✔ 用到，**但只从 `mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage()` 取现成实例** | 见下方**岔路口** |
| `OrderedSubmitNodeCollector#submitCustomGeometry` | 提交快照 | ✔ 大量使用 | 直接可用 |
| `BedrockRenderSnapshot#write / writeFiltered / isEmpty` | 提交快照 | ✔ 三处 reticle 渲染器都在用 | 直接可用 |
| `ReticleMarkFilter#isThinMark` | 过滤 | ✔ | 直接可用 |
| `Minecraft#getMainRenderTarget()#getColorTextureView()/getDepthTextureView()` | 重定向输出 | 需核 | 核 jar |

**岔路口 —— `ensureDispatcher` 怎么写。** 1.21.11 是「自建一套 RenderBuffers + SubmitNodeStorage +
FeatureRenderDispatcher」，刻意跟主帧隔离。26.1.2 上本仓代码走的是另一条路：
`mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage()`
（`GunPreviewRenderer:91-93`，注释里写明是照抄 vanilla 26.1.2 的
`OversizedItemRenderer#renderToTexture`，且已对 `minecraft-merged-0d09a28b48-26.1.2.jar`
逐符号验证过）。
**建议：先试 1.21.11 的自建写法（语义更安全，不污染主帧）；编译不过再退回取现成实例。**
但取现成实例意味着你把准星塞进主帧的提交队列 —— 那不是 1.21.11 的语义，改了要写进自审说明。

---

## 2. 四个版本差异核对点：**已经查过了**，不用再 grep

上一版交接文档里这四个是「动手前先 grep 确认」。这次我直接对两个分支的源码核了一遍：

**① `RenderPipeline` 的 info 访问器（姊妹 `189a1bd` 修的那个）**
1.21.11 的 `ScopeFinalOverlayState` **根本不碰** `GlRenderPipeline` 的 info —— 那 205 行里
没有任何 `getInfo` / `info()` 调用（我 grep 过，只有两条 `GunMod.LOGGER.info` 的日志命中）。
真正用 `GlRenderPipeline` 的是 `GlCommandEncoderScopeDepthCopyMixin`，而
**26.1.2 那份把形参写成了 `@Coerce Object`** —— 说明当初在 26.1.2 上这个类型就不好直接引用。
→ **26.1.2 上先确认 `GlRenderPipeline` 能否直接引用**（access transformer /
`accesstransformer.cfg` 里 1.21.11 那次提交动过 6 行，八成与此有关）。不能引用就别硬搬
`needsForcedAlwaysDepth` 那段。

**② `GlCommandEncoder` 的 depth-copy 注入点**
两边**完全一致**：都是 `@Mixin(targets="com.mojang.blaze3d.opengl.GlCommandEncoder")` +
`@Inject(method="drawFromBuffers", at=@At("HEAD"), cancellable=true, require=1)`。
差别只在形参类型和 1.21.11 多出来的 `tacz$forceAlwaysDepthIfNeeded`。
→ 注入点不用动。

**③ `ScopeRenderTypes` 的管线配方常量**
两边都用 `clonePipeline(source, id)` + `RenderPipelines.register(builder.build())`，`clonePipeline`
在 26.1.2 上**已有**（大量使用）。1.21.11 的 final 三条是：
`clonePipeline(RenderPipelines.ENTITY_CUTOUT / ENTITY_TRANSLUCENT_EMISSIVE, …)` →
`withFragmentShader(core/scope_reticle_final | core/scope_ring_final)` →
`withSampler(MASK_WORLD_SAMPLER_UNIFORM)` + `withSampler(APERTURE_SAMPLER_UNIFORM)`
（目镜框那条只要 fragment，不要 sampler）→
`withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)` + `withDepthWrite(false)` →
注册后 `FORCE_ALWAYS_DEPTH_PIPELINES.add(pipeline)`。
**坑**：1.21.11 的注释写得很直白 —— 「`DepthTestFunction` **没有** ALWAYS」，所以他们用
`NO_DEPTH_TEST` 声明 + 在 encoder mixin 里手动 `GL_ALWAYS` 重开深度测试。
→ **26.1.2 上先确认 `DepthTestFunction` 有没有 ALWAYS**：有就直接声明，省掉整个
force-always 机制（`FORCE_ALWAYS_DEPTH_PIPELINES` + `needsForcedAlwaysDepth` +
mixin 里那段）；没有才照搬。

**④ `RenderSystem` 的投影/模型视图方法名**
26.1.2 本仓代码里只出现过 `RenderSystem.setShaderColor`（3 处）和
`RenderSystem.getModelViewStack`（3 处），**其余全部未引用** ——
也就是说 1.21.11 用的那 6 个符号在 26.1.2 上一个都还没被验证过。
→ 对着 `minecraft-merged-0d09a28b48-26.1.2.jar` 核完再写。**尤其 `getModelViewMatrix()`**：
26.2 上已改名 `getModelViewMatrixCopy()`，26.1.2 上别想当然。

---

## 3. 搬完自检

- [ ] `ScopeFinalOverlayState` / `ScopeLateReticleState`：编译通过（若 `RenderBuffers`、`new SubmitNodeStorage()`、`new FeatureRenderDispatcher(...)`、`endFrame()` 在 26.1.2 上不存在，按 §1.3 的岔路口处理，并把选择写进自审说明）。
- [ ] `RenderSystem.getModelViewMatrix()` 的实际名字已对 jar 核过（不是靠 grep 命中就下结论）。
- [ ] `DepthTestFunction` 有没有 ALWAYS 已确认；据此决定要不要 force-always 那一套。
- [ ] `GlRenderPipeline` 在 26.1.2 上能否直接引用已确认；不能就保留 `@Coerce Object` 并放弃 `needsForcedAlwaysDepth`。
- [ ] 两个新 mixin 都登记进 `tacz.iris.mixins.json`，且 `require = 0` / `required: false` + `IrisCompatMixinPlugin` 的 `isModLoaded` 双兜底保留（没装 Iris 不能受影响）。
- [ ] `IrisCompat.supportsFinalScopeOverlay()` 的版本号 gate **已按 26.1.2 上实际的 Iris 版本重写**（不要照抄 `1.10.7`）。
- [ ] `IReticleRenderer.Context` 从 9 分量改 11 分量后，全部构造点都改了（26.1.2 上只有 `BedrockAttachmentModel:660` 一处）。
- [ ] 目镜框的三分支逻辑（`final` / `late` / 行内）与 1.21.11 `:663` `:717-736` 一一对应，特别是「本帧没有准星（淡入中）就退回行内」那条 fallback 别漏。

## 4. 回归复测

见 [`../HANDOFF_26_1_2_CATCHUP_20260830.md`](../HANDOFF_26_1_2_CATCHUP_20260830.md) §5。

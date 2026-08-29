# 从姊妹分支 26.2 同步「镜内画中画（Scope PIP）」一族

日期：2026-08-30
作者：Arena agent（本仓 `arena/01a04ea3-tacz-renovated`）
本仓分支：`arena/01a04ea3-tacz-renovated`（基线 `d0c69a8` = `26.2` 尖端）
来源：`q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `26.2(main)`，尖端 **`fcaa2b8`**
上一次已知同步点：`7f6d1bf`（见 `SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md` 的记录）
状态：**源码级移植，未经本仓编译与实机验证**（沙箱内 Maven 源与 JDK 均不可达），禁止写 PASS。
下一步：本地首次 `./gradlew build` 按 §5 的 13 条假设逐条核对；过编译后再按 §6 实机验一轮。

---

## 0. 结论一句话

把姊妹分支整套「镜内画中画（PIP）」—— 重投影模式 + 实验性二次渲染模式 + Iris/Voxy/
Sodium/PhysicsMod 兼容层 + 8/29 那次帧率衰减泄漏的修复 —— 按 NeoForge 表面重写后
接进本仓。<b>默认关闭</b>（`ScopePipEnable=false`），关着的时候运行路径与同步前逐位等价；
任何一环运行期出错都会自我停用并退回原有的整屏变焦。

---

## 1. 同步了什么

### 1.1 新增文件（全部按本仓 NeoForge 表面改写）

| 本仓路径 | 来源 | 内容 |
|---|---|---|
| `client/render/scope/ScopePipRenderer.java` | `ScopePipRenderer`（基线 + `052e600` + `7fc605e`） | PIP 主体：闸门判定、倍率拆分、抓取世界画面、二次渲染、两种合成 |
| `client/render/scope/ScopePipTarget.java` | 同名（基线） | 镜内画面的离屏拷贝纹理 |
| `client/render/scope/ScopePipTrace.java` | 同名（基线） | 【诊断】渲染目标解析顺序追踪（默认关） |
| `compat/sodium/SodiumCompat.java` | 同名（基线） | 同步 Sodium 自己的地形投影快照 + 区块 uniform 上传闸 |
| `compat/physicsmod/PhysicsModCompat.java` | 同名（基线） | 同步 Physics Mod 自己那份投影 |
| `compat/voxy/VoxyCompat.java` | 同名（基线） | 取 `VoxyRenderSystem` 实例 |
| `compat/voxy/VoxyScopePipelineCompat.java` | 同名（基线） | 为镜内那一遍建/换第二套 Voxy 渲染栈 |
| `compat/iris/IrisScopePipelineCompat.java` | 同名（基线，`prewarm` 部分） | 瞄具管线的维度 id 与预热 |
| `mixin/client/LevelRendererAccessor.java` | 同名（`052e600`） | 暴露 `LevelRenderer.submitNodeStorage` |
| `mixin/client/PreparedFrameAccessor.java` | 同名（基线） | 读 `PreparedFrame.context` |
| `mixin/client/SimpleFeatureRenderPhaseMixin.java` | 同名（基线 + `052e600`） | 镜内那一遍不清空提交节点 |
| `mixin/client/TranslucentFeatureRenderPhaseMixin.java` | 同名（基线 + `052e600`） | 同上（半透明队列） |
| `mixin/client/SkyRendererMixin.java` | 同名（基线） | 二次渲染时把天空也画进离屏 target |
| `mixin/client/LevelExtractorScopePassMixin.java` | 同名（基线） | 镜内期间拦掉 `allChanged()` |
| `mixin/client/iris/IrisScopeDimensionMixin.java` | 同名（基线） | 镜内那一遍换用瞄具维度 id |
| `mixin/client/iris/IrisShadowResolutionMixin.java` | 同名（基线） | 给镜内那一遍更小的阴影贴图 |
| `mixin/client/voxy/*`（4 个，含插件） | 同名（基线） | Voxy 视口隔离 / 缺席 / 节点 tick 节流 |
| `assets/tacz/shaders/core/scope_pip.fsh` | 同名（`fcaa2b8`） | 合成片元着色器（Catmull-Rom 重建 + 锐化） |

### 1.2 修改的现有文件

| 文件 | 改动 |
|---|---|
| `mixin/client/GameRendererMixin.java` | 新增 `extract` HEAD（帧状态归零 + 管线预热）、`renderLevel` 的 BEFORE/AFTER（二次渲染 / 抓取 + 光影合成）、`mainRenderTarget` 重定向；`renderItemInHand` HEAD 顺带记下手部投影 |
| `mixin/client/FeatureRenderDispatcherMixin.java` | 阶段边界追加 `ScopePipRenderer.compositeAtPhaseBoundary()`；新增 PreparedFrame 泄漏补救与「当前准备的 storage」跟踪 |
| `client/render/scope/ScopeMaskRenderer.java` | 新增帧状态（本帧/上帧掩码、合成闸门）与**目镜屏幕包围盒**；绘制成功后置位 |
| `client/event/CameraSetupEvent.java` | PIP 生效时改问 `ScopePipRenderer.currentWorldZoom()`，不再整屏变焦 |
| `client/init/ClientSetupEvent.java` | 注册 PIP 合成管线 |
| `config/client/RenderConfig.java` | 新增 12 个 `ScopePip*` 选项（默认值与值域全部跟随姊妹分支） |
| `compat/cloth/client/RenderClothConfig.java` + `lang/{en_us,zh_cn}.json` | 8 条游戏内菜单项与文案 |
| `tacz.mixins.json` / `tacz.iris.mixins.json` / 新增 `tacz.voxy.mixins.json` / `neoforge.mods.toml` | 注册新混入 |

---

## 2. 为了脱离 Fabric 表面改了什么（逐类）

1. **`@Environment(EnvType.CLIENT)` → 删除**。本仓按包区分端（这些都是
   `client.*` 包下的类），不靠 Fabric 注解；姊妹分支的包结构两边一致，删掉即可。
2. **`FabricLoader#isModLoaded` → `ModList#isLoaded`**（四处：`SodiumCompat`、
   `PhysicsModCompat`、`VoxyCompat`、`VoxyScopePipelineCompat`、`IrisScopePipelineCompat`
   与 `VoxyCompatMixinPlugin`）。反射目标类名与加载器无关，原样保留。
3. **`ForgeConfigSpec` → `ModConfigSpec`**。姊妹分支用的是 Fabric 侧的
   ForgeConfigAPIPort，本仓直接用 NeoForge 的 `ModConfigSpec`；字段与
   `defineInRange` 用法一一对应。
4. **管线注册 → `RegisterRenderPipelinesEvent`**。Fabric 侧是懒建即用，本仓的
   26.2 渲染管线要过 mod-bus 注册（与 `ScopeMaskRenderer` 一致）。为保留姊妹分支
   「构建失败不要连累不用 PIP 的玩家」这条性质，注册包在 try/catch 里：
   失败只把 `failed` 置位，不阻断进游戏。
5. **`ScopeMaskRenderer` 的包围盒改走斜率空间**。姊妹分支在写顶点时直接累积 NDC
   包围盒（她那边仍读投影 UBO）；本仓 855989c 把凸包孔径填充改成了斜率空间、
   刻意不碰投影矩阵，所以这里改成「斜率空间累积 + 用手部投影的 `m00/m11`
   换算成 NDC」。投影取不到时 `hasMaskBounds()` 返回 false，合成退回纯掩码约束
   （= 姊妹分支拿不到包围盒时的兜底行为）。

---

## 3. 明确没有同步的部分（以及为什么）

| 项 | 来源 | 不同步的理由 |
|---|---|---|
| `ScopePipResourceProbe`、`ScopePipDebugGpuMem`、`ScopePipReleaseIdlePipeline` + `ScopePipIdleReleaseDelayFrames` | `12edcf9` / `d53c018` | 这是「光影下开镜帧率衰减」那次调查的**实验装置**，不是修复。调查本身作为记录另存（见下），但每帧探针与实验开关不进运行路径。 |
| `IrisHandPhaseSplitFix`（`613fa67`/`627a59b`/`df8f14c`） | 8/29 | 姊妹分支自己核实过：本仓三条线的 `ShaderCompat#shouldRenderInCurrentHandPhase` 已是等价闸门（`HANDOFF_TO_SISTER_NEOFORGE.md` 的 H1）。**不要再加那个开关。** |
| `AIMING_SWAY_INTENSITY` | 姊妹分支 RenderConfig | 那是她的开镜晃动旋钮，属于另一条功能线（牵扯视模晃动代码），不是 PIP 的前提。要的话另开一轮单独同步。 |
| LR 0.4.3 那批、耳鸣/消声、`UsePressGate`/`StuckUseRecovery` | 8/27 那批 | 本仓 `26.2` 已经通过 `5f54866` 落地，逐文件核对过，**不要重复同步**。 |
| 掩码写入顺序无关（`23b1e9b`） | 8/28 | 本仓已有自己的 `5c02c5f`（同一加固，且是交接给她的那一版）。她的实现与本仓同族，不再搬一次。 |
| 姊妹分支的 `Scalar` 选择与其它 refab 专属功能 | — | 与本仓 26.2 的瞄具架构（离屏掩码）不是同一族，按 `HANDOFF_COMMON` §4.2 的禁令不搬。 |

---

## 4. 同步过来的那次<b>修复</b>（`052e600`）

「开镜帧率持续衰减」的根因与修复，见
[`REFAB_SCOPE_PIP_FPS_DECAY_20260829.md`](REFAB_SCOPE_PIP_FPS_DECAY_20260829.md)。
本仓已包含修复本体：`shouldPreserveSubmits()` 只保留主画面共用的
`LevelRenderer.submitNodeStorage`，Iris `ShadowRenderer` 那份专用存储每帧照常清空。

---

## 5. 未验证清单（本地首次构建请逐条看）

沙箱内 Maven 源与 Adoptium 都不通、没有 JDK，**这些改动没有经过一次编译**。

### 5.0 已完成的一次源码级核对（不必重做）

| 核对项 | 结论 |
|---|---|
| `registerPipeline(RegisterRenderPipelinesEvent)` 签名 | ✅ 与本仓 `ScopeMaskRenderer#registerPipeline` / `ScopeBodyRenderTypes#registerPipelines` 同为 `event.registerPipeline(RenderPipeline)` |
| 管线 id `tacz:pipeline/scope_pip_composite` | ✅ 与本仓 `pipeline/scope_mask` 同款 `Identifier.fromNamespaceAndPath` 写法 |
| 片元着色器 id `tacz:core/scope_pip` → `assets/tacz/shaders/core/scope_pip.fsh` | ✅ 文件在位（9132 B）；id 解析规则与 `core/scope_body` 一致 |
| 顶点着色器 `core/screenquad` | ✅ 与姊妹分支逐字一致（她逐项对照 vanilla `ENTITY_OUTLINE_BLIT` 的字节码读出来的） |
| `pass.bindTexture(name, view, sampler)` / `pass.draw(3,1,0,0)` / `pass.enableScissor(x,y,w,h)` | ✅ 与姊妹分支逐字一致（同一 MC 版本） |
| `RenderSystem.getProjectionMatrixBuffer()` → `GpuBufferSlice`、`getProjectionType()` → `ProjectionType`、`setProjectionMatrix(slice, type)`、`new ProjectionMatrixBuffer(String)`、`Projection#setupPerspective(near, far, fov, w, h)`（5 参）、`Projection#getMatrix(Matrix4f)` | ✅ 与姊妹分支逐字一致 |
| `pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(Matrix4f, Vector4f))` | ✅ 与本仓 `ScopeMaskRenderer` 第 457 行同款 |
| 合成 pass 不声明 `DepthStencilState` + 用 3 参 `createRenderPass` + 第三个参数 `Optional.empty()` | ✅ 自洽：`wantsDepthTexture()` 判据是字段是否为 null，null ⇔ 不挂深度 |
| 三个 bind group 的顺序 `IN_SAMPLER` → 掩码 layout → `DYNAMIC_TRANSFORMS` | ✅ 与 `scope_pip.fsh` 的声明顺序一致（`globals.glsl` 由 snippet 提供，`InSampler`、`ScopeMaskSampler`、`dynamictransforms.glsl` 依次声明） |

### 5.1 剩下的常量名风险（构建时才见分晓）

下面这几个名字**本仓别处从未出现过**，只能靠姊妹分支「逐项对照 vanilla 字节码」的
记录背书（她的 `ScopePipRenderer` 注释：vanilla `ENTITY_OUTLINE_BLIT` =
`builder(GLOBALS_SNIPPET)…withBindGroupLayout(BindGroupLayouts.IN_SAMPLER).withPrimitiveTopology(TRIANGLES)`，
且 `POST_PROCESSING_SNIPPET` 是同配方的 public 版）。证据强度同本仓
`ScopeBodyRenderTypes` 引 `BindGroupLayouts.<clinit>` 偏移的做法，但没有本仓编译背书：

| 常量 | 若不存在时的退路 |
|---|---|
| `RenderPipelines.POST_PROCESSING_SNIPPET` | 改从本仓已验证存在的 `RenderPipelines.MATRICES_FOG_SNIPPET` 起建（它含 Globals），其余 layout 追加在后 |
| `BindGroupLayouts.IN_SAMPLER` | 自建 `BindGroupLayout.builder().withSampler("InSampler").build()`（本仓 `ScopeBodyRenderTypes` 的掩码 layout 就是这个写法） |
| `BindGroupLayouts.DYNAMIC_TRANSFORMS` | 同法自建；若 `ColorModulator` 走不通，改把倍率塞进 `InSampler` 的 alpha 之外另开一张 1×1 纹理（要同时改 shader） |
| `ColorTargetState.WRITE_COLOR` | 直接用 `7`（姊妹分支注释：vanilla `ENTITY_OUTLINE_BLIT` 传的就是 7） |
| `FilterMode.LINEAR` | 枚举常量，同类的 `FilterMode.NEAREST` 本仓已在用 |

### 5.2 混入目标与字段名（对不上会**启动即崩**，不是 PIP 失效）

| # | 假设 | 位置 | 失败形态 |
|---|---|---|---|
| 1 | `GameRenderer` 的 `resourcePool` / `fogRenderer` / `gameRenderState` 三个字段（`@Shadow`） | `GameRendererMixin` | **混入失败即崩启动** |
| 2 | `LevelRenderer#render(GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState, Matrix4fc, GpuBufferSlice, Vector4f, boolean)` 描述符 | `GameRendererMixin` 的两个 `INVOKE` 注入点 | 混入告警/失效（`defaultRequire=1`，会炸） |
| 3 | `GameRenderer#extract(DeltaTracker, boolean)` 与 `mainRenderTarget()` | 同上 | 同上 |
| 4 | `FeatureRenderDispatcher.preparedFrame` 字段名、`PreparedFrame.context` 字段名 | `FeatureRenderDispatcherMixin` / `PreparedFrameAccessor` | 混入失败 |
| 5 | `LevelRenderer.submitNodeStorage` 字段名 | `LevelRendererAccessor` | 混入失败 |
| 6 | `SimpleFeatureRenderPhase#clear()`、`TranslucentFeatureRenderPhase#sortInto` 里的 `List#clear()` | 两个 phase 混入 | 注入点找不到 |
| 7 | `SkyRenderer.renderTarget` 字段 | `SkyRendererMixin` | 注入点找不到 |
| 8 | `mc.levelRenderer` 可访问（public 字段） | `ScopePipRenderer#shouldPreserveSubmits` 等 | 编译错 |

第 1–7 条若对不上，表现是**启动即崩**（不是 PIP 失效）。首次构建若在这些点上报错，
按 NeoForge 26.2 的实际名字改字段名/描述符即可，逻辑不用动。

---

## 5.5 2026-08-30 实机第一轮：剪裁盒拿错了矩阵（已修正，未复验）

### 现象（用户报）

> 以面对北方到正西/正东/正下/正上为一个半球形时表现正常；另一个半球越面向正南方、
> 视角越平时，PIP 的范围作为一个矩形越大（但也不是很大）；越往东/西偏矩形的长越小、
> 越往上下偏矩形的宽越窄。

### 根因

`GameRendererMixin` 在 `renderItemInHand` 的 HEAD 把<b>第三个参数</b>当成投影矩阵
送给了 `ScopeMaskRenderer.setHandProjection`。它在 26.2 上<b>不是投影矩阵，是视图矩阵</b>：

| | m00 | m11 | 随朝向变化？ |
|---|---|---|---|
| 透视投影 | `1/(aspect·tan(fovY/2))`，恒正 | `1/tan(fovY/2)`，恒正 | 否 |
| 视图/旋转矩阵 | `∝ cos(yaw)` | `∝ cos(pitch)` | **是，且变号** |

三条现象逐条对上：

1. **硬边界（整半球正常、另一半球异常）**：`hasMaskBounds()` 的判据是
   `projectionP00 > 0`。负的那半个球面 `m00 < 0` ⇒ 判据不成立 ⇒ **硬件剪裁压根没开**，
   只剩着色器里的软掩码 ⇒ 看起来「正常」。
2. **尺寸随 yaw/pitch 缩小**：剪裁盒 = `|cos(yaw)| · slope`，比真实孔径小。
   着色器里「掩码为假就 discard」是软约束，**只有盒子比孔径小时才看得见盒子**，
   于是看到的就是那个矩形 —— 往东西偏 `|cos| → 0` 长变短，往上下偏 `|cos(pitch)| → 0`
   宽变窄，正南 + 视角平时两者同时最大。
3. **「但不是很大」**：正南水平时盒子约为孔径的 `1/0.8 ≈ 1.25`（横）与 `1/1.43 ≈ 0.70`（纵），
   横向由孔径兜住、纵向被盒子切掉 ⇒ 一个扁矩形。

### 修正

不靠参数名（混淆表里没意义），靠<b>透视投影的恒定特征</b>判定，并按优先级取用：

1. `renderItemInHand` 第三参数 —— **仅当** `m33 == 0 && m00 > 0 && m11 > 0`
   （`m33 == 0` 是透视投影独有：视图/模型矩阵与正交投影都是 1）；
2. 否则用 `CameraRenderState#projectionMatrix` —— 本仓已在用的真投影
   （`ScopePipRenderer#buildNarrowProjection` 由 `m11 = 1/tan(fovY/2)` 从它反解当帧 FOV）；
3. 都不是 ⇒ 返回 `null` ⇒ 不开硬件剪裁，退回纯掩码约束（旧行为）。

`ScopeMaskRenderer#setHandProjection` 里再加一道同样的门：不是透视投影就当取不到。
失败方向是刻意的 —— 盒子偏大由掩码兜底（无害），盒子偏小才会切出矩形（有害）。

PIP 默认 `WorldZoomShare = 0`，世界 FOV 不缩放，所以 ② 与手持那一遍用的是同一个投影；
把 share 调大时盒子只会偏大，属无害方向。

**未复验**（沙箱无 JDK/游戏）。判定的第一条证据是用户那句「整半球正常」——
投影矩阵造不出这种边界。

## 6. 怎么验（给实机那一轮）

前置：`ScopeMaskEnable` 必须是 true（PIP 靠目镜掩码知道「镜内是哪些像素」）。

1. 关着的时候（默认）先跑一遍原有回归：开镜仍是整屏变焦，镜内裁切与同步前一致。
2. 开 `ScopePipEnable`：
   - 日志应出现 `[TACZ Scope] Scope PIP gate -> ACTIVE`；没出现就说明卡在某一道
     闸门上，日志会把**卡在哪一道**直接打印出来（每种理由只打一次）。
   - 预期观感：镜外 1×、只有镜片里放大；6× 以上会明显变软（这是原理决定的，
     见 `ScopePipRenderer` 类注释）。
3. 想验「放大画面溢出镜外」这类症状时，再开 `ScopePipDebugPaintLens`（合成覆盖区
   涂品红）与 `ScopePipDebugTrace`（打印 target 解析顺序，只记前几帧）。
4. 二次渲染（`ScopePipRerender`）与光影组合属于实验路径，**不要在没有实机记录前
   写成已验证**。

---

## 7. 风险与回退

- 所有新代码的入口都先过 `RenderConfig.SCOPE_PIP_ENABLE`，默认 false；
- 运行期任何异常都会把 `failed` 置位并永久停用，日志会写明原因，
  世界 FOV 的整屏变焦在**下一帧**自动接管（判据每帧重问、不缓存）；
- Voxy 的三条混入由 `VoxyCompatMixinPlugin` 把着，Voxy 不在时整份配置不应用；
- 真要整体回退：把 `ScopePipEnable` 关掉即可，运行路径回到同步前；代码层面的回退
  是还原本次提交的这批文件。

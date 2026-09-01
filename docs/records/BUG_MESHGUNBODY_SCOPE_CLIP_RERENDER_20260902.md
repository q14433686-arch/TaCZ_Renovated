# BUG 记录：高模枪枪身仅在二次渲染时被「高倍镜」裁切（2026-09-02）

- **分支**：`arena/01a05e66-tacz-renovated`（NeoForge 26.2）。PR #28 在办。
- **报告**（用户原话）：「高模枪的枪身（配件未知）在**仅开启二次渲染**
  （`ScopePipRerender`）时才会被"高倍镜"裁切，否则不会，**与是否开启光影无关**。」
- **波及面**：用户确认 26.2 两条线（本线 + Fabric 姊妹线 `arena/01a05e3e`）
  独有；1.21.11 / 26.1.2 线没有。

## 1. 波及面为什么恰好是这两条线（已核对）

| 线 | 二次渲染 PIP | TML mesh 枪 | 高倍开镜时枪身裁剪 |
|---|---|---|---|
| 1.21.11 | 无 PIP | 无 TML | 无 mesh 枪身可言 |
| 26.1.2（`origin/26.1.2` @ a3d32410） | **无** `ScopePipRenderer` | **无** `PolyMeshGpuRenderer`（该 ref） | 仅 cube 裁剪（`clipForViewmodel` 存在） |
| 26.2 两条线 | 有（重投影 + 二次渲染两形态） | 有（R5 轮起含 mesh 枪身裁剪） | cube + mesh 都裁 |

「仅二次渲染」这一触发条件只能出现在有二次渲染 PIP 的线上 —— 与用户观察
吻合。

## 2. 静态排查：裁剪链本身**模式无关**（穷举结论）

`mesh/cube 枪身被孔径裁`的完整链条，逐环核对后确认与 PIP 形态无关
（重投影 / 二次渲染 / PIP 关，三者逐位相同，无光影）：

1. **gate**：`ScopeBodyRenderTypes.maskReadyForViewmodel(true)` ——
   `SCOPE_MASK_ENABLE` + `!Iris 屏蔽` + `ScopeMaskGeometry 非空` +
   `isViewmodelClipEnabled` + `syncToMaskTarget`；四个调用方（cube 镜身 /
   cube 枪身配件 / mesh 自定义 pass / mesh Iris pass）共用同一函数，
   **无一读 PIP 状态**。
2. **掩码内容**：只在手部 pass 的阶段边界绘制（`renderAtPhaseBoundary`
   要求 `isInHandPass`），用**主投影**；无光影下镜内那一遍是纯世界渲染
   （`levelRenderer.render`，不含手部 —— MV-PROBE 字节码取证已证
   26.2 世界 pass 不经 `renderAllFeatures`），不会画第二张掩码。
3. **绘制顺序 / 目标**：重投影与二次渲染在阶段边界的
   「掩码 → 合成 → executeSolid → renderAfterSolid(mesh)」顺序完全一致；
   手部绘制取 `mainRenderTarget()` 时二次渲染的重定向窗口早已关闭
   （`renderScopeView` 的 try/finally）。
4. **lightmap**：`resolveLightmap = gameRenderer.levelLightmap()`，
   模式无关。

⇒ **无光影形态下，「裁剪 gate 只在二次渲染生效」在代码上不成立。**
用户「与光影无关」的说法若要坐实，存在第二个机制 —— 本轮已加一次性
诊断（§4）让下一次 A/B 直接给出每种形态下的 gate 取值，不再靠画面反推。

## 3. 钉死的机制：Iris 二次渲染时**镜内那一遍自带手部 pass**

**证据一 —— 用户实机 latest.log（她线，2026-09-02 19:29，Iris 1.11.2+mc26.2
+ Sodium 0.9.1 + ComplementaryUnbound r5.8.1 + 4.25x 二次渲染）**：
掩码绘制调用栈显示手部 pass 在 `LevelRenderer.render` **帧图内部**：

```
FeatureRenderDispatcher.renderAllFeatures(110)
  ← net.irisshaders.iris.pathways.HandRenderer.renderSolid(119)
  ← LevelRenderer.handler$boc000$iris$beginTranslucents(5263)
  ← LevelRenderer.lambda$addMainPass$0(440)   ← 帧图主 pass
  ← LevelRenderer.render(240)
```

**证据二 —— Iris 26.2 分支 `HandRenderer` 源码（线上核对，与 1.11.2+mc26.2
对应）**：`renderSolid(Matrix4fc, float, Camera, CameraRenderState,
GameRenderer, WorldRenderingPipeline)` 体内
`iris$renderHandsWithCustomRenderer(…)` + `renderAllFeatures(submitNodeCollector)`
—— **视模提交 + 绘制都在这个方法里**；`renderTranslucent` 同构（一帧两趟）。

**机制闭环**：

1. 二次渲染的镜内那一遍是又一次 `levelRenderer.render`
   （`ScopePipRenderer#renderScopeView`：窄投影 + 主 target 重定向）。
2. Iris 下这一次 render **内含手部 pass**（证据一/二）：视模立方体
   （枪身/镜筒/准星）按**窄投影**画进镜内画面；其中立方体枪身走
   `clipForViewmodel`（Iris `scope_body_clipped` 管线），而那一遍阶段边界
   刚画的掩码是**窄投影孔径** ⇒ 枪身被裁出一个孔径形状的孔。
3. 合成把这整幅（世界 + 被孔径裁孔的巨型枪身）贴进主画面孔径 ⇒
   用户通过镜片看到「枪身被高倍镜裁切」。
4. 重投影模式没有第二次 `levelRenderer.render` ⇒ 没有镜内手部 pass ⇒
   「否则不会」。1.21.11 / 26.1.2 无二次渲染 ⇒ 触发不了。
5. 历史注脚：`renderAfterSolid` 里 mesh 表那道 `isInsideScopeLevelRender`
   闸（R5 移植件，注释原文「孔径里本来就该是干净的世界画面，不该有枪件」）
   **只挡了 mesh 表**；立方体（executeSolid）当年漏挡 —— 本 bug 即其症状。

## 4. 修复（本轮）

| 件 | 内容 |
|---|---|
| **`IrisHandRendererMixin`（新）** | `@Mixin(targets="net.irisshaders.iris.pathways.HandRenderer")`：`renderSolid` / `renderTranslucent` 两个 HEAD 注入，`ScopePipRenderer.isInsideScopeLevelRender()` 时 `ci.cancel()` —— 镜内那一遍的手部 pass 整趟不跑（视模不进镜内、不消费 HAND_DRAWS、不画窄投影掩码）。Iris 类型用 `@Coerce Object`（本仓不引 Iris 编译依赖，与 `IrisGlCommandEncoderMixin` 同手法）；注册进 `tacz.iris.mixins.json`（plugin 已在 Iris 缺席时整体跳过该配置）。首帧跳过打一行 log —— 该行同时是「注入匹配成功」的实机证据（Iris 升级改签名时 `defaultRequire=0` 静默脱靶，靠这行定位）。 |
| **`ScopeBodyRenderTypes` 诊断** | `maskReadyForViewmodel` 外包一层 log-once：每种「判定结果 × 帧形态」组合一行（封顶 8 行），摊出全部 gate 位 + PIP 形态 + 手部 pass 状态。签名只用帧内稳定的位；封顶后零字符串开销。 |
| **`ScopePipRenderer.pipDiagnosticState()`** | 供诊断拼日志的 PIP 状态快照（`rerender` / `sceneCaptured` / `inScopePass`，刻意不含随帧计数器）。 |
| **`renderAfterSolid` 注释** | mesh 闸标注为「第二道防线」（注入脱靶时兜底，保留不删）。 |
| 文档 | `MESH_LOADER.md` §5.2 第 18 条；CHANGELOG；本记录。 |

**取消整趟的安全性**（逐条核过 Iris 源码）：HEAD 取消 ⇒ 方法体未执行，
`ACTIVE`/`renderingSolid`/投影备份/相位切换均未发生，无状态残留；
`renderTranslucent` 的 `bufferSource.endFrame()` 被跳过无碍（镜内那趟
不分配手部缓冲，主画面那趟照常每帧一次）；镜内孤立管线（tacz:scope_pip）
相位停在世界末相位，该管线此后到拷贝/归还之间无使用点，主画面管线是
另一实例。

## 5. 姊妹线等价（她需要同样的修复）

Iris 的 `HandRenderer` 在 **common** 源码集（fabric / neoforge 两 loader
共用同一份类与方法签名）⇒ 她线（Fabric 26.2）移植 = 同一个 mixin 文件
+ 她线 `tacz.iris.mixins.json` 注册一行 + 同一套诊断，无 loader 差异
需处理。诊断与注释直接可搬。

## 6. 证据级别与未决项（如实）

- **证据级别**：静态闭环（裁剪链模式无关性穷举 + Iris 源码核对）+
  用户实机日志调用栈。本线 CI 编译门与实机**均未跑**，**不宣称已修**。
- **未决**：用户「无光影也复现」若坐实，按 §2 的穷举结论需第二机制 ——
  请用户 A/B 复测（重投影 vs 二次渲染，各带一次高倍开镜），回传
  latest.log：新的 `[diag] viewmodel clip gate -> …` 行会直接给出每种
  形态下 gate 的取值与拦截位；无光影复现的话请补一句「重投影模式下
  枪身是否看得见穿进镜片」以区分「镜内含枪」与「主画面被裁」两种观感。

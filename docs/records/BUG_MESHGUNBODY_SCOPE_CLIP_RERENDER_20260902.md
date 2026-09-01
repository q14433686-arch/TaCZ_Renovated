# BUG 记录：高模枪身未被高倍镜孔径裁切（「仅二次渲染时看起来裁了」，2026-09-02）

- **分支**：`arena/01a05e66-tacz-renovated`（NeoForge 26.2）。PR #28 在办。
- **报告**（用户原话）：「高模枪的枪身（配件未知）在**仅开启二次渲染**
  （`ScopePipRerender`）时才会被"高倍镜"裁切，否则不会，与是否开启光影无关。」
- **用户澄清（方向纠正）**：高倍镜下裁切高模枪身、配件等是**正确行为**
  （与 cube 枪身、26.1.2 基线一致）；**不裁切才是 bug**。
- **波及面**：26.2 两条线（本线 + Fabric 姊妹线 `arena/01a05e3e`）独有；
  1.21.11 / 26.1.2 线无 TML mesh 枪身，无从谈起。

## 1. 根因：裁剪判据的**时序**错误（R5 移植件被静默禁用）

mesh 手部表的绘制点在 `renderAllFeatures` 的 **executeSolid 之后**
（`FeatureRenderDispatcherMixin#tacz$polyMeshGpuAfterSolid`，shift=AFTER），
而阶段边界的掩码绘制在 executeSolid **之前**（`tacz$scopeMaskAtPhaseBoundary`，
shift=BEFORE），其 `finally` **无条件清空** `ScopeMaskGeometry`
（entries + `viewmodelClipEnabled`，防「收起瞄具后掩码粘住」）。

每帧时序（无光影，重投影 / 二次渲染 / PIP 关三者相同）：

```
手部 submit（瞄具先登记目镜几何 + enableViewmodelClip）
  → 阶段边界：renderAtPhaseBoundary 画掩码 → finally: ScopeMaskGeometry.clear()
  → executeSolid（cube 的裁剪 RenderType 在 submit 时已定 —— 几何在场，正确）
  → renderAfterSolid → drawList：maskReadyForViewmodel(true)
        → ScopeMaskGeometry.isEmpty() == true（刚被清）→ 恒 false
        → mesh 枪身从未被裁（自定义 pass 与 Iris RenderType 两路同病）
```

cube 枪身/配件的 `clipForViewmodel` 在 **submit 时**判定（几何在场）⇒ 一直
正常工作；mesh 在**绘制时**判定（几何已清）⇒ R5 移植（她 `7227ff99`）的
裁剪**从未生效** —— 两线同病（同一 26.2 架构 + 同一份移植）。

## 2. 为什么表现为「仅二次渲染时看起来裁了」

mesh 枪身实际在**所有形态**都没裁（主画面枪管一直穿进镜片画面）；差异只在
**镜内画面**的内容：

- **二次渲染**：镜内 = 全新世界渲染（无光影）/ 不含 mesh 视模（光影，
  mesh 表有 `isInsideScopeLevelRender` 镜内闸）⇒ 枪身在镜内「消失」
  ⇒ 看起来裁了（恰好是正确观感）；
- **重投影**：镜内 = 主画面中央 1/Z 区域的重采样 ⇒ 主画面里没裁的枪身
  被一并采进镜片 ⇒ 枪身穿进镜内画面 ⇒ 「不裁」（错误观感）；
- 用户据此报「仅二次渲染裁切」。方向纠正后，真正缺陷 = **其余形态下
  mesh 枪身未被裁**。

## 3. 误判与回滚（如实记录）

第一轮把报告读反（以为「二次渲染误裁」），提交 `ae30b9a`（Iris 镜内手部
pass 取消 + gate 诊断）；用户澄清后由 `d918fc1` 整笔撤销。误判的教训：
「裁切」二字没有指明方向时，先核对**基线行为**（cube 枪身 / 26.1.2 线
怎么裁）再定缺陷方向。

## 4. 修复（本轮）

| 件 | 内容 |
|---|---|
| `ScopeMaskRenderer` | 新增帧快照 `viewmodelClipMaskThisFrame`：`drawMask` 成功路径上、`finally` 清空**之前**记下 `isViewmodelClipEnabled`；`beginFrame` 复位。访问器 `hasViewmodelClipMaskThisFrame()`（与 `maskDrawnThisFrame` 与）。 |
| `ScopeBodyRenderTypes` | 新增绘制时变体 `maskReadyForViewmodelAtDraw()`（同义 gate：掩码开关 / 光影回退 / **帧快照** / `syncToMaskTarget`）与 `clipForViewmodelAtDraw()`（Iris RenderType 路用）。submit 时的 `maskReadyForViewmodel` / `clipForViewmodel` 原样保留（cube / 配件 / 手臂 / 火光仍在 submit 时判定，几何在场，正确）。 |
| `PolyMeshGpuRenderer` | `drawList`（自定义 pass）与 `drawViaRenderTypeCore`（Iris 路）的判据换成绘制时变体；裁剪首次生效打一行 log-once（`GPU hand mesh pass: ocular clip ACTIVE`）——上轮误判的教训：让「裁剪生效」在日志里有直接证据。 |

**同开同关论证**：帧快照为真 ⇔ 本帧阶段边界画过掩码且画时
`viewmodelClipEnabled=true` ⇔ 同帧 submit 时 `maskReadyForViewmodel(true)`
为真（几何在场 + 同 flag）⇒ mesh 与 cube 的裁剪在任何帧形态下同时开关
（含低倍 sight 的 reticle-only 掩码不裁枪身、光影回退、配置关闭、
掩码失败熔断——逐条对照过，行为一致）。

## 5. 姊妹线等价

她线同架构（阶段边界 finally 清空 + mesh 绘制在 executeSolid 之后）+
同一份 R5 移植 ⇒ 同一潜在 bug ⇒ **同一套修复**：`ScopeMaskRenderer`
快照 + `ScopeBodyRenderTypes` 两个 AtDraw 变体 + `PolyMeshGpuRenderer`
两处判据 + log-once，无 loader 差异（三文件均无加载器专属代码）。
她的 `7227ff99` 实机证据证的是**缺口**（枪管穿镜），修复本身从未生效 ——
本修复落地前，她线重投影模式下枪管同样穿镜。

## 6. 证据级别与实机判据

- **证据级别**：静态闭环（时序穷举 + 逐 gate 对照）；CI 编译在跑、
  实机未跑，**不宣称已修**。
- **实机判据**（MESH_LOADER §5.2 第 18 条）：高倍镜开镜，mesh 枪身/
  配件与 cube 一样被孔径裁掉（重投影与二次渲染**都**裁）；镜内画面
  干净；日志出现一次 `GPU hand mesh pass: ocular clip ACTIVE`（这一行
  出现即证明判据不再恒 false）。

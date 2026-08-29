# 光影下遮光环被镜内合成盖掉（2026-08-30）

状态：**已动源码，未编译、未实机**（沙箱无 JDK / Maven 源）。禁止写 PASS。
复测：开光影 + `ScopePipEnable=true` + `ScopePipAllowShaderPacks=true`，对着光源开镜。

---

## 1. 症状

开了镜内画中画（PIP）+ 光影包，面对光开镜时，**目镜的遮光环变成半透明**
（镜筒内那一圈黑的没了，透出放大的世界）。无光影时同一支枪完全正常。

用户复现条件：**必须开光影才出现**。

## 2. 真因：光影下「合成 vs 手持」的先后关系是反的

| 路径 | 合成时机 | 与手持的先后 |
|---|---|---|
| 无光影 | `compositeAtPhaseBoundary()`：`renderAllFeatures` 里 `executeSolid()` **之前** | 合成 → 手持 ⇒ 手持（含遮光环）盖在合成之上 ✔ |
| 有光影 | `compositeAfterLevelUnderShaders()`：`LevelRenderer#render` 调用**之后** | 手持 → 合成 ✘ |

Iris 把整个手部 pass 搬进了 `LevelRenderer#render` 内部，而「最终画面」要等整条
Iris 管线收工才存在，所以 PIP 的合成只能排在 `LevelRenderer#render` 的返回处
（见 `captureScene` 那段注释）。于是**合成把刚刚画好的物理目镜框整片盖掉**。

被盖掉的正是案例⑨（`fd8d1bf`，姊妹项目 2026-08-12，PIP 之前）用无裁剪重画
救回来的那部分：`ocular_ring` 的内圈与目镜投影（掩码）重叠，而无光影时它是
在合成**之后**画的，能盖住合成多铺出来的那一圈；光影下这个保护整个失效。

为什么「面对光」才看得见：被盖掉的地方露出的是放大的世界拷贝 —— 世界本身
是暗的时候与黑环无异，对着光就是一片亮。

## 3. 机制来源：1.21.11 邻链 commit `2710c7c`

姊妹项目 1.21.11 分支早就处理过同一类问题（遮光环被光影包的后置 pass 盖掉）：

- `ScopeFinalOverlayState` —— 排队 + 延后重画；
- `scope_ring_final.fsh` —— entity 片元路径去掉 `apply_fog()`，「作为物理前景层
  在 Iris 跑完所有 composite/final pass 之后提交」；
- `IrisFinalScopeOverlayMixin` —— 挂在 `IrisRenderingPipeline#finalizeLevelRendering` TAIL。

本仓（26.2 掩码架构）没有这三个文件（`git ls-tree fcaa2b8` 确认只有
`scope_body.fsh` / `scope_body.vsh` / `scope_pip.fsh`），所以需要移植。

## 4. 26.2 的三处适配（不能照抄 1.21.11）

1. **不自建 `FeatureRenderDispatcher`**：26.2 的构造函数改成吃 `RenderBuffers`
   （NeoForge 26.2 迁移指南原文），1.21.11 那版 7 参构造在这里编译不过。
   改用官方配方 `Minecraft.getInstance().gameRenderer.featureRenderDispatcher()
   .renderAllFeatures(storage)`。
2. **不调 `SubmitNodeStorage#endFrame`**：26.2 里它与 `clear` 一起被移除，
   `renderAllFeatures` 自己收尾。
3. **刷新点不是 Iris 的 `finalizeLevelRendering` TAIL**：那一步跑在
   `LevelRenderer#render` 内部，**早于**我们的合成。改由 `GameRendererMixin`
   在 `compositeAfterLevelUnderShaders()` 之后直接刷新。

## 5. 实现

| 文件 | 改动 |
|---|---|
| `shaders/core/scope_ring_final.fsh` | 新增：`scope_body.fsh` 去掉 SCOPE_MASK 段与 `apply_fog` |
| `ScopeBodyRenderTypes` | 新增 `RING_FINAL_PIPELINE`（ENTITY_SNIPPET + 上述 fsh）+ `ringFinal(texture)` + 注册；**刻意不给 Iris 的 HAND 程序注册**（注册了就会被塞回光影管线，等于白做） |
| `ScopeFinalRingOverlay` | 新增：排队 / 捕获手持变换 / 刷新重画 |
| `BedrockAttachmentModel#submitOcularRingPlain` | 仅当 `ScopePipRenderer.wantsIrisComposite()` 为真时改排队，其余路径逐位不变 |
| `FeatureRenderDispatcherMixin` | 阶段边界（掩码之后、手部几何之前）捕获手持投影/模型视图 |
| `GameRendererMixin` | 光影合成之后刷新 |
| `ScopePipRenderer#beginFrame` | 帧首清空队列（防跨帧残留） |

### 为什么变换要在阶段边界取，不能在排队时取

排队发生在 `submit`（收集节点）阶段，那时 `RenderSystem` 里挂的还是**世界**的
投影/模型视图，手持那一遍用的是另一套（固定窄 FOV）。几何快照的顶点已套过
poseStack（世界坐标）、节点 pose 是单位矩阵，落点完全取决于绘制那一刻的两个
矩阵 —— 取错就整个飘出画面。阶段边界正是「矩阵就位、手部几何未画」的那一刻，
与 `ScopeMaskRenderer#renderAtPhaseBoundary` 取掩码投影的位置逐字相同。
（只取切片对象，不读回内容 —— 那 64 字节在光影下不可读，本仓日志有实录。）

## 6. 未验证点 / 已知缺口

1. **未经编译与实机。** 三处 26.2 API（`new SubmitNodeStorage()`、
   `storage.order(int)`、`gameRenderer.featureRenderDispatcher()`）取自 NeoForge
   官方 26.2 迁移指南与 rendering/feature 文档，非本仓既有用法。
2. **深度**：延后重画沿用 vanilla entity cutout 的深度测试（靠它做自遮挡，
   因为 `withCull(false)` 会画背面）。若光影的 final pass 改写了主深度缓冲，
   可能导致目镜框整片被判遮挡而不可见 —— 届时给这条管线关掉深度测试即可。
3. **准星**：同一机理下，光影 + PIP 时蚀刻/发光准星**同样**会被合成盖住
   （它也在手持那一遍画）。本次只修遮光环（用户报的就是这个），准星是否受影响
   待实测确认；若受影响，`ScopeFinalRingOverlay` 已留好结构，加一路
   `queueReticle`（order 更小，画在目镜框之前）+ 对应管线即可。

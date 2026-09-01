# TML GPU 第 2 步 v2：手部 flush 钩子（1.21.11 字节码审计）

> 适用版本：Minecraft 1.21.11 + NeoForge 21.11.x。
> 本文是 `cn.sh1rocu.tacz.compat.meshloader.render.PolyMeshGpuRenderer` 的配套证据文档：
> GPU 路径第 2 步 v2 把「光影下的手部 pass」画进 Iris 自己的那次手部 flush 里。
> 绘制点选在哪些方法、为什么选在那里，全靠本文记录的 1.21.11 字节码审计
> （CI javap 实测）支撑。审计时点：2026-08-31，游戏/NeoForge 版本见 `gradle.properties`。

## 背景

第 1 步（无光影第一人称 GPU 烘焙）的绘制点最初选在
`ItemInHandRenderer#renderItemInHand` 的 RETURN —— 结果引入「相对人物世界位置恒定」的
bug：该点处矩阵已被还原。第 2 步 v2 把绘制点改到**手部几何「当次 flush」之后**：

- 1.21.11 的手部几何不是延迟到世界渲染末尾统一 flush 的；
- `ItemInHandRenderer#renderHandsWithItems` 自己就以
  `featureRenderDispatcher.renderAllFeatures()` + `bufferSource.endBatch()` 收尾；
- Iris 正是通过 `@WrapWithCondition` / `@WrapOperation` 这两个调用来接管手部绘制
  （见 `MixinItemInHandRenderer`）。

因此本仓的绘制点放在**该方法返回处**（= 那次 flush 的紧后，仍在同一条栈上）：
`ItemInHandRendererMixin#tacz$drawMeshGpuAfterHandFeatureFlush`，
`@Inject(renderHandsWithItems, RETURN)`，`require=0`。

## §1 输出目标选择：override 优先（与 vanilla `RenderType#draw` 逐条同款）

`PolyMeshGpuRenderer#drawList` 开自己的 pass 时，输出目标按原版 `RenderType#draw`
的同款规则解析（1.21.11 字节码审计）：

1. 颜色视图：`RenderSystem.outputColorTextureOverride != null` 时用它，否则
   `mainTarget.getColorTextureView()`；
2. 深度视图：**仅当** `mainTarget.useDepth` 为真时才挂深度附着 ——
   此时再按 `RenderSystem.outputDepthTextureOverride` 优先解析，否则
   `mainTarget.getDepthTextureView()`。

原版刚刚 flush 的那批手部几何用的就是这两个值；跟着它走：

- 无光影时落进主渲染目标；
- 光影时落进 Iris 当刻绑定的 gbuffer：Iris 1.10.x 的 `MixinGlCommandEncoder` 用
  `@Redirect` 拦掉了 `createRenderPass` 里的 `glBindFramebuffer`（条件
  `ImmediateState.safeToMultiply` / 阴影 pass），并在 `trySetup` 里只把
  「非 ExtendedShader」的 pass 复位回原版 FBO —— 因此在世界渲染阶段内新建的 pass
  会留在 Iris 绑定的 framebuffer 上。

## §2 手部 flush 结构与 Iris 接管

- 无光影：`renderHandsWithItems` RETURN 处的 ModelView / Projection 与原版刚用过的
  完全一致 —— 不再需要在 submit 时刻偷拍 `Bᵀ`（第 1 步 bug 的根源）。
- 光影：Iris 用 `@WrapWithCondition` / `@WrapOperation` 把上面那两个 flush 调用换成
  它自己的 `HandRenderer#endRender()`，并且它是从 `iris$renderHandsWithCustomRenderer`
  → **同一个** `renderHandsWithItems` 进来的，所以同一个注入点天然落在 Iris 的
  `HAND_SOLID` 阶段内：gbuffer 还绑着、投影是 Iris 的手部投影、ModelView 与刚 flush 完
  的手部几何同一个。在这里开自己的 pass，输出目标按 §1 规则解析，因此常驻 VBO
  进得了 `gbuffers_hand`。**不需要 mixin Iris 内部类。**

## §3 字节码审计：`renderHandsWithItems` 与 `renderAllFeatures`

CI 上的 1.21.11 字节码核实（javap 实测）：

1. `ItemInHandRenderer#renderHandsWithItems` 共 **143 行、只有 1 个 return**；
   那两个 flush 调用（`renderAllFeatures()` + `endBatch()`）就是**倒数第二/最后一条
   指令**。⇒ RETURN 注入点 = flush 紧后，同一栈帧上，矩阵语义与刚 flush 的几何一致。
2. `FeatureRenderDispatcher#renderAllFeatures` 里**根本没有 `RenderPass` 这个局部
   变量** —— 它只是逐个调用各 feature renderer；每个批次真正的 `RenderPass` 在
   `RenderType#draw(MeshData)` 内部创建（局部槽位 13），并按
   `RenderSystem.outputColorTextureOverride / outputDepthTextureOverride` 解析输出目标。

## §4 世界绘制点：`renderAllFeatures` 返回处（世界表消费点）

世界语境（第三人称手持 / 掉落物 / 展示框 / 展示台雕像）的 GPU 表 `WORLD_DRAWS`
消费点选在**世界那一次** `FeatureRenderDispatcher#renderAllFeatures` 的返回处：

1.21.11 的 `LevelRenderer` 主通道里那一段是（CI javap 实测）：

```text
popPush("renderFeatures") -> renderAllFeatures() -> bufferSource.endLastBatch()
```

即地形深度已就绪、本帧立方体/实体几何还压在 builder 里等 `endLastBatch`；
此刻 `RenderSystem.getModelViewMatrix()` 正是那些批次待会儿在 `RenderType#draw`
里要写进 `DynamicTransforms.ModelViewMat` 的**同一个值**（1.21.11 的
`RenderType#draw` 就是在 draw 当刻现取 `getModelViewMatrix()`）。

因此「GPU 骨骼用 flush 当刻的 MV + submit 当刻的骨骼 pose」与 collector
「pose 烘进顶点 + flush 当刻 MV」**逐帧等价** —— 这正是隔壁 26.2 分支踩的
「相对视角固定」坑的解法：MV 不能取自别的时刻。

## 兜底：钩子存活证明

两条路（手部/世界）共用「钩子存活证明」兜底：`PolyMeshGpuRenderer#shouldSubmitGpu`
只有在**上一帧真的跑过 flush 钩子**时才允许跳过 collector。映射漂移、mixin 没装上
（`require=0` 静默失效）→ 下一帧自动回 collector，不会出现「collector 被跳过 +
GPU 没画」的枪体消失。

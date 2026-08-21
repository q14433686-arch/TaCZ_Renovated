# WP-262-3 证据：26.2 渲染层与 GL-only 瞄具

日期：2026-08-21  
目标：Minecraft 26.2 + NeoForge 26.2.0.64

## 技术决策

首发采用 **OpenGL depth-aperture 前滚**，不切换到 refab 26.2 的离屏 scope-mask
架构，也不实现 Vulkan 平行 depth copy：

1. 当前 NeoForge 26.1.2 基线的 depth-aperture 已有完整的世界深度备份、目镜深度、
   枪身遮挡、选择性恢复、准星/火光裁剪与 Iris hand shader 桥；
2. 26.2 OpenGL 仍可在真实 draw 边界取得同一深度附件，且 26.2 的 framebuffer cache
   可能换 FBO id、共享 depth attachment——本实现原本就按 attachment identity 比较，
   不按 FBO id 比较；
3. refab 的 26.2 mask bridge 是游戏语义参考，但替换现有架构会重新打开其 hull/第三方
   目镜几何/Iris 掩码风险面；没有证据说明 26.2 GL 迫使我们这样重写；
4. Vulkan 是实验后端，当前 raw depth copy 明确依赖 OpenGL FBO/texture API；首发在 Vulkan
   下隐藏不透明 ocular、改走普通未掩码 RenderType，并输出一次降级日志。不会加载或编译
   GL-only 自定义 pipeline，也不会静默尝试 OpenGL 调用；
5. Aperture 尚不是本期已发布依赖，`ShaderCompat` 继续只封装 Iris/OpenGL。

这是一项有意降级，不宣称 Vulkan 瞄具 depth、Iris-on-Vulkan 或 Aperture 支持。

## 证据输入

### ① Minecraft 26.2 未混淆 classfile / shader

merged jar SHA-256：
`703604cea43c6e720e7ce658ea7565c4a2e5db5d7ab428113776bbf8a5196b21`。
来源为游戏语义上游 refab `26.2(main)` commit
`5a29159902f5dddf26cfd0cd0f0fa3b75fbe94e6` 保存的未混淆 Loom merged jar。

已逐项 `javap`：

| 类#方法 | 26.2 descriptor / 结论 |
|---|---|
| `RenderPipeline#getBindGroupLayouts()` | `()List<BindGroupLayout>`；替代 sampler/uniform 列表 |
| `RenderPipeline#getColorTargetStates()` | `()[ColorTargetState]`；支持多 color target |
| `RenderPipeline#getVertexFormatBindings()` | `()[VertexFormat]`；支持多 vertex buffer |
| `RenderPipeline#getPrimitiveTopology()` | `()PrimitiveTopology` |
| `RenderPipeline.Builder#withBindGroupLayout` | `(BindGroupLayout)Builder` |
| `RenderPipeline.Builder#withVertexBinding` | `(int, VertexFormat)Builder` |
| `RenderPipeline.Builder#withPrimitiveTopology` | `(PrimitiveTopology)Builder` |
| `ColorTargetState#<init>` | `(Optional<BlendFunction>, GpuFormat, int)` |
| `RenderType#prepare()` | `()PreparedRenderType`；`RenderType#draw` 已删除 |
| `PreparedRenderType#drawFromBuffer` | `(StagedVertexBuffer.ExecuteInfo)void` |
| `GlCommandEncoder#drawFromBuffers` | `(GlRenderPass,int,int,int,IndexType,GlRenderPipeline,int,int)void`；`IndexType` 已移出 `VertexFormat`，末参是新增 first-instance |
| `ItemInHandRenderer#submitHandsWithItems` | `(float,PoseStack,SubmitNodeCollector,LocalPlayer,int)void` |
| `ItemInHandRenderer#submitArmWithItem` | `(AbstractClientPlayer,float,float,InteractionHand,float,ItemStack,float,PoseStack,SubmitNodeCollector,int)void` |
| `GameRenderer#mainCamera()` | `()Camera`；替代 `getMainCamera()` |
| `OrderedSubmitNodeCollector#submitShapeOutline` | `(PoseStack,VoxelShape,RenderType,int,float,boolean)void` |
| `PictureInPictureRenderer#renderToTexture` | `(state,PoseStack,SubmitNodeCollector)void` |

`ItemInHandRenderer#submitHandsWithItems` 的字节码在两个 call site 均调用上述
`submitArmWithItem` 精确 descriptor，故 mixin wrapper 不是只按改名猜测。

26.2 与 26.1.2 的 `assets/minecraft/shaders/core/entity.fsh` SHA-256 都是
`14cdae28f2a2e30029ef4278034a8d73f302b03a24f20aaf40afafd5d93bb620`，片元主体未变；
`entity.vsh` 只把 `light.glsl` import 加了条件门禁。本实现自定义的是 entity fragment
clone，顶点 shader 继续取源 pipeline，因此无需复制旧 vertex shader。

### ② NeoForge 26.2 sources

官方 `neoforged/NeoForge` commit
`e973c1d1bbd2d2cf013b6df2b3c4c050f2b7d2f0`：

- `RegisterRenderPipelinesEvent#registerPipeline(RenderPipeline)` 是 26.2 mod-bus 自定义
  pipeline 注册入口；全部 8 条 GL pipeline 改由该事件注册，不再调用 vanilla
  `RenderPipelines#register`；
- `RenderPipeline` patch 提供 26.2 多 target/bind-group 数据结构及 NeoForge stencil 扩展；
- `RegisterPictureInPictureRenderersEvent#register` factory 从
  `Function<BufferSource, Renderer>` 改为 `Supplier<Renderer>`；
- `RegisterGuiLayersEvent` / `RenderLivingEvent` 相关调用面保持可用。

### ② 官方 primer

<https://docs.neoforged.net/primer/docs/26.2/>（CC-BY-4.0）已通读。直接采用的迁移结论：

- source/destination blend factor 合并、`GpuFormat`、bind group layout、多个 color target；
- vertex format binding 与 primitive topology 分离；
- 26.2 使用反向深度：默认 compare 从 `LESS_THAN_OR_EQUAL` 变为
  `GREATER_THAN_OR_EQUAL`，depth bias 两个数取加法逆元；
- `MultiBufferSource` 删除、PiP collector 签名、Gizmo feature、GameRenderer getter 改名；
- OpenGL/Vulkan 双 backend。

### ③ refab 26.2 语义

已读 `AGENTS.md`、`PORTING_NOTES.md`、`PORT_SYNC_26_2_TO_26_1_2.md`、scope 历史、
第一人称兼容与 lifecycle 审计。借用的是：

- 26.2 pipeline/Feature/PiP 的游戏语义和精确调用顺序；
- 官方 `submitShapeOutline` 取代手写 line vertices；
- Iris 26.2 `assignPipeline`/hand phase 语义；

没有复制 Fabric 注册、事件或网络表面，也没有采用 refab 的 scope-mask 实现。

### Iris 1.11.2 26.2 source

官方 `IrisShaders/Iris` 分支 `26.2` commit
`8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a`：

- `IrisApi#getMinorApiRevision()` implementation 返回 `3`；
- `IrisApi#assignPipeline(RenderPipeline,IrisProgram)` 存在；
- `IrisApi#isRenderingShadowPass()` 存在；
- `HandRenderer#isActive/#isRenderingSolid/#isHandTranslucent(ItemStack)` 存在；
- `ShaderCreator#link` 依次创建 vertex、geometry、tess-control、tess-eval、fragment，
  fragment 是 `createShader` ordinal 4，与 optional Iris mixin 精确一致。

## 实现映射

### 1. Pipeline 重写

`ScopeRenderTypes` 现在：

- 复制 source pipeline 的所有 bind-group layouts、color-target slots、vertex-buffer slots、
  topology、depth、cull、polygon 与 shader defines；
- 自定义 sampler 用独立 `BindGroupLayout` 声明，避免重复 sampler/uniform；
- color-write-none 保留 source `GpuFormat`；
- depth aperture 默认 compare 改为 `GREATER_THAN_OR_EQUAL`，bias 从 `(-1,-1)` 改为
  `(1,1)`；
- 通过 `RegisterRenderPipelinesEvent` 注册唯一 location 的 8 条 pipeline；
- Vulkan 下不构造也不注册这些 GL shader/pipeline。

### 2. draw 边界

26.1.2 的 `DepthCopyRenderType#draw` 已不可能存在。26.2 改为：

1. `PipelineOverrideRenderType#prepare()` 保留原 RenderType 的 textures、output、scissor、
   dynamic transforms，只换成带 operation identity 的克隆 pipeline；
2. `PreparedRenderTypeScopeDepthCopyMixin` 在
   `PreparedRenderType#drawFromBuffer(ExecuteInfo)` 外层设置/清理 operation，异常也 finally 清理；
3. `GlCommandEncoderScopeDepthCopyMixin` 在精确的 8 参数 `drawFromBuffers` HEAD 执行
   `beforeDraw()`。从 `GlCommandEncoder#executeDraw` 字节码可见，在进入这里之前已经完成
   `trySetup`，故 active program、render pass 与 destination FBO 都已绑定；
4. Vulkan 不进入该 operation，直接执行原 prepared draw。

### 3. 反向深度

GLSL aperture 判据从：

```glsl
apertureDepth < worldDepth - epsilon
```

改为 26.2 reversed-Z 的：

```glsl
apertureDepth > worldDepth + epsilon
```

vanilla custom fragments 与注入 Iris hand fragment 的两份判据同步修改。精确 world depth
restore 仍是直接采样写回，不需数值变换。

### 4. Vulkan 降级

用实际 device 的 `DeviceInfo#backendName()` 判定，而不是只信菜单偏好；日志同时带
`Options#preferredGraphicsBackend()` 值。非 OpenGL 时：

- pipeline registration event 不构造/注册 GL-only pipeline；
- 第一人称开镜仍隐藏 opaque ocular，避免黑片堵住镜口；
- 枪身、准星与枪口火光用原普通 RenderType，不执行 depth backup/mask/restore；
- 每次进程只记录一次明确 warning。

### 5. 其余 26.2 渲染入口

- PiP 删除 `BufferSource` 构造器，直接消费框架传入 `SubmitNodeCollector`；NeoForge
  registration method reference 现在满足 `Supplier`；
- 爆头 AABB 改用 `submitShapeOutline`，保留 2.5 px 线宽与黄色；
- 第一人称 mixin 改为 `submitHandsWithItems -> submitArmWithItem` 精确 call site；
- `GameRenderer#mainCamera()` 替代两处旧 getter；
- Bedrock 在役路径已是 `SubmitNodeCollector#submitCustomGeometry`；全仓没有实际
  `MultiBufferSource`、直接 immediate upload 或已删除 RenderSystem setter；
- 现有 `collector.order(int)` 能表达 aperture/body/cleanup/ring/reticle 的严格顺序，
  本期无需新增自定义 `FeatureRenderPhase`。

## 静态验证

已执行：

```text
java-parser 3.0.1：全部改动 Java 文件 PARSE OK
tacz*.mixins.json：JSON OK，全部注册类文件存在
26.1.2→26.2 removed method/field name 与本仓实际调用交叉检查：0 actionable
26.2 game-class import existence：0 missing
Minecraft shader import existence：0 missing
ItemInHand / PreparedRenderType / GlCommandEncoder mixin descriptor：逐项 javap 命中
git diff --check：success
```

额外使用 scratch-only 的 JDK 17 API typecheck：把 26.2 classfile major 临时降到 61、只在
`/tmp` 将 AT constructor 临时开放，并提供 Fabric 注入接口/NeoForge event 的最小声明；
`ScopeRenderTypes` 与 PiP 两个纯游戏 API 单元通过 javac。这个检查只证明所引用的 26.2
类/方法/构造器能完成 Java 类型归因，**不等于生产 JDK 25 + NeoForge 全仓构建**。

## 尚未完成的验收

执行环境仍无法从 Gradle/Maven 端点下载 wrapper、NeoForge 和 JDK 25，且没有 GPU。因此：

- `compileJava` / `build` 未执行；
- OpenGL vanilla、OpenGL + Iris 1.11.2、Vulkan 降级均未实机；
- 瞄具矩阵（普通镜/组合镜/低倍 sight、开镜渐进、火光、水/粒子/云、左右手）未实机；
- 不声称 shader pack 或 Vulkan PASS。

有 GPU 的最终矩阵至少执行：

```text
OpenGL, no Iris: depth aperture / etched+illuminated reticle / body+flash clip / cleanup
OpenGL + Iris 1.11.2: HAND/HAND_TRANSLUCENT, shadow pass, water/particles/clouds
Vulkan: client starts, no GL call/crash, ocular transparent fallback, warning exactly once
PiP workbench preview: rotate/scale/scissor
F3+B + HeadShotDebugHitbox: official shape outline
```

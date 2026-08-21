# WP-262-3 证据：26.2 Feature Rendering 与离屏目镜掩码

日期：2026-08-21  
目标：Minecraft 26.2 + NeoForge 26.2.0.64

## 技术决策（用户裁决后修订）

当前实现采用 refab `26.2(main)` 已验证的**离屏 scope-mask**游戏语义，不再保留本分支
早先的 OpenGL raw-depth/depth-aperture 前滚。只移植游戏与渲染语义；注册、配置、事件仍使用
NeoForge 26.2 表面。

来源：`q14433686-arch/TaCZ_Refabricated_Unofficial` 分支 `26.2(main)`，commit
`5a29159902f5dddf26cfd0cd0f0fa3b75fbe94e6`。已完整阅读该仓 `AGENTS.md`、
`docs/PORTING_NOTES.md`、`docs/COMPAT_AND_ROADMAP.md`、scope 审计与第一人称兼容文档。

## 为什么是离屏掩码

26.2 的公开 pipeline 不提供旧 stencil 表面。等价语义是：

1. 第一人称瞄具提交阶段收集活动 `ocular` 的完整模型矩阵与 cube；
2. `FeatureRenderDispatcher#prepareFrame` 之后、`PreparedFrame#executeSolid()` 之前，在阶段
   边界一次性打开无 depth 的离屏 target；
3. 把全部活动目镜投影写成 RGBA8 二值 mask；R=镜内，G=开镜进度；
4. 镜身、枪身、非瞄具配件、枪口火光的片元 shader 在镜内 `discard`；
5. 准星用反向判据，只保留镜内像素；
6. 任一前置条件失败就回退普通 RenderType，不做半套裁剪。

这条路径不调用 OpenGL FBO/texture API，因此普通 mask 路径可同时进入 26.2 OpenGL 与
Vulkan backend。Iris/OpenGL 的 shader replacement 另由可选 dormant uniform branch 桥接；
没有已核 bridge 的 shader replacement（例如 Sulkan）仍安全回退。

## ① Minecraft 26.2 classfile 证据

merged jar SHA-256：
`703604cea43c6e720e7ce658ea7565c4a2e5db5d7ab428113776bbf8a5196b21`。

| 类#方法/字段 | descriptor / access |
|---|---|
| `FeatureRenderDispatcher#renderAllFeatures` | `(SubmitNodeStorage)V` |
| `PreparedFrame#executeSolid` | `()V`，为阶段边界精确 invoke target |
| `TextureTarget#<init>` | `(String,int,int,boolean,GpuFormat)V` |
| `RenderTarget#getColorTextureView` | `()GpuTextureView` |
| `RenderTarget#getColorTexture` | `()GpuTexture` |
| `RenderTarget#destroyBuffers` | `()V` |
| `CommandEncoder#createRenderPass` | `(Supplier,GpuTextureView,Optional<Vector4fc>)RenderPass` |
| `RenderPass#setUniform` | `(String,GpuBufferSlice)V` |
| `RenderPass#setVertexBuffer` | `(int,GpuBufferSlice)V` |
| `RenderPass#setIndexBuffer` | `(GpuBuffer,IndexType)V` |
| `RenderPass#drawIndexed` | `(int,int,int,int,int)V` |
| `GpuBufferSlice#map` | `(boolean,boolean)MappedView` |
| `RenderSystem#getProjectionMatrixBuffer` | `()GpuBufferSlice` |
| `RenderSystem#getModelViewMatrixCopy` | `()Matrix4f` |
| `AbstractTexture#texture/textureView/sampler` | `protected`；动态 texture handle 可安全指向 target 资源 |
| `Hud#getGuiTicks` | `()I` |

`FeatureRenderDispatcher#renderAllFeatures` 字节码顺序为
`prepareFrame → executeSolid → executeTranslucent → executeTranslucentAfterTerrain → executeAlwaysOnTop`；
因此 mixin 的 `executeSolid BEFORE` 位于 frame upload 之后且不在已有 render pass 内。

## ② NeoForge 26.2 证据

官方 `neoforged/NeoForge` commit
`e973c1d1bbd2d2cf013b6df2b3c4c050f2b7d2f0`：

- 七条自定义 pipeline 全部通过 mod-bus
  `RegisterRenderPipelinesEvent#registerPipeline(RenderPipeline)` 注册；
- debug mask overlay 通过 `RegisterGuiLayersEvent#registerAboveAll` 注册；
- 没有复制 Fabric event、registration、network 或 config 表面；
- mask target/texture/pipeline 只依赖 26.2 游戏渲染 API。

## 实现映射

### 掩码采集与阶段边界

- `ScopeMaskGeometry`：当帧 ocular 矩阵/cube 清单；矩阵和 list 防御性快照；
- `FeatureRenderDispatcherMixin`：在 `executeSolid()` 之前调用一次
  `ScopeMaskRenderer#renderAtPhaseBoundary`；
- `GameRendererMixin`：标记 vanilla hand pass；Iris bypass 路径仍查询
  `HandRenderer#isActive`；
- `ScopeMaskRenderer`：构造 POSITION/QUADS mesh，打开无 depth target，写 R/G mask，
  finally 无条件清空当帧清单；
- `ScopeMaskTarget`：按物理窗口尺寸创建/重建 RGBA8 target；
- `ScopeMaskTextureHandle`：以 `tacz:scope_mask` 将 target view 暴露给 RenderSetup，
  `close()` 不释放非自身所有的 target。

### 镜身、准星和视模

`ScopeBodyRenderTypes` 注册并缓存：

- `scope_body_clipped`；
- `scope_reticle_clipped`；
- `scope_reticle_emissive_clipped` 与无 mask emissive fallback；
- `scope_flash_translucent_clipped`；
- `scope_flash_swirl_clipped`。

`BedrockAttachmentModel` 采用 refab 已核语义：

- 只在第一人称、开镜进度超过阈值时登记 ocular；
- `ScopeMaskHullFill=true` 时以 ocular 投影凸包补全稀疏板条目镜；
- `ScopeSightClipFix=true` 时纯红点/低倍 sight 通道不裁镜身；
- `ScopeOcularRingFix=true` 时 `ocular_ring` 从裁剪批摘除，以普通 RenderType 重画；
- 组合镜按 display `scope/sight + views[]` 与 ocular/division 序号选择活动组；
- 镜身在 mask 外、准星在 mask 内；mask 不可用时蚀刻大面不会冒险提交。

枪身、非瞄具配件与枪口火光共享同一 `clipForViewmodel/maskReadyForViewmodel` 入口，避免
镜内仍出现护木、激光盒或火团。

### Iris

Iris 1.11.2 source commit
`8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a` 已核：API revision 3、
`assignPipeline`、HAND 状态与 ShaderCreator `link` fragment 参数。

- 自定义 pipeline 经 `IrisApi#assignPipeline(..., HAND)` 分类；
- `IrisShaderCreatorMixin` 在 linked fragment 中加入默认 `mode=0` 的 dormant branch；
- `IrisExtendedShaderMixin` 每次 setup 重置 mode；
- `IrisGlCommandEncoderMixin` 在成功 `trySetup` 后按 pipeline location 设置 body/reticle mode
  并绑定当帧 mask texture；
- 非 scope draw 每次写回 mode 0，防止 uniform 泄漏。

## 删除的旧债

已删除：

- `ScopeDepthCopyState`；
- `ScopeRenderTypes` raw-depth 实现；
- `PreparedRenderTypeScopeDepthCopyMixin`；
- `GlCommandEncoderScopeDepthCopyMixin`；
- `IrisDepthRestoreShaderMixin`；
- 三个 depth-copy/restore fragment shader；
- 仅为私有 `RenderType` 构造器存在的 AT。

不同时维护两套默认实现，避免 scope 语义继续分叉。

## 当前静态验证

- `java-parser 3.0.1`：696 个 Java 文件全部 parse；
- mixin JSON：合法；旧 depth mixin/类/resource 引用为 0；
- `ScopeMask*` / `ScopeBodyRenderTypes` 以 26.2 classfile + 最小 loader/project stub 做
  scratch `javac`：PASS；
- `FeatureRenderDispatcherMixin` 对 26.2 classfile 的精确 invoke target 做 scratch
  `javac`：PASS；
- 三个 Iris optional mixin 与 `IrisScopeMaskState` 以 26.2 classfile + 最小 Iris/LWJGL stub
  做 scratch `javac`：PASS；
- `git diff --check`：PASS。

以上不是生产 Gradle 或 GPU PASS。用户此前的 JDK 25 build PASS 对应 depth-aperture commit
`c40dab9`；替换为 mask 后必须重新执行 build 与下列运行矩阵。

## 必须重跑

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew build --no-configuration-cache
./gradlew runServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
```

GPU：

```text
OpenGL, no Iris: raw ocular / hull-fill, sight/scope combo groups, ring, reticle, body/attachment/flash clip
OpenGL + Iris: HAND solid/translucent, shadow, water/fog/particles/clouds, uniform mode leakage
Vulkan: stage-boundary target switch, mask debug preview, no device loss, same sight picture
Resize/reload: target rebuild and dynamic texture handle refresh
Third-party scopes: sparse/sliver ocular and non-solid ocular geometry
```

## 2026-08-21 Vulkan 启动反馈

用户上传到默认分支 commit `972742496180624af2b8811b2a61c211be979267` 的
`RawOutput.log` 显示 Vulkan 尝试在进入任何 TACZ scope target/pipeline/render pass 之前失败：

```text
Window size: <not initialized>
Surface Info: <no surface>
glfwCreateWindowSurface
VulkanGpuSurface.<init>
Minecraft.<init>
```

崩溃报告表面的 NPE 是 `Minecraft#cursorEntered()` 在 `mouseHandler` 初始化前被 GLFW
错误对话框重入；它掩盖了先发生的 window-surface 创建错误。日志中没有
`ScopeMaskRenderer`、`FeatureRenderDispatcherMixin`、TACZ pipeline draw 或 mask target 的
调用帧，也没有进入第一帧。因此该次结果记为：

- Vulkan 启动：**FAIL（surface 创建阶段）**；
- TACZ Vulkan scope-mask：**未执行，不能归因也不能标 PASS**；
- 环境证据：加载了 `RTSSVkLayer64.dll`，Vulkan loader 同时报 Epic EOS overlay JSON
  缺失与 `mediasdkhook/game_detour_64.dll` 缺失；两块 AMD GPU 驱动版本不同，进程中出现
  两个不同版本的 `amdvlk64.dll`；
- 精确失败类还被 Sodium Extra 的 `MixinVulkanGpuSurface`/`MixinWindow` 修改，故必须先做
  无隐式 Vulkan layer、最小 Mod 集与驱动清理的隔离测试，不能在 TACZ 中伪造 workaround。

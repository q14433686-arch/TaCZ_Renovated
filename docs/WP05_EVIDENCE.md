# 工作包⑤ 证据清单

语义：Fabric 26.1.2（depth-aperture / Feature Rendering / 第一人称 SubmitNodeCollector）。
加载器：NeoForge 26.1.2.97。禁止抄 MUKSC 渲染；禁止 1.21.1 游戏 API 签名。

## 加载器 / 游戏 API（① patched 26.1.2.97 + ② NF 26.1.2.97 sources / loader-11.0.15）

| 调用 | 证据 |
|---|---|
| `AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)` | ② `AddClientReloadListenersEvent.java` / `SortedReloadListenerEvent` |
| `RegisterKeyMappingsEvent#register` / `#registerCategory` | ② `RegisterKeyMappingsEvent.java` |
| `RegisterGuiLayersEvent#registerAboveAll` / `GuiLayer#render(GuiGraphicsExtractor, DeltaTracker)` | ② |
| `RegisterItemModelsEvent#register(Identifier, MapCodec)` | ② `RegisterItemModelsEvent.java`（`ItemModels.ID_MAPPER` 私有） |
| `RegisterSelectItemModelPropertyEvent#register` | ② |
| `RegisterPictureInPictureRenderersEvent#register(Class, Function)` | ② |
| `RegisterMenuScreensEvent#register` | ② |
| `RegisterParticleProvidersEvent#registerSpecial` | ② |
| `EntityRenderersEvent.RegisterRenderers#registerEntityRenderer` / `#registerBlockEntityRenderer` | ② |
| `RegisterClientTooltipComponentFactoriesEvent#register` | ② |
| `ClientTickEvent.Pre/Post`、`RenderFrameEvent.Pre`、`ViewportEvent.ComputeFov/ComputeCameraAngles`、`InputEvent` | ② |
| `ModList#getModContainerById` | ② loader-11.0.15 javap |
| `RenderPipelines#register(RenderPipeline)` 私有，AT 开放 | ① `RenderPipelines` javap |
| `RenderType(String, RenderSetup)` 私有构造，AT 开放 | ① |
| `GuiGraphicsExtractor#submitPictureInPictureRenderState` | ① |
| `AvatarRenderer#renderRightHand/renderLeftHand(..., SubmitNodeCollector, ...)` | ① |
| `EventBusSubscriber` 仅 `modid`/`value`；IModBusEvent 由 MDK 26.1.2 `ExampleModClient` 的 `FMLClientSetupEvent` 证明可投递 | 官方 MDK |

## 实现要点

- 瞄具：完整迁入 Fabric 26.1.2 `ScopeRenderTypes` / `ScopeDepthCopyState` / reticle 过滤 + `GlCommandEncoderScopeDepthCopyMixin`（draw 边界，不是顶点写入阶段）。
- 第一人称：`ItemInHandRendererMixin` 在 `renderHandsWithItems` 拦截 `renderArmWithItem`，走 `AnimateGeoItemRenderer#renderFirstPerson`（干净 PoseStack）。
- 物品模型：`tacz:dynamic_item` 经 `RegisterItemModelsEvent`；弹药盒 `tacz:ammo_statue` 经 `RegisterSelectItemModelPropertyEvent`。
- ShaderCompat 接口预留：`com.tacz.guns.compat.shader.ShaderCompat`，Iris 仅反射。
- 工作台 `RenderShape.INVISIBLE` + BER 已恢复。
- 动画/gltf API 已取消 `sourceSets` exclude。
- S2C 仍走 `ClientPacketBridge`；dedicated 常量池不引用 `LocalPlayer`。

## 冒烟

本沙盒无 GPU / `runClient`。`compileJava` 成功（19 条既有 `@Deprecated(forRemoval)` transfer API 警告）。二进制见 `build/libs/`。

未接触 `tacz-port` jar。

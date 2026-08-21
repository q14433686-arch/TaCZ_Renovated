# 工作包⑤ 证据清单

语义：Fabric 26.1.2（depth-aperture / Feature Rendering / 第一人称 SubmitNodeCollector）。
加载器：NeoForge 26.1.2.97。禁止抄 MUKSC 渲染；禁止 1.21.1 游戏 API 签名。

## 加载器 / 游戏 API（① patched 26.1.2.97 + ② NF 26.1.2.97 sources / loader-11.0.15）

| 调用 | 证据 |
|---|---|
| `AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)` | ② `AddClientReloadListenersEvent.java` / `SortedReloadListenerEvent` |
| PAL API（`com.zigythebird.playeranim[core]`）：`PlayerAnimationFactory.ANIMATION_DATA_FACTORY#registerFactory(Identifier,int,Factory)`、`PlayerAnimationAccess#getPlayerAnimationLayer`、`PlayerAnimationController#setFirstPersonMode/replaceAnimationWithFade/triggerAnimation/stop/removeModifierIf`、`AbstractFadeModifier.standardFadeIn`、`UniversalAnimLoader#loadAnimations`、`AdjustmentModifier.PartModifier`、`EasingType/PlayState/FirstPersonMode/Vec3f` | ② ZigyTheBird/PlayerAnimationLibrary 分支 `26.1`（= 发布 1.2.5+26.1 的源码线）`PlayerAnimationFactory.java:40` 等逐条核对；modid 两加载器均为 `player_animation_library`（fabric.mod.json / neoforge.mods.toml 同名核实） |
| **PAL 编译坐标坑（r17/r18 失效根因）**：`maven.modrinth:player-animation-library:1.2.5` 永远解析不到——该项目 Modrinth version number 是 `1.2.5+26.1` 形式；正确通道 = CurseMaven `curse.maven:player-animation-library-1283899:8454167`（8454167 = "1.2.5+26.1"，26.1.2+2，**merged Fabric+NeoForge 双加载器 jar**，同文件两加载器可装） | CurseForge 文件列表页逐文件核对（fetch）：8454167=1.2.5+26.1、8674772=1.2.6+26.1、8271835=1.2.4+26.1 |
| Controllable 编译坐标坑（两连）：① CurseForge 文件页带 "Curse Maven does not yet support mods that have disabled 3rd party sharing" 警告（第三方分发存疑）；② r19 曾改为配置期从 GitHub Releases 下载，但用户的 Gradle JVM 在代理环境下无法通过 github.com 的 TLS 校验（`PKIX path building failed`，配置期直接崩）。终态 = 依赖解析期走 CurseMaven 坐标（用户 JVM 已验证可达 cursemaven.com——REI 先例），并以 `libs/` 本地 jar 为逃生舱（`controllable-neoforge*.jar` 存在即优先），**配置期零网络** | CurseForge 7943194 文件页 + 用户 r19 构建日志（PKIX）+ fetch_page 对照实验（REI jar 同样 500 → 工具对二进制无信息量，CurseMaven POM 200 为有效信号） |
| Controllable API（`com.mrcrayfish.controllable.*`，全在 multiloader common 模块）：`ButtonBinding(int,String,String,BindingContext,ButtonHandler)`、`OnPressAndReleaseHandler#create(Function,Function)`、`InGameContext(Identifier)`（protected，子类可 super）+ `#priority()`、`BindingRegistry#register`、`Controllable#getBindingRegistry/#getController`、`Controller#isButtonPressed(int)/#rumble(float,float,int)`、`ButtonBinding#getButton`、`Buttons.{LEFT_TRIGGER,RIGHT_TRIGGER,B,X,LEFT_THUMB_STICK,DPAD_LEFT}` | ② MrCrayfish/Controllable 分支 `multiloader/26.1.2`（= CurseForge 文件 7943194 / Controllable 0.26.0 NeoForge 26.1.2 的源码线）`common/.../binding/**`、`input/{Controller,Buttons}.java`、`Controllable.java`；文件号经 CurseForge 文件页核实（web） |
| **26.1 线渲染格式 = `DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH`：`RenderTypes.lines()` 管线要求每个线顶点都写 `VertexConsumer#setLineWidth(float)`，缺失即抛 `IllegalStateException: Missing elements in vertex: LineWidth`**（r15 开"显示爆头范围"即崩，crash 2026-08-21 13:46） | ① 26.1 反编译源 `DebugScreenOverlay.java:110-123`（格式名 + 每顶点 setLineWidth 习语）、`ShapeRenderer#renderShape`（`addVertex().setColor().setNormal().setLineWidth(w)` ×2）；② NF `VertexConsumerWrapper#setLineWidth` |
| 线宽取值 2.5F = 原版 F3+B 碰撞箱默认（`GizmoStyle.DEFAULT_WIDTH`，`stroke(argb)` 默认宽度） | ① 26.1 反编译源 `net/minecraft/gizmos/GizmoStyle.java:6-9`；原版实体碰撞箱走 `Gizmos.cuboid` + `EntityHitboxDebugRenderer` |
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
- `RenderHeadShotAABB`：26.1 线渲染补 `setLineWidth(2.5F)`（refab 26.1.2 同源文件同 bug，
  属上游遗留而非移植引入）。全仓库排查：`RenderTypes.lines()` 仅此一处，
  其余 `submitCustomGeometry` 全为面渲染类型（`entityTranslucent`/emissive，无 LineWidth 元素），
  无直接 `new BufferBuilder`——同类"缺顶点元素"崩溃无第二处。
- **PAL（ZigyTheBird Player Animation Library，modid `player_animation_library`）第三人称兼容已恢复**：
  `compat/playeranimator/**` 取自 refab 26.1.2（pal 四类 + AnimationName + 门面），NeoForge 适配三点——
  `ModList#isLoaded` 判装、PalAssetManager 去 `IdentifiableResourceReloadListener` 改经
  `AddClientReloadListenersEvent#addListener(ID, listener)` 注册、事件订阅由 Fabric `CALLBACK.register`
  改 `NeoForge.EVENT_BUS.addListener`（事件本就 post 在 game bus）。compileOnly
  `maven.modrinth:player-animation-library:1.2.5`（refab 同款坐标）。内建规避 PAL 1.2.5 两处坑
  （fadeOut 永久哑化、AdjustmentModifier NPE，见 PalAnimationManager/SafeAdjustmentModifier 注释）。
- 兼容层状态盘点（r18）：cloth（活）、playeranimator（活，r17）、**controllable（活，r18**：refab 26.1.2
  `ControllableInner` 移植，Fabric END_CLIENT_TICK → `ClientTickEvent.Post`；输入钩子
  `onXxxControllerPress/shootControllerTick` 本就在位）、carryon（活，反射桥）、
  firstperson（活）、shouldersurfing（活）、iris/shader（活，反射）、jei/rei/recipeviewer（活）、
  ar（禁用——AR 无 26.1.2 Feature Rendering 版）、
  **immediatelyfast（有据 no-op**：IF 有 26.1.2 NeoForge 版 1.15.3，但 26.x 渲染经 collector，
  旧 HUD batching API 已不存在，上游 26.2 同样保留空实现）、
  **zoomify（无的放矢 no-op**：Zoomify 26.1 仅有 Fabric/Quilt 构建，NeoForge 无法安装该 mod）。
- S2C 仍走 `ClientPacketBridge`；dedicated 常量池不引用 `LocalPlayer`。

## 冒烟

本沙盒无 GPU / `runClient`。`compileJava` 成功（19 条既有 `@Deprecated(forRemoval)` transfer API 警告）。二进制见 `build/libs/`。

未接触 `tacz-port` jar。

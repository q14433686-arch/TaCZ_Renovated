# WP-11211-2 证据：编译收敛全过程（26.1.2 → 1.21.11）

> 2026-08-22。全部结论以 1.21.11 named dev artifact
> （`build/moddev/artifacts/neoforge-21.11.45.jar`）javap 逐符号核实 +
> 姊妹项目 1.21.11 分支实现对照（③ 级语义权威）为准。

## 一、收敛曲线

| 轮次 | 错误数 | 本轮动作 |
|---:|---:|---|
| 01 | 100 | 基线（首轮，javac 100 上限截断） |
| 02 | 72 | F1 GuiGraphicsExtractor 改名族 + 包迁移族 + 单点（脚本化） |
| 03 | 41 | GUI 覆写族（extract*→render*）+ 动态物品模型三处接口 + PAL 1.1.9 + 单点 |
| 04 | 21 | RenderType 包装器 + Player 消息 API + RecipeSerializer 接口 + 粒子层 |
| 05 | 5 | 正则误伤修复（PlayerEnterWorld 括号） |
| 06 | **0** | renderer 的 extractRenderState 误改回退 + AT 恢复 + UpdateCause + 构造器 |

`compileJava` → `build` 全绿（jar 56.4MB，jarJar 两库内嵌）。19 条 transfer-API
forRemoval 警告为 26.1.2 基线既有（26.2 线 WP-262-0 的卫生范围，本线不动）。

## 二、错误族与处置（详细版）

### F1 GuiGraphicsExtractor（76 错 / 30+ 文件）

26.x 把 `GuiGraphics` 改名 `GuiGraphicsExtractor` 并缩短方法名。1.21.11 回退两者：

- 类型/import：`net.minecraft.client.gui.GuiGraphicsExtractor` → `net.minecraft.client.gui.GuiGraphics`
- 方法改名表（姊妹 Phase2 表，javap 双侧核实）：`text→drawString`、`centeredText→drawCenteredString`、
  `textWithWordWrap→drawWordWrap`、`textWithBackdrop→drawStringWithBackdrop`、`item→renderItem`、
  `fakeItem→renderFakeItem`、`itemDecorations→renderItemDecorations`、`outline→renderOutline`、
  `horizontalLine→hLine`、`verticalLine→vLine`、`tooltip→renderTooltip`、
  `map/entity/skin/book/bannerPattern/sign/profilerChart→submit*RenderState`
- LR mixin 注点：`itemCooldown(ItemStack;II)V` → **private** `renderItemCooldown(ItemStack;II)V`
  （1.21.11 javap 核实，类名 `GuiGraphicsExtractorMixin` 保留、目标改 `GuiGraphics.class`）

### F2/3/5/8/9 包迁移（javap 核实目标包存在）

```
renderer.state.level.CameraRenderState / QuadParticleRenderState -> renderer.state.*
renderer.state.gui.pip.PictureInPictureRenderState                  -> client.gui.render.state.pip.*
resources.model.cuboid.ItemTransform(s)                             -> renderer.block.model.*
resources.model.sprite.TextureSlots                                 -> renderer.block.model.*
```

### F4 GUI 覆写族（extract*→render*）

26.1 把原版 GUI 的 render* 改名 extract*；1.21.11 改回。KEEP 名单（1.21.11 真实存在/自有 API）：
`extractItem`、`extractIndices`、`extractArgument`、`extractRotatedQuad`、`extract`。
**已知缺陷记录**：姊妹脚本对 `EntityRenderer/BlockEntityRenderer` 子类的
`extractRenderState`（两版同名存在）也被改成了 `render`——姊妹自己的第 07 轮踩过同一坑；
本线第 06 轮以 git diff 逐一回退 6 个 renderer 文件。

### F11 单点

| 26.1.2 | 1.21.11 |
|---|---|
| `ItemStackTemplate` | 类不存在：`SlotDisplay.ItemStackSlotDisplay(ItemStack)` 直收；CarryOn mixin 回退 `CallbackInfoReturnable<ItemStack>`（姊妹同款） |
| `Recipe#assemble(SingleRecipeInput)` | 补 `HolderLookup.Provider` 形参 |
| `RecipeSerializer` 抽象类+构造器 | **接口**（codec/streamCodec 两抽象方法）→ 匿名实现 |
| `LightCoordsUtil.pack` | `LightTexture.pack` |
| `ItemStack#typeHolder()` | `getItemHolder()` |
| `Camera#getCameraEntityPartialTicks(DeltaTracker)` | `getPartialTickTime()` |
| `Player#sendSystemMessage/sendOverlayMessage` | `displayClientMessage(Component, boolean)`（ServerPlayer/CommandSourceStack 同名方法仍在，未动） |
| `ModelManager#getBlockStateModelSet()` | `getBlockModelShaper().getParticleIcon(state)` / `getMissingBlockStateModel().particleIcon()` |
| `AbstractContainerScreen` 构造器带宽高 | 构造后写 `imageWidth/imageHeight`；`renderBg` 变抽象方法 → 空实现（背景画在 renderBackground） |
| `SingleQuadParticle$Layer.TRANSLUCENT_TERRAIN` | `Layer.TERRAIN`（姊妹实测：TERRAIN 的 translucent=true） |
| `Particle#getLightCoords` | `getLightColor` |
| `RenderTypes.itemCutout(Identifier)` | 1.21.11 无该管线 → 与 entityCutout 合流（姊妹同款） |
| `TagsUpdatedEvent.ServerDataLoad` | `event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD` |
| `BlockModelRenderState`（矿车内容物） | 裸 `BlockState`（姊妹同款） |
| PAL 1.1.9 `get3DTransform` | 返回 `PlayerAnimBone`（1.2.5 为 void），SafeAdjustmentModifier 改签名并 return |

### 动态物品模型（Tacz / Lr 两份，姊妹脚本适配后原样生效）

1. `SpecialModelRenderer#submit` 多 `ItemDisplayContext` 形参；
2. `ItemModel.Unbaked#bake` 少 `Matrix4fc inheritedTransform` 形参；
3. `LayerRenderState#setLocalTransform` 不存在 → 变换随 `RenderArgument` 下传、
   在 submit() 里 `poseStack.last().pose().mul(...)` 手动施加。

### 瞄具（WP-11211-3 的代码面，编译收敛一并完成）

- `ScopeRenderTypes`：`ColorTargetState/DepthStencilState/CompareOp` 三聚合对象在
  1.21.11 不存在 → Builder 扁平 setter（`withColorWrite/withDepthWrite/
  withDepthTestFunction/withDepthBias`，clonePipeline 逐项复制）；决策 A 落地：
  `ALWAYS_PASS_KEEPING_DEPTH_WRITES = GREATER_DEPTH_TEST`；reticle 改
  `NO_DEPTH_TEST + depthWrite=false` + `FORCE_ALWAYS_DEPTH_PIPELINES` 白名单；
- `GlCommandEncoderScopeDepthCopyMixin`：改用姊妹 1.21.11 版（typed `GlRenderPipeline` +
  `tacz$forceAlwaysDepthIfNeeded`：`_enableDepthTest` + `_depthFunc(GL_ALWAYS)`，
  不碰深度写入掩码）；
- `DepthCopyRenderType` 删掉 1.21.11 不存在的 `hasBlending()/outputTarget()` 转发，
  新增 `hasBlending(type)` helper（`pipeline().getBlendFunction().isPresent()`）。

### AT（教训记录：核对对象必须是编译类路径 jar）

首轮曾据 **dev 运行产物 jar**（NeoForge AT 已烘焙、register/构造器显 public）
误删两条渲染 AT；编译类路径 jar 上二者实为 private → 第 06 轮恢复：
`rendertype.RenderType <init>(String;RenderSetup)V`、`RenderPipelines#register`。
accesstransformer.cfg 现共 5 条，已注明证据口径。

## 三、refmap 不需要（NeoForge 侧事实，与 Fabric 相反）

NeoForge 官方 21.11 发布博文：**mod 的混淆早在两年前就从工具链中移除**
——mod 以官方命名分发，运行期由 NeoForge 处理游戏的混淆。因此本线 jar
**不带 refmap、class 中保留官方名引用是正确形态**（与姊妹 Fabric 线需
Loom refmap 的机制不同）。此条已写入 PORT_11211_DEPS.md 第六节。

## 四、专服冒烟（runServer，2026-08-22）

- 首次启动：MC 1.21.11 + NeoForge 21.11.45，tacz 注册完成（WP② registries）、
  synced data 9 键注册、WP③ payloads（play+configuration, v1.0.5）、
  LR 层注册、默认枪包导出、配方解析（LR test_* 空配方警告为内容包设计使然）；
- 世界生成阶段沙箱 OOM（2 GiB，exit 137）→ flat 世界 + view-distance 2 +
  服务端堆 448M（build.gradle run 参数已按沙箱适配）；
- 复跑 **Done (0.848s)**，监听 25565，PermissionAPI 初始化完成——mixin/AT/注册/
  网络面在 1.21.11 运行期全部成立。
- 日志：`port-11211-server-01/02.log`（不入库，build 产物外）。

**结论：WP-11211-1（构建骨架）、WP-11211-2（编译收敛）验收达成。**
WP-11211-3 的 GPU 实机量化、WP-11211-4 的剩余 mixin 注点复核（客户端
GameRenderer/Camera 等 26.1↔1.21.11 变动面）、WP-11211-6 完整 L0–L3 由
有显示设备/正常内存的环境执行。

## 五、生产 jar 实测（2026-08-22，真实 NeoForge 服务器，非 dev 环境）

用官方 `neoforge-21.11.45-installer.jar --installServer` 安装真实服务器，
将成品 `tacz-1.1.8+neoforge.1.21.11.r0.jar` 直接放入 `mods/` 启动：

- FML 模组列表确认：`TaCZ: Renovated 1.1.8+neoforge.1.21.11.r0 (tacz)`，
  jarJar 内嵌库（commons-math3 / luaj）被正确解出；
- LRTactical 内置层注册、tacz 注册表（gun/workbench/gun_smith_table/recipe）就绪、
  synced data 9 键注册、默认枪包导出；
- **Done (0.735s)**，60 秒空服自动暂停（行为正常）。
- 结论：混淆版 MC 1.21.11 运行期 mixin / AT / jarJar / 注册面全部成立，
  成品 jar 可直接分发测试。

## 六、WP-11211-4 客户端 mixin 全量审计与修复（2026-08-22，用户实机崩溃驱动）

### 崩溃（用户 RawOutput.log，NeoForge 21.11.45 + Iris 1.10.7 环境）

```
Mixin apply for mod tacz failed tacz.mixins.json:client.GameRendererMixin ->
net.minecraft.client.renderer.GameRenderer: InvalidInjectionException Invalid descriptor
@Inject::tacz$beginHandPass(CameraRenderState;F;Matrix4fc;CI)V
! Expected (FZLorg/joml/Matrix4f;CI)V
```

根因：26.1.2 → 1.21.11 客户端 vanilla 方法签名漂移。1.21.11 named jar（rename 中间产物，
`~/.gradle/caches/neoformruntime/intermediate_results/rename_*_output.jar`）javap 实证：

| 目标 | 1.21.11 实际签名 | 处置 |
|---|---|---|
| `GameRenderer#renderItemInHand` | `(float, boolean, Matrix4f)`（26.1.2 是 `(CameraRenderState, float, Matrix4fc)`） | handler 改 `(float, boolean, Matrix4f, CI)` ×2（HEAD/RETURN） |
| `GameRenderer#bobHurt/bobView` | `(PoseStack, float)`（26.1.2 是 `(CameraRenderState, PoseStack)`） | handler 改 `(PoseStack, float, CI)`，partialTick 用形参（语义来源：姊妹 1.21.11 分支同款修正） |
| `Camera#update(DeltaTracker)` | **不存在**（26.1 新增；1.21.11 是 `setup(Level, Entity, boolean, boolean, float)`） | 注点改 `setup` TAIL，handler 5 形参 + CI，partialTick 用形参；保留 title-screen 空 level 守卫 |

### 全量审计（docs/records/PORT_11211_MIXIN_AUDIT.txt，逐条 javap）

其余全部 mixin 与 1.21.11 兼容，要点：

- **继承链解析**：`LocalPlayer;turn(DD)V` 实际声明在 `Entity`、
  `setSprinting(Z)`/`stopUsingItem()` 声明在 `LivingEntity`——mixin 按层级解析，均命中；
- `ItemInHandLayer#submit(PoseStack, SubmitNodeCollector, int, S, float, float)`
  泛型擦除后 = `ArmedEntityRenderState`，与注点描述符一致；
- `Screen#renderables` / `SoundManager$Preparations#listResources` /
  `SoundEngine#calculateVolume(SoundInstance)` / `GuiGraphics#renderItemCooldown` /
  `PlayerModel#setupAnim(AvatarRenderState)` / `AbstractButton#onClick(MouseButtonEvent,boolean)` 全部存在；
- `LanguageMixin` 注点全部处于注释状态（惰性，无风险）；
- **Iris mixin（tacz.iris.mixins.json）在 Iris 1.10.7 上惰性**：`ShaderCreator` 类存在但
  `createShader` 方法不存在，`defaultRequire=0` → 静默跳过，不崩溃（姊妹线同款状态）；
- `IrisCompat` 全反射 + catch(Throwable) → 1.10.7 内部结构差异只造成特性静默降级，无崩溃面；
- CarryOn 三 mixin：用户环境未装 Carry On（`@Pseudo` + require=0，目标类缺失时跳过）。
  CarryOn 2.9.2（1.21.11 NeoForge 有构建）装上的场景列入 WP-11211-5 实测矩阵；
- 已知非致命：`tacz-pre.toml is not correct. Correcting`（NeoForge 配置规格自动纠正，见用户日志）。

### 修复后验证

- `./gradlew build` BUILD SUCCESSFUL；服务端面无改动（client 段 mixin 不参与专服）；
- 最终实机验证依赖用户环境（沙箱无显示设备，客户端无法本地启动）。

## 七、光影下准星被云/粒子覆盖的修复（2026-08-22，用户实机反馈驱动）

### 症状

开启 Iris 光影（用户环境 Iris 1.10.7+mc1.21.11）后，瞄准镜准星（分划）会被云和
药水粒子"覆盖"。无光影时正常。

### 根因（绘制时序，非深度问题）

本线的 etched/visible reticle 管线被分配给 Iris 的 `HAND_TRANSLUCENT` 程序
（`IrisCompat.assignPipelineToIris(pipeline, "HAND_TRANSLUCENT", ...)`）——准星颜色在
shaderpack 的 hand-water 阶段就烘焙进了 gbuffer。而多数 shaderpack（用户实测的
Complementary 类）会在**更晚**的 composite/final 阶段用不可变的 pre-hand 深度快照重画
云、雾与半透明粒子，直接盖在准星颜色之上。深度测试（GL_ALWAYS）对此无能为力：
后画的颜色就是会覆盖先画的颜色。

### 修复（平移姊妹 1.21.11 分支的 R8/R9/R11 机制，字节码级审计过 Iris 1.10.7）

| 组件 | 内容 |
|---|---|
| `ScopeLateReticleState`（新增） | HAND_SOLID 阶段冻结准星/镜框的不可变 3D 快照；Iris 世界透明绘制完成后在 HAND_TRANSLUCENT 补交 |
| `ScopeFinalOverlayState`（新增） | 同一份快照延迟到 `IrisRenderingPipeline#finalizeLevelRendering()` TAIL——即所有 composite/final pass 结束之后，用 no-fog vanilla fragment 直接画在主渲染目标上 |
| `IrisHandRendererReticlePassMixin`（新增） | 修改 `HandRenderer#renderTranslucent` 的 `isAnyHandTranslucent()` 门（枪不是 BlockItem，原版不会触发 translucent hand pass）；并在 setPhase 之后 `submitPending` |
| `IrisFinalScopeOverlayMixin`（新增） | `finalizeLevelRendering` TAIL → `ScopeFinalOverlayState.renderAfterFinalComposite()` |
| `ScopeRenderTypes` | 新增 6 管线：late etched/visible/ocular-ring（HAND_TRANSLUCENT + depthWrite=true 写前景深度）、final etched/visible/ocular-ring（不分配 Iris 程序，NO_DEPTH_TEST + 不写深度） |
| `ScopeDepthCopyState` | 新增 `FINAL_OVERLAY_UNIFORM` 与 final-overlay 专用世界深度私有副本（Iris 收尾后 depthtex2 已解绑，不能再用） |
| `BedrockAttachmentModel` | `orderedScopeSequence` 判定 + 双 defer 标志：`isRenderingSolidHandPass() && supportsFinalScopeOverlay()`（仅 1.10.7 走 final overlay）→ 否则 R8/R9 translucent 回退 → 否则原版即时路径；镜框顺序修正为「先准星后镜框」（上游顺序） |
| `IReticleRenderer.Context` | 新增 `deferToIrisTranslucent` / `deferToIrisFinalOverlay` 两字段；两个渲染器改走 `ScopeLateReticleState.submitReticle(...)` |
| `IrisCompat` | 新增 `isRenderingSolidHandPass()`（反射 `pathways.HandRenderer` INSTANCE.isActive && isRenderingSolid）与 `supportsFinalScopeOverlay()`（版本串 `1.10.7` 前缀门） |
| `tacz.iris.mixins.json` | 注册两个新 mixin（`defaultRequire: 0` + plugin 的 iris-loaded 门双重兜底） |
| shader | 新增 `core/scope_reticle_final.fsh`、`core/scope_ring_final.fsh`（无 fog 的实体材质变体） |

注点已对真实 Iris 1.10.7 jar javap 复核：`HandRenderer.submitNodeCollector` /
`isAnyHandTranslucent()` / `renderTranslucent(Matrix4fc, float, Camera, GameRenderer,
WorldRenderingPipeline)`（内含 setPhase INVOKE）/ `isActive()` / `isRenderingSolid()`，
`IrisRenderingPipeline.finalizeLevelRendering()` —— 全部命中。

未装 Iris / 非 1.10.7 / 无光影：三条路径全部失效为原版即时绘制，行为不变。

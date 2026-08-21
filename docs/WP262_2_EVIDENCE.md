# WP-262-2 证据：非渲染 API、Gui 重组与 AT

日期：2026-08-21  
目标：Minecraft 26.2 + NeoForge 26.2.0.64

## 证据输入

### ① Minecraft 26.2 未混淆 jar

- 语义上游：`q14433686-arch/TaCZ_Refabricated_Unofficial` 分支 `26.2(main)`，
  commit `5a29159902f5dddf26cfd0cd0f0fa3b75fbe94e6`；
- merged jar SHA-256：
  `703604cea43c6e720e7ce658ea7565c4a2e5db5d7ab428113776bbf8a5196b21`；
- 使用 JS 版 `javap` 对 classfile 的 access flags 与 descriptor 直接核对，并用
  26.1.2/26.2 class member 集合差分检查本仓调用。

### ② NeoForge 26.2.0.64 sources

官方仓库 `neoforged/NeoForge` commit
`e973c1d1bbd2d2cf013b6df2b3c4c050f2b7d2f0`。对本仓实际 import 的 NeoForge 类
逐文件比较 `26.1.x` 与 `26.2.x`：

- `DeferredRegister` 本仓使用的注册面无签名变化；
- `RegisterGuiLayersEvent`、`GuiLayer`、`RenderGuiLayerEvent` 无签名变化；
- `RegisterKeyMappingsEvent`、item-model 注册事件、
  `RegisterParticleProvidersEvent#registerSpecial(ParticleType, ParticleProvider)` 无签名变化；
- `RegisterPictureInPictureRenderersEvent` 有 factory 变化，留给 WP-262-3 与 vanilla PiP
  签名一起处理；
- `ViewportEvent.ComputeFov` 构造参数减少，但本仓只消费 event getter，不构造事件；
- `ItemTooltipEvent` 构造参数增加，但本仓只订阅并读取原有 getter。

## Gui 重组

26.2 classfile 已核：

```text
Minecraft.gui : public final Gui
Gui#screen() : Screen
Gui#setScreen(Screen) : void
```

已将当前基线中全部直接 `Minecraft.screen` / `Minecraft#setScreen` 使用点迁到
`Minecraft.gui.screen()` / `Minecraft.gui.setScreen(...)`。本仓实际命中是 14 个表达式、
9 个文件（工单中的“约 39 处”是调研快照，不是本提交基线的实际数量）。复查 grep 为 0：

```text
(Minecraft.getInstance()|mc|minecraft).screen
(Minecraft.getInstance()|mc|minecraft).setScreen(...)
```

HUD 注册继续使用 NeoForge 26.2 未改签名的 `RegisterGuiLayersEvent`。全屏 TACZ 界面隐藏
HUD 的决定改由已在役的 `RenderGuiLayerEvent.Pre` 逐层取消，不依赖 26.2 已拆出的 vanilla
`Hud#extractRenderState` 私有流程；准星层的单独取消逻辑保持不变。

## 文本格式

26.2 classfile 已核：

```text
TextColor.YELLOW : TextColor
TextColor.GRAY : TextColor
TextColor#getValue() : int
```

`ChatFormatting#getColor()` 已不存在。因此 `InteractKeyTextOverlay` 改用
`TextColor.YELLOW/GRAY#getValue()`，仍显式 OR `0xFF000000`，避免六位 RGB 的 alpha=0
被 GUI text 提取短路丢弃。

## Access Transformer 逐条重验

26.2 classfile 结果：

| 原 AT 项 | 26.2 descriptor / access | 处理 |
|---|---|---|
| `RenderType.<init>` | `(String, RenderSetup)V`，`private` | **保留**，签名未变 |
| `MultiPlayerGameMode#ensureHasSentCarriedItem` | `()V`，`public final` | 删除冗余 AT |
| `Minecraft#startUseItem` | `()V`，`public final` | 删除冗余 AT |
| `LivingEntity#jumping` | `Z`，`public` | 删除冗余 AT |
| `RenderPipelines#register` | `(RenderPipeline)RenderPipeline`，`public static` | 删除冗余 AT |

AT 现在只包含一个确实需要 widening 的精确 descriptor，减少启动期签名漂移风险。

## 其他非渲染核验

- 26.1.2 与 26.2 的 Minecraft class member 名集合做差，再对本仓实际调用进行匹配；
  非渲染侧没有发现额外已删除方法。
- 该检查明确找到并保留给 WP-262-3 的渲染断点：PiP 的
  `getFeatureRenderDispatcher/getSubmitNodeStorage/getLighting`，GameRenderer camera getters，
  以及 `ScopeRenderTypes` 的旧 pipeline/vertex API。没有把它们伪报成“本包已解决”。
- 26.2 `EntityType.Builder#build(ResourceKey<EntityType<?>>)`、
  `BlockEntityType(BlockEntitySupplier, Set<Block>)`、`ParticleType(boolean)`（protected）均与
  本仓当前注册写法兼容；自定义粒子通过 `ParticleType` 子类调用 protected 构造器，不直接
  `new SimpleParticleType`。
- common mixin 目标 `Entity#tick`、`LivingEntity#tick`、
  `ServerGamePacketListenerImpl#handlePlayerCommand/#handlePlayerAction`、
  `ServerPlayer#restoreFrom(ServerPlayer, boolean)` 均在 26.2 descriptor 中存在。
- mixin compatibility level 已与 JDK 25/toolchain 对齐为 `JAVA_25`。

## 静态验证

```text
legacy direct screen access: 0
legacy ChatFormatting.getColor calls: 0
legacy IItemHandler imports/calls: 0
registered mixin JSON parse: 3 x OK
java-parser 3.0.1 on changed Java files: all PARSE OK
git diff --check: success
```

## 未完成的动态验收

本沙盒仍因无可下载的 JDK 25 / Gradle 依赖而不能执行 `compileJava` 或 `runServer`。
因此没有声称专服已出现 `Done`，枪包装载数字也尚未与 26.1.2 实跑对比。需要在可联网
环境补跑：

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew runServer --no-configuration-cache
```

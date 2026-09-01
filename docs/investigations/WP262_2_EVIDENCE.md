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
- `ViewportEvent.ComputeFov` 构造参数减少，并删除了区分 world/HUD 的
  `usedConfiguredFov()`；外部 JDK 25 首次编译据此报错。现由 `CameraMixin` 精确包围
  `Camera#calculateFov(F)F` 与 `#calculateHudFov(F)F`，在 NeoForge 仍于共享 helper
  内发事件时恢复 pass 身份；不从 FOV 数值猜测；
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

最初的静态检查误读了 refab Loom 已应用 access widener 的 merged jar，把三个被 AW
开放的成员当成了原版 public。用户在真实 JDK 25 + ModDevGradle 2.0.144 + NeoForge
26.2.0.64 transformed compile classpath 上的首次 `compileJava` 纠正了这一点。refab 26.2
自己的 `tacz.accesswidener` 也明确保留同三个入口。

| AT 项 | 26.2 descriptor / 实际 access | 处理 |
|---|---|---|
| `RenderType.<init>` | `(String, RenderSetup)V`，`private` | scope 改用公开 `RenderType#create` 后删除该 AT |
| `MultiPlayerGameMode#ensureHasSentCarriedItem` | `()V`，NeoForge patched source 为 `private` | **恢复** `public` AT |
| `Minecraft#startUseItem` | `()V`，真实 compile 为 `private` | **恢复** `public` AT |
| `LivingEntity#jumping` | `Z`，真实 compile 为 `protected` | **恢复** `public-f` AT |
| `Player#canCriticalAttack` | `(Entity)Z`，26.2 package-private | LR 近战前滚后新增 `public` AT；与 refab 26.2 access widener 同 descriptor |
| `RenderPipelines#register` | `(RenderPipeline)RenderPipeline`，`public static` | 仍删除；本仓已改用 NeoForge pipeline event |

AT 现在包含四个在役源码访问所必需的精确目标；`RenderType` 构造器与已不用的
`RenderPipelines#register` 均不再开放。前三个来自核心端口，第四个是 2026-08-22 LR 前滚新增。

## 26.1.2 R1 多人修复回流

26.2 最初切自 Beta-1，后续读取 R1 分支
`arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0` 并回流三个必要提交：

- `ServerMessageGunDraw` 两个天然可空栈改用 `ItemStack.OPTIONAL_STREAM_CODEC`；
- `AttachmentsTagManager` / `RecipeFilterManager` 改走 `registerNetwork`；
- Iris already-assigned 视为已有成功分类；
- 四个双端 `Item#getName(ItemStack)` 改查 common index；
- mods.toml 注释不再包含未知 dollar-brace 模板表达式。

来源提交、26.2 descriptor、LAN/专服根因与冻结测试记录见
`docs/records/R1_SYNC_26_2_20260821.md`。R1 的测试 PASS 不外推到 26.2；当前 HEAD 按
`docs/DEDICATED_SERVER_TEST.md` 重跑 L0-L3 与 L2.5。

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

## 外部编译与多人验收

2026-08-21，用户在 Windows 的真实 JDK 25 / Gradle 9.2.1 环境运行 `gradlew build`：
NeoForm 成功下载并处理 Minecraft 26.2，随后 `compileJava` 报 9 个错误。本轮逐项处理：

- 恢复上述三个必须的 AT；
- `Gui#getGuiTicks()` 改为 26.2 的 `Gui#hud` → `Hud#getGuiTicks()I`；
- `ComputeFov#usedConfiguredFov()` 改为 Camera world/HUD caller context；
- `AvatarRenderer#renderRightHand/#renderLeftHand` 改用 26.2 五参 descriptor，并把末参恢复为
  对应 sleeve model-part 是否显示，而不是旧代码误传的 slim/player 参数。

上述修复、scope-mask 替换和 R1 多人修复均进入当前候选后，用户报告
`docs/DEDICATED_SERVER_TEST.md` **L0-L3 全部 PASS**：当前 build、`runServer`、真实生产专服
和双客户端联机基础矩阵关闭。冻结回执：
`docs/records/SERVER_TEST_20260821_262_R1.md`。L2.5 未被单独点名，继续保持待确认。

2026-08-22 又前滚 LR 内置层并增加第四条 AT；上述 PASS 因而只覆盖 LR 合入前核心候选。
当前 artifact 必须按 `docs/DEDICATED_SERVER_TEST.md` 重跑，LR descriptor 证据见
`docs/records/LR_R1_SYNC_26_2_20260822.md`。

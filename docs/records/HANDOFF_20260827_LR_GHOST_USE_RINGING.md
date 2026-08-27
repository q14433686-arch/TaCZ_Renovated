# Cross-branch handoff port (2026-08-27) — «幽灵使用» / 耳鸣资源 / 音效失败可见

Source: `TaCZ_Refabricated_Unofficial` 的 `arena/01a043be-tacz-refabricated-unofficial`
（`26.2(main)`），共用核心 `docs/handoff/HANDOFF_COMMON_2026_08_27.md` +
分支差异 `docs/handoff/HANDOFF_TO_SISTER_NEOFORGE.md`。
本仓库为 **NeoForge 1.21.11** 线（`41fc53d` 基线上做）。

> **验证状态**：本环境无 JDK / 无 MC，**全部为源码级闭环，未实机**。
> 每一条都必须按共用核心 §6 的实机清单回归后再对外宣称「支持/修复」。

## 任务 A — 长按右键不松手的「幽灵使用」

| 项 | 内容 |
| --- | --- |
| A1 门禁 | 新建 `me/xjqsh/lrtactical/client/input/UsePressGate.java`（纯客户端，无 mixin）+ `com/tacz/guns/mixin/client/MinecraftUseRestartMixin.java`（`@Inject("startUseItem", HEAD, cancellable)`，仅在 `UsePressGate.shouldBlockRestart()` 时 cancel），注册进 `tacz.mixins.json` 的 `client` 数组。 |
| A2 冷却 | `ThrowableItem#use` / `ConsumableItem#use` 去掉 `if (!level.isClientSide())`，改为 `ModCapabilities#coolDowns` 按端返回的 `SERVER_COOL_DOWNS` / `CLIENT_COOL_DOWNS` **两端各查一次**。 |
| A2 兜底 | 新建 `me/xjqsh/lrtactical/client/input/StuckUseRecovery.java`，越过 `prepare + life + 20 tick` 本地 `stopUsingItem()`（不 `releaseUsingItem()`，那会真把手雷扔出去）。 |
| 接线 | 两者挂在 `LrClientEvents.onClientTickPost`（NeoForge `ClientTickEvent.Post`），与既有 `DeafenState.tick` 同处。 |

**先决条件核对（本分支真实环境）**：本仓 `ModCapabilities` 用的是 **WeakHashMap 按端分表**
（`SERVER_COOL_DOWNS` / `CLIENT_COOL_DOWNS`，`coolDowns(player)` 按 `player.level().isClientSide()` 选），
不是 doc 里写的 NeoForge attachment —— 以实际代码为准。
两条前提均成立：
1. `ModCapabilities#init` 把 `coolDowns(player).tick()` 挂在 NeoForge `PlayerTickEvent.Pre`，
   该事件客户端与服务端玩家都会触发，配合按端分表，两侧各自每游戏刻恰好 tick 一次；
2. `ServerMessageCustomCooldown` → `LrClientBridge` → `LrClientPacketHandlers.onCustomCooldown`
   → `ModCapabilities.coolDowns(player)` 的 `addCooldown` / `removeCooldown` 正常往返。

## 任务 B — 耳鸣资源（此前三条 NeoForge 线全缺，效果图标为紫黑块）

| 项 | 内容 |
| --- | --- |
| B1 | 新建 `assets/lrtactical/sounds.json`（顶层无反序列化注解键，逐字节照抄）。 |
| B2 | 从 refab 取 `sounds/stun_ringing.ogg`（28566 B）、`textures/mob_effect/deafened.png`（302 B）、`textures/mob_effect/blinded.png`（188 B），并取 `scripts/verify_lr_assets.py`、`scripts/gen_effect_icons.py`。 |
| B3 | `DeafenState#tick` 接住 `SoundManager#play` 的 `PlayResult`，非 `STARTED` 时 WARN 一次（含三个已知坑）。 |

**B3 返回类型核对**：本线先确认了 1.21.x 的 `SoundManager#play(SoundInstance)` 返回
`SoundEngine.PlayResult`（Mojaangs: `SoundEngine$PlayResult`；Yarn 称作 `SoundSystem$PlayResult`，
见于 yarn-1.21.6 API），故可照抄 26.2 的 `var result = ...` 写法。

## 任务 C — 耳鸣消声注入点：本线**不改**

`SoundEngineMixin` 仍注入 `SoundEngine#calculateVolume(SoundInstance)`。
依据：用户实测 1.21.11 消声**生效**；同一份代码在 26.x 与 1.21.x 表现不同 ⇒ 差异在引擎。
**不要把 26.2 的 `AbstractSoundInstance#getVolume()` 改动搬到本线。**

## LR 0.4.3 一批 —— 不重复同步

cook=`prepare+life`、`life>=0` 引信、idle 只给近战、`display_offset`/`entity_transform`、
`ConsumableItemRenderer`、`getActionCount` 本线已有，已逐文件核对，**未动**。

## 自检

```bash
python3 scripts/verify_lr_assets.py --strict   # sounds.json 结构 / ogg / 效果图标（通过）
bash scripts/check_release_consistency.sh --strict   # 版本号三处 README + CHANGELOG（通过）
```

版本号已按约定升到 `1.1.8+neoforge.1.21.11.R1-hotfix2`（hotfix 序号直接接 `hotfix`，
不放 `.`/`-`/`_`），并同步 README 4 处与 CHANGELOG。

## 未实机的边界（如实记录）

- 未在真实客户端/专服长按右键验证「不再自动重读」「松手再按正常」；
- 未验证耳鸣声「清晰可闻、不被消声压掉」（本线未改动消声注入点，理论上不回归，但仍未实测）；
- 未验证 `blinded` / `deafened` 图标在 HUD 正常显示（仅资源结构自检通过）。

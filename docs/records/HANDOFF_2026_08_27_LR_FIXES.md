# HANDOFF 2026-08-27 · LR 长按幽灵使用 / 耳鸣资源与消声注入点（26.1.2 NeoForge 线）

> 依据：refab 仓 `docs/handoff/HANDOFF_COMMON_2026_08_27.md` +
> `HANDOFF_TO_SISTER_NEOFORGE.md`（来源分支提交链 82cc3e1…ae606f5，合并于
> refab `26.2(main)`）。本记录是本分支（姊妹仓 26.1.2，基线 `e6e5cbd`）的执行记录。
>
> 与交接文档的两处出入（以本分支实际代码为准）：
> 1. 交接文档假设本仓冷却表走 NeoForge attachment（`player.getData(...)`）；
>    实际是 `ModCapabilities` 的 **WeakHashMap 按端分表**
>    （`player.level().isClientSide() ? CLIENT : SERVER`）——「两端各查各表」
>    的前提同样成立，且 tick 驱动已挂在 NeoForge 原生 `PlayerTickEvent.Pre`。
> 2. 交接文档状态矩阵称姊妹 26.1.2 的 `lrtactical.mixins.json`
>    `compatibilityLevel` 为 `JAVA_17` —— 实际文件确为 `JAVA_17`，未改动。

## 一、任务清单与文件改动

| 任务 | 内容 | 文件 |
|---|---|---|
| A1 | 长按右键「一次按压只消耗一次使用」门禁 | 新增 `client/input/UsePressGate.java`；新增 `com/tacz/guns/mixin/client/MinecraftUseRestartMixin.java`（`Minecraft#startUseItem` HEAD 取消）；`tacz.mixins.json` 注册 |
| A2 | `use()` 两端都查各自冷却表 + 分叉兜底 | `item/ThrowableItem.java`、`item/ConsumableItem.java`（去掉 `!level.isClientSide()` 外壳）；新增 `client/input/StuckUseRecovery.java`（可预燃投掷物超时本地 `stopUsingItem()`） |
| 接线 | 两个 tick 钩子挂在 NeoForge `ClientTickEvent.Post` | `client/LrClientEvents.java` `onClientTickPost` |
| B1 | `assets/lrtactical/sounds.json` 新建（顶层无 `_comment`，逐字节照抄） | 新增 |
| B2 | `sounds/stun_ringing.ogg`（28566 B）、`textures/mob_effect/deafened.png`（302 B）、`blinded.png`（188 B）、`scripts/verify_lr_assets.py`、`scripts/gen_effect_icons.py` | 全部新增（自 refab `26.2(main)` 二进制/脚本复制） |
| B3 | 播放失败可见：`DeafenState#tick` 接住 `SoundManager#play` 返回值，非 `STARTED` WARN 一次 | `client/audio/DeafenState.java` |
| C | 耳鸣消声注入点：`SoundEngine#calculateVolume(SoundInstance)` → `AbstractSoundInstance#getVolume()` | 删除 `mixin/client/SoundEngineMixin.java`；新增 `mixin/client/SoundInstanceVolumeMixin.java`；`lrtactical.mixins.json` 更新条目；`DeafenState` 移除 `isRingingSound`；`StunRingingSound` 保持 `SoundSource.PLAYERS` 不动 |

## 二、API 证据（宪章 §3 层级）

| API | 证据 |
|---|---|
| `Minecraft#startUseItem()V` | ① 本仓先例 `InteractKey.java:90/97` 直接调用（编译于 26.1.2）；HEAD 取消注入 |
| `Options#keyUse` / `KeyMapping#isDown()` | ① 本仓先例 `InteractKeyTextOverlay.java:113`（`mc.options.keyUse`）、`InteractKey.isDown()` 等 |
| `LocalPlayer#isUsingItem/getUseItem/getTicksUsingItem/stopUsingItem` | ① 本仓先例（`UsingProgressOverlay`、`ConsumableItemRenderer`、`ThrowableItemRendererWrapper`） |
| `IThrowable#getThrowableIndex`、`ThrowableData#isCookable/getPrepareTime/getEntityData().getLifeTime` | ① 本仓既有 API（`IThrowable.java:147`、`ThrowableData.java:46/67/75`、`EntityData.java:46`） |
| `PlayerTickEvent.Pre` 双端玩家每游戏刻触发 | ① 本仓 `ModCapabilities.init()` 既有用法（类注释已载明双端各 tick 一次） |
| 客户端冷却表 `addCooldown`/`removeCooldown` 由 `ServerMessageCustomCooldown` 驱动 | ① `LrClientPacketHandlers.java:32-41` |
| `SoundManager#play(SoundInstance)` 返回 `SoundEngine.PlayResult`；`AbstractSoundInstance#getVolume()F` public 非 final | ② refab 对 26.2 jar 字节码核对（其提交 707078e 载明）；26.1.2 同代引擎，**推断**，待实机编译确认 |
| 26.x `SoundEngine#play` 绕过 `calculateVolume(SoundInstance)`（@154 getVolume → @189 内层重载） | ② refab 26.2 字节码、两种方法互证；26.1.2 无 jar 可核（沙箱 piston-meta / NeoForge maven 均不可达），属推断 |
| 注入点覆盖超集论证 | 无论 26.1.2 的 `play()` 走外层还是直接内层，都读 `getVolume()`（外层实现即 `calculateVolume(getVolume(), getSource())`），故 `getVolume()` 注入为旧注入点的覆盖超集；唯一边界：不继承 `AbstractSoundInstance` 的音效不消声（原版与绝大多数模组不受影响） |

## 三、状态（如实区分）

- **源码级闭环**：A1 / A2 / B1 / B2 / B3 / C 全部改动完成；`scripts/verify_lr_assets.py --strict` 通过（3 ok / 0 fail）。
- **未实机验证**：沙箱无 JDK 与依赖源，无法编译/运行。需用户实机确认清单见下。

## 四、实机复测清单（对应用户验收）

1. 长按右键把手雷/闪光弹/消耗品用完 → 不应自动再读一次条、姿势不应定格；松手再按应正常开始下一次；连点投掷手感不变。
2. 冷却期内（20–40 tick）松手再按 → 不应出现「读条但物品不消耗」。
3. 被闪光弹震到 → 枪声/脚步/环境音应立刻整体变闷（本分支注入点已换，重点确认**消声仍生效**且**耳鸣蜂鸣可闻**两条同时成立；若耳鸣听不见，看日志 `[LRTactical] Stun ringing sound did not start: result=…`，`NOT_STARTED` = 资源/音量问题，无这行 = 没走到播放）。
4. 效果列表/HUD 上 `blinded` 与 `deafened` 图标都不是紫黑块。
5. 高延迟下重复 1、2 条：分叉也应约 1 秒内自行恢复（`StuckUseRecovery`）。
6. 编译关：`SoundManager#play` 返回类型、`AbstractSoundInstance#getVolume()`、`SoundSource#getName()` 若在 26.1.2 有出入，属单行修正，回报即可。

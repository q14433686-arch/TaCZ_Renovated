# 记录：静默失效事件处理器清点与接线（26.2 线，2026-09-02）

- **分支**：`arena/01a06423-tacz-renovated`（基线 `65e1b88`，NeoForge 26.2.0.64 / MC 26.2 / Java 25）。
- **触发**：26.1.2 线工单《核查并修复「静默失效」事件处理器》（该线 records
  `WIRE_DEAD_HANDLERS_2612_20260902.md` + `HANDOFF_DEAD_HANDLERS_TO_262_12111_20260903.md`，
  本仓库无此二文件——独立移植线按工单要求**先行自查扫描**，未照抄 diff）。
- **缺陷形态**：方法体已移植、可编译、启动无报错，但既无 `@EventBusSubscriber`
  注解、也未被已接线类静态调用或 `addListener` 注册 ⇒ 功能整块静默失效。
- **证据级别**：静态闭环（逐类清点 + NeoForge 26.2 源码逐 API 指认）+
  CI 编译门；**运行期未实机验证**，不宣称已修。

## 1. 扫描方法（可复现命令）

### 1.1 event 目录逐类清点（注解 / 静态调用 / addListener 三者至少居一）

```bash
# 清点候选目录：com/tacz/guns/event、event/ammo、client/event
for f in src/main/java/com/tacz/guns/event/*.java \
         src/main/java/com/tacz/guns/event/ammo/*.java \
         src/main/java/com/tacz/guns/client/event/*.java; do
  grep -L "@EventBusSubscriber" "$f";           # 无注解类
done
# 再对每个无注解类找真实调用点（排除自身、import、注释行）：
grep -rn "ClassName\." src/main/java --include="*.java" \
  | grep -v "event/ClassName.java" | grep -v "import\|//\|^\s*\*"
```

**两个假阳性陷阱的规避**（工单点名）：

1. **tacz 自有 `ServerTickEvent` 与 NeoForge 同名类撞 grep**：本仓
   `com.tacz.guns.event.ServerTickEvent`（自有）与
   `net.neoforged.neoforge.event.tick.ServerTickEvent`（NeoForge，注意在
   `event/tick/` 包，**不在** `event/server/`——26.2 的 `event/server/` 只剩
   生命周期类）。判据：看 import / 全限定名，不看简单类名。
   本线已接线类 `SyncedEntityDataEvent.java:16,75` import 并订阅的就是
   NeoForge 侧 `ServerTickEvent.Post`，勿计入自有类死活。
2. **javadoc 里引用的 `@SubscribeEvent` 字样**：grep `@SubscribeEvent` 时
   排除注释行（`^\s*\*`、`//`），本线所有真注解均在方法修饰符行。

### 1.2 四类全仓扫描

```bash
# ① handler 孤儿：无注解且无调用点（= §2 表中「死」者）
# ② 自有事件发布点：api/event 与 api/client/event 逐个找 new/post
for e in $(ls src/main/java/com/tacz/guns/api/event/{common,server}/ | grep .java); do
  grep -rn "new ${e%.java}\|EntityHurtByGunEvent\.\(Pre\|Post\)" src/main/java \
    --include="*.java" | grep -v "api/event"; done
# ③ 配置死链：ConfigRead 缓存的 init() 调用方、spec 定义与注册链
grep -rn "HeadShotAABBConfigRead.init\|InteractKeyConfigRead.init" src/main/java
# ④ 网络包注册：network/message/** 逐 payload 对照 NetworkHandler.register
```

## 2. 清点结果（26.2 线自有缺陷集合）

### 2.1 `event/` 与 `event/ammo/`（13 类）

| 类 | 注解 | 静态调用 | 判定 |
|---|---|---|---|
| `EntityDamageEvent` / `HitboxHelperEvent` / `KnockbackChange` / `SyncBaseTimestamp` / `SyncedEntityDataEvent` | ✅ | — | 在役 |
| `ChangeGunPropertyEvent` | ❌ | ✅ `AttachmentPropertyManager.java:64`（该类经 `GunMod` 构造器 + 多处 gameplay 调用在役） | **在役，无需修**（26.1.2 清单未列；本线非孤儿） |
| `CommonLoadPack` | ❌ | ❌ | **有意空壳**（方法体只剩注释，全谱系如此）⇒ 不修 |
| `TravelToDimensionEvent` | ❌ | ❌ | **死 ⇒ 修复 #1** |
| `ServerTickEvent`（自有） | ❌ | ❌ | **死 ⇒ 修复 #2（最严重：BURST 连发只出第一发、Lua `safeAsyncTask` 永不执行）** |
| `LoadingConfigEvent` | ❌ | ❌ | **死 ⇒ 修复 #3** |
| `PlayerRespawnEvent` | ❌ | ❌ | **死 ⇒ 修复 #4** |
| `ammo/BellRing`、`ammo/DestroyGlassBlock` | ❌ | ❌ | **死 ⇒ 修复 #5**（发布点在：`EntityKineticBullet#onHitBlock`，本仓 489–492 行 `NeoForge.EVENT_BUS.post(ammoHitBlockEvent)` ✅） |
| `PreventGunClick` | ❌ | ❌ | **死 ⇒ 修复 #7** |

### 2.2 `client/event/`（16 类）

仅 `ClientGameEvents` 带 `@EventBusSubscriber(Dist.CLIENT)`，其余 15 类全部由它
静态转发（逐类 grep 核对：`CameraSetupEvent`/`ClientHitMark`/`ClientPreventGunClick`
`onClickInput`/`CommonNetworkCacheEvent`/`FirstPersonRenderGunEvent`/`InventoryEvent`
/`PlayerEnterWorld`/`PlayerHurtByGunEvent`/`PreventsHotbarEvent`/`RefreshClonePlayerDataEvent`
/`RenderCrosshairEvent`/`RenderHeadShotAABB`/`TickAnimationEvent`/`TooltipEvent`
——调用点均在 `ClientGameEvents.java:51–193`）。**无孤儿**。
工单第 6 项的「GuiMixin 转发是幻影调用点」警告在本线**不成立**：
本线 `PreventsHotbarEvent` 注释声称的转发方是 `RenderGuiLayerEvent.Pre`
（`ClientGameEvents#onRenderGuiLayer`，83 行），grep 证实为**真实调用点**。
26.2 线该项在基线即已接线，非死模块 ⇒ **不改代码**；但见 §5.1 的语义分歧论证。

### 2.3 四类全仓扫描结果

- **自有事件发布点**：`AttachmentPropertyEvent`(1)、`EntityHurtByGunEvent.Pre/Post`(4)、
  `EntityKillByGunEvent`(2)、`GunDrawEvent`(3)、`GunFireEvent`(3)、`GunFireSelectEvent`(3)、
  `GunMeleeEvent`(3)、`GunReloadEvent`(3)、`GunShootEvent`(3)、`AmmoHitBlockEvent`(1)
  ——全部在役。`GunFinishReloadEvent` = 0 发布点：官方 1.20.1 也从未 post
  （工单第 8 项确认不修，**不补发射点**）。
- **配置死链**：`HeadShotAABBConfigRead.init()`/`InteractKeyConfigRead.init()`
  仅被死的 `LoadingConfigEvent` 调用 ⇒ 爆头 AABB 与交互键黑白名单两缓存永远为空
  （消费方 `EntityUtil.java:92`、`InteractKey.java:89,96`、
  `InteractKeyTextOverlay.java:95,118`、`RenderHeadShotAABB.java:24` 全部读空）。
  spec 链完好：`SyncConfig` → `ServerConfig.init` → `GunMod:41`
  `registerConfig(Type.SERVER, ...)` ⇒ 默认文件名即 `tacz-server.toml`，修 #3 接线后即通。
- **网络包注册**：`network/message/**` 24 个 play payload + 2 个 configuration
  payload 全部在 `NetworkHandler#register`（31–73 行）注册，无孤儿。
- 附带核对（不在工单目录但属同类风险）：`lrtactical/event/MeleeAttackHandler`
  由 `EquipmentMod.java:74` 调用、`lrtactical/client/event/LrTickAnimationEvent`
  由 `LrClientEvents` 调用——均在役。

## 3. NeoForge 26.2 API 证据（宪章 §3）

来源：GitHub `neoforged/NeoForge` 分支 `26.2.x`
@ `7b9ea6b1103c33e99784da75f3e8e3c808ec0727`（tag `26.2.0` 与分支 HEAD 对下列
文件逐字节 diff 一致，`ServerTickEvent.java` 已验证 identical；编译目标
`neo_version=26.2.0.64` 属同分支后续 build）；FML 部分取
`neoforged/FancyModLoader` main @ `177a6b059a30d4869c04ed8a377f93f65bce56ab`
（`net.neoforged.fml.*` 不在 NeoForge 仓库内）。

| # | 引用 API | 指认（类#方法(签名) @ 文件:行） |
|---|---|---|
| 1 | 生物跨维度 | `EntityTravelToDimensionEvent#getDimension()→ResourceKey<Level>`（event/entity/EntityTravelToDimensionEvent.java:28；javadoc：**传送前**事件、目标「may be the same as the entity's current dimension」⇒ 需过滤同维度）；发布点 `CommonHooks#onTravelToDimension(Entity,ResourceKey<Level>)→boolean`（common/CommonHooks.java:838–842，`NeoForge.EVENT_BUS.post`）；**patch 触发行** `patches/net/minecraft/world/entity/Entity.java.patch` 于 `Entity#teleport(TeleportTransition)` 首行注入（同 patch 上下文含 `newLevel.dimension() != serverLevel.dimension()` —— `Level#dimension()` 与 `==` 比较的①级证据；`this.level()` 亦在同上下文出现） |
| 1 | 玩家跨维度 | `PlayerEvent.PlayerChangedDimensionEvent`（event/entity/player/PlayerEvent.java:517；`getFrom()→ResourceKey<Level>` :527、`getTo()` :531）；发布点 `EventHooks#firePlayerChangedDimensionEvent(Player,ResourceKey<Level>,ResourceKey<Level>)`（event/EventHooks.java:919–920）——传送**后**时序，对应 refab `AFTER_PLAYER_CHANGE_LEVEL` |
| 2 | 服务端 tick | `net.neoforged.neoforge.event.tick.ServerTickEvent.Post`（event/tick/ServerTickEvent.java:54；`getServer()→MinecraftServer` :43；`Pre`/`Post` 为独立事件类，26.2 的 `event/server/` 包**无**此类——grep 陷阱①）；本仓在役先例 `SyncedEntityDataEvent.java:75` 同款订阅 |
| 3 | 配置加载 | `ModConfigEvent.Loading` / `ModConfigEvent.Reloading`（fml/event/config/ModConfigEvent.java:27/38；`getConfig()→ModConfig` :19）；`ModConfig#getFileName()→String`（fml/config/ModConfig.java:43）；`ModConfigEvent implements IModBusEvent` ⇒ mod 总线；`fml/common/EventBusSubscriber.java` **无 `bus()` 参数**（仅 `value()→Dist[]` :54），javadoc :40「events inheriting IModBusEvent will be registered to the mod's event bus」⇒ 自动路由 |
| 4 | 重生 | `PlayerEvent.PlayerRespawnEvent`（PlayerEvent.java:493；`isEndConquered()` :506——**按官方对齐不过滤**）；发布点 `EventHooks#firePlayerRespawnEvent(ServerPlayer,boolean)`（EventHooks.java:952–953），`getEntity()` 即新玩家实例 |
| 5 | 弹击方块 | 自有事件 `AmmoHitBlockEvent`，发布点本仓 `EntityKineticBullet#onHitBlock`（489–492 行） |
| 6 | HUD 层 | `RenderGuiLayerEvent.Pre`（`getName()`/`setCanceled(true)`）——本线在役用法见 `ClientGameEvents#onRenderGuiLayer`（79–92 行，含 `VanillaGuiLayers.CROSSHAIR` 比较），基线编译绿即证 API 存在 |
| 7 | 左键兜底 | `PlayerInteractEvent.LeftClickBlock`（event/entity/player/PlayerInteractEvent.java:252；**`setCanceled(boolean)` 覆写 @293：置 canceled 时同时 `useBlock`/`useItem`→`TriState.FALSE`** ⇒ 完整阻断；`getEntity()`、`getHand()` :345、`getPos()` :364、`getFace()` :372、`getLevel()` :379；构造器固定 `InteractionHand.MAIN_HAND` ⇒ 恒主手）；客户端侧对应类 `ClientPreventGunClick` 经 `ClientGameEvents#onClickInput`（`InputEvent.InteractionKeyMappingTriggered`）在役 |

谱系语义对照（官方 1.20.1 `MCModderAnchor/TACZ@1.20.1`，ref=`1.20.1` 逐文件取回）：
`PreventGunClick`（`event.getEntity().getItemInHand(MAIN_HAND)`+`setCanceled`，无逻辑侧过滤）、
`PlayerRespawnEvent`（`PlayerEvent.PlayerRespawnEvent`，无 `isEndConquered` 过滤）、
`LoadingConfigEvent`（`ModConfigEvent.Loading/Reloading` + `getFileName()`）、
`ServerTickEvent`（旧 `TickEvent.ServerTickEvent` 不分相位每 tick 两次 ⇒ 26.x 取 `Post` 每 tick 一次为最近映射）、
`TravelToDimensionEvent`（仅 `EntityTravelToDimensionEvent`，玩家从未被覆盖——refab 已论证为上游继承缺陷）、
`ammo/BellRing`、`ammo/DestroyGlassBlock`（`AmmoHitBlockEvent` 注解订阅）。

## 4. 修复清单（7 文件，全部为「补接线」，方法体语义保持移植版）

| # | 文件 | 改动 |
|---|---|---|
| 1 | `event/TravelToDimensionEvent.java` | `@EventBusSubscriber` + 两个 `@SubscribeEvent`：生物接 `EntityTravelToDimensionEvent`（排除 `Player` 防与玩家路双触发；`getDimension()==level().dimension()` 过滤同维度）；玩家接 `PlayerEvent.PlayerChangedDimensionEvent`（传送后，`instanceof ServerPlayer` 防御） |
| 2 | `event/ServerTickEvent.java` | `@SubscribeEvent onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post)`（类名冲突故用 FQN）→ `CycleTaskHelper.tick()` |
| 3 | `event/LoadingConfigEvent.java` | `ModConfigEvent.Loading`/`.Reloading` 两注解订阅 → 共享 `onConfigLoaded(ModConfig)`，按 `tacz-server.toml` 过滤（保留原文件名判据） |
| 4 | `event/PlayerRespawnEvent.java` | `PlayerEvent.PlayerRespawnEvent` 注解订阅；`instanceof ServerPlayer` 取新玩家；不过滤 `isEndConquered()`；保留 `getGunIndex()` null 防御与 26.x `getNonEquipmentItems()` |
| 5 | `event/ammo/BellRing.java`、`event/ammo/DestroyGlassBlock.java` | 补 `@EventBusSubscriber` + `@SubscribeEvent`（签名本就吃 `AmmoHitBlockEvent`，仅缺注解） |
| 7 | `event/PreventGunClick.java` | `PlayerInteractEvent.LeftClickBlock` 注解订阅，官方形状（`getItemInHand(MAIN_HAND)`+`setCanceled(true)`），替换未接线的旧签名静态方法 |

不改动：`CommonLoadPack`（空壳）、`GunFinishReloadEvent`（不补发射点）、
`ChangeGunPropertyEvent`（在役）、`PreventsHotbarEvent`/`ClientGameEvents`（在役，见 §5.1）。

## 5. 与 26.1.2 线结论的分歧点

### 5.1 第 6 项（PreventsHotbarEvent）：本线不改为 HOTBAR-only

- 26.1.2 修法 = `RenderGuiLayerEvent.Pre` 仅取消 `VanillaGuiLayers.HOTBAR` 一层。
- 本线现状 = `ClientGameEvents#onRenderGuiLayer` 对**每一层**转发判定并取消
  （合成台/改装台界面开起时全 HUD 隐藏）。
- **谱系证据**：官方 1.20.1 用 `RenderGuiOverlayEvent.Pre` 且**无 overlay ID 过滤**
  （= 逐 overlay 全部取消）；游戏语义权威 refab `26.2(main)` 的该类注释明写
  「GuiMixin 于 `Hud#extractRenderState` HEAD 调用；返回 true 会取消本帧**整个原版
  HUD 提取**……避免背后的快捷栏/HUD 透出」——即权威语义就是全 HUD 隐藏。
- 结论：本线行为与两个权威一致，26.1.2 的 HOTBAR-only 反而是收窄；且本线
  转发点为真实调用点（非幻影）。**维持现状不改**，26.1.2 线如需对齐请反向考虑。

### 5.2 第 8 项确认

`CommonLoadPack`：方法体只剩 `//DedicatedServerReloadManager.loadGunPack();` 注释，
全谱系（官方→refab→本线）空壳一致 ⇒ 有意不接线，不修。
`GunFinishReloadEvent`：官方 1.20.1 从未 post（本线 0 发布点与官方一致）⇒ 不修、不补发射点。

## 6. 运行期验收清单（**未执行**，7 条）

1. 跨维度往返（主世界↔下界）后换弹：服务端子弹实际增加（修复 #1 的
   `reloadStateType` 残留症状消失）；持枪僵尸跨维度不掉枪械数据。
2. BURST 模式连发：一次开火周期全部子弹射出（>第一发）。
3. 枪包 Lua `safeAsyncTask`：延迟/周期任务实际执行（如爆炸延时）。
4. `tacz-server.toml` 配 `HeadShotAABB` 条目后重启：对应生物爆头判定命中自定义 AABB；
   `InteractKeyWhitelist/Blacklist` 生效（对名单方块/实体显示/放行交互键提示）。
5. `AutoReloadWhenRespawn=true` 死亡重生（含末地返回）：背包枪械自动补满弹匣；
   背包直读（INVENTORY）与燃料（FUEL）枪不走补弹。
6. 子弹击钟：钟响；`DestroyGlass=true` 时玻璃/玻璃板/铁栏杆(HAT 音色)被击碎。
7. 主手持枪左键方块：无挖掘进度、无方块交互（服务端兜底生效，含客户端绕过场景）；
   合成台/改装台界面背后无 HUD 透出（#6 维持项回归确认）。

## 7. CI

`compile-check`（push 触发，`./gradlew compileJava`，Java 25）为本线编译门——
结果见本文件末尾追记或 Actions 页。本地沙箱无 maven.neoforged.net 出网，
无法本地 gradle 编译，编译验证以 CI 为准。

---
追记（CI）：

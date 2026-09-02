# 静默失效事件处理器接线记录（26.1.2 线，2026-09-02）

> 本轮核查目标：找出「方法逻辑已移植、但**没有任何事件总线注册**导致整块功能静默失效」
> 的处理器，补齐 NeoForge 侧接线，并对 `event`/`client/event` 两个目录做整体扫描复核。
>
> 编译验证：本地沙箱无 JDK / 无 NeoForge 仓库访问，编译门走 CI
> `compile-check-2612.yml`（push 至 `arena/**` 自动触发）。运行期**未实机验证**。

## 1. 接线的处理器与事件选型

| 处理器 | 事件（游戏总线，除注明外） | 恢复的行为 |
|---|---|---|
| `event/TravelToDimensionEvent` | `PlayerEvent.PlayerChangedDimensionEvent`（玩家）＋ `EntityTravelToDimensionEvent`（非玩家，防御性） | 跨维度后服务端枪械状态机重置，修「客户端演完整套换弹、服务端早退不加子弹」 |
| `event/LoadingConfigEvent` | `ModConfigEvent.Loading` / `.Reloading`（**mod 总线**，自动路由） | `HeadShotAABBConfigRead`（第三方生物爆头 AABB）与 `InteractKeyConfigRead`（交互黑白名单）随 tacz-server.toml 加载/热重载/登入同步而解析 |
| `event/PlayerRespawnEvent` | `PlayerEvent.PlayerRespawnEvent` | 配置 `AutoReloadWhenRespawn` 生效：重生自动填弹 |
| `event/ammo/BellRing` | `AmmoHitBlockEvent`（本 mod 自有事件，`EntityKineticBullet#onHitBlock` 发布） | 子弹射钟会响 |
| `event/ammo/DestroyGlassBlock` | `AmmoHitBlockEvent` | 配置 `DestroyGlass` 开启时子弹碎玻璃 |
| `event/ServerTickEvent`（扫描新发现，**严重**） | `net.neoforged.neoforge.event.tick.ServerTickEvent.Post` | `CycleTaskHelper.tick()` 恢复运转：`addCycleTask` 只在入队时立刻执行第一次，后续循环全靠 `tick()` —— 未接线时 **BURST 连发服务端只打出第一发**、Lua `safeAsyncTask` 延迟任务永不执行 |
| `client/event/PreventsHotbarEvent` | 经 `ClientGameEvents#onRenderGuiLayer`（`RenderGuiLayerEvent.Pre` + `VanillaGuiLayers.HOTBAR`） | 工作台/改装台全屏界面隐藏底部快捷栏 |
| `event/PreventGunClick`（扫描新发现） | `PlayerInteractEvent.LeftClickBlock` | 主手持枪禁止攻击/挖掘方块的**服务端**兜底（客户端拦截 `ClientPreventGunClick` 原本就已接线） |

## 2. API 证据（宪章 §3：类#方法(签名) + 来源层级）

来源仓库与提交（均为 ② 级 NeoForge 官方源码 / FML 官方源码）：

- `github.com/neoforged/NeoForge`，分支 `26.1.x` @ `014f4885`（2026-09-02，与
  `neo_version=26.1.2.97` 同线）；
- `github.com/neoforged/FancyModLoader`，分支 `main` @ `177a6b05`（运行时 FML 11.0.15，
  见玩家实机 latest.log）。

| API | 证据 |
|---|---|
| `PlayerEvent.PlayerChangedDimensionEvent(Player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim)`；`getFrom()`/`getTo()` | `src/main/java/net/neoforged/neoforge/event/entity/player/PlayerEvent.java` L517–534；游戏总线（类 javadoc "fired on the game event bus"） |
| 触发点：仅服务端、跨维度**完成后**、同一 `ServerPlayer` 实例 | `patches/net/minecraft/server/level/ServerPlayer.java.patch`：`teleport(TeleportTransition)` 跨维度分支内 `EventHooks.firePlayerChangedDimensionEvent(this, lastDimension, transition.newLevel().dimension())` |
| `PlayerEvent.PlayerRespawnEvent(Player, boolean endConquered)`；`isEndConquered()` | 同文件 L493–509 |
| 触发点：`PlayerList#respawn`，新玩家 `restoreFrom`＋`initInventoryMenu` 之后 → `event.getEntity()` 即重生后的新实例、背包已恢复 | `patches/net/minecraft/server/players/PlayerList.java.patch`：`EventHooks.firePlayerRespawnEvent(player, keepAllPlayerData)` |
| `EntityTravelToDimensionEvent(Entity, ResourceKey<Level> dimension)`：cancellable，传送**前**、旧实体上触发，"may be the same as the entity's current dimension" | `src/main/java/net/neoforged/neoforge/event/entity/EntityTravelToDimensionEvent.java`（全文）；触发点 `patches/net/minecraft/world/entity/Entity.java.patch`：`Entity#teleport(TeleportTransition)` 首行 `CommonHooks.onTravelToDimension(this, transition.newLevel().dimension())` |
| `ModConfigEvent.Loading` / `.Reloading` / `.Unloading`，`getConfig()`；`ModConfigEvent implements IModBusEvent` | FML `loader/src/main/java/net/neoforged/fml/event/config/ModConfigEvent.java`（全文） |
| `@EventBusSubscriber` 自动把 `IModBusEvent` 子类注册到 mod 总线、其余注册到游戏总线；26.1 起**无 Bus 参数** | FML `loader/src/main/java/net/neoforged/fml/common/EventBusSubscriber.java` javadoc："Event subscribers for events inheriting from IModBusEvent will be registered to the mod's event bus, while the rest will be registered to the NeoForge#EVENT_BUS"；本仓先例：`ClientSetupEvent` 头注释与既有接线 |
| `ModConfig#getFileName()` | NeoForge 自身用法 `NeoForgeMod#onConfigLoad(ModConfigEvent.Loading)`（`NeoForgeMod.java` L664）；SERVER 类型默认文件名 = `tacz-server.toml`（`GunMod` 注册时未传自定义名） |
| `VanillaGuiLayers.HOTBAR = Identifier.withDefaultNamespace("hotbar")` | `src/client/java/net/neoforged/neoforge/client/gui/VanillaGuiLayers.java` L21（CROSSHAIR L18，本仓已用） |
| `RenderGuiLayerEvent.Pre` cancellable：取消则该层不渲染 | `src/client/java/net/neoforged/neoforge/client/event/RenderGuiLayerEvent.java`（Pre 类 javadoc） |
| `PlayerInteractEvent.LeftClickBlock` cancellable；注意「创造模式直接破坏方块，不走 use 逻辑」 | `src/main/java/net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java` L290–338 |
| `net.neoforged.neoforge.event.tick.ServerTickEvent.Post`（服务端每 tick 末） | `src/main/java/net/neoforged/neoforge/event/tick/ServerTickEvent.java` L59；本仓先例 `SyncedEntityDataEvent#onServerTick`（已编译验证） |

## 3. 跨维度语义要点（为什么玩家/生物两条路不同）

- **玩家**：26.x 中跨维度是**同一个 `ServerPlayer` 实例被物理移动**（`ServerPlayer` patch
  的事件在 `this` 上触发即为证据）。枪械状态机存放在 `LivingEntityMixin` 的
  `@Unique ShooterDataHolder` 字段（不进 NBT），因此残值随实例带进新维度 →
  必须在 `PlayerChangedDimensionEvent` 里 `initialData()`。
- **非玩家生物**：跨维度是**复制**（vanilla `teleportCrossDimension` 新建实体 + NBT 恢复），
  `@Unique` 字段不复制，新实体天然干净，且 `LivingEntityMixin` tick 有
  `currentGunItem == null → initialData()` 兜底。NeoForge **没有** Fabric
  `AFTER_ENTITY_CHANGE_LEVEL` 的等价「完成后」事件，故对生物只在
  `EntityTravelToDimensionEvent`（传送前）做防御性重置，并按其 javadoc
  过滤掉同维度传送，避免误伤正在换弹的持枪生物。
- 玩家死亡/重生的新实例初始化早已由 `ServerPlayerMixin#restoreFrom` 注入覆盖，
  与本轮改动无关，不会双重触发切枪冷却（`initialData()` 不触碰 `drawTimestamp`）。

## 4. 目录整体扫描结论（复核「其他代码」）

扫描方法：对 `com/tacz/guns/event`、`com/tacz/guns/event/ammo`、
`com/tacz/guns/client/event` 全部类，逐个核对「@EventBusSubscriber 注解 or
被已接线类静态调用 or 被 addListener 注册」三者至少居一。

| 类 | 结论 |
|---|---|
| `EntityDamageEvent` / `HitboxHelperEvent` / `KnockbackChange` / `SyncBaseTimestamp` / `SyncedEntityDataEvent` | 已有 `@EventBusSubscriber`，正常 |
| `ServerTickEvent`（tacz 自有类，跑 `CycleTaskHelper.tick()`） | **本轮扫描发现的死模块**：全仓无调用点（此前仅 `SyncedEntityDataEvent` import 过同名的 NeoForge 事件类，易误判为已接线）。已接 `ServerTickEvent.Post`，见 §1 表 |
| `ChangeGunPropertyEvent` | 由 `AttachmentPropertyManager#postChangeEvent` 直接调用（L64），有意不走总线，正常 |
| `CommonLoadPack` | 遗留空壳；服务端枪包加载由 `CommonAssetsManager`（`AddServerReloadListenersEvent`，已接线）接管，专服冒烟已过。已加注释声明「有意不接线」 |
| `client/event` 全部（CameraSetup / ClientHitMark / ClientPreventGunClick / CommonNetworkCacheEvent / FirstPersonRenderGunEvent / InventoryEvent / PlayerEnterWorld / PlayerHurtByGunEvent / RefreshClonePlayerData / ReloadResource / RenderCrosshair / RenderHeadShotAABB / TickAnimation / Tooltip） | 全部经 `ClientGameEvents` / `ClientSetupEvent`（均有注解）分发，正常 |
| `lrtactical` 侧 | `ModCapabilities` 等用 lambda `addListener` 注册，正常；`LrClientAssetsManager` 中的 `@SubscribeEvent` 字样仅存在于 javadoc（谱系说明），实际经 `ModEntitiesRender#reloadAndRegister` 接线，正常 |

### 运行期待验证清单（本轮均未实机验证）

1. 跨维度（下界/末地往返）后立刻换弹：服务端应真实补弹；
2. BURST 模式连发应打满配置发数（对照修复前只响一发）；
3. `AutoReloadWhenRespawn=true` 时重生自动填弹（含创造/生存、燃料型枪械）；
4. 子弹射钟响、`DestroyGlass=true` 时碎玻璃 / 染色玻璃板 / 玻璃板；
5. 服务端配置里第三方生物爆头 AABB 与交互键黑白名单在专服 + 联机客户端生效（登入同步）；
6. 工作台/改装台界面打开时快捷栏隐藏，关闭后恢复；
7. 主手持枪左键方块不再破坏方块（生存），交互键白名单方块仍可交互。

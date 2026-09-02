# 静默失效事件处理器接线记录（1.21.11 线，2026-09-02）

> 任务来源：26.1.2 线交接工单
> `HANDOFF_DEAD_HANDLERS_TO_262_12111_20260903.md`（该文件只存在于 26.1.2 线，
> 本线无此文件，故不给相对链接；归档 commit：
> [d3e1cd1](https://github.com/q14433686-arch/TaCZ_Renovated/commit/d3e1cd11758d2b59046fb1a30cc938a933ff3a29)；
> 26.1.2 线修复 commit `e5828f0`/`c216a66`）。
> 本线**未照抄 26.1.2 diff**：先跑 §1 注册面扫描确定本线实际缺陷清单（与本线
> 现状一致：8 项），再按 §3 逐项在本线（NeoForge 21.11.45 / FML 10.0.36 / MC
> 1.21.11，官方 Mojang mappings）指认 API 后落笔。
>
> **本记录结论**：① `@EventBusSubscriber` 的 `Bus` 参数在本线 FML **已删除**
> （分叉点早于 26.1+——FML 9.0 已删、FML 8.0 仍有），**不得写 `bus = Bus.MOD`**；
> ② 背包遍历访问器 = `getInventory().getNonEquipmentItems()`；③
> `net.neoforged.neoforge.event.tick.ServerTickEvent.Post` 存在；④ 玩家跨维度 =
> `PlayerEvent.PlayerChangedDimensionEvent`（ServerPlayer#teleport 内
> `firePlayerChangedDimensionEvent` 为证据）；⑤ `VanillaGuiLayers.HOTBAR` +
> `RenderGuiLayerEvent.Pre` 存在（本线回调参数是 `GuiGraphics`，本修复只用
> `getName()`/`setCanceled(true)`）。
>
> 编译验证：本地沙箱无 JDK、maven.neoforged.net 对 bash 网络被断（TLS
> SSL_ERROR_SYSCALL），编译门走 CI `compile-check.yml`（本分支 push 自动触发）。
> **运行期未实机验证**；运行期验收 7 条见 §5。

## 1. 注册面扫描（勿信清单，信扫描）

命令（本线实际执行，与工单 §3 同形）：

```bash
for f in src/main/java/com/tacz/guns/event/*.java \
         src/main/java/com/tacz/guns/event/ammo/*.java \
         src/main/java/com/tacz/guns/client/event/*.java; do
  cls=$(basename $f .java); ann=$(grep -c "EventBusSubscriber" $f)
  refs=$(grep -rln --include="*.java" "\b$cls\b" src | grep -v "$f" | tr '\n' ' ')
  echo "$cls | ann=$ann | refs: $refs"
done
```

判读规则：`ann=0` 且 refs 为空（或只剩 javadoc/注释引用）= 死模块。
两个假阳性陷阱（工单 §3 指明，本线均已踩到并排除）：

1. **同名撞车**：`com.tacz.guns.event.ServerTickEvent` 与 NeoForge
   `net.neoforged.neoforge.event.tick.ServerTickEvent` 同名——`ServerTickEvent |
   ann=0 | refs: .../SyncedEntityDataEvent.java` 是 NeoForge 事件的 import，
   不是本类的调用点（须看全限定名）；
2. **javadoc 引用**：`PlayerRespawnEvent | refs: .../ModCapabilities.java` 仅是
   `// WP07 C 映射表：... PlayerEvent.PlayerRespawnEvent` 注释。

扫描结论（修复前）：**8 项全部与 26.1.2 线同构**——

| 类 | 修复前状态 | 症状 |
|---|---|---|
| `event/TravelToDimensionEvent` | ann=0、无调用 | 跨维度后服务端枪械状态机残留 |
| `event/ServerTickEvent` | ann=0、无调用（refs 为同名撞车） | BURST 服务端只打第一发、Lua 延迟任务不执行 |
| `event/LoadingConfigEvent` | ann=0、无调用 | 爆头 AABB / 交互黑白名单永不解析 |
| `event/PlayerRespawnEvent` | ann=0、refs 仅注释 | `AutoReloadWhenRespawn` 无反应 |
| `event/ammo/BellRing` | ann=0、无调用 | 子弹射钟不响 |
| `event/ammo/DestroyGlassBlock` | ann=0、无调用 | 射玻璃不碎 |
| `client/event/PreventsHotbarEvent` | ann=0、无调用；类内注释谎称「GuiMixin 转发」 | 工作台/改装台快捷栏穿模（幻影调用点：全仓 grep 无任何 `GuiMixin` 类） |
| `event/PreventGunClick` | ann=0、无调用 | 主手持枪仍可左键挖方块（服务端无兜底） |

「确认无需修」两项（与 26.1.2 线结论一致）：

- `event/CommonLoadPack`：全谱系遗留空壳（官方就是空的调用注释）；本线服务端
  枪包加载由 `CommonAssetsManager`（`AddServerReloadListenersEvent`，已接线）
  接管 → 注释声明「有意不接线」（本轮补）。
- `api/event/common/GunFinishReloadEvent`：定义在、构造器在，**全仓无任何
  `new GunFinishReloadEvent(...)` 发布点**（官方 1.20.1 同样从未 post，仅
  KubeJS wrapper 用之）→ 谱系性 API 空壳，留档不补发射点。

## 2. 本线修复内容（10 个文件）

| 处理器 | 事件（游戏总线，除注明外） | 本线适配点 |
|---|---|---|
| `event/TravelToDimensionEvent#onPlayerTravelToDimension` | `PlayerEvent.PlayerChangedDimensionEvent` | 仅服务端、跨维度完成后、同一 `ServerPlayer` 实例（patch 证据 §3.2） |
| `event/TravelToDimensionEvent#onEntityTravelToDimension` | `EntityTravelToDimensionEvent`（防御性） | 排除 `ServerPlayer`（上面已覆盖）、排除同维度（`level().dimension().equals(event.getDimension())`） |
| `event/ServerTickEvent#onServerTick` | `net.neoforged.neoforge.event.tick.ServerTickEvent.Post` | 全限定名引用避免与本类同名冲突 |
| `event/LoadingConfigEvent#onLoadingConfig` / `#onReloadingConfig` | `ModConfigEvent.Loading` / `.Reloading`（**mod 总线**） | `@EventBusSubscriber(modid=...)` 自动路由（§3.1） |
| `event/PlayerRespawnEvent#onPlayerRespawn` | `PlayerEvent.PlayerRespawnEvent` | `event.getEntity() instanceof ServerPlayer`；`getInventory().getNonEquipmentItems()`（§3.5） |
| `event/ammo/BellRing#onAmmoHitBlock` | `AmmoHitBlockEvent`（本 mod 自有） | 发布点确认：`EntityKineticBullet#onHitBlock` L490-491 `NeoForge.EVENT_BUS.post(...)` |
| `event/ammo/DestroyGlassBlock#onAmmoHitBlock` | `AmmoHitBlockEvent` | 同上 |
| `client/event/ClientGameEvents#onRenderGuiLayer`（调用 `PreventsHotbarEvent#shouldHideHotbar`） | `RenderGuiLayerEvent.Pre` + `VanillaGuiLayers.HOTBAR` | `PreventsHotbarEvent` 从幻影 `AtomicBoolean` 回调改为纯查询方法；删除「GuiMixin」谎称注释 |
| `event/PreventGunClick#onLeftClickBlock` | `PlayerInteractEvent.LeftClickBlock` | `event.setCanceled(true)`（取消后 `handleBlockBreakAction` 直接 return，§3.4） |
| `event/CommonLoadPack` | — | 仅补「有意不接线」注释（无代码语义变化） |

## 3. API 证据（宪章 §3：类#方法(签名) + 来源层级）

来源仓库与提交（均为 ②/① 级官方源码；1.21.11 为混淆时代，游戏 API 经官方
Mojang mappings，见 §3.5）：

- `github.com/neoforged/NeoForge`，分支 `1.21.11`，tip `bef35d5`
  （2026-07-29）；`gradle.properties` 即 `minecraft_version=1.21.11`、
  `neoform_version=20251209.172050`、`fancy_mod_loader_version=10.0.36`、
  `moddevgradle_plugin_version=2.0.124`（FML 10.0.36 钉版由该分支 commit
  `64a5a7f`「Update fancy_mod_loader_version to v10.0.36 (1.21.x)」引入，
  2025-12-15）；
- `github.com/neoforged/FancyModLoader`，FML 10.0 线根 tag `10.0` =
  `6e97477`（2025-08-24，`Add MC-independent error display (#333)`）；
  另核 FML 8.0/9.0 tag（见 §3.1）。

### 3.1 `@EventBusSubscriber`：本线 FML 无 `Bus` 参数（分叉 #1，已核实）

FML `loader/src/main/java/net/neoforged/fml/common/EventBusSubscriber.java`
（tag `10.0`@`6e97477` 与 main tip `177a6b0` **逐字节一致**）：

```java
public @interface EventBusSubscriber {
    Dist[] value() default { Dist.CLIENT, Dist.DEDICATED_SERVER };
    String modid() default "";
}
```

- 无 `Bus` 枚举、无 `bus` 成员；javadoc 明示：
  "Event subscribers for events inheriting from {@link IModBusEvent} will be
  registered to the {@link ModContainer#getEventBus() mod's event bus}, while
  the rest will be registered to the {@code NeoForge#EVENT_BUS}."
- 分叉时间线（同一文件各 tag）：
  - FML `8.0`（`7086f2b`，1.21.6 代）：`Bus bus() default Bus.GAME;` **仍在**；
  - FML `9.0`（`e5611d2`，2025-06-24）：`bus` **已删**（仅残留 `@see Bus`
    javadoc）；
  - FML `10.0`（本线所用 10.0.36 所在线）：**无**。
- **结论**：本线写 `@EventBusSubscriber(modid = GunMod.MOD_ID)`（无 value 即
  双端注册；mod 总线事件自动路由），**不写 `bus = Bus.MOD`**——FML 10.0 无
  `Bus` 类型，写了编译即错；「不写会静默收不到」的担忧仅适用于 FML ≤8 时代。
- 佐证：本仓既有同类接线先例——`ClientSetupEvent`（mod 总线事件，见其头注释）
  与 `ModCapabilities`（`addListener` 于 mod 总线）均已在此线编译通过。

### 3.2 跨维度触发点（分叉 #4，1.21.11 ≥ 1.21.2 传送重写语义）

- `PlayerEvent.PlayerChangedDimensionEvent(Player, ResourceKey<Level> fromDim,
  ResourceKey<Level> toDim)`；`getFrom()`/`getTo()`
  — `src/main/java/net/neoforged/neoforge/event/entity/player/PlayerEvent.java`
  （类 javadoc：fired via `ServerPlayer#teleport(TeleportTransition)`，游戏总线）。
- **触发位置证据（指认要求的 patch 行）**：
  `patches/net/minecraft/server/level/ServerPlayer.java.patch`
  `ServerPlayer#teleport(TeleportTransition)`（方法体 L1090 起）跨维度分支内
  （上下文：`serverlevel1.removePlayerImmediately(this, CHANGED_DIMENSION)` →
  `this.revive()` → … → `this.teleportSpectators(...)`）末尾：
  ```
  + net.neoforged.neoforge.event.EventHooks.firePlayerChangedDimensionEvent(this, resourcekey, p_379854_.newLevel().dimension());
  ```
  实例即 `this`（同一 `ServerPlayer` 物理移动）；`EventHooks.java` L886 将该
  事件 post 到 `NeoForge.EVENT_BUS`。
- 非玩家生物：`EntityTravelToDimensionEvent(Entity, ResourceKey<Level>
  dimension)`，`ICancellableEvent`（`getDimension()`）；
  `src/main/java/net/neoforged/neoforge/event/entity/EntityTravelToDimensionEvent.java`
  （javadoc："before an Entity travels… may be the same as the entity's current
  dimension"）；触发点 `patches/net/minecraft/world/entity/Entity.java.patch`
  `Entity#teleport(TeleportTransition)` 首行
  `CommonHooks.onTravelToDimension(this, p_379899_.newLevel().dimension())`
  （`CommonHooks.java` L828-833 post 并响应取消）。

### 3.3 tick 事件包位（分叉 #3）

`src/main/java/net/neoforged/neoforge/event/tick/ServerTickEvent.java`：
`net.neoforged.neoforge.event.tick.ServerTickEvent.Post extends ServerTickEvent`
— "fired once per server tick, after the server performs work for the current
tick. This event only fires on the logical server." ✓ 21.x tick 重构后包位与本
仓先例 `SyncedEntityDataEvent#onServerTick(ServerTickEvent.Post)` 一致（先例
已编译通过）。任务墙钟驱动，Post 每 tick 一次即可（官方 Forge 版未判 phase 每
tick 双跑的旧闻不适用）。

### 3.4 持枪禁挖方块（服务端）

- `PlayerInteractEvent.LeftClickBlock extends PlayerInteractEvent implements
  ICancellableEvent`（`src/main/java/net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java`
  L290 起；`setCanceled` 同时把 `useBlock/useItem` 置 `TriState.FALSE`）。
- 触发点 `patches/net/minecraft/server/level/ServerPlayerGameMode.java.patch`
  `handleBlockBreakAction(...)` 开头：
  ```
  + ... CommonHooks.onLeftClickBlock(player, ..., p_215121_);
  + if (event.isCanceled()) { return; }
  ```
  （`CommonHooks.java` L861-866 post。）取消后 `Block#attack` / 挖掘/创造直破
  均不进（取消先于一切分支）。javadoc 另有「创造模式 use 逻辑无效」提醒，
  与本类「只 setCanceled」的写法无冲突；客户端拦截（`ClientPreventGunClick`）
  保持不动。

### 3.5 mappings 来源与游戏 API 指认（混淆时代，必须写明）

- **映射来源**：本线 `build.gradle` / `gradle.properties` 注释明示——
  "1.21.11 is an OBFUSCATED release… Official Mojang mappings are wired
  automatically by ModDevGradle. Do not add mappings / parchment / yarn
  configuration."（`net.neoforged.moddev` 2.0.144）。类/方法名为官方
  Mojang 发布映射名（前置记录：
  `docs/records/PORT_11211_COMPILE_RECORD.md` 即以
  `neoforge-21.11.45.jar` javap 逐符号核实；`PORT_11211_DEPS.md` §六总结：
  本线 mod 以官方命名分发、无 refmap 需求）。
- **本修复用到的游戏 API**（按本线官方映射名）：
  - `Player#getInventory()` → `Inventory#getNonEquipmentItems()`（返回
    `NonNullList<ItemStack>`）——本修复沿用既有代码（`ClientIndexManager`
    L181/189、`GunSmithTableScreen` L310、`PlayerRespawnEvent` 原实现均已在用，
    且为本线 CI 已编译通过的调用）。**不要**改成 26.x 的
    `getNonEquipmentItems()`（本就是）；也不要抄官方 1.20.1 的
    `getInventory().items`（1.21.x 无公开 `items` 字段语义）。
  - `BellBlock#attemptToRing(Level, BlockPos, Direction)`：`BellRing` 原有
    调用体未动，仅补订阅注解；按本线映射，`state.getBlock() instanceof
    BellBlock` 后直接调用（`Level`/`BlockPos`/`Direction` 均为 mojmap 名）。
  - `BlockState#instrument()` 返回 `NoteBlockInstrument`：`DestroyGlassBlock`
    原有调用体未动。
  - `Entity#level()`、`ServerLevel#dimension()`：已有代码/NeoForge patch
    同映射下使用（`EntityKineticBullet.java` L490；patch 上下文
    `serverlevel.dimension()`）。
- **签名正确性的直接证据**：上述调用全部位于基础 commit 既有代码中（本线
  CI 编译门此前已绿——基础 commit `1c7ab3d` 为合并后状态），本轮只新增
  `@EventBusSubscriber`/`@SubscribeEvent` 与事件参数类型，编译门由
  `compile-check.yml` 再次把关（§4）。

### 3.6 GUI 层（分叉 #5）

- `src/client/java/net/neoforged/neoforge/client/gui/VanillaGuiLayers.java`：
  `HOTBAR = Identifier.withDefaultNamespace("hotbar")` ✓（本仓既有 `CROSSHAIR`
  同源，`ClientSetupEvent` 已用 `VanillaGuiLayers`）。
- `src/client/java/net/neoforged/neoforge/client/event/RenderGuiLayerEvent.java`：
  `Pre extends RenderGuiLayerEvent implements ICancellableEvent`，构造器
  `(GuiGraphics, DeltaTracker, Identifier, GuiLayer)`，`getName()` 返回
  `Identifier`——回调参数确实是 `GuiGraphics`（1.21.x 渲染纪元），本修复只用
  `getName()` + `setCanceled(true)`，不触参数。
- 发布点 `src/client/java/net/neoforged/neoforge/client/gui/GuiLayerManager.java`
  L75：每层渲染前 `NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(...))`，
  取消则该层不渲染；`Gui.java.patch` 中 `HOTBAR` 层只在 `hideGui` 关闭时渲染
  （全屏 Screen 打开时 HUD 层仍会走——这正是工作台/改装台需要隐藏的原因）。

### 3.7 FML 配置事件（分叉 #1 配套）

`loader/src/main/java/net/neoforged/fml/event/config/ModConfigEvent.java`
（tag `10.0`@`6e97477`）：`class ModConfigEvent extends Event implements
IModBusEvent`；静态子类 `Loading`/`Reloading`/`Unloading`（各自构造器收
`ModConfig`）；`getConfig()`。→ `@EventBusSubscriber` 自动路由至 mod 总线。
`Loading` javadoc："Fired during mod and server loading, depending on
{@link ModConfig.Type} of config file"——覆盖 tacz-server.toml 首次加载/热重载/
登入同步。

## 4. 编译与发布纪律

- 本地沙箱无 JDK；maven.neoforged.net 经 bash TLS 握手被断（curl
  SSL_ERROR_SYSCALL；`fetch_page` 工具可读该站文本，但二进制 jar 无法获取，
  故本会话不做 javap，改用 GitHub 官方源码 + 既有 CI 绿基线 + 编译门复核）。
- 已推送本分支后由 `compile-check.yml`（Java 21 + ModDevGradle + 官方 jar）
  给出编译结论；**CI 编译门绿后才算完成**。
- 洁净室：本会话未以任何形式接触 CurseForge `tacz-port` 制品（全程仅
  GitHub 官方仓库源码与 maven 元数据文本）。
- CHANGELOG 记入「未发布」段，标注**运行期未实机验证**。

## 5. 运行期验收 7 条（未执行，待实机）

1. 跨维度（如主世界→下界/末地）后立刻换弹：客户端动画与弹匣数一致（服务端
   `reloadStateType` 被重置，不再早退）；
2. BURST 档按住扳机：服务端打满连发（`CycleTaskHelper` 后续循环执行）；
3. `AutoReloadWhenRespawn=true`：死亡重生后背包内枪自动填弹；
4. 用枪射击钟：钟响（`BellBlock#attemptToRing` 触发，含摆动动画）；
5. `DestroyGlass=true`：子弹击碎普通玻璃/染色玻璃/铁栏杆（HAT 音色）;
6. 打开枪械工作台/改装台：底部原版 Hotbar 不再渲染；
7. 主手持枪左键点击方块：不产生破坏/攻击（服务端权威；客户端保持原拦截）。

## 6. 与 26.1.2 线 diff 的两处有意差异

1. `EventBusSubscriber` 不加 `bus = Bus.MOD`——FML 10.0 已无 `Bus` 类型
   （§3.1；26.1+ 同样无，但删除时点更早）；
2. `TravelToDimensionEvent#onEntityTravelToDimension` 的同维度过滤用
   `ResourceKey#equals`（`serverLevel.dimension().equals(event.getDimension())`），
   避免依赖同一注册表键的实例恒等；语义与 26.1.2 的 `==` 等价（两者现网均为
   同一 Registry 键对象）。

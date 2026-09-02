# 交接工单：静默失效事件处理器 —— 26.2 线与 1.21.11 线适配指引（2026-09-03）

> 发起：26.1.2 线（本工单随修复一并入库）。
> 26.1.2 线已完成修复 + 两轮全目录审核，全部证据见
> [`WIRE_DEAD_HANDLERS_2612_20260902.md`](WIRE_DEAD_HANDLERS_2612_20260902.md)
> （commit `e5828f0` + `c216a66`，CI compile-check / build / consistency 全绿，
> **运行期未实机验证**）。
>
> 本工单给 26.2 线与 1.21.11 线的 AI：**不要照抄 26.1.2 的 diff**。你们的分支
> 是独立移植线，缺陷集合未必相同；先跑 §3 的扫描确定自己线上的实际缺陷清单，
> 再按 §2 的逐项说明因地制宜，每个 API 都要按宪章 §3 在**你自己那条线的**
> NeoForge/FML/游戏源码里指认签名并留档。

## 1. 缺陷模式（为什么会整批静默失效）

上游 Forge 靠 `@Mod.EventBusSubscriber` + `@SubscribeEvent` 接线；移植时若只带走了
方法体、没带注解（或 Fabric 中转版把签名改成了回调风格），类就成了「编译通过、
永不执行」的死代码——没有报错、没有日志，只有玩家侧的功能缺失。此类缺陷
**逃过编译门与启动冒烟**，只能靠注册面清点抓出来。

## 2. 26.1.2 线确认的 8 个点（逐项：症状 → 修法 → 两线适配要点）

### 2.1 跨维度服务端枪械状态机不刷新（`event/TravelToDimensionEvent`）

- **症状**：跨维度后换弹，客户端演完整套动画、服务端因 `reloadStateType` 残留
  直接早退不加子弹（客户端刷新走 `RefreshClonePlayerDataEvent` 轮询，另一条路，
  所以只有服务端坏）。
- **26.1.2 修法**：玩家接 `PlayerEvent.PlayerChangedDimensionEvent`（仅服务端、
  传送完成后、同一 `ServerPlayer` 实例上触发）；非玩家生物接
  `EntityTravelToDimensionEvent` 做防御性重置，并过滤同维度传送
  （该事件是**传送前**、旧实体上触发的，javadoc 明示同维度也会触发）。
- **适配要点（两线通用）**：官方 1.20.1 对玩家也走 `EntityTravelToDimensionEvent`；
  1.21.2+ 传送重写后玩家仍是同实例物理移动、生物是复制（mixin `@Unique` 状态
  不进 NBT，新实体天然干净）。**验证点**：在你线的 NeoForge patch 里指认
  `ServerPlayer#teleport` 内 `firePlayerChangedDimensionEvent` 的触发位置；
  若你线（1.21.11）游戏版本 ≥1.21.2 则与 26.x 同构，直接用玩家事件。

### 2.2 BURST 连发服务端只打第一发（`event/ServerTickEvent`）★最严重、最易漏

- **症状**：`CycleTaskHelper.addCycleTask` 入队时立刻执行第一次，后续循环全靠
  `tick()`——没接线时**连发只出一发**、Lua `safeAsyncTask` 延迟任务永不执行。
  排查时极易误判为已接线：`SyncedEntityDataEvent` import 过**同名的** NeoForge
  `ServerTickEvent`，grep 类名会撞车，务必看全限定名。
- **26.1.2 修法**：接 `net.neoforged.neoforge.event.tick.ServerTickEvent.Post`。
- **适配要点**：官方 Forge 版没判 phase、每 tick 跑两次；任务是墙钟驱动的，
  接 Post 每 tick 一次即可。1.21.11 线确认 `event.tick.ServerTickEvent` 是否
  已存在（NeoForge 21.x tick 事件重构后的包位），不在就用你线等价物。

### 2.3 服务端配置解析器不跑（`event/LoadingConfigEvent`）

- **症状**：`HeadShotAABBConfigRead`（第三方生物爆头 AABB）与
  `InteractKeyConfigRead`（交互键黑白名单）永不解析——女仆等生物没有爆头判定、
  交互白名单全失效。
- **26.1.2 修法**：接 `ModConfigEvent.Loading` / `.Reloading`
  （`net.neoforged.fml.event.config`），按 `getConfig().getFileName()` 过滤
  `tacz-server.toml`。SERVER 配置登入时由 NeoForge 同步给客户端并在客户端触发
  `Reloading`，联机白名单因此生效。
- **适配要点（关键分叉）**：这是 **mod 总线**事件。26.1+ 的
  `@EventBusSubscriber` **没有 `bus` 参数**，`IModBusEvent` 子类自动路由；
  1.21.11 线必须先看你那个 FML 版本的 `EventBusSubscriber` 源码——若 `Bus` 枚举
  还在，就要显式 `bus = Bus.MOD`（官方 1.20.1 正是这么写的），否则事件一样收不到，
  等于白修。

### 2.4 重生自动装弹无反应（`event/PlayerRespawnEvent`）

- **症状**：配置 `AutoReloadWhenRespawn=true` 毫无反应。
- **26.1.2 修法**：接 `PlayerEvent.PlayerRespawnEvent`（NeoForge patch 证据：
  在 `PlayerList#respawn`、新玩家 `restoreFrom`+`initInventoryMenu` **之后**触发，
  `getEntity()` 即背包已恢复的新实例）；不过滤 `isEndConquered()`（对齐官方）；
  对 `api.getGunIndex()` 判 null（枪包未加载时官方实现会 NPE，防御住）。
- **适配要点**：背包遍历的访问器名随版本变——官方 1.20.1 是
  `getInventory().items`，26.x 是 `getInventory().getNonEquipmentItems()`，
  1.21.11 用你线 mappings 下的实际名字，别抄。

### 2.5 射钟不响 / 射玻璃不碎（`event/ammo/BellRing`、`DestroyGlassBlock`）

- **26.1.2 修法**：补 `@EventBusSubscriber` + `@SubscribeEvent`。
  `AmmoHitBlockEvent` 是 **tacz 自有事件**、发在游戏总线（仅服务端），两线的
  事件类本体应已随移植带过来，缺的只是订阅注解。
- **适配要点**：确认你线 `EntityKineticBullet#onHitBlock` 确实还在 post 该事件
  （发布点丢了的话光加注解也没用）；`BellBlock#attemptToRing` /
  `state.instrument()` 的签名按你线游戏源码指认。

### 2.6 工作台/改装台快捷栏穿模（`client/event/PreventsHotbarEvent`）

- **症状**：全屏工作台/改装台界面下原版 Hotbar 照常渲染。
- **26.1.2 修法**：在客户端分发器（本线是 `ClientGameEvents#onRenderGuiLayer`）
  对 `RenderGuiLayerEvent.Pre` + `VanillaGuiLayers.HOTBAR` 取消该层。
- **适配要点**：⚠️ 本线该类原注释声称「由 GuiMixin 转发」——**那个 Mixin 根本
  不存在**，是幻影调用点；你们线如有同款注释同样别信，grep 验证。
  `RenderGuiLayerEvent` 的回调参数随渲染纪元变（26.1 是 `GuiGraphicsExtractor`），
  但本修复只用 `getName()` + `setCanceled(true)`，两线应可平移；
  1.21.11 线确认 `VanillaGuiLayers` 存在（NeoForge 21.0+ 应有）。

### 2.7 持枪可挖方块（`event/PreventGunClick`，服务端兜底）

- **26.1.2 修法**：接 `PlayerInteractEvent.LeftClickBlock`，主手 `IGun` 则
  `setCanceled(true)`，与官方 1.20.1 实现逐行等价。客户端拦截
  （`ClientPreventGunClick`）是另一个类，别混淆——它可能早已接线。

### 2.8 两个「确认无需修」项（省得你们再查一遍）

- **`event/CommonLoadPack`**：全谱系遗留空壳（官方就是空的调用注释）。26.1.2 线
  服务端枪包加载由 `CommonAssetsManager`（`AddServerReloadListenersEvent`）接管。
  确认你线有对应的服务端资源加载路径后，注释声明「有意不接线」即可，勿删勿接。
- **`GunFinishReloadEvent`**：**官方 1.20.1 也从未 post 过**（只有 KubeJS wrapper），
  谱系性 API 空壳。按「不得声称未实现的东西」纪律**不要顺手补发射点**，留档即可。

## 3. 你必须自己跑的注册面扫描（勿信清单，信扫描）

在你的分支根目录执行（26.1.2 线原始命令，grep 语法通用）：

```bash
# 1) 事件目录逐类清点：注解 / 被已接线类调用 / addListener，三者至少居一
for f in src/main/java/com/tacz/guns/event/*.java \
         src/main/java/com/tacz/guns/event/ammo/*.java \
         src/main/java/com/tacz/guns/client/event/*.java; do
  cls=$(basename $f .java); ann=$(grep -c "EventBusSubscriber" $f)
  refs=$(grep -rln --include="*.java" "\b$cls\b" src | grep -v "$f" | tr '\n' ' ')
  echo "$cls | ann=$ann | refs: $refs"
done
# 2) 全仓 handler 孤儿 / 3) 自有事件发布点 / 4) 配置死链 / 5) 网络包注册：
#    命令全文见 26.1.2 线 docs/records/WIRE_DEAD_HANDLERS_2612_20260902.md §5.1
```

判读规则：`ann=0` 且 refs 为空（或只剩 javadoc 引用）= 死模块。注意两个假阳性
陷阱：同名 NeoForge 事件类撞 grep（§2.2）、javadoc 里的 `@SubscribeEvent` 字样
（26.1.2 线 `LrClientAssetsManager` 即此情况）。

## 4. 纪律与验收（两线通用）

1. **宪章 §3**：每个事件 API 在你线的 NeoForge/FML tag 里指认
   `类#方法(签名)` + 触发点 patch 行，证据写入你线 `docs/records/`；
   凭本工单或训练数据直接写 = 打回。26.1.2 的证据仓在
   `/tmp` 克隆法：`git clone --depth 1 --branch <你的线> --filter=blob:none
   https://github.com/neoforged/NeoForge.git`（26.2 线注意选对 beta tag）。
2. **洁净室**：照旧，禁碰 CurseForge `tacz-port` jar。
3. CHANGELOG 记入你线未发布段，标注「运行期未实机验证」；CI 编译门必须绿
   （26.1.2 线首推就红过一次：**加注解别忘了补 `GunMod` import**）。
4. 运行期验收 7 条（跨维度换弹 / BURST 打满 / 重生装弹 / 射钟 / 碎玻璃 /
   工作台藏 Hotbar / 持枪不可挖方块），全文见
   `WIRE_DEAD_HANDLERS_2612_20260902.md` §4。

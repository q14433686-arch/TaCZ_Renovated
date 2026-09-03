# 创造模式搜索栏搜不到物品修复（1.21.11 线，2026-09-03）

> 症状（维护者报告）：1.21.11 与 26.2 线在创造模式搜索栏输入任何关键词都搜不到物品，
> 仅 26.1.2 线正常。
>
> 修复落点：`com.tacz.guns.client.network.ClientPacketHandlers#onSyncGunPack`
> —— 在枪包同步重建创造栏之后，补一次搜索树（`SessionSearchTrees`）重建。

## 1. 根因

MC 1.21.x 起创造模式搜索栏不再由 `getHoverName()` 即时过滤，而是查
`ClientPacketListener#searchTrees()` 返回的 `SessionSearchTrees` 里那个异步构建的
`FullTextSearchTree`（证据见 §3 的 1.21.11 反编译源码）。

该搜索树的构建入口只有一个：`CreativeModeInventoryScreen#tryRebuildTabContents`
（屏幕私有方法）里的

```java
if (!CreativeModeTabs.tryRebuildTabContents(enabledFeatures, hasPermissions, holders)) {
    return false;
} else {
    if (searchTrees != null) {
        List<ItemStack> creativeSearchItems =
                List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        searchTrees.updateCreativeTooltips(holders, creativeSearchItems);
        searchTrees.updateCreativeTags(creativeSearchItems);
    }
    return true;
}
```

即：**只有**当 `CreativeModeTabs.tryRebuildTabContents(...)` 返回 `true`（静态
`CACHED_PARAMETERS` 为空或参数发生变化）时，搜索树才会跟着重建。该方法返回 `false`
时（参数与上次一致）直接 `return false`，搜索树被跳过。

本 mod 的 `onSyncGunPack` 在收到 `ServerMessageSyncGunPack`（`OnDatapackSyncEvent`
在进服/建世界阶段发出）后调用了**两次**静态
`CreativeModeTabs.tryRebuildTabContents(...)`（先 `!hasPermissions` 再 `hasPermissions`
翻转一次，目的见下）。这两次调用：

- 只重建了各标签页的**展示列表**（含 `CreativeModeTabs.searchTab()` 的
  `getDisplayItems()`），**没有**调用 `SessionSearchTrees#updateCreativeTooltips /
  updateCreativeTags`；
- 同时把静态 `CACHED_PARAMETERS` 钉成了与屏幕后续相同的
  `(enabledFeatures, hasPermissions, registryAccess)`。

于是玩家随后打开创造模式背包时，`CreativeModeInventoryScreen` 构造器里的
`tryRebuildTabContents` 因为参数未变而返回 `false`，**跳过搜索树重建**；`containerTick`
每 tick 的 `tryRefreshInvalidatedTabs` 同理。搜索树始终停留在同步前那个
`SearchTree.empty()` 初始值上 → 输入任何关键词 `tree.search(...)` 都返回空 →
「搜索栏搜不到任何物品」。

26.1.2 与 1.21.11/26.2 的差别不在本 mod 的 `onSyncGunPack` 逻辑（三线的
`onSyncGunPack` 方法体一致；26.2 仅有一处无关的 `gui.screen()` API 适配差异），
`ModCreativeTabs`/item/creative/index 代码也一致，而在于 MC 版本的进服时序：
`minecraft.player` 为空的窗口期是否落在 `OnDatapackSyncEvent` 之前。若同步到达时
`minecraft.player` 仍为 null，本方法里的 `if (minecraft.player != null)` 会跳过
`tryRebuildTabContents`，`CACHED_PARAMETERS` 保持 null，屏幕打开时自然会重建搜索树
（正常）；若同步到达时 `minecraft.player` 已非 null（26.2/1.21.11 的时序），
`tryRebuildTabContents` 被执行、`CACHED_PARAMETERS` 被钉住、搜索树被永久跳过（故障）。
修复让两种情况都显式重建搜索树，与版本时序解耦。

## 2. 修复

在 `onSyncGunPack` 的两次 `CreativeModeTabs.tryRebuildTabContents(...)` 之后，镜像原版
`CreativeModeInventoryScreen#tryRebuildTabContents` 的搜索树段：

```java
net.minecraft.client.multiplayer.SessionSearchTrees searchTrees =
        minecraft.getConnection().searchTrees();
List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
searchTrees.updateCreativeTooltips(minecraft.level.registryAccess(), searchItems);
searchTrees.updateCreativeTags(searchItems);
```

说明：

- 两次 `tryRebuildTabContents` 已把 `searchTab().getDisplayItems()` 重建为含本 mod
  （枪/弹/配件/工作台/弹药盒）初始化后的堆栈列表，`List.copyOf` 做不可变快照后交给
  `updateCreativeTooltips` 内部 `Util.backgroundExecutor()` 上的异步构建，线程安全；
- `updateCreativeTooltips` 只注册并派生一个 `CompletableFuture`，同步段不抛异常、不访问
  客户端资源，主线程调用安全；
- 该处 `Item.TooltipContext` 由 `Item.TooltipContext.of(registries)` 生成、flag 为
  `TooltipFlag.Default.NORMAL.asCreative()`（`isAdvanced()==false`），因此本 mod 的
  `TooltipEvent#onTooltip`（`isAdvanced()` 门禁）不会在该异步构建中触发，无额外副作用。

## 3. API 证据（1.21.11 反编译源码，Mojang mappings）

- `net.minecraft.client.multiplayer.ClientPacketListener`
  `public SessionSearchTrees searchTrees()`
- `net.minecraft.client.multiplayer.SessionSearchTrees`
  - `public void updateCreativeTooltips(final HolderLookup.Provider registries, final List<ItemStack> itemStacks)`
    —— 内部 `register(CREATIVE_NAMES, ...)`，用 `TooltipFlag.Default.NORMAL.asCreative()`
    + `Util.backgroundExecutor()` 构建 `FullTextSearchTree`（`CompletableFuture.supplyAsync`），
    并 `previous.cancel(true)` 取消上一份构建；
  - `public void updateCreativeTags(final List<ItemStack> items)`
    —— 同理构建 `IdSearchTree`（`itemStack.getTags().map(TagKey::location)`）；
  - `public SearchTree<ItemStack> creativeNameSearch()` → `this.creativeByNameSearch.join()`。
- `net.minecraft.world.item.CreativeModeTabs`
  - `public static CreativeModeTab searchTab()`
  - `public static boolean tryRebuildTabContents(FeatureFlagSet, boolean, HolderLookup.Provider)`
    —— `CACHED_PARAMETERS.needsUpdate(...)` 为 false 时直接 `return false` 跳过重建。
- `net.minecraft.world.item.CreativeModeTab#buildContents(ItemDisplayParameters)`
  逐 tab 调 `displayItemsGenerator.accept(...)` 填充 `displayItems` /
  `displayItemsSearchTab`；`getDisplayItems()` 返回 `displayItems`。
- `net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters#needsUpdate`
  `!enabledFeatures.equals(...) || hasPermissions != ... || holders != holders`（引用比较）。
- `net.minecraft.core.RegistryAccess extends HolderLookup.Provider`（
  `Level#registryAccess()` 返回该接口实现，可直接作 `updateCreativeTooltips` 首参）。
- `net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen`
  构造器（`tryRebuildTabContents(player.connection.searchTrees(), ...)`）与
  `containerTick()`（每 tick `tryRefreshInvalidatedTabs(...)`）为仅有的
  `updateCreativeTooltips` 调用方（全源码 `grep updateCreativeTooltips` 仅命中
  `SessionSearchTrees` 与 `CreativeModeInventoryScreen` 两个文件）。

## 4. 验证状态

- 编译门：CI `compile-check.yml`（本地沙箱无 JDK 且 maven.neoforged.net 对沙箱网络
  不可达，无法本地编译）。
- **运行期未实机验证**：需在 1.21.11 单机创造 + 专服各验证一次——
  ① 进服后打开创造搜索栏，输入枪名/子弹名/配件名/`tacz:` 前缀能出结果；
  ② 空关键词时搜索页仍列出全部物品；③ `/reload`（服务端）后搜索仍正常。

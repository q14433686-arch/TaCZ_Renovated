# 创造模式搜索栏搜不到物品——防御性修复记录（26.1.2 线，2026-09-03）

> 性质：**时序隐患的防御性补齐**。症状（创造模式搜索栏输入任何关键词都搜不到物品）由维护者
> 在 26.2 与 1.21.11 线实机报告，**26.1.2 线当前未复现**；但三线 `onSyncGunPack` 方法体一致，
> 代码缺陷完全相同，只是被进服时序差异掩盖。本记录与 1.21.11 线
> `docs/records/CREATIVE_SEARCH_SYNC_FIX_20260903.md`（PR #41）同源，但**不沿用其「已修复症状」
> 的结论**——本线是「补上一个目前恰好没有被踩到的坑」。
>
> 修复落点：`com.tacz.guns.client.network.ClientPacketHandlers#onSyncGunPack`。
> 编译门走 CI（本地沙箱无 JDK、maven.neoforged.net 不可达），**运行期未实机验证**。

## 1. 根因

MC 1.21.x 起创造模式搜索栏不再由 `getHoverName()` 即时过滤，而是查
`ClientPacketListener#searchTrees()` 返回的 `SessionSearchTrees` 里那个异步构建的
`FullTextSearchTree`。该搜索树的唯一构建入口是屏幕私有方法
`CreativeModeInventoryScreen#tryRebuildTabContents`：

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

即**只有**静态 `CreativeModeTabs.tryRebuildTabContents(...)` 返回 `true`（`CACHED_PARAMETERS`
为空或参数变化）时，搜索树才会跟着重建。

本 mod 的 `onSyncGunPack` 收到 `ServerMessageSyncGunPack` 后，在
`if (level != null && connection != null && player != null)` 块内调用两次静态
`tryRebuildTabContents(...)`（`!hasPermissions` → `hasPermissions` 翻转一次，用于让创造栏
拿到带枪包 id / 动态模型的初始化堆栈）。这两次调用：

1. 只重建了各标签页的**展示列表**，**没有**调用 `SessionSearchTrees#updateCreativeTooltips /
   updateCreativeTags`；
2. 把 vanilla 静态 `CACHED_PARAMETERS` 钉成与屏幕后续相同的 `(enabledFeatures, hasPermissions,
   registryAccess)`。

于是玩家打开创造背包时，原版屏幕内 `tryRebuildTabContents` 因「参数未变」返回 `false`，
跳过搜索树重建；`containerTick` 每 tick 的 `tryRefreshInvalidatedTabs` 同理。搜索树停留在
`SearchTree.empty()` 初始值上 → 任何关键词都无结果。

### 1.1 为什么 26.1.2 线目前没复现（推断，未实机验证）

三线差别不在 mod 代码，而在**进服时序**：

- 26.2 / 1.21.11：同步到达时 `minecraft.player` 已非 null → if 块执行 → `CACHED_PARAMETERS`
  被钉住 → 屏幕跳过搜索树重建（**故障**）。
- 26.1.2：同步到达更早、`player`/`level` 尚未就绪 → if 块整体跳过 → `CACHED_PARAMETERS`
  保持 null → 屏幕首次打开自然重建（**正常**）。

另一个可能的掩盖因素：即使 if 块执行，也只有钉住的 `player.isCreative()` 与屏幕后来问的
`player.canUseGameMasterBlocks() && operatorItemsTab` 恰好相等时才会跳过重建，这同样随
时序漂移。**两条路径都是巧合而非保证**——任何 NeoForge 26.1.x 小版本、任何改变登入包顺序
的第三方 mod、甚至专服与单机之间的差异，都可能让 26.1.2 线掉进与 26.2 相同的坑。故本线
选择在同一位置显式补建搜索树，把「依赖时序巧合」变成「确定性正确」。

## 2. 修复

在 `onSyncGunPack` 的两次 `CreativeModeTabs.tryRebuildTabContents(...)` 之后、if 块结束之前，
镜像原版屏幕内同款逻辑：

```java
net.minecraft.client.multiplayer.SessionSearchTrees searchTrees = minecraft.getConnection().searchTrees();
List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
searchTrees.updateCreativeTooltips(minecraft.level.registryAccess(), searchItems);
searchTrees.updateCreativeTags(searchItems);
```

补齐 import：`net.minecraft.world.item.ItemStack`、`java.util.List`。仅改这一个源文件。

说明：

- **惰性**：补丁位于既有 if 块内。26.1.2 线若 if 块整体被跳过（§1.1 的正常路径），补丁一行
  都不执行，行为与修复前完全一致；若 if 块执行（掉坑路径），补丁恰好把缺的搜索树补上。
  因此不存在「修复引入回归」的路径，最坏情况是多做一次与屏幕打开时等价的搜索树构建。
- `List.copyOf` 做不可变快照后交给 `updateCreativeTooltips` 内部 `Util.backgroundExecutor()`
  上的 `CompletableFuture.supplyAsync` 构建，线程安全；同步段只注册 future 并 `cancel(true)`
  上一份构建，不抛异常、不访问客户端资源，主线程调用安全。
- 该处 `Item.TooltipContext.of(registries)` + `TooltipFlag.Default.NORMAL.asCreative()`
  （`isAdvanced()==false`），本 mod 带 `isAdvanced()` 门禁的 `TooltipEvent#onTooltip` 不会在
  异步构建里触发，无额外副作用。

## 3. API 证据（已对照 1.21.11 / 26.1.2 / 26.2 反编译源码，Mojang mappings，三版签名一致）

| API | 说明 |
|---|---|
| `net.minecraft.client.multiplayer.ClientPacketListener#searchTrees()` → `SessionSearchTrees` | 会话级搜索树容器 |
| `SessionSearchTrees#updateCreativeTooltips(HolderLookup.Provider, List<ItemStack>)` | `register(CREATIVE_NAMES, ...)`，`Util.backgroundExecutor()` 上异步构建 `FullTextSearchTree`，`previous.cancel(true)` |
| `SessionSearchTrees#updateCreativeTags(List<ItemStack>)` | 同理构建 `IdSearchTree`（`itemStack.getTags().map(TagKey::location)`） |
| `SessionSearchTrees#creativeNameSearch()` | `creativeByNameSearch.join()`，屏幕 `refreshSearchResults` 的查询入口 |
| `net.minecraft.world.item.CreativeModeTabs#searchTab()` | 返回 SEARCH 标签页，`getDisplayItems()` 即搜索页全量列表 |
| `CreativeModeTabs#tryRebuildTabContents(FeatureFlagSet, boolean, HolderLookup.Provider)` | `CACHED_PARAMETERS.needsUpdate(...)` 为 false 时直接 `return false` |
| `CreativeModeTab.ItemDisplayParameters#needsUpdate` | `!enabledFeatures.equals(...) \|\| hasPermissions != ... \|\| holders != holders`（引用比较） |
| `net.minecraft.core.RegistryAccess extends HolderLookup.Provider` | `Level#registryAccess()` 可直接作 `updateCreativeTooltips` 首参 |
| `CreativeModeInventoryScreen` 构造器 / `containerTick()` | 全源码中仅有的两个 `updateCreativeTooltips` 调用方；本修复即镜像该段 |

26.1.2 与 1.21.11 在 `SessionSearchTrees` / `CreativeModeInventoryScreen#tryRebuildTabContents` /
`CreativeModeTabs#tryRebuildTabContents` / `ItemDisplayParameters#needsUpdate` 上功能逐行相同
（仅 `tags()/getTags()`、`typeHolder()/getItemHolder()` 一类改名与 decompiler 泛型显示差异）。

## 4. 验收清单（需实机）

- [ ] ① 创造单机 + 专服各一次：搜索栏输入枪名 / 子弹名 / 配件名 / `tacz:` 前缀能出结果；
- [ ] ② 空关键词时搜索页仍列出全部物品；
- [ ] ③ `/reload`（服务端）后搜索仍正常；
- [ ] ④ **26.1.2 线重点**：修复前后行为无回归——本线 if 块可能整体跳过、补丁应为惰性；
  若要一锤定音，可在 `onSyncGunPack` 开头临时打印 `player == null` / `level == null` /
  `player.isCreative()` 及两次 `tryRebuildTabContents` 的返回值，对照 26.2 / 1.21.11 日志确认
  §1.1 的时序推断。

## 5. 关联

- 1.21.11 线同源修复：PR #41，`docs/records/CREATIVE_SEARCH_SYNC_FIX_20260903.md`（该线为症状修复）。
- 26.2 线：同为时序隐患的防御性补齐（另开 PR）。

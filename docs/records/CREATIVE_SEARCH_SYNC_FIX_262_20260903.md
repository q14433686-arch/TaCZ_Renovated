# 记录：创造模式搜索栏搜不到任何物品（26.2 线，2026-09-03）

- **分支**：`arena/01a066bc-tacz-renovated`（基线 `279426c`，NeoForge 26.2.0.64 / MC 26.2 / Java 25）。
- **触发**：项目发起人报障「创造模式搜索栏搜不到物品（至少 tacz 枪/弹药/配件/工作台与
  lrtactical 物品应可搜到）」；同时给出姊妹线（`TaCZ_Refabricated_Unofficial` 的
  `1.21.11` 分支，会话分支 `arena/01a066bd-tacz-renovated`）Agent 的独立诊断与修法，
  **该修法已由发起人在 1.21.11 线实机测试 PASS**。本页为本线（26.2 / NeoForge）的
  独立推导、26.2 逐 API 指认与落地差异。
- **缺陷形态**：可编译、启动无报错、创造栏各页物品齐全，但搜索页任何关键词都返回空
  （无异常、无日志）。属「静默失效」类。
- **证据级别**：静态闭环（NeoForge 26.2.x 官方 patch / 源码逐 API 指认 + 同世代在役
  Mod 交叉印证）+ **CI 编译门已绿**（见 §7 首项）；**本线运行期未实机验证，不宣称已修**。
  同根因同修法在 1.21.11 线由发起人实机 PASS（见 §6），作为跨线旁证而非本线验收。

## 1. 症状与判定

| 观察 | 结论 |
|---|---|
| 创造栏 tacz / lrtactical 各页内容正常 | 标签页 `displayItems` 构建链路完好，不是 tab 生成器问题 |
| 搜索页任何关键词（含原版物品）均无结果 | 不是「tacz 物品名不可翻译」这类内容问题，而是**索引整体为空** |
| 无崩溃、`latest.log` 无相关异常 | 排除后台 tooltip 抛异常路径（该路径会在 `.join()` 处抛出并崩在 `charTyped`，见 Ars Nouveau 先例 §5.3） |

「整体为空且不报错」这一点由 NeoForge 26.2 的实现解释：
`CreativeModeTabSearchRegistry#getNameSearchTree(key)` / `getTagSearchTree(key)` 在 key
从未被写入时返回 `DEFAULT_SEARCH = CompletableFuture.completedFuture(SearchTree.empty())`
——即**空树是静默兜底值**，不是异常。

## 2. 26.2 搜索链路（逐环证据）

1. **索引存放处（NeoForge 26.2 改为静态表）**
   `net.neoforged.neoforge.client.CreativeModeTabSearchRegistry`（NeoForge 26.2.x
   `src/client/java/.../CreativeModeTabSearchRegistry.java`，全文已核）：
   `NAME_SEARCH_TREES` / `TAG_SEARCH_TREES` 两张 `IdentityHashMap<Key, CompletableFuture<SearchTree<ItemStack>>>`，
   `getNameSearchKey(tab)` 对全局搜索页返回 `SessionSearchTrees.CREATIVE_NAMES`、
   对无搜索栏的页返回 `null`、其余按页懒建 key。

2. **查询处**
   `CreativeModeInventoryScreen#refreshSearchResults()`（NeoForge 26.2.x
   `patches/net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen.java.patch`
   hunk `@@ -455,10 +_,10 @@`）：
   `tree = searchTrees.creativeNameSearch(CreativeModeTabSearchRegistry.getNameSearchKey(selectedTab))`，
   而 `SessionSearchTrees#creativeNameSearch(Key)` = `CreativeModeTabSearchRegistry.getNameSearchTree(key).join()`
   （`patches/net/minecraft/client/multiplayer/SessionSearchTrees.java.patch`）。
   ⇒ **表里没有 key，搜索就恒为空。**

3. **唯一写入处**
   `SessionSearchTrees#updateCreativeTooltips(HolderLookup.Provider, List<ItemStack>[, Key])`
   与 `#updateCreativeTags(List<ItemStack>[, Key])`（同上 patch，四个方法均为 public，
   两参版委托给三参版并使用 `CREATIVE_NAMES` / `CREATIVE_TAGS`）；内部
   `register(key, () -> { ...putNameSearchTree(key, supplyAsync(new FullTextSearchTree<>(...), Util.backgroundExecutor())); previous.cancel(true); })`。

4. **谁调用写入**
   - `CreativeModeInventoryScreen#tryRebuildTabContents(SessionSearchTrees, FeatureFlagSet, boolean, HolderLookup.Provider)`
     ——NeoForge 26.2 把原版两行换成按页循环（patch hunk `@@ -153,9 +_,11 @@`）：
     ```java
     if (searchTrees != null) {
         CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar).forEach(tab -> {
             List<ItemStack> list = List.copyOf(tab.getDisplayItems());
             searchTrees.updateCreativeTooltips(holders, list, CreativeModeTabSearchRegistry.getNameSearchKey(tab));
             searchTrees.updateCreativeTags(list, CreativeModeTabSearchRegistry.getTagSearchKey(tab));
         });
     }
     return true;
     ```
   - `ClientPacketListener#handleUpdateTags`（patch hunk `@@ -1820,8 +_,11 @@`）：原版
     `this.searchTrees.updateCreativeTags(List.copyOf(CreativeModeTabs.searchTab().getDisplayItems()))`，
     NeoForge 改为同样的按页循环——**只重建 tag 树，不重建 name 树**。
     （这条同时证明 `ClientPacketListener` 在 26.2 持有 `searchTrees` 字段，见 §4 AT 依据。）

5. **写入被跳过的条件（关键一环，推定 + 旁证）**
   同一 hunk 的几何形状给出：153 行是某块的 `}`、155–159 是 `if (searchTrees != null) {...}`、
   161 行是**无条件** `return true;`。方法返回 `boolean`（Kilt 1.21.1
   `CreativeModeInventoryScreenInject` 对同名方法用 `CallbackInfoReturnable<Boolean>` 注入），
   故唯一的 `false` 出口必在 153 行闭合的那个块里，即索引步骤**位于提前返回之后**：
   ```java
   if (!CreativeModeTabs.tryRebuildTabContents(enabledFeatures, hasPermissions, holders)) {
       return false;               // ← 参数被记忆化命中 ⇒ 索引步骤整段跳过
   }
   ```
   旁证：Cobblemon GitLab work item #960 的 1.21 崩溃栈
   `CreativeModeInventoryScreen.tryRebuildTabContents(:148)` → `CreativeModeTabs.tryRebuildTabContents(:2177)`
   → `buildAllTabContents(:2166)` → `CreativeModeTab.buildContents(:145)`，说明该方法开头即调
   `CreativeModeTabs.tryRebuildTabContents`。
   `CreativeModeTabs.tryRebuildTabContents` 的记忆化在 26.2 的确切形状由
   ViaFabricPlus `ver/26.2` `MixinCreativeModeTabs` 给出：
   `@Shadow @Nullable private static CreativeModeTab.@Nullable ItemDisplayParameters CACHED_PARAMETERS;`
   + `@Shadow private static void buildAllTabContents(ItemDisplayParameters)`，
   参数相同即不重建（VFP 因此直接改 `CACHED_PARAMETERS` 强行重建）。
   **本环无 26.2 原版源码可逐字引用（无公开 vanilla 26.2 源镜像），标注为推定；
   §6 的跨线实机 PASS 覆盖了这一环。**

## 3. 本仓根因

`ClientPacketHandlers#onSyncGunPack`（枪包缓存同步，`context.enqueueWork` ⇒ 客户端主线程）
在重建标签内容后，把 `CACHED_PARAMETERS` 钉在
`(connection.enabledFeatures(), player.isCreative(), level.registryAccess())`：

- 玩家开创造界面时，界面用**它自己的**参数调 `CreativeModeTabs.tryRebuildTabContents`；
- 若该参数与上面钉住的相等 ⇒ 记忆化命中 ⇒ 界面方法提前 `return false` ⇒
  **`updateCreativeTooltips` / `updateCreativeTags` 一次都没跑** ⇒
  `NAME_SEARCH_TREES` / `TAG_SEARCH_TREES` 里没有 `CREATIVE_NAMES` / `CREATIVE_TAGS` ⇒
  查询落到 `SearchTree.empty()` ⇒ 任何关键词都搜不到，且无任何报错。
- 两次调用（`!hasPermissions` 再 `hasPermissions`）本身是为了绕过记忆化强制重建内容，
  这一步是有效的（所以标签页内容正常）；问题在于**重建内容 ≠ 重建索引**，而索引唯一
  的写入点恰好被这次记忆化命中挡掉了。

附带缺陷（同一处、同一误解）：`hasPermissions` 取 `player.isCreative()`，注释断言
「Creative mode is the same gate used by the client tab screen」。26.2 并非如此：
界面侧的门是 `Player#canUseGameMasterBlocks() && Options#operatorItemsTab()`。
`canUseGameMasterBlocks()` 自 1.21.11 起读权限集而非 `Abilities#instabuild`
（旁证：Carpet-Igny-Addition `PlayerMixin` 的 `//#if MC >= 12111` 分支把 wrap 目标从
`Abilities;instabuild` 换成 `PermissionSet;hasPermission`）。两者不等价 ⇒
「创造但非 op」的玩家会拿到按 op 门构建的操作员页内容（且因记忆化命中，界面不会自行纠正）。

## 4. 修法

`src/main/java/com/tacz/guns/client/network/ClientPacketHandlers.java`：

1. `hasPermissions` 改为界面同款表达式 `player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get()`
   ⇒ 本处构建的内容与界面将构建的内容一致，记忆化命中也不再产生错误状态。
2. 两次 `tryRebuildTabContents` 之后，新增 `refreshCreativeSearchTrees(connection, level)`，
   **逐字镜像 NeoForge 26.2 界面方法里的索引循环**（按页 + 按 key），把刚重建出来的
   内容喂进 `SessionSearchTrees`。这一步不依赖记忆化是否命中，也不依赖玩家之后是否重开界面。
3. 保留原有「翻一次再翻回来」的双调用：它仍是强制内容重建的手段，且第二遍是搜索页
   聚合到其他页新内容的前提（搜索页 generator 聚合各页 `getSearchTabDisplayItems()`，
   见 polymer `dev/26.2` `CreativeModeTabsMixin` 对 `lambda$bootstrap$16` 的注入点）。

`src/main/resources/META-INF/accesstransformer.cfg`：新增
`public net.minecraft.client.multiplayer.ClientPacketListener searchTrees`。
**未走「猜测 public 访问器」路线**：`searchTrees()` 访问器在 1.21.11 由姊妹线核对过，
但本线只拿到字段存在的直接证据（§2.4 的原版被删行 `this.searchTrees.updateCreativeTags(...)`），
故按本仓既有做法（该 cfg 已有 4 条同类条目，且 `Minecraft#startUseItem`、
`MultiPlayerGameMode#ensureHasSentCarriedItem`、`LivingEntity#jumping`、
`Player#canCriticalAttack` 均由此在役）用 AT 打开字段读取，编译期确定性最高。
客户端类的 AT 与专服共存已有先例（同上两条 client-only 条目 + `docs/records/SERVER_TEST_20260821_DEDICATED*.md`）。

### 4.1 26.2 逐 API 指认

| 用到的 API | 26.2 证据 |
|---|---|
| `ClientPacketListener#searchTrees`（字段，AT 打开） | NeoForge 26.2.x `patches/net/minecraft/client/multiplayer/ClientPacketListener.java.patch` hunk `@@ -1820,8 +_,11 @@` 的原版被删行 `this.searchTrees.updateCreativeTags(searchItems);` |
| `SessionSearchTrees#updateCreativeTooltips(HolderLookup.Provider, List<ItemStack>, SessionSearchTrees.Key)` | NeoForge 26.2.x `patches/.../SessionSearchTrees.java.patch`（public，NeoForge 新增三参重载） |
| `SessionSearchTrees#updateCreativeTags(List<ItemStack>, SessionSearchTrees.Key)` | 同上 |
| `SessionSearchTrees#CREATIVE_NAMES` / `CREATIVE_TAGS` | 同上（`public static final Key`），仅 javadoc 引用 |
| `CreativeModeTabs#allTabs()` | NeoForge 26.2.x 界面 patch hunk `@@ -153,9 +_,11 @@` 与 `ClientPacketListener` patch 同一 hunk，两处官方代码均 `CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar)` |
| `CreativeModeTab#hasSearchBar()` / `#getDisplayItems()` | 同上两处官方代码 |
| `CreativeModeTabSearchRegistry#getNameSearchKey(CreativeModeTab)` / `#getTagSearchKey(CreativeModeTab)` | NeoForge 26.2.x `src/client/java/net/neoforged/neoforge/client/CreativeModeTabSearchRegistry.java`（全文已核，public static，搜索页返回 `CREATIVE_NAMES`/`CREATIVE_TAGS`） |
| `Player#canUseGameMasterBlocks()`（客户端可调） | NeoForge 26.2.x `src/main/java/net/neoforged/neoforge/event/entity/player/BreakBlockEvent.java` javadoc `{@link Player#canUseGameMasterBlocks()}`；KubeJS 分支 `2601` `KubeJSClient`：`mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get()` 作为 `tryRebuildTabContents` 第二参 |
| `Options#operatorItemsTab()`（返回带 `.get()` 的选项） | JEI 分支 `26.2` `ItemStackListFactory`：`minecraft.options.operatorItemsTab().get()`；KubeJS `2601` 同 |
| `Level#registryAccess()` 作 `HolderLookup.Provider` | 本仓既有代码已把 `minecraft.level.registryAccess()` 传给 `tryRebuildTabContents(FeatureFlagSet, boolean, HolderLookup.Provider)`（CI 编译门在役）；`RegistryAccess extends HolderLookup.Provider` |
| `ClientLevel`（`Minecraft#level` 的静态类型） | polytone `1.21.1` `PlatStuffImpl#hackyGetRegistryAccess`：`ClientLevel level = Minecraft.getInstance().level;` |
| `CreativeModeTabs#tryRebuildTabContents(FeatureFlagSet, boolean, HolderLookup.Provider)`（沿用，未改签名） | 本仓既有调用；ViaFabricPlus `ver/26.2` `MixinCreativeModeTabs` HEAD 注入同签名并 `cir.setReturnValue(true)` |

## 5. 本轮排除的假设（避免重复劳动）

1. **`accept` vs `acceptAll`**：Fabric 权威线（`TaCZ_Refabricated_Unofficial`
   分支 `26.2(main)`）`ModCreativeTabs` 用 `output.acceptAll(...)`，本线用
   `forEach(output::accept)`。26.2 的 `CreativeModeTab.Output` 默认可见性即
   `PARENT_AND_SEARCH_TABS`（Fabric API 26.2 `FabricCreativeModeTabOutput`：同时写入
   displayStacks 与 searchTabStacks，并按 `isEnabled(stack)` 过滤特性旗标），
   NeoForge 自家 `Builder.displayItems(Collection)` 也用 `forEach(o::accept)` ⇒ 两者搜索等价，**非本 bug**。
2. **`TabVisibility` 误用**：本仓无任何可见性参数调用。
3. **后台线程 tooltip 崩/挂**（Ars Nouveau `glfwGetKey` 先例：异常在 `.join()` 抛出、
   崩在 `charTyped`）：`GunTooltip` 为纯数据持有者；`ClientGunTooltip`（内含
   `Minecraft.getInstance().font`）与 `ClientAttachmentItemTooltip`（内含 GLFW 按键查询）
   经 `RegisterClientTooltipComponentFactoriesEvent` 注册，仅在渲染期实例化，不在索引路径；
   各 item 的 `appendHoverText` 区段无 `Minecraft.getInstance()` / `InputConstants` / `RenderSystem`。
   且该故障形态是崩溃而非静默空结果 ⇒ 与症状不符，排除。
4. **`LanguageMixin`**：整个文件的注入全被注释，与语言/名称解析无关（死路）。
5. **`latest.log`**：只有枪包数据侧 WARN（部分配件/allow-tags 解析失败、空配料配方被跳过），
   无创造搜索异常，也无任何创造/搜索使用记录 ⇒ 不构成正反证据。
6. **`TooltipEvent#onTooltip`（`ClientGameEvents`）**：只在 advanced tooltip 下动作，
   索引用的是 `TooltipFlag.Default.NORMAL.asCreative()` ⇒ 不在路径上。

## 6. 与姊妹线（1.21.11）的关系

- 姊妹线 Agent 独立得出同一根因（记忆化命中 ⇒ 界面跳过 `SessionSearchTrees` 重建 ⇒
  树停在初始空值），修法为在两次 `tryRebuildTabContents` 后调用
  `updateCreativeTooltips` / `updateCreativeTags`；**发起人已在 1.21.11 线实机 PASS**。
- 本线差异（有意，均有 26.2 证据）：
  1. 用 NeoForge 26.2 的**三参按页重载 + `CreativeModeTabSearchRegistry` key**，
     而非原版两参调用——26.2 的查询侧是按 `selectedTab` 的 key 取树的
     （§2.2），只喂 `CREATIVE_NAMES`/`CREATIVE_TAGS` 会漏掉任何自带搜索栏的第三方页；
     全局搜索页仍映射到同两个 key，故对本报障症状是超集。
  2. 顺带修正 `hasPermissions` 的取值门（§3 附带缺陷）。1.21.11 线的同名参数若仍用
     `isCreative()`，同样与界面门不一致，建议姊妹线一并核对（不属本分支权限）。
- **机制归属判定**：缺陷位于本移植线自有的 `onSyncGunPack` 重建块（Fabric 权威线
  `client/` 下无 `network/` 目录、亦无该重建块），不是上游官方 1.20.1 时代的陈年机制
  问题 ⇒ 属「应修 + 应记 CHANGELOG」，不适用「不适用」豁免。
  cleanroom 纪律：全程未接触、未反编译 CurseForge `tacz-port` jar；
  对官方 1.20.1 行为不作任何断言。

## 7. 未验证项与验收清单（本线运行期）

沙箱无 JDK 且无网络（`~/.gradle` 为空、`maven.neoforged.net` 不可达），
**编译门只能由 CI 承担**；运行期需实机：

- [x] **CI 编译门绿**（2026-09-03，PR #43，代码提交 `39a5b12`）：`compile-check` 1m43s、
      `build` 1m44s、`consistency` 8s 三个 workflow 均 success。CI 回推的
      `build-reports/compile-java.log`（头部 `commit: 39a5b12f4d4e54dd749675e372b0e8615e69676f`）
      末行 `BUILD SUCCESSFUL in 1m 14s`，无 error、无与本改动相关的 warning（仅既有的
      binarypatcher artifact manifest 与 Gradle 弃用提示）⇒ ①新增 AT 条目
      `public net.minecraft.client.multiplayer.ClientPacketListener searchTrees` 已被
      ModDevGradle 自动检测并应用到编译类路径；②§4.1 表内全部 26.2 API 签名编译通过。
      其后 CI 自推的 `ad3c652 ci-log: compile result (success) for 39a5b12f…` 只改该日志文件，
      其 head 上三个 run 显示 `action_required`（审批门，非失败）。
- [ ] 下列各项仍需实机（本线运行期未验证）：
- [ ] 单机（创造 + 开作弊）：进世界后**第一次**开创造界面即切到搜索页，
      输 `tacz` 枪名关键词（如某枪包内的枪 id 片段）、`ammo`、配件名、`workbench`
      均有结果；`lrtactical` 投掷物可搜到。
- [ ] `#` 前缀标签搜索（如 `#tacz`）有结果（tag 树同样被重建）。
- [ ] 单机（创造、**关**作弊/非 op）：同上可搜；且操作员页内容不出现（门修正的反向验证）。
- [ ] 选项「操作员物品页」开/关两态各测一次（两态下 `hasPermissions` 与界面一致）。
- [ ] 专服 + 客户端：登录后搜索同样可用（同步包在专服链路上到达）。
- [ ] 搜索期无 `Rendersystem called from wrong thread` / `CompletionException`（§5.3 的回归哨兵）。
- [ ] `/reload` 或枪包热重载后再次搜索仍可用（二次同步路径）。
- [ ] 已知残留：若**创造界面已打开时**才收到同步包，索引要等下次重开界面才刷新
      （与原版对动态内容的行为同形，本次未额外处理）。

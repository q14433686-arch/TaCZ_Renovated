# 工作包④ 证据清单

语义：Fabric 26.1.2（`/tmp/tacz-fabric-2612`）非渲染游戏逻辑。
加载器：NeoForge 26.1.2.97（MUKSC 习语 × 26.1 API，禁止 1.21.1 游戏签名）。

## 加载器 / 游戏 API（① client.jar / patched 26.1.2.97 + ② NF sources / loader-11.0.15）

| 调用 | 证据 |
|---|---|
| `AddPackFindersEvent` 实现 `IModBusEvent`，`#addRepositorySource` | ② `AddPackFindersEvent.java` |
| `AddServerReloadListenersEvent#addListener(Identifier, PreparableReloadListener)` | ② `SortedReloadListenerEvent.java` |
| `IEntityWithComplexSpawn#writeSpawnData/readSpawnData(RegistryFriendlyByteBuf)` | ② `IEntityWithComplexSpawn.java`（26.1 取代 `IEntityAdditionalSpawnData`） |
| `FMLEnvironment#getDist()` 静态 | ② loader-11.0.15 javap |
| `FMLPaths.GAMEDIR#get()` | ② 同上 |
| `Pack.readMetaAndCreate(PackLocationInfo, ResourcesSupplier, PackType, PackSelectionConfig)` | ① |
| `PackMetadataSection(Component, InclusiveRange<PackFormat>)` | ① |
| `WorldVersion#packVersion(PackType)` → `PackFormat` | ① |
| `PathPackResources.PathResourcesSupplier(Path)#openPrimary` | ① |
| `FilePackResources.FileResourcesSupplier` | ① |
| `EntityType.Builder#build(ResourceKey)` | ① |
| `Entity#getPersistentData()`（patched） | ①+NF patch |
| `AttachmentType.builder(Supplier)#build` | ② |
| `IMenuTypeExtension.create(IContainerFactory)` | ② |
| `LivingDamageEvent.Pre#getNewDamage/setNewDamage` | ② |
| `OnDatapackSyncEvent#getRelevantPlayers` | ② |
| `TagsUpdatedEvent.ServerDataLoad` | ② |
| `EventBusSubscriber` 仅 `value()`/`modid()`（无 `Bus`） | ② loader javap |
| `IEventBus#post(T)` 返回事件 | ② bus-8.0.5 |
| Cloth Config（可选运行时依赖，modid `cloth_config`；compileOnly `me.shedaniel.cloth:cloth-config-neoforge:26.1.154`）：`ConfigBuilder#create/#setParentScreen/#setTitle/#setSavingRunnable/#setGlobalized/#setGlobalizedExpanded/#entryBuilder/#getOrCreateCategory(Component)/#build` | ② ClothConfig 分支 `v26.1` `common/.../api/ConfigBuilder.java` |
| `ConfigEntryBuilder#startBooleanToggle/startIntField/startDoubleField/startStrList/startDropdownMenu` + builder 链 `setDefaultValue/setTooltip/setSaveConsumer/setMin/setMax/setSelections/build` | ② 同分支 `api/ConfigEntryBuilder.java` + `impl/builders/*` |
| `ConfigCategory#addEntry(AbstractConfigListEntry)`；`AbstractConfigListEntry#extractRenderState(GuiGraphicsExtractor,int×8,boolean,float)`；`DropdownBoxEntry` 嵌套类（`DefaultSelectionTopCellElement`/`DefaultSelectionCellCreator`/`DefaultSelectionCellElement` 及 protected 字段） | ② 同分支 `api/ConfigCategory.java`、`api/AbstractConfigListEntry.java`、`gui/entries/DropdownBoxEntry.java` |
| `ModConfigSpec#save()`（Cloth 界面保存回写） | ② `ModConfigSpec.java:186`（NF tag `26.1.2-stable`） |
| `ModList#isLoaded(String)`（cloth 在场判断；modid 证据 `modId = "cloth_config"`） | ② FML `11.0` `ModList.java` + ClothConfig v26.1 `neoforge/.../neoforge.mods.toml` |
| **26.1.2 vanilla `Screen#extractRenderState` 默认实现内部会调 `extractBackground`（含 blur）；自定义 Screen 严禁在 `extractRenderState` 里再手动调一次 `extractBackground`** | ① crash 日志（main `RawOutput.log`，2026-08-21 13:21）：`IllegalStateException: Can only blur once per frame` ← `GuiRenderState#blurBeforeThisStratum` ← `Screen#extractBackground` ← `TaczConfigHomeScreen.extractRenderState:87`（:85 已 blur 一次）；同仓库 `GunSmithTableScreen` 注释亦载明背景须放 `extractBackground` 覆写 |

## 实现要点

- `LivingEntityMixin` 实现 `IGunOperator`（`tacz.mixins.json` + `[[mixins]]`）。
- `IGunOperator.fromLivingEntity` 在 mixin 未生效时仍返回 NoOp。
- 默认枪包从 jar/resources 导出到 `run/tacz/tacz_default_gun`（`ResourceManager.registerExportResource`）。
- 枪包版本比较剥离 `+` build metadata，避免宪章 7.4 SemVer 陷阱。
- S2C 客户端应用走 `ClientPacketBridge` 反射 → `ClientPacketHandlers`（dedicated 常量池无 `LocalPlayer`）。
- 弹道：`EntityKineticBullet` + `ModDamageTypes` + `LivingKnockBackEvent`。
- 配件 modifier：`AttachmentPropertyManager.registerModifier()`。
- 配置界面 = TACZ 经典 Cloth 八分类（Key/Render/Resource/Sound/Zoom + Gun/Ammo/Other）：
  `compat/cloth/*` 取自 TaCZ_Refabricated_Unofficial 26.1.2（语义权威，含 26.1 特有 `scope_mask_enable` 项），
  注册与保存习语取自 MUKSC/TACZ-1.21.1（`IConfigScreenFactory` × `setSavingRunnable{Common,Client}.spec.save()`）。
  cloth 为可选依赖：在场 → Mods 菜单与 T 键均开 Cloth 界面；缺席 → T 键发聊天下载链接（refab 语义，26.1.2
  `ClickEvent.OpenUrl`/`HoverEvent.ShowText`），Mods 菜单回落 `ClothConfigScreen` 警告屏（MUKSC
  `registerNoClothConfigPage` 习语；该屏本次复活并修复双 blur 崩溃模式）。语言键 `config.tacz.*`（102 条）原已齐备。
  r14 曾短暂改用 NF 原生 `ConfigurationScreen`，按项目对齐基准（MUKSC × refab）回退为 Cloth 方案。
- 已知遗留（无引用死代码，同样的双 blur 潜在模式，待后续工作包清理）：`gui/GunPackProgressScreen`。

## 冒烟（dedicated server）

```
TaCZ NeoForge 26.1.2 port work package ④ loading. modId=tacz
WP③ payloads registered (play + configuration), version=1.0.5
Exporting resource pack /assets/tacz/custom/tacz_default_gun to /home/user/run/tacz/tacz_default_gun
- tacz_default_gun, Main namespace: tacz
Found 1 possible gunpack(s) and added them to resource set.
WP④ gun pack loaded: guns=54 ammo=24 attachments=99 blocks=4 recipes=182
Done (0.442s)! For help, type "help"
```

已知：原版 `RecipeManager` 仍警告工作台配方 `empty ingredients`（延迟解析占位；真正合成走 `CommonAssetsManager` / `TableRecipe`）。`@OnlyIn` 仅警告、不再剥成员。

未接触 `tacz-port` jar。

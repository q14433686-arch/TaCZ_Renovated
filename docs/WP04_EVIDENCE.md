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
| `ConfigurationScreen.ConfigurationSectionScreen(Screen, ModConfig.Type, ModConfig, Component)`（26.1 起收注册的 `ModConfig`，不再收 `ModConfigSpec`） | ② `ConfigurationScreen.java`（NF tag `26.1.2-stable`；编译错误 "ModConfigSpec无法转换为ModConfig" 反证） |
| `ModConfigs#getConfigSet(Type)`、`ModConfig#getModId()/getSpec()`（反查 spec 对应的注册 config） | ② 同上（NF 自家 `ConfigurationScreen#addOptions()` 同款查找路径） |

## 实现要点

- `LivingEntityMixin` 实现 `IGunOperator`（`tacz.mixins.json` + `[[mixins]]`）。
- `IGunOperator.fromLivingEntity` 在 mixin 未生效时仍返回 NoOp。
- 默认枪包从 jar/resources 导出到 `run/tacz/tacz_default_gun`（`ResourceManager.registerExportResource`）。
- 枪包版本比较剥离 `+` build metadata，避免宪章 7.4 SemVer 陷阱。
- S2C 客户端应用走 `ClientPacketBridge` 反射 → `ClientPacketHandlers`（dedicated 常量池无 `LocalPlayer`）。
- 弹道：`EntityKineticBullet` + `ModDamageTypes` + `LivingKnockBackEvent`。
- 配件 modifier：`AttachmentPropertyManager.registerModifier()`。
- `TaczConfigHomeScreen`：用 `ModConfigs#getConfigSet(Type)` 按 spec 反查注册的 `ModConfig`
  再传给 `ConfigurationSectionScreen`；查不到（config 未注册）则隐藏该入口。

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

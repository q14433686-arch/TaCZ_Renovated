# 工作包② 证据清单

Fabric 语义：`q14433686-arch/TaCZ_Refabricated_Unofficial` 分支 `26.1.2`（`f493a56`）。
加载器骨架：MUKSC `DeferredRegister` + 官方 26.1.2 MDK / NeoForge 26.1.2.97。

## 注册 API

| 调用 | 证据 |
|---|---|
| `DeferredRegister.Items#registerItem(String, Function<Item.Properties, I>)` 自动 `Properties#setId(ResourceKey)` | ⑤ NeoForge `DeferredRegister.java` 26.1.2.97-sources：`func.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, key)))` |
| `DeferredRegister.Blocks#registerBlock(String, Function<Properties, B>, UnaryOperator<Properties>)` 自动 `setId` | ⑤ 同上 BLOCKS 内层 |
| `new BlockEntityType<>(supplier, Block...)` | ⑤ NeoForge userdev patch `BlockEntityType.java.patch`（vanilla 无 public Builder；Neo 增加 vararg 构造器） |
| `CreativeModeTab#builder(Row, int)` | ① 26.1.2 `CreativeModeTab#builder(Row, int)`；Fabric 26.1.2 `ModCreativeTabs` 同签名 |
| `Item.Properties#useBlockDescriptionPrefix()` | ③ Fabric 26.1.2 `ModItems#blockItemProps` 注释（26.2 起 BlockItem 不再覆写 descriptionId；26.1.2 已有该方法，工作台 BlockItem 显式调用） |
| `RecipeSerializer(MapCodec, StreamCodec)` record | ① 26.1.2 `RecipeSerializer`；③ Fabric `GunSmithTableSerializer#create` |
| `RecipeType` 匿名 `toString` | ③ Fabric `ModRecipe`（① `RecipeType.simple` 已不存在） |
| `Registries.RECIPE_BOOK_CATEGORY` + `new RecipeBookCategory()` | ① `RecipeBookCategory()` 公开构造器；③ Fabric `GunSmithTableRecipe#recipeBookCategory` |
| `PlacementInfo.NOT_PLACEABLE` | ① `PlacementInfo`；③ Fabric 工作台配方不进原版合成网格 |
| `Identifier.fromNamespaceAndPath` / `readIdentifier` / `writeIdentifier` | ① 入场考试已证 |
| `Ingredient.CODEC` 只收字符串 / `#tag` | ① Ingredient.CODEC；③ RecipeCompat `normalizeLegacyIngredient` |
| `result.group` 裸名补 `tacz:` | ③ Fabric `GROUP_CODEC`；本仓 `RecipeCompat#parseGroup` |
| `FMLCommonSetupEvent` 里 `GunItemManager.registerGunItem` | ④ MUKSC 在 `RegisterEvent` 注册枪物品；② primer 不强制事件名，26.1 MDK 用 `modEventBus.addListener` |

## 明确未做（留给后续包）

- 枪械/弹药/配件的完整逻辑、枪包填充创造标签（④）
- 工作台菜单 / payload（③）
- Target/Statue 方块实体与矿车实体（④）
- 第一人称渲染；工作台 `RenderShape` 暂用 `MODEL` 便于冒烟看见方块（⑤ 再改回 Fabric 的 INVISIBLE+BER）
- 未接触 `tacz-port` jar

## 冒烟

`run/logs/latest.log`（专用服务器，无显示器无法 `/give` 给玩家，但注册表 ID 已绑定）：

```
WP② registries ready: gun=tacz:modern_kinetic_gun workbench_a=tacz:workbench_a gun_smith_table=tacz:gun_smith_table recipe=tacz:gun_smith_table_crafting
Done (0.388s)! For help, type "help"
```

`/give @s tacz:modern_kinetic_gun` 与放置 `tacz:workbench_a` 使用上述已注册 ID。

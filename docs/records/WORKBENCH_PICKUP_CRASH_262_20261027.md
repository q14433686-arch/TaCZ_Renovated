# 26.2：工作台配方校验与 Carry On 拾取检查（2026-10-27，第二轮同步）

- 同步来源：本仓库 **1.21.11 线** `581daaa`（PR #66，「Guard malformed workbench recipes and companion pickup」）
  及其 CI 快照 `72775b8`。
- 本线版本**不变**：仍是 `1.1.8+neoforge.26.2.R3-hotfix4`。1.21.11 线明确撤回了本轮临时提出的
  hotfix5 版本号（其记录 `WORKBENCH_PICKUP_VERSION_12111_20261027.md`），本线同样不动版本。
- 第一轮（方块掉落 / 雕像持枪 / 标靶主人）的同步见 `SISTER_SYNC_262_20261027.md`；本轮只做
  配方校验 + Carry On 拾取。

## 日志证据与边界（来自 1.21.11 线的用户日志）

1.21.11 线用户 `latest.log` 显示：REI 准备 `ra1k:attachments/fast_mag` 时，
`GunSmithTableRecipe#resolveIngredients` 对空 `input` 调 `resolve` 抛 NPE 并跳过；随后打开工作台
界面时同一 NPE 出现在 `GunSmithTableScreen#classifyRecipes` 里，`ClientPayloadHandler` 报错**断线**。

- 本线**没有**该日志或实机复现，本条按「同因可复现」处理：本线 `resolveIngredients` 与
  `classifyRecipes` 的写法与 1.21.11 线修前相同，`GunSmithTableIngredient#resolve` 同样
  `Objects.requireNonNull`，因此**同样的空材料配方在本线会造成同样的断线**（源码推演，未实测）。
- 日志里没有该配方原文，不能断言是显式 `null`、空项还是尾逗号；也不能据此推断用户的枪包内容。

## 处理

| 位置 | 处理 |
|---|---|
| `TableRecipe#validationError(TableRecipe)`（新增） | 把「配方为空 / `result` 缺失 / `materials` 缺失 / `materials[i]` 为空」判为**整条无效**。**不删除空项**——删掉会降低合成成本；显式的 `"materials": []` 仍是合法配方。 |
| `TableRecipeManager#parseJson`（覆写） | 校验失败时抛 `JsonParseException`，由父类 `JsonDataManager` 记录**配方 ID** 并整条拒绝。 |
| `TableRecipeManager#apply` | 父类无论解析成功与否都会缓存原文；这里在 `super.apply` 之后把未进入 `dataMap` 的条目从 `networkCache` 剔除，避免**把被拒的原始 JSON 同步给旧客户端**。 |
| `CommonNetworkCache#fromNetwork` | 接收端再校验一次（兼容仍发送无效原文的旧服务端）。 |
| 消费端 | `GunSmithTableScreen`（2 处）、`GunSmithTableMenu`、`TimelessAPI`（2 处）、JEI `GunModPlugin`、REI `REIClientPlugin` 一律先 `TableRecipe.validationError(...) != null` 再 continue/return，构造前就把坏配方挡掉。 |
| `GunSmithTableRecipe` 构造器 | 对 `inputs` 做快照（POJO 之后被改不会让已显示的配方少材料），并拒绝 `null` 列表或 `null` 成员；合法空列表不受影响。 |
| `CarryOnPickupHandlerMixin` | 见下节。 |

**本线未搬的部分**：1.21.11 线同一提交里 `GunSmithTableMenu` 的扣材料实现是把
`IItemHandler` + `extractItem` 换成 `ResourceHandler<ItemResource>` + `Transaction` 原子提交。本线
（26.2）的这一段早已是 `ResourceHandler` + `Transaction.openRoot()` 形态且行为一致，属**同源已存在
的适配**，没有改动。

## Carry On：注入点与 2.11.0 的证据

1.21.11 线针对 Carry On **2.9.2**（`c65c63fb`）推演；本线钉的是 **2.11.0**，因此逐条核对了
2.11.0 的源码（`gh api repos/Tschipp/CarryOn/contents/...?ref=v2.11.0`，blob `c0014165d90c561bda6731dbe8bbdde19264a98a`）：

- 签名一致：`public static boolean tryPickUpBlock(ServerPlayer, BlockPos, Level, @Nullable BiFunction<BlockState, BlockPos, Boolean>)`。
- 顺序一致：先 `isBlockInDistance(player, pos)` → `canCarryGeneral(player)` →
  `CarryOnDataManager.getCarryData(player)`。因此新注入点（`getCarryData` 的 INVOKE）位于
  **距离检查与通用条件之后**，与 1.21.11 线的修法语义相同。
- 拾取收尾仍是 `carry.setBlock(state, blockEntity, player, pos)` → `removeBlockEntity(pos)` →
  `removeBlock(pos, false)`；`removeBlock` 会传播邻接更新，非根半边经 `updateShape` 消失。
- `hasPropertyType(state, DoorBlock.HALF)` 的实现是
  `p.getValueClass().equals(prop.getValueClass())`：B 用的是 `BlockStateProperties.BED_PART`、
  C 用的是本仓自有 `TableHalf`，都不等于 `DoubleBlockHalf`，因此两张双格工作台不会被这条规则拒绝。

改动内容（与 1.21.11 线逐字一致）：

1. 把「非根半边 → 根」的 `@ModifyVariable` 从 `@At("HEAD")` 移到 `getCarryData` 的 INVOKE 之前。
   旧写法在 HEAD 就换成根位置，于是 Carry On 的**距离检查检查的是根**，而玩家实际点的是另一格；
   现在先按点击位置过距离/通用条件，之后的所有状态、方块实体、回调与移除才用根位置。
2. 新增 `@Inject`（可取消）：当点击的是非根半边、且**找不到匹配的根**（残缺/被别的方块顶替/
   `getCompanionState` 对不上）时直接返回 `false`，避免 `pickupAllBlocks` 把孤立半边当普通方块搬走。
3. 新增 `@Unique tacz$hasMatchingRoot(...)`：根是同一种工作台、确为根、位置与状态互相匹配才算数。

注入点全部 `require = 0`：Carry On 换签名时**静默不注入**，不会崩游戏，但也等于退回旧行为
——所以「混合器是否真的应用」必须看实机日志（见待测矩阵）。2.11.0 的源码核对只是静态依据，
本仓没有 Carry On 的编译期依赖。

## 测试与守卫

从 1.21.11 线移植（本线做了 26.2 适配）：

- `tests/workbench-regression/`：`WorkbenchRecipeValidationTest`（真实 `TableRecipe` /
  Gson adapter / `TableRecipeManager` / `CommonDataManager` / `CommonNetworkCache` /
  `GunSmithTableRecipe`，跑在 Minecraft/NeoForge 桩上）与 `CarryOnPickupTest`（真实 mixin handler）。
  其 `net/minecraft/world/item/crafting/Recipe` 桩已改为 **26.2 的一参 `assemble(T)`**
  （1.21.11 是 `assemble(T, HolderLookup.Provider)`，本线 `GunSmithTableRecipe` 早就没有该参数）。
- `tests/carryon-transform/`：用 Mixin 0.8.7 transformer 对**模拟的** Carry On `PickupHandler`
  做真实注入与执行，覆盖根/非根、可达非根但不可达根、通用条件、缺根与方向不匹配。
- `scripts/verify_sister_recipe_sync.py`：静态接线守卫。其中两条**按本线 codec 重写**：
  1.21.11 线的注册表解码器走 `ExtraCodecs.JSON` + 惰性归一化，本线仍是 `Ingredient.CODEC` +
  `GunSmithTableIngredient#resolve`。这个差异**早于本轮同步**、本轮不处理，只是把断言改成
  本线的真实契约，避免守卫变成「照抄对方实现」。
- Gradle：新增 `compileWorkbenchRegression` / `workbenchRecipeValidationTest` / `carryOnPickupTest` /
  `compileCarryOnTransformTest` / `carryOnTransformTest`，全部用本工程 **Java 25** toolchain，并接入
  `check`（`build` 会连带跑）。`build.yml` 增加 `python3 scripts/verify_sister_recipe_sync.py`。

以上都是**离线**回归：不起 Minecraft、不加载真实 Carry On jar、不验证 FML 的 mixin plugin 选择。

## 待实测矩阵

- 单人 + 专服，各用一份含下列问题的枪包：`materials` 含 `null`、尾逗号、缺 `materials`、
  缺 `result`；新服务端/新客户端与**旧服务端/新客户端**都要能开工作台且不断线，日志要点出被跳过的
  具体配方 ID；正常配方的材料数量与合成行为不变；显式 `materials: []` 维持原样。
- Carry On 2.11.0：B/C 从根与非根半边分别拾取、放下，验证不掉落/不复制、`BlockId` 与方块实体内容
  保留；覆盖普通/创造、可达性边缘、权限拒绝、空间不足、`pickupAllBlocks` 开关、残缺半边。
- 真机日志确认 `CarryOnPickupHandlerMixin` 真的应用（`require = 0` 可能静默跳过）。

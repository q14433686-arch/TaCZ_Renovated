# 26.1.2 工作台空材料断线防护与 Carry On 双格拾取同步审计（2026-10-27）

## 1. 同步来源与版本策略

- **上游跟进来源**：本仓库 `1.21.11` 分支 [PR #66](https://github.com/q14433686-arch/TaCZ_Renovated/pull/66)（提交 `581daaacc8e21ef55259480c804442e17b455fbe`、`72775b8668592d867bd930e9bb5a793a1520aef0`）。
- **版本策略**：沿用 `1.21.11` PR #66 不再递增版本号的决定，`26.1.2` 分支保持尚未发布的 `1.1.8+neoforge.26.1.2.R3-hotfix4`（`gradle.properties`、`README.md`、`CHANGELOG.md` 一致）。
- **历史记录冻结**：前序方块/雕像/标靶同步审计 [`SISTER_SYNC_2612_20261027.md`](SISTER_SYNC_2612_20261027.md) 与其首轮 CI 记录 [`SISTER_SYNC_2612_CI_20261027.md`](SISTER_SYNC_2612_CI_20261027.md) 保持原样不回写；本轮变更单独成档。

## 2. 工作台配方整条校验与同步边界

1. **`TableRecipe#validationError(TableRecipe)`**：将 `null` 配方、缺失/`null` 的 `result`、缺失/`null` 的 `materials` 列表或列表内任意 `null` 条目判定为整条无效配方并返回带下标的原因说明（兼容宽松 Gson 将尾逗号解析为数组 `null` 元素的场景）。**禁止静默剔除空材料项**，避免无意降低合成成本；显式 `materials: []` 继续视为合法零材料配方。
2. **服务端加载与同步过滤（`TableRecipeManager`）**：`parseJson` 在 `validationError != null` 时抛出 `JsonParseException`，交由 `JsonDataManager#apply` 记录具体配方 `Identifier` 并拒绝入表；`apply` 在 `super.apply` 后按权威 `dataMap` 过滤 `networkCache`，避免父类默认缓存把解析失败的原始 JSON 继续同步给旧客户端。
3. **客户端同步与消费端兜底**：
   - `CommonNetworkCache#fromNetwork`（`DataType.RECIPES`）二次执行 `TableRecipe.validationError`，兼容仍下发未过滤原始 JSON 的旧服务端并记录问题配方 ID；
   - `GunSmithTableRecipe` 构造器对材料列表做 `new ArrayList<>(inputs)` 防御性快照（兼容不可变 `List.of()` 调用且防止后续 POJO 列表被篡改），拒绝 `null` 列表或 `null` 成员；
   - `GunSmithTableScreen`（分类与当前选中 2 处）、`GunSmithTableMenu`（服务端合成校验 1 处）、`TimelessAPI`（单条与全量查询 2 处）、`compat/jei/GunModPlugin`（1 处）、`compat/rei/REIClientPlugin`（1 处）在构造 `GunSmithTableRecipe` 前统一调用 `TableRecipe.validationError`。

## 3. Carry On 2.10.0（26.1.2）源码核对与双格拾取调整

核对 [`Tschipp/CarryOn` `26.1` 分支提交 `e609245b4952e8705ed4e5957e81673e8785b0c9`（`version=2.10.0`、`minecraft_version=26.1.2`、`neoforge_version=26.1.2.93`）的 `PickupHandler#tryPickUpBlock`](https://github.com/Tschipp/CarryOn/blob/e609245b4952e8705ed4e5957e81673e8785b0c9/Common/src/main/java/tschipp/carryon/common/carry/PickupHandler.java) 与 [`CarryOnDataManager#getCarryData`](https://github.com/Tschipp/CarryOn/blob/e609245b4952e8705ed4e5957e81673e8785b0c9/Common/src/main/java/tschipp/carryon/common/carry/CarryOnDataManager.java)：

- `tryPickUpBlock(ServerPlayer, BlockPos, Level, BiFunction)` 先对点击传入的 `pos` 依次调用 `isBlockInDistance(player, pos)` 与 `canCarryGeneral(player)`，随后首次调用 `CarryOnDataManager.getCarryData(player)`，之后才读取 `BlockEntity` / `BlockState`、执行 `pickupCallback`、写入 `CarryOnData` 并移除方块。
- 旧 `CarryOnPickupHandlerMixin` 在 `@At("HEAD")` 即把非根半边改为根坐标，会导致距离判定按另一格（根半边）而非玩家实际点击的一格计算。
- 现将 `@ModifyVariable`（`tacz$resolveRootTablePart`）与新增 `@Inject`（`tacz$rejectOrphanTablePart`）统一锚定在首个 `CarryOnDataManager#getCarryData(Player)` 调用前：
  - 先按玩家实际点击半边完成距离与通用条件检查；
  - 仅在 `tacz$hasMatchingRoot` 确认相邻根半边为同一工作台方块、确为根、且 `getCompanionPos` / `getCompanionState` 双向匹配时重定向到根；
  - 若非根半边缺失根或朝向/类型不匹配，直接 `cir.setReturnValue(false)` 拒绝拾取，防止 `pickupAllBlocks` 将孤立半边当作普通方块搬走。
- 根半边的破坏/权限回调与放置侧双格空间预检仍分别由 Carry On 与既有 `CarryOnPlacementHandlerMixin` 负责；跨保护区边界的双格权限交互仍属待实机集成验证项。

## 4. 26.1.2 分支相对 1.21.11 的适配差异

| 维度 | `1.21.11`（PR #66） | `26.1.2` 本次落地 |
|---|---|---|
| 原版 `Recipe#assemble` 签名 | `assemble(T, HolderLookup.Provider)` | `26.1.2` 为单参 `assemble(T)`；保留 `GunSmithTableRecipe` 单参实现，并同步调整 `tests/workbench-regression/stubs/.../Recipe.java` 桩接口 |
| 工具链与 Mixin 兼容级别 | `JavaLanguageVersion.of(21)` / `pickup-test.mixins.json` 为 `JAVA_21` | `build.gradle` 全部新增编译/运行任务改为 `JavaLanguageVersion.of(25)`，`pickup-test.mixins.json` 改为 `JAVA_25`（与 `src/main/resources/tacz.carryon.mixins.json` 对齐） |
| `scripts/verify_sister_recipe_sync.py` | 含 `1.21.11` PR #55 遗留的 `GunSmithTableSerializer` 断言 + 工作台校验断言 | `26.1.2` 未回移 PR #55，脚本仅覆盖 `26.1.2` 真实存在的工作台 5 类消费端校验、服务端/客户端网络同步校验及 Carry On 拾取锚点守卫 |
| 启动期 `VoxyCompatMixinPlugin` | `1.21.11` 日志曾记录 `ModList.get()` 过早调用的独立问题 | `26.1.2` 既有实现已使用 `FMLLoader.getCurrent().getLoadingModList().getModFileById("voxy")`，无需改动 |

## 5. 本地校验与未验证边界

### 已在本地沙箱执行的静态校验
- `bash scripts/check_release_consistency.sh --strict`
- `python3 scripts/verify_sister_recipe_sync.py`
- `python3 scripts/verify_block_drops.py --strict`、`python3 scripts/verify_block_entity_persistence.py --strict`
- `python3 tests/test_sister_block_sync.py`、`python3 tests/test_sync_guards.py`
- `python3 scripts/verify_bullet_hole_orientation.py`、`python3 scripts/verify_ysm_compat.py`、`python3 scripts/verify_lr_assets.py`
- `python3 docs/check_mixin_registration.py`、`python3 docs/check_lang_keys.py`、`python3 docs/check_mesh_config_parity.py`

### 尚未在本地执行、交由 GitHub Actions 或实机验证的部分
- **Java 编译与桩/字节码转换测试**：当前沙箱无本地 `java` / `javac` / `gradle`，`compileJava` 及接入 `check` 的 `workbenchRecipeValidationTest`、`carryOnPickupTest`、`carryOnTransformTest` 均需由本仓库 GitHub Actions（`compile-check` 与 `build`）在远端 JDK 25 环境实跑确认（CI 结果将另行记录，不在本文件预言）。
- **实机与真实模组运行期未验证**：`carryOnTransformTest` 仅对同签名模拟 `PickupHandler` 执行 Mixin 转换，不含 NeoForge `CarryOnCompatMixinPlugin` 加载期筛选与真实 Carry On `2.10.0` jar；生产配置 `defaultRequire = 0` 在目标签名不兼容时仍可能静默跳过注入。**本轮未启动 Minecraft 26.1.2 客户端或专用服务器，未进行游戏内合成、第三方异常枪包联机或 Carry On 实机测试。**

## 6. 待实机验收矩阵

1. **异常工作台配方容错**：在单人及专服放置含 `materials: null`、缺失 `materials`、数组含 `null` 或尾逗号的测试枪包配方，验证新服务端/新客户端与旧服务端/新客户端打开工作台及 JEI/REI 均不掉线，日志输出具体被拒配方 ID，且该配方不可见/不可合成；正常配方材料成本不变，显式 `materials: []` 仍按零材料配方工作。
2. **Carry On 2.10.0 双格工作台搬运**：对 B（水平双格）与 C（垂直双格）工作台分别在极限可达距离点击根半边与伴随半边拾取、旋转方向放置，确认两半同时移除与重建、无复制掉落、`BlockId` 与内部状态保留。
3. **残缺半边与权限边界**：开启 `pickupAllBlocks` 时构造缺失根半边或朝向错位的孤立伴随半边，确认无法被 Carry On 单独搬起；在领地/权限保护边界核对拾取与放置拒绝行为，并检查真实 `latest.log` 无 Mixin 应用告警。

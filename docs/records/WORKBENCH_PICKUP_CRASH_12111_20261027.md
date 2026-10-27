# 1.21.11：工作台断线日志与 Carry On 拾取检查审计（2026-10-27）

## 日志证据与边界

用户提供的 `latest.log` 中，TaCZ 版本为 `1.1.8+neoforge.1.21.11.R3-hotfix4`，NeoForge `21.11.45`，Carry On `2.9.2`，REI `21.11.816`。日志显示：

- REI 准备 `ra1k:attachments/fast_mag` 时，`GunSmithTableRecipe.resolveIngredients` 对空 `input` 调用 `resolve` 抛出 NPE，并跳过该条。
- 随后客户端收到工作台界面打开消息时，`GunSmithTableScreen.classifyRecipes` → `resolveIngredients` 出现同一种 NPE；`ClientPayloadHandler` 报错并断线。日志没有在该 GUI 堆栈标注当前配方 ID，因此不能把具体那条配方断言为唯一触发源。
- 日志没有 Carry On 抛出的崩溃堆栈，也没有该枪包配方原文；不能据此确认是 JSON 显式 `null`、空项、尾逗号还是其他来源，更不能证明当时拾取未成功的原因。

另有启动阶段 `VoxyCompatMixinPlugin` 调用 `ModList.get()` 过早导致的独立错误；本轮没有修改 Voxy，也没有证据表明该错误导致本次工作台断线。

## 处理

1. `TableRecipe.validationError(TableRecipe)` 将缺失/空材料列表、列表内任意空条目或缺失结果视为整条无效配方。**不删除空材料项**，因为这会降低合成成本；显式 `materials: []` 保持原语义。
2. 服务端 `TableRecipeManager#parseJson` 抛出带原因的解析异常，由 `JsonDataManager#apply` 记录配方 ID；`TableRecipeManager#apply` 从网络缓存中移除未进入服务端数据表的原始 JSON。后者必要，因为父类原本无论解析是否成功都缓存原文。
3. `CommonNetworkCache#fromNetwork` 再校验，兼容仍发送无效原文的旧服务端；屏幕、合成菜单、REI、JEI 和 `TimelessAPI` 在构造配方前也防御性校验。`GunSmithTableRecipe` 对输入列表作快照并拒绝 `null` 列表引用或 `null` 成员；不影响合法空列表。
4. Gson 宽松解析会把数组尾逗号作为空项。离线测试同时覆盖尾逗号及显式 `null`，但**不能据此推断用户的原始配方**。被拒绝的配方不会出现或可被合成；修正枪包原文件后重新加载才能恢复该配方。

## Carry On 源码与注入依据

- [Carry On 2.9.2 固定源码 `PickupHandler#tryPickUpBlock(ServerPlayer, BlockPos, Level, BiFunction)`](https://github.com/Tschipp/CarryOn/blob/c65c63fbe1f6a089fc5afb1fc38d469f9f36262c/Common/src/main/java/tschipp/carryon/common/carry/PickupHandler.java)：先对传入的点击 `pos` 调用 `isBlockInDistance`，再调用 `canCarryGeneral`，之后调用 `CarryOnDataManager#getCarryData(Player)` 并读取该位置的方块/方块实体，最后调用 `pickupCallback`、保存和移除。
- 旧 mixin 在 HEAD 先把非根位置改为根，因而上述距离检查可能检查的是根而非玩家实际点击的一格；这是源码推演，不是用户日志已证明的失败原因。现在在 `getCarryData` 调用前修改 `BlockPos`，使距离与通用条件先执行，后续状态、方块实体与回调仍指向根。
- 与既有 B/C 的 `getRootPos`、`getCompanionPos`、`getCompanionState` 配合，只在根是同一种工作台、确为根、位置/状态互相匹配时重定向；否则取消对残缺非根的拾取，避免 `pickupAllBlocks` 把孤立半边当普通方块搬走。
- [NeoForge 事件入口](https://github.com/Tschipp/CarryOn/blob/c65c63fbe1f6a089fc5afb1fc38d469f9f36262c/NeoForge/src/main/java/tschipp/carryon/events/CommonEvents.java) 仅在拾取成功时取消原方块交互；根位置的 `BreakEvent` 回调仍由 Carry On/其他模组决定。此路径没有新增对另一半单独的权限事件；跨保护区相邻方块的行为仍需集成测试。
- 分别用 [Sponge Mixin `releases/0.8.7`（4053421aa10aaac6127d969028a29c94fe3054f6）](https://github.com/SpongePowered/Mixin/tree/4053421aa10aaac6127d969028a29c94fe3054f6) 和 [Fabric Mixin（a3cd81528f0f12d94a2aafc18696fdd14833c1c7）](https://github.com/FabricMC/Mixin/tree/a3cd81528f0f12d94a2aafc18696fdd14833c1c7) 源码编译 transformer；后者的构建属性为 `0.16.5+mixin.0.8.7`，与日志版本号一致（未校验用户 jar 的字节哈希）。使用测试服务与模拟 Carry On 同签名目标执行真实注入与 JVM 加载：根/非根、可达非根但不可达根、通用条件、权限回调、缺根及方向不匹配均通过。该测试不含 NeoForge 的 mixin plugin、真实 Carry On jar 或游戏；`require=0` 仍允许对不兼容版本静默不注入，必须实机确认。

## 验证记录

- Java 21 ECJ + Gson 2.14.0：生产 `TableRecipe`、材料 Gson adapter、`JsonDataManager` / `CommonDataManager` / `TableRecipeManager`、`CommonNetworkCache` 与 `GunSmithTableRecipe` 在 Minecraft/NeoForge 桩上通过空材料、无列表、明确空列表、尾逗号、旧服务器原文、服务端同步、非法材料、不可变列表及快照测试。真实服务端菜单、MC codec、网络传输和注册表未运行。
- Java 21 ECJ：拾取 mixin 处理函数桩测试及上面的 Mixin transformer 模拟目标测试通过。配置了 `workbenchRecipeValidationTest`、`carryOnPickupTest`、`carryOnTransformTest` 接入 `check`；这些 Gradle 任务本地未能执行。
- 静态接线、方块掉落/方块实体守卫、两格离线生命周期模型、历史回归及版本一致性 `--strict` 通过；历史标靶/雕像与 mesh Java 桩测试通过。
- 本次尝试 `./gradlew compileJava --console=plain --no-daemon`，Gradle 9.2.1 下载在 TLS 握手阶段失败，**未进入项目编译**；未执行真实 Minecraft/NeoForge/Carry On 或真实枪包的端到端测试。先前 hotfix4 的 CI 成功不代表本次 hotfix5 已通过编译。

## 待实测矩阵

- 单人及专服分别使用有上述空项的枪包：新服务端/新客户端与旧服务端/新客户端都能开工作台而不掉线，日志显示被跳过的具体 ID；正常配方的材料数量和合成行为不变，显式空列表维持现有行为。
- Carry On 2.9.2：B/C 两种工作台各从根和非根半边拾取、放下，验证无掉落/复制、`BlockId` 与内容保留；覆盖普通与创造模式、拾取范围边缘、权限拒绝、空间不足、`pickupAllBlocks` 开关及损坏结构。
- 核对真实运行日志确认 mixin 应用；如果仍失败，请保留新日志，并提供对应枪包内 `ra1k:attachments/fast_mag` 的原始配方 JSON（及实际枪包路径），不能凭日志猜补合成材料。

# WP-262-0 证据：26.1.2 transfer API 前置卫生

日期：2026-08-21  
目标基线：Minecraft 26.1.2 + NeoForge 26.1.2.97（尚未 bump）

## 范围

本包只处理工单 WP-262-0：

1. 清除本仓库对 NeoForge 旧 `items.IItemHandler` / `ItemHandlerHelper` / `InvWrapper` /
   `EmptyItemHandler` 的依赖；
2. 删除无引用死代码 `client/gui/GunPackProgressScreen`；
3. 不改 Minecraft、NeoForge 或 mod 版本号，不提前处理 26.2 编译差异。

## 事实纠正

工单将 26.2 的 transfer removal 标为“最可能落地”。执行日核对
NeoForge `26.2.x` 源码后，旧 `IItemHandler` 实际仍存在且仍标为
`@Deprecated(since = "1.21.9", forRemoval = true)`；它并未在 26.2.0.64 删除。
本包仍按计划迁移，因为消除 for-removal API 是独立成立的前置卫生目标。

## API 证据

### ② NeoForge 26.1.2.97 sources

官方仓库：<https://github.com/neoforged/NeoForge>，`26.1.x` commit
`5696d60a1bca23c9fb449441224462e6234bb4e3`。该 commit 是 `26.1.2` tag 后第 97 个
release commit，对应本基线 `26.1.2.97`。

| 使用点 | 已核签名 / 语义 |
|---|---|
| 玩家背包 wrapper | `VanillaContainerWrapper#of(Container) -> ResourceHandler<ItemResource>`；源码明确要求 `Inventory` 使用其返回的 `PlayerInventoryWrapper` 路径 |
| 空 handler | `EmptyResourceHandler#instance() -> EmptyResourceHandler<T>` |
| 遍历 | `ResourceHandler#size()`、`#getResource(int)`、`#getAmountAsInt(int)` |
| ItemStack 视图 | `ItemUtil#getStack(ResourceHandler<ItemResource>, int) -> ItemStack`；返回新 stack，不暴露 handler 内部可变对象 |
| 扣除/写回 | `ResourceHandler#extract(int, T, int, TransactionContext)`、`#insert(int, T, int, TransactionContext)`，返回实际传输量 |
| 事务 | `Transaction#openRoot()`；只有调用 `Transaction#commit()` 才应用根事务，未提交关闭即回滚 |
| resource 转换 | `ItemResource#of(ItemStack)`；`ItemResource` 不含 count，数量由 handler 单独保存 |

因此 `AmmoSourceRegistry` 的普通弹药扣除改成事务内 `extract`；弹药盒的数据组件不能再靠
修改 `ItemUtil#getStack` 的返回值写穿 handler，而是在同一事务内“取出旧 resource → 插入
更新后 resource”。写回数量不完整时整个调用回滚并返回 0。

`GunSmithTableMenu` 的多 slot 扣料也放进同一个根事务；任一 slot 未完整扣除时不提交，
避免发生部分扣料。

### ① Minecraft 26.1.2 未混淆 jar

核对对象来自 refab `26.1.2` 分支保存的未混淆 merged jar（只用于公开源码谱系内的游戏
语义核验，不涉及禁用的 `tacz-port`）：

- refab commit：`b2238f052e12f5125b25ea88dfe6b7eb902d9d8f`
- jar SHA-256：`6b5ed69454afd7f32bb4f842a19f602b08e497ab6769faaf498eb56a588f6693`
- `net.minecraft.world.entity.player.Inventory#placeItemBackInInventory(ItemStack)void`
  descriptor：`(Lnet/minecraft/world/item/ItemStack;)V`
- 同类另有 `#placeItemBackInInventory(ItemStack, boolean)void`。

反编译方法体确认单参数重载委托给双参数重载；后者先尝试合并/空 slot，背包满时调用
`Player#drop(ItemStack, false)`，并在服务端发送 slot 更新。因此它等价覆盖旧
`ItemHandlerHelper#giveItemToPlayer` 在本仓库退弹路径所需的“尽量入包，否则掉落”语义。

## 改动映射

| 文件 | 改动 |
|---|---|
| `api/item/ammo/AmmoSourceRegistry.java` | 公共 helper 参数改为 `ResourceHandler<ItemResource>`；玩家/空来源改用新 wrapper；普通弹药与弹药盒消费事务化 |
| `api/item/gun/AbstractGunItem.java` | inventory helper 参数同步；退弹改走原版 `Inventory#placeItemBackInInventory` |
| `inventory/GunSmithTableMenu.java` | 配方盘点改用 `ItemUtil#getStack`，扣料改为原子 transfer transaction |
| `client/gui/GunPackProgressScreen.java` | 删除；全仓库无引用 |

## 验证

已执行：

```text
grep -RInE 'net\.neoforged\.neoforge\.items|IItemHandler|ItemHandlerHelper|InvWrapper|EmptyItemHandler' \
  src/main/java --include='*.java'
# 0 matches

grep -RIn 'GunPackProgressScreen' src/main/java src/main/resources
# 0 matches

git diff --check
# success

# java-parser 3.0.1 对三个改动后的 Java 文件执行语法解析
# 3 x PARSE OK
```

构建验收尚未在本沙盒完成：沙盒初始无 Java，且 Gradle wrapper 下载
`https://services.gradle.org/distributions/gradle-9.2.1-bin.zip` 时被执行环境的 TLS egress
策略中断（`SSLHandshakeException: Remote host terminated the handshake`）。因此本文不声称
“compileJava 已通过”或“19 条 warning 已由实编译证明清零”；后续获得可用 JDK 25 / Gradle
环境后必须补跑：

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
```

验收标准：构建成功，且输出中没有 transfer API 的 `[removal]` warning。

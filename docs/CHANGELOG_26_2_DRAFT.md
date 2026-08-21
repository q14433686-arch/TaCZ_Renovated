# TaCZ NeoForge 26.2 changelog（草稿）

> 仅记录已经落入本分支的改动。未执行的实机/兼容矩阵不得写成 PASS。

## Unreleased — `1.1.8+neoforge.26.2.0.r0`

### WP-262-0（仍基于 26.1.2）

- 将弹药来源、工作台扣料与相关公开 helper 从 NeoForge 已标记
  `forRemoval` 的 `IItemHandler` API 迁移到事务式
  `ResourceHandler<ItemResource>` API。
- 退弹入包改用原版 `Inventory#placeItemBackInInventory(ItemStack)`；背包满时仍会掉落实体。
- 删除无引用的 `GunPackProgressScreen`。
- 构建验收待在具备 JDK 25 且可下载 Gradle/依赖的环境补跑；当前不声明编译或实机 PASS。

### WP-262-1

- 构建目标前滚到 Minecraft 26.2 + NeoForge 26.2.0.64（release）。
- 版本设为 `1.1.8+neoforge.26.2.0.r0`，保持 `1.1.8` SemVer core 与枪包依赖兼容。
- 对齐官方 MDK-26.2：Gradle 9.2.1、ModDevGradle 2.0.144、Foojay 1.0.0、
  Java 25、`[26.2]` 精确游戏范围，并修正 `gradlew` 可执行位。
- 保留 OpenGL/depth-aperture 为本期瞄具主路径；没有宣称 Vulkan 或 Aperture 支持。
- 服务端启动验收仍待可联网的 JDK 25 环境执行。

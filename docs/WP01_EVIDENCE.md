# 工作包① 证据清单

每个非平凡 API / 构建选择一行：`类#成员(签名)` ← 来源。

来源层级见 `CHARTER.md` 第 3 节。本包几乎全是 **加载器 / 构建** 表面，不是游戏 API。

## 构建

| 选择 | 证据 |
|---|---|
| 构建插件 `net.neoforged.moddev` 2.0.144 | ④ 官方 [MDK-26.1.2-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle) `build.gradle`（2026-08-16 提交） |
| Gradle Wrapper 9.2.1 | ④ 同上 `gradle/wrapper/gradle-wrapper.properties` |
| Java toolchain 25 | ④ 同上 `java.toolchain.languageVersion = JavaLanguageVersion.of(25)`；② [Getting Started](https://docs.neoforged.net/docs/gettingstarted/)「Java 25 JDK」 |
| `neo_version=26.1.2.97` | ⑤ [projects.neoforged.net/neoforged/neoforge](https://projects.neoforged.net/neoforged/neoforge) 在 26.1 通道显示 Latest = 26.1.2.97；Maven `net.neoforged:neoforge` metadata `release=26.1.2.97`。四段号 = MC 26.1.2 + 构建 97，无 `-beta` = release |
| `minecraft_version=26.1.2` / range `[26.1.2]` | ④ MDK `gradle.properties` |
| **不配置 mappings / parchment** | 宪章 4.1；④ 26.1.2 MDK 无 mappings 块（对比 MUKSC 1.21.1 的 `parchment {}`，禁止抄到 26.1.2） |
| 元数据文件只有 `src/main/templates/META-INF/neoforge.mods.toml` | ④ 26.1.2 MDK **没有** `moddedmc.mod.json`。宪章 5：「按 NeoForge 26.1 实际模板为准」 |
| `generateModMetadata` 从 templates expand `${mod_*}` | ④ MDK `build.gradle` |
| 不依赖 Forge Config API Port | 宪章 5：NeoForge 原生即该 config API |

## 入口与事件（加载器 API）

| 调用 | 证据 |
|---|---|
| `net.neoforged.fml.common.Mod#Mod(String)` | ② [Mod Files](https://docs.neoforged.net/docs/gettingstarted/modfiles/)「javafml and @Mod」；④ MDK `ExampleMod`；④ MUKSC `com.tacz.guns.GunMod` |
| `GunMod(IEventBus modEventBus, ModContainer modContainer)` 构造器注入 | ② 同上（「constructor may have IEventBus or ModContainer」）；④ MDK `ExampleMod(IEventBus, ModContainer)`；④ MUKSC `GunMod(IEventBus, ModContainer)` |
| `net.neoforged.fml.common.Mod#Mod(String value, Dist dist)` `dist=Dist.CLIENT` | ② 同上 `Dist.CLIENT` 示例；④ MDK `ExampleModClient` |
| `net.neoforged.api.distmarker.Dist#CLIENT` | ④ MDK `ExampleModClient` |
| `net.neoforged.fml.common.EventBusSubscriber` `(modid, value=Dist.CLIENT)` | ④ MDK `ExampleModClient` |
| `net.neoforged.fml.event.lifecycle.FMLClientSetupEvent` | ④ MDK `ExampleModClient#onClientSetup` |
| `com.mojang.logging.LogUtils#getLogger()` | ④ MDK `ExampleMod`（26.1.2 模板；未抄 MUKSC 的 log4j `LogManager`） |
| 包名 `com.tacz.guns` / `MOD_ID="tacz"` | ④ MUKSC `GunMod.MOD_ID`（加载器骨架允许范围）；保持 id=`tacz` 以兼容枪包 |

## 冒烟

见 [`WP01_SMOKE.md`](WP01_SMOKE.md)。`run/logs/latest.log`：

`Timeless and Classics Zero 1.1.8+neoforge.26.1.2.r0 (tacz)` ← FML Mod List；`GunMod` 构造器日志；`DedicatedServer` `Done (13.801s)`。

`disableRecompilation = true` 仅因本沙盒 2 GiB 内存扛不住 Vineflower `-Xmx4g`。本机开发请改回 `false` 以挂上 Minecraft 源码（① 级查证仍可用官方 `client.jar` javap，见入场考试）。

## 明确未做（禁止跨包顺手改）

- 无 `DeferredRegister` / 物品 / 方块 / 数据组件（工作包②）
- 无 payload / 网络（工作包③）
- 无 mixin / AT（尚未需要）
- 无渲染、无枪包加载
- 未接触 CurseForge `tacz-port` jar

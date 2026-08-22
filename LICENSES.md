# 依赖与许可证清单

本文件记录本仓库及其直接依赖的许可证。发布二进制时必须随附对应源码（GPL-3.0 义务）。

## 本模组

| 组件 | 许可 |
|---|---|
| 本仓库代码（NeoForge 26.1.2 移植） | GPL-3.0-only |
| 上游代码谱系 MCModderAnchor/TACZ、Sh1roCu/TACZ-Refabricated、q14433686-arch/TaCZ_Refabricated_Unofficial、MUKSC/TACZ-1.21.1 | GPL-3.0 |
| 原版枪模资源（模型/贴图/音效） | CC BY-NC-ND 4.0 |
| 本仓库品牌图标 `icon.png` / `logo.png` | 本项目原创（`scripts/generate_branding.py`），**不是**官方 TaCZ 美术的衍生品 |

## 构建与开发依赖

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| NeoForge `26.1.2.97` | 模组加载器 | LGPL-2.1（NeoForged） |
| Minecraft 26.1.2 | 游戏本体（开发依赖，不 redistributable） | Mojang EULA |
| `net.neoforged.moddev` 2.0.144（ModDevGradle） | 构建插件 | NeoForged |
| MDK-26.1.2-ModDevGradle | 构建脚本模板 | NeoForge MDK template license |
| Gradle 9.2.1 Wrapper | 构建 | Apache-2.0 |

## 运行时 Jar-in-Jar（必须打进发布 jar）

`implementation files(...)` 只覆盖 Gradle 开发 classpath。玩家把 mod jar 丢进 `mods/` 时，FML 的模块类加载器看不到这些类，会在 `GunMod` 构造期直接崩：

`java.lang.NoClassDefFoundError: org/luaj/vm2/LuaError`（`ModItems` → `ModernKineticGunItem`）。

因此 `build.gradle` 对下列本地 jar 声明 `implementation`，再经 `jarJarPrepare_*` 盖上 `Automatic-Module-Name` 后 `jarJar`（FML `META-INF/jarjar/`）。ModDevGradle 2 拒绝嵌入没有 JPMS 名的本地文件；这与官方 MDK「local file jarJar」示例一致。

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| `libs/luaj-jse-3.0.1.jar`（`org.luaj.vm2`） | 枪包 Lua 脚本（开火/换弹/动画状态机） | MIT（LuaJ） |
| `libs/commons-math3-3.6.1.jar` | 后坐力样条插值（`GunRecoil`） | Apache-2.0 |
| SimpleBedrockModel v1 | 基岩版几何渲染；本仓库以源码形式 vendored（`com.github.mcmodderanchor.simplebedrockmodel`） | 上游 GPL-3.0 谱系 |

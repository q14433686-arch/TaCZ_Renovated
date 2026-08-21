# 依赖与许可证清单

本文件记录本仓库及其直接依赖的许可证。发布二进制时必须随附对应源码（GPL-3.0 义务）。

## 本模组

| 组件 | 许可 |
|---|---|
| 本仓库代码（NeoForge 26.2 移植） | GPL-3.0-only |
| 上游代码谱系 MCModderAnchor/TACZ、Sh1roCu/TACZ-Refabricated、q14433686-arch/TaCZ_Refabricated_Unofficial、MUKSC/TACZ-1.21.1 | GPL-3.0 |
| 原版枪模资源（模型/贴图/音效） | CC BY-NC-ND 4.0 |

## 构建骨架

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| NeoForge `26.2.0.64` | 模组加载器 | LGPL-2.1（NeoForged） |
| Minecraft 26.2 | 游戏本体（开发依赖，不 redistributable） | Mojang EULA |
| `net.neoforged.moddev` 2.0.144（ModDevGradle） | 构建插件 | NeoForged |
| MDK-26.2-ModDevGradle | 构建脚本模板 | NeoForge MDK template license |
| Gradle 9.2.1 Wrapper | 构建 | Apache-2.0 |

## 运行时 Jar-in-Jar（必须打进发布 jar）

`implementation files(...)` 只覆盖 Gradle 开发 classpath。玩家把 mod jar 放进 `mods/` 时，
FML 的模块类加载器看不到这些本地库，会在 `GunMod` 构造期触发
`NoClassDefFoundError`。因此 `build.gradle` 先为本地 jar 写入
`Automatic-Module-Name`，再通过 ModDevGradle `jarJar` 放入发布物的 `META-INF/jarjar/`。

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| `libs/luaj-jse-3.0.1.jar`（`org.luaj.vm2`） | 枪包 Lua 脚本（开火/换弹/动画状态机） | MIT（LuaJ） |
| `libs/commons-math3-3.6.1.jar` | 后坐力样条插值 | Apache-2.0 |
| SimpleBedrockModel v1（源码 vendored） | 基岩版几何渲染 | 上游 GPL-3.0 谱系 |

## 可选 compile-only / 运行时兼容

这些 Mod 不会被打进 TaCZ jar；玩家按需单独安装。版本、artifact 与验证状态见
[`COMPATIBILITY.md`](COMPATIBILITY.md)。

| 组件 | 许可 |
|---|---|
| Cloth Config | LGPL-3.0 |
| Player Animation Library | MIT |
| Controllable | MIT |
| Shoulder Surfing Reloaded | MIT |
| JEI | MIT |
| REI | MIT |
| Architectury API | LGPL-3.0 |
| Iris | LGPL-3.0 |
| Carry On | LGPL-3.0 |
| First-person Model | MIT |

实际发布前仍需对最终解析到的 artifact 内许可文件做一次归档核对；本文不是对第三方许可
条款的替代。

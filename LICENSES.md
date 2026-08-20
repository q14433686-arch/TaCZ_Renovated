# 依赖与许可证清单

本文件记录本仓库及其直接依赖的许可证。发布二进制时必须随附对应源码（GPL-3.0 义务）。

## 本模组

| 组件 | 许可 |
|---|---|
| 本仓库代码（NeoForge 26.1.2 移植） | GPL-3.0-only |
| 上游代码谱系 MCModderAnchor/TACZ、Sh1roCu/TACZ-Refabricated、q14433686-arch/TaCZ_Refabricated_Unofficial、MUKSC/TACZ-1.21.1 | GPL-3.0 |
| 原版枪模资源（模型/贴图/音效，待工作包后续引入） | CC BY-NC-ND 4.0 |

## 构建骨架（工作包①）直接使用

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| NeoForge `26.1.2.97` | 模组加载器 | LGPL-2.1（NeoForged） |
| Minecraft 26.1.2 | 游戏本体（开发依赖，不 redistributable） | Mojang EULA |
| `net.neoforged.moddev` 2.0.144（ModDevGradle） | 构建插件 | NeoForged |
| MDK-26.1.2-ModDevGradle | 构建脚本模板 | NeoForge MDK template license |
| Gradle 9.2.1 Wrapper | 构建 | Apache-2.0 |

工作包①为空 mod，尚未引入 SimpleBedrockModel、luaj、commons-math3 等运行时重打包依赖。后续工作包引入时在此追加。

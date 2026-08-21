# Timeless and Classics Zero — NeoForge 26.1.2（非官方移植）

TaCZ（Timeless and Classics Zero）枪械 mod 的 **Minecraft 26.1.2 + NeoForge** 非官方移植。
代码开源（GPL-3.0-only），谱系可审计。

> Unofficial NeoForge 26.1.2 port of TaCZ. Documentation is in Chinese; the mod itself ships the upstream localizations.

> **⚠️ 非官方版本。** 一切问题请到[本仓库 Issues](https://github.com/q14433686-arch/tacz-1.1.8-neoforge.26.1.2.r0-sources/issues) 反馈，
> **不要**向原作者（MCModderAnchor / Serene Wave Studio）报 bug。

## 安装

| 要求 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.x（release 通道；开发基于 26.1.2.97） |

把 mod jar 放进 `mods/` 即可，**无必装前置**。首次启动会把默认枪包解压到 `游戏目录/tacz/`。

按 TaCZ 1.1.8 制作的枪包可以直接用（`>=1.1.8` 版本检查照常通过）。

可选搭配（图形配置界面要 Cloth Config，光影要 Iris，第三人称动画要 Player Animation Library 等）：
完整的可选 mod 矩阵、验证过的版本号和已知限制见 **[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)**。

## 与其它 TaCZ 版本的差异

- 瞄具采用 26.1 线 depth-aperture 方案；装 Iris 时自动走兼容分支，不装则完全不加载 Iris 相关代码。
- **未内置 LRTactical**（近战/投掷物框架）：依赖 `lrtactical` 的枪包能装载、枪械部分可用，
  但近战/投掷等 LR 道具不可用。

## 反馈 bug

发 [Issues](https://github.com/q14433686-arch/tacz-1.1.8-neoforge.26.1.2.r0-sources/issues)，请附：

1. `logs/latest.log`（必带；崩溃再附 crash report）
2. mod 列表与枪包列表
3. 复现步骤

## 从源码构建

```bash
./gradlew build      # 需要 JDK 25，产物在 build/libs/
```

开发环境、项目规则与文档结构见 **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)**。
版本历史见 **[CHANGELOG.md](CHANGELOG.md)**。

## 许可

- 代码：**GPL-3.0-only**。发布二进制必须随附完整对应源码，保留原作者版权声明。
- 原版枪模资源：**CC BY-NC-ND 4.0**（沿用上游）。

上游谱系（MCModderAnchor → Sh1roCu → q14433686-arch Fabric 26.x → 本仓库）与第三方依赖清单见
[LICENSES.md](LICENSES.md)。

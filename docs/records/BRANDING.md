# 品牌图标决策（2026-08-22）

> 冻结快照。本仓库使用**原创** `icon.png` / `logo.png`，不使用、不改绘官方 TaCZ 图标。

## 为什么要换

- 官方 `icon.png`（256² 工作台场景）与 `logo.png`（`TAC ZERO` 字标）是原作美术，
  随默认资源按 **CC BY-NC-ND 4.0** 声明——**禁止再创作**。把两张拼在一起即违约。
- Fabric 姊妹项目 [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
  原样使用官方那一对文件（hash 一致）。本仓库若再原样拷贝，Mods 列表 / GitHub /
  日后平台页会和官方、姊妹项目三家撞脸，也削弱 README 里的「非官方」声明。
- 更名前的本仓库甚至没有 `logoFile`，Mods 菜单是空白。

## 采用方案

青色四段瞄具环 + 中心铜色 **R** + `TACZ` / `RENOVATED` 字标。

- 青色环：和 TaCZ 家族的瞄具/青色语言有亲缘，但构图不是官方 `TAC ZERO` 圆标。
- 铜色 R：Renovated /「锻修」的区分色，对上 NeoForge 社区的 renovate 行话。
- 矢量脚本生成：`scripts/generate_branding.py`（Pillow + DejaVu Sans Bold）。
  不把官方 PNG 当输入，不采样官方像素。

| 文件 | 用途 | 尺寸 |
|---|---|---|
| `src/main/resources/icon.png` | 方形头像（README / 平台） | 512×512 |
| `src/main/resources/logo.png` | NeoForge `logoFile`（Mods 详情横幅） | 1280×360 |

`neoforge.mods.toml`：`logoFile="logo.png"`，`logoBlur=false`（几何标不要被默认模糊）。

## 明确不做

- 不改、不裁、不叠官方 `icon.png` / `logo.png`。
- 不把官方场景图当本仓库头像。
- 不把这套标同步进 Fabric 姊妹项目——两边要能一眼分开。

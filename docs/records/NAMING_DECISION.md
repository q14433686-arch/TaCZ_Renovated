# 命名决策记录（2026-08-21）

> 冻结快照。项目显示名自本日起为 **TaCZ: Renovated**。

## 背景

一期（26.1.2 Beta-1）完成后，项目实质上已是按 26.x 语义重写的独立代码库：
游戏语义全部来自姊妹项目 TaCZ_Refabricated_Unofficial（Fabric 26.x），
MUKSC/TACZ-1.21.1 仅贡献加载器习语（宪章 §2 边界，渲染禁抄），26.2 起将完全退场。
旧名"TaCZ NeoForge 26.1.2（非官方移植）"把版本号焊进了名字，也无品牌可言。

## 硬约束

1. **modId 永远是 `tacz`**（枪包 `>=1.1.8` 依赖检查解析该 id），只改显示名。
2. 名字必须能看出非官方，并与 CurseForge 无源码的 `tacz-port`（guilhermez1989）拉开距离。
3. GPL 义务与名字无关：credits、版权声明、谱系文档照留。

## 候选与核查（联网核查日 2026-08-21）

| 候选 | 结论 | 理由 |
|---|---|---|
| **TaCZ: Renovated** | ✅ **采用** | 与姊妹项目 Refabricated 构词法对仗；"renovated" 是 NeoForged 官方仓库 topics 之一（社区行话 = renovated Forge），圈内人秒懂"NeoForge 版"；检索无占用 |
| TaCZ: Kinetic | 次选备用 | 取自本 mod 核心概念（`modern_kinetic_gun` / `EntityKineticBullet`）；不绑加载器，可做未来多加载器伞品牌；本次不用 |
| TaCZ: Reforged | ❌ | 语义反向（原版 TaCZ 就是 Forge，"re-forge"像回迁 Forge） |
| TaCZ NeoForge (Unofficial) | ❌ | 功能式无品牌，与 MUKSC 的 Modrinth 命名风格（`[UNOFFICIAL] TaCZ 1.21.1 NeoForge Port`）撞车 |

同日检索现状：`TaCZ: Refabricated` = Sh1roCu（Modrinth 47.5 万下载）；
`[UNOFFICIAL] TaCZ 1.21.1 NeoForge Port` = MUKSC；"TACZ Renovated" 无占用。

## 变更范围（本次提交）

- `gradle.properties` `mod_name`、`neoforge.mods.toml` description；
- `README.md` 全文按姊妹项目规格重写（标题、双语免责、版本导航、枪包指引）；
- `docs/DEVELOPMENT.md` 谱系树重画为双亲结构（refab 主、MUKSC 辅）；
- 新增根 `AGENTS.md` 与 `scripts/check_release_consistency.sh`（引自姊妹项目的约定）。

**不变**：modId `tacz`、`mod_version` 串、包名 `com.tacz.guns`、jar 命名、全部许可与 credits。

发布页副标题固定语：
*"Unofficial NeoForge 26.x port of Timeless and Classics Zero — open source, auditable GPL lineage."*

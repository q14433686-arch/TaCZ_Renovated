# 发布物料：TaCZ: Renovated 26.1.2_R1

> 使用说明：§1 是 GitHub Release 正文（整段复制）；§2 是发布操作清单；
> §3 是平台规则分析（为什么首发只上 GitHub）。
> 文案纪律：只声称实测过的（AGENTS §2）；首行环境行（姊妹项目发布惯例）。

---

## §1 GitHub Release 正文（tag 建议：`26.1.2_R1`，标题：`TaCZ: Renovated 26.1.2 R1`）

**环境：Minecraft 26.1.2 · NeoForge 26.1.2.x（开发基于 26.1.2.97）· 无必装前置 · 版本串 `1.1.8+neoforge.26.1.2.R1`**

> **Unofficial NeoForge port of TaCZ (Timeless & Classics Guns: Zero). Not an
> official TaCZ release; not reviewed or endorsed by the TACZ Dev Team. GPL-3.0.**
>
> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。
> 问题请报本仓库 Issues，不要打扰原作者。**

### 这是什么

TaCZ 枪械 mod 的 Minecraft 26.1.2 NeoForge 移植首个发布版，代码开源（GPL-3.0-only）、
谱系可审计。游戏语义源自姊妹项目
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)（Fabric 26.x）。

### 内容

- **完整枪械玩法**：注册/数据/网络/弹道/枪包装载 + depth-aperture 瞄具、
  第一人称 Feature Rendering。按 TaCZ 1.1.8 制作的枪包直接可用（`>=1.1.8` 检查照常通过）。
- **LRTactical 内置框架**：投掷物/近战/引爆器/消耗品四类物品、五类投掷行为、
  tooltip/HUD/冷却遮罩反馈层——依赖 `lrtactical` 的内容包完整可用。
  范围界定：flash_shield 未含；**原作美术零打包**，道具外观由内容包提供，
  无内容包时显示原版占位模型。
- **可选集成**：Cloth Config、Iris、Player Animation Library、Controllable、
  Shoulder Surfing Reloaded、JEI/REI、Carry On、FirstPerson Model——
  验证版本号与状态矩阵见
  [docs/COMPATIBILITY.md](https://github.com/q14433686-arch/TaCZ-Renovated/blob/main(26.1.2)/docs/COMPATIBILITY.md)。

### 实测覆盖（全部有日志归档，docs/records/）

✅ 单机 · ✅ 局域网双客户端 · ✅ 专用服务器（生产 jar + 双客户端，含 /give、
工作台合成、枪包热重载、LR 全道具专项）
❌ 未测试：面板服、Velocity 代理、混合服（Youer/Arclight 系）、Geyser——
这些环境的问题**须先在原生 NeoForge 专服复现**后再提交。

### 安装

1. Minecraft 26.1.2 + NeoForge 26.1.2.x；
2. jar 放入 `mods/`（无必装前置）；首次启动默认枪包自动解压到 `游戏目录/tacz/`；
3. 第三方枪包放 `游戏目录/tacz/`；联机需**双端安装**同一枪包
   （服务端 `/tacz reload` 生效；客户端新增包按 F3+T 重载）。

### 反馈

[Issues](https://github.com/q14433686-arch/TaCZ-Renovated/issues) 按模板提交，
必附完整 latest.log（联机问题双端都要）。

### 许可与源码

- 代码 **GPL-3.0-only**：本 Release 的 Source code 归档即完整对应源码
  （构建脚本、文档、审计记录齐全）；
- 默认枪包资源 **CC BY-NC-ND 4.0**（沿用上游声明）；内嵌 LuaJ（MIT）、
  commons-math3（Apache-2.0），详见仓库 `LICENSES.md`；
- 谱系：MCModderAnchor/TACZ → Sh1roCu/TACZ-Refabricated →
  TaCZ_Refabricated_Unofficial → 本仓库（LRTactical 原作：LesRaisins-Studios，
  Programmer xjqsh / Artist LeComte，代码 GPL-3.0）。

本项目按"原样"提供，不附带担保。

---

## §2 发布操作清单（发起人执行）

1. 合并 PR #6（`arena/01a023bf-...` → `main(26.1.2)`）；
2. 本地最终构建：`./gradlew build`，L0 静态自检
   （jar 内 `META-INF/jarjar/` 含 luaj + commons-math3、四个 mixin json、AT、
   mods.toml 版本串 = `1.1.8+neoforge.26.1.2.R1`）；
3. `bash scripts/check_release_consistency.sh --strict` 必须通过；
4. GitHub → Releases → New release：tag `26.1.2_R1`（对准 main(26.1.2) 合并后
   commit）、标题 `TaCZ: Renovated 26.1.2 R1`、正文粘 §1、附件上传构建出的
   mod jar（Source code zip/tar 由 GitHub 自动附带，即 GPL 对应源码）；
5. 发布后回填 README「选择你的版本」表的 Release 链接（此前占位文案
   "Releases 发布前请从源码构建"可删）。

## §3 平台规则分析（2026-08-22 发起人勘定后修订）

| 平台 | 判定 | 依据 |
|---|---|---|
| **GitHub Release** | ✅ 首发 | 源码仓库自发布，GPL 义务天然满足 |
| **Modrinth** | ✅ 可发 | 默认枪包资产为 CC BY-NC-ND **原样承载**（未修改）——ND 禁止的是演绎后再分发，不禁止逐字节再分发，无授权障碍（发起人勘定；早先引用姊妹项目 DISCOVERABILITY §4 的"障碍"判断据此作废）。运营注意：NC 条款下建议**关闭项目端货币化**（Modrinth 的 monetization 开关），规避"商业性使用"争议 |
| **CurseForge** | ✅ 可发 | 同上；建议**不领取 Author Rewards Points**（同 NC 理由）。上架本身即是对无源码 `tacz-port` 的最好回应：同一平台、全链开源 |
| **MCMod 百科等社区** | ✅ | 介绍页 + 链接 |

三平台共同红线（每个描述页必须包含）：非官方声明 + 源码仓库链接（GPL）+
"问题报本仓库、勿扰原作者" + 实测覆盖如实分级。

---

## §4 Modrinth 上架物料

**见 [`Modrinth.md`](Modrinth.md)**（规则速查 + 标题/Summary/Description 全文 + 字段设置）。

> 勘误：本文件早先版本建议的标题 `TaCZ: Renovated` 与含 "TaCZ" 的 Summary
> 违反 Modrinth §5.2/§5.3，已废止；以 Modrinth.md 为准
> （标题 `[UNOFFICIAL]TaCZ Renovated`，Summary 不含标题词）。

## §5 CurseForge 上架物料

**见 [`CurseForge.md`](CurseForge.md)**（规则速查 + 名称/Summary/Description 全文 + 字段设置）。

> 勘误：早先建议的 `TaCZ: Renovated [Unofficial NeoForge Port]` 含 filler
> 信息违反 CF 命名规则，已废止；描述内不得出现 GitHub Releases 等外部
> 下载链接（Source/Issues 走 Links 字段）。

## §5.5 MC 百科物料

**见 [`MCMOD.md`](MCMOD.md)**（词条判断 + 简介/正文/属性/提交附言全套）。

## §6 发布顺序建议

1. GitHub Release 先发（§2 清单）；
2. Modrinth、CurseForge 当日跟进（文件用同一个 jar）；
3. 三处发完后回填 README 版本导航表链接；
4. MCMod 百科介绍页最后做（引流到 GitHub/Modrinth）。

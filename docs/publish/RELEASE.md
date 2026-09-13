# 发布与更新文档规范：TaCZ: Renovated

> 本文件是发布文案的内部维护说明。项目级介绍保持简洁稳定；版本级变化来自对应分支的
> `CHANGELOG.md`、兼容矩阵和测试记录。禁止把多个历史版本的修复列表持续堆进平台首页。

## 1. 当前项目范围（2026-08-22）

| Minecraft | 加载器 | Java | 发布页 | 源码分支 |
|---|---|---|---|---|
| 26.2 | NeoForge | 25+ | [26.2_R1](https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.2_R1) | [`26.2`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/26.2) |
| 26.1.2 | NeoForge | 25+ | [26.1.2_R1](https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.1.2_R1) | [`26.1.2`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/26.1.2) |
| 1.21.11 | NeoForge | 21+ | [1.21.11_R1](https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/1.21.11_R1) | [`1.21.11`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/1.21.11) |

- 必需前置：无；不同 Minecraft 版本的 jar 不能混用。
- 本项目 CurseForge 页面为 [Project ID 1663324](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)。
- Fabric 版本由[姊妹项目 TaCZ Refabricated Unofficial](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated)提供。
- 三个版本共用项目级边界，但**测试结论不自动跨版本继承**。

## 2. 文案分层

| 层级 | 文件 / 平台 | 应写内容 | 不应写内容 |
|---|---|---|---|
| 项目级 | `CurseForge.md`、`Modrinth.md`、`MCMOD.md` | 项目定位、支持范围、安装、长期边界、来源与许可 | 某次提交的详细修复、过期测试结论 |
| 版本级 | 平台文件 Changelog、GitHub Release | 该构建的变化、准确环境、已核验项目、升级注意 | 其他分支未经验证的结论 |
| 仓库活文档 | 各分支 README、CHANGELOG、兼容矩阵 | 当前实现与证据索引 | 平台营销性措辞 |
| 审计记录 | `docs/records/` | 当时的测试和 API 证据 | 事后覆盖改写 |

更新顺序必须是：**代码/配置 → 分支活文档 → Release notes → 平台文件 Changelog → 必要时项目级介绍**。

## 3. GitHub Release 正文模板

```markdown
# TaCZ: Renovated — [[Minecraft 版本]] / NeoForge

> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。
> 本移植的问题请提交到本仓库，不要打扰原作者。**

## 环境

- Minecraft：**[[版本]]**
- NeoForge：**[[准确版本或范围]]**
- Java：**[[版本]]+**
- Mod：**`[[gradle.properties 中的完整 mod_version]]`**
- 必需前置：**无**

不同 Minecraft 版本的文件不能混用。

## 本次变化

- [[逐项摘自对应分支 CHANGELOG；区分新增、修复、调整和移除]]

## 已核验

- [[只列本构建实际执行并有记录的结果]]

## 已知边界

- [[列未实测、回退路径、不支持内容；不得把“未崩”写成“完全兼容”]]
- LRTactical 是部分兼容框架，不含 flash_shield 或原作完整美术资源。
- 明确依赖 TacZ:Arcana 的内容不受支持。

## 安装与枪包

将 jar 放入 `mods/`。现代枪包放入 `tacz/`；旧布局枪包备份后放入
`tacz_backup/` 并执行 `/tacz convert`。联机时枪包需要服务端和客户端同时安装；
服务端执行 `/tacz reload`，客户端按 F3+T 重载资源。

## 链接

- [源码](https://github.com/q14433686-arch/TaCZ_Renovated)
- [问题反馈](https://github.com/q14433686-arch/TaCZ_Renovated/issues)
- [Fabric 姊妹项目](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated)
- [原始 TaCZ](https://github.com/MCModderAnchor/TACZ)
- [直接上游](https://github.com/Sh1roCu/TACZ-Refabricated)
- [NeoForge 移植骨架参考（GPL-3.0）](https://github.com/MUKSC/TACZ-1.21.1)

代码 GPL-3.0-only；默认枪包资源 CC BY-NC-ND 4.0；其他组件见
[LICENSES.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/[[分支]]/LICENSES.md)。
```

## 4. 发布前核对

1. 从目标分支读取 `minecraft_version`、`neo_version`、`mod_version`，不要凭记忆填写。
2. 运行 `bash scripts/check_release_consistency.sh --strict`；必须通过。
3. 运行目标分支规定的构建与测试；未执行项明确写“未测试”。
4. 冒烟必须含 **LAN 双人加入**（第二名玩家完整进入世界）：配方/实体数据全量同步类
   缺陷只在该路径触发，单机日志零痕迹（R2 复盘：
   [`../records/R2_RELEASE_RETRO_20260903.md`](../records/R2_RELEASE_RETRO_20260903.md)）。
5. 检查 jar 内版本元数据、mixin、AT 与 jar-in-jar 依赖。
6. 新版本首发使用适配后的 `release` workflow（**已由维护者上线**，见 §4.1）；
   已有 Release 的资产维护保留 `release-assets` 模板。两者都不得绕过**禁止同名换弹**：
   热修先 bump build metadata（如 `R2 → R2.1`），新版本使用新 tag；
   世代记录（构建 commit + sha256）必须保留。操作与模板见 [`ci/README.md`](ci/README.md)。
7. 平台文件的 Minecraft / NeoForge / Java / Loader 标签与 jar 一致。
8. 文件 Changelog 只包含该版本事实，不复制别的分支的 PASS。
9. GitHub Release 正文按 §3 模板逐段填写（环境、本次变化、链接与署名），不得直接套用平台自动生成的 changelog。
10. 项目页保留非官方声明、来源（原始项目 + 语义主线 + NeoForge 骨架参考（MUKSC））、许可、姊妹项目及反馈链接。
11. CurseForge Rewards 与 Modrinth Monetization 保持关闭。
12. 发布后验证 GitHub、CurseForge、Modrinth 和 MC 百科链接没有失效。

### 4.1 按 tag 构建并创建 GitHub Release

从 1.21.11 的发布自动化适配而来，但使用本线 **Java 25 / NeoForge 26.1.2**，
保留上述版本、实机、禁止同名替换和来源署名规则。已发布的 R3 实机 PASS 来自
维护者本轮确认，见 [`R3 签收记录`](../records/R3_CONFIRMATION_2612_20260907.md)，不是跨版本继承；
当前 R3-hotfix2 的新增改动仍须在最终构建上完成实机验收。

- 正式工作流 `.github/workflows/release.yml` **已由维护者上线**；
  [`ci/release.yml`](ci/release.yml) 是逐字节一致的同源模板，本次无需重复复制。
  定义已上线不表示已经执行发布上传。
- 版本正文：[`RELEASE_NOTES.md`](RELEASE_NOTES.md)，本线 **R3-hotfix2 正文与版本头已同步**
  为 `1.1.8+neoforge.26.1.2.R3-hotfix2`。以后每次发布仍须按本线 CHANGELOG 复核；
  未定稿时可保留 `release-version: UNRELEASED` 阻止误发布，不得删除版本头门禁。
- tag 必须已存在且与版本对应：当前 **R3-hotfix2 → `26.1.2_R3-hotfix2`**；
  已发布的 `26.1.2_R3` 不得覆盖。发布 tag 应指向合并后的最终发布 commit。
  R3-hotfix2 源码/正文准备与 PR 合并不自动创建 tag 或 GitHub Release。
- 流程：checkout **tag** → `--strict` 与版本/正文预检 → Lua 回归 → `gradlew build`
  → L0 检查（精确 jar 文件、mods.toml 版本/依赖、mixin 清单及内容、AT、JarJar 登记与
  内嵌库）→ 添加构建 commit / sha256 → 创建 GitHub Release，**默认草稿**。
- 不接受分支/SHA 代替 tag，不允许更新已存在的 Release（含草稿），发布前复查远端 tag
  没有移动；不存在选择“第一个 jar”或 `--clobber` 的路径。
- 在最终 tag 代码上可先本地核对：
  `python3 scripts/verify_release.py --tag <tag> --artifact`（需 Python 3.11+、已构建 jar）。
  自动化门禁不替代 §4 的 LAN / 实机检查，也不会上传到 CurseForge 或 Modrinth。

上线步骤、`gh workflow run` 示例和失败处理见 [`ci/README.md`](ci/README.md)。

## 5. 项目级文案何时需要更新

仅在以下情况修改三站正文：

- 新增或停止支持 Minecraft / Loader；
- Java、必需前置、枪包目录或转换流程改变；
- LRTactical / Arcana 的支持边界实质改变；
- 新增长期功能类别或移除既有类别；
- 源码、下载、Issues、姊妹项目、许可链接改变。

普通 bugfix、单个兼容修复、构建号变化只更新分支 CHANGELOG 和文件 Changelog。

## 6. 文案事实纪律

- “内置部分兼容框架”不能改写成“完整 LRTactical 发行版”或“所有内容包完整可用”。
- “能够扫描/显示条目”不能改写成“枪包完全兼容”。
- “有回退路径”不能改写成“完整支持该图形后端或 shader pack”。
- 26.1.2 的专服 PASS 不自动证明 26.2 或 1.21.11 已完成同一矩阵。
- 反馈统一指向本项目 Issues，不引导用户向 TaCZ、TaCZ Refabricated 或 LRTactical 原作者报错。
- MUKSC 当前如实列为 NeoForge 移植骨架参考（辅，未采用其渲染代码）。仅当 26.2+ 真正重写网络层、使该依赖实质"退场"之后，才可把措辞降级为历史致谢——**先退场、后降词，顺序不得反过来**；退场完成前不得提前弱化或删除该席位（CI 已对三站文案与 Release 模板的 MUKSC URL 设硬断言）。

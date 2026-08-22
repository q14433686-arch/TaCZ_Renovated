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

代码 GPL-3.0-only；默认枪包资源 CC BY-NC-ND 4.0；其他组件见
[LICENSES.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/[[分支]]/LICENSES.md)。
```

## 4. 发布前核对

1. 从目标分支读取 `minecraft_version`、`neo_version`、`mod_version`，不要凭记忆填写。
2. 运行 `bash scripts/check_release_consistency.sh --strict`；必须通过。
3. 运行目标分支规定的构建与测试；未执行项明确写“未测试”。
4. 检查 jar 内版本元数据、mixin、AT 与 jar-in-jar 依赖。
5. 平台文件的 Minecraft / NeoForge / Java / Loader 标签与 jar 一致。
6. 文件 Changelog 只包含该版本事实，不复制别的分支的 PASS。
7. 项目页保留非官方声明、来源、许可、姊妹项目及反馈链接。
8. CurseForge Rewards 与 Modrinth Monetization 保持关闭。
9. 发布后验证 GitHub、CurseForge、Modrinth 和 MC 百科链接没有失效。

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

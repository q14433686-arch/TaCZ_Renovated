# TaCZ NeoForge 移植宪章（26.1.2 首发版）

> **本文档是给参与移植的所有 AI（及人类协作者）的工作合同。**
> 任何一个 AI 在写下第一行代码之前，必须完整读完本文，并通过第 8 节的入场考试。
> 本文的规则优先于 AI 自身训练数据中的任何"常识"。
>
> 编写日期：2026-08-20。作者：项目发起人 + 调研 AI（基于本仓库 docs/ 体系、
> NeoForged 官方公告/primer、CurseForge/Modrinth 公开数据逐条核实）。
>
> **状态补注（2026-08-22）**：下文 §0/§1 的版本决策是撰写时点的决策记录，此后
> 26.1.2 / 26.2 / 1.21.11 三线均已发布 R1（现状见本仓库 GitHub Releases 与
> CurseForge [Project 1663324](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)）。
> §2 参考边界与洁净室红线、§3 API 证据规则等规范性条款不受版本进度影响，持续有效。

---

## 0. 一句话目标

以 **Minecraft 26.1.2 + NeoForge 26.1.2.x（release 通道）** 为第一个目标版本，
做一个**有公开源码、谱系可审计**的 TaCZ NeoForge 移植；1.21.11 与 26.2 是后续阶段，
不在首发范围内。

---

## 1. 版本决策及理由（已定案，不再讨论）

| 候选 | 结论 | 理由 |
|---|---|---|
| **26.1.2** | ✅ **首发** | NeoForge 26.1.2.x 是 release 通道（26.2 的 NeoForge 截至 2026-07 仍是 beta，API 随时破坏性变更）；本仓库有现成的 26.1.2 Fabric 分支和整套 26.2→26.1.2 语义差异文档；26.1+ 游戏本体已去混淆，AI 可直接读源码查证；26.1.2 线的瞄具用 depth-aperture 方案，不依赖 26.2 线那套高危的 Iris GL 掩码桥 |
| 1.21.11 | ⏸ 暂缓 | 混淆时代末代版本（要背 mappings 心智负担），生态正整体迁往 26.x，属自然萎缩版本；视需求决定是否回移 |
| 26.2 | ⏸ 二期 | 等 NeoForge 26.2 转 release 再做；届时 Aperture（Iris 的 Vulkan 继任者）大概率也已发布，光影兼容按 `docs/` 中 ShaderCompat 接口方案一并处理 |

**市场空缺核实（2026-08）**：CurseForge 上存在一个自称 26.1.2 NeoForge 的
"Unofficial TaCZ Port"（作者 guilhermez1989，项目 slug `tacz-port`），但它
**无任何公开源码**、jar 体积 103.6 MB（正常量级的两倍）、标 GPLv3 却不提供源码
（已处于违反 GPL 的状态）、仅一个 Beta 文件且一次性上传后无维护。
它不构成"生态已被占据"，反而证明该位置空缺且有真实需求。

---

## 2. 参考仓库与用途边界（每个仓库只能抄什么）

移植 = **MUKSC 的加载器骨架 × 本仓库的 26.x 游戏语义**。谁越界谁返工。

| 仓库 | 是什么 | ✅ 允许参考 | ❌ 禁止参考 |
|---|---|---|---|
| [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)（本仓库，`26.1.2` 分支为主、`26.2(main)` 为辅） | 26.x Fabric 移植，游戏语义的唯一权威 | 渲染层架构（26.1.2 的 depth-aperture 瞄具、HAND_TRANSLUCENT 分类）、codec 化配方兼容层（`RecipeCompat`）、数据组件迁移、docs/ 全部移植经验 | Fabric API 表面（事件、注册、网络写法）——NeoForge 侧全部重写 |
| [MUKSC/TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)（分支 `neoforge/1.21.1`） | 1.21.1 NeoForge 移植，加载器习语的唯一权威 | NeoForge 事件总线用法、`DeferredRegister`、payload 网络、AT 配置、`neoforge.mods.toml`、Kotlin DSL 构建结构 | **渲染代码一行都不能抄**（1.21.1 是旧渲染纪元：stencil 还在、无 Feature Rendering）；任何游戏类的 API 签名（1.21.1↔26.1.2 之间隔着两轮大改名） |
| [MCModderAnchor/TACZ](https://github.com/MCModderAnchor/TACZ)（1.20.1 Forge 官方源） | 谱系源头 | 业务逻辑原始意图存疑时的最终仲裁 | 一切 API 写法（太老） |
| [Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated)（1.21.1 Fabric） | 本仓库的直接上游 | 一般不需要；本仓库 docs 已消化其内容 | 同上 |

### 2.1 洁净室条款（红线，违反即整段代码作废重写）

> **禁止任何 AI 以任何形式接触 CurseForge `tacz-port`（guilhermez1989）的 jar。**
> 不下载、不反编译、不"看看它怎么解决某个问题"、不引用任何声称来自它的代码片段。

理由：该 jar 无源码、来源不可审计。我们的合法性根基是每一环都有公开源码的
GPL 谱系（MCModderAnchor → Sh1roCu → 本仓库 → 新 port；MUKSC 平行一环）。
一旦掺入反编译自不明 jar 的代码，整个项目将无法自证清白。
AI 卡壳时"找个现成实现看看"是本能动作——**本条款显式封死这条路**。
卡壳的正确动作见第 7.3 节。

---

## 3. 事实来源层级（回答任何 API 问题时的查证顺序）

**规则：任何 API 调用，必须能在下述 ①/② 级来源中指认出确切的类与方法签名，
并在代码评审/交接文档中附上证据（`类名#方法名(参数)` + 来源）。
凭训练数据记忆写 API = 直接打回。**

1. **① 26.1.2 未混淆游戏源码（唯一真理）**
2. **② NeoForge 官方 primer 链**（https://docs.neoforged.net/primer/ ）
3. **③ 本仓库 docs/**
4. **④ MUKSC 源码**：仅限第 2 节表格允许的范围。
5. **⑤ NeoForged 官方文档**（https://docs.neoforged.net/docs/ ）：加载器概念性问题。
6. **⑥ AI 训练数据：禁止作为 API 依据。**

---

## 4–10

完整条文以项目发起人下发的宪章原文为准。工作包①已按第 4.1 节（无 mappings）、第 5 节（ModDevGradle / `neoforge.mods.toml` / `@Mod` + `Dist.CLIENT`）、第 6 节工作包①验收、第 7.4 节 SemVer `+` 陷阱执行。

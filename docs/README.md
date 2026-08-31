# TaCZ: Renovated 26.2 文档索引

当前版本：`1.1.8+neoforge.26.2.R2`（R2 候选，待发布命令）。

一条总纪律：**不要把冻结记录当成当前状态，也不要把工作包过程写回根 README。**
当前口径以“开发与发布”这一节为准；带日期的文件是档案，原则上写完不改结论。

## 用户文档

| 文档 | 用途 |
|---|---|
| [`../README.md`](../README.md) | 支持环境、安装、功能边界和入口导航 |
| [`../COMPATIBILITY.md`](../COMPATIBILITY.md) | 可选 Mod 与图形后端的逐项状态 |
| [`GUNPACKS.md`](GUNPACKS.md) | 枪包双端安装、重载、版本谓词与故障判读 |
| [`DEDICATED_SERVER_TEST.md`](DEDICATED_SERVER_TEST.md) | 专服搭建和 L0-L4 多人验收 |
| [`../LICENSES.md`](../LICENSES.md) | 代码、资源与依赖许可 |

## 开发与发布

| 文档 | 用途 |
|---|---|
| [`DEVELOPMENT.md`](DEVELOPMENT.md) | JDK/Gradle、构建、运行、源码权威和验证纪律 |
| [`PORTING_STATUS.md`](PORTING_STATUS.md) | 当前候选真实状态与剩余闸门 |
| [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) | **R2** 发布前逐项检查；未全部关闭不得发布 |
| [`../CHANGELOG.md`](../CHANGELOG.md) | 对外版本变更；R2 条目待发布命令 |
| [`../AGENTS.md`](../AGENTS.md) / [`../CHARTER.md`](../CHARTER.md) | AI/人类协作规则与洁净室红线 |

## 子系统口径

| 文档 | 用途 |
|---|---|
| [`MESH_LOADER.md`](MESH_LOADER.md) | 内置 TacZ Mesh Loader：第 0/1/2 步机制、配置表、验证清单 |

## 档案：调查（冻结，供谱系与回归定位）

`investigations/` 下是工作包证据与路线图，**只追加不改结论**：

| 文件 | 内容 |
|---|---|
| `WP262_0_EVIDENCE.md` → `WP262_5_EVIDENCE.md` | 26.2 六个工作包证据（transfer API、build skeleton、非渲染 API、Feature Rendering/ocular mask/Iris/Vulkan、可选 Mod、发布准备） |
| `WP01_EVIDENCE.md` … `WP07_LRTACTICAL_PLAN.md` | 26.1.2 历史证据与 LR 计划 |
| `TML_PERF_DIRECTIONS_2026_08_29.md` | Mesh Loader 性能路线图，第 2 段 GPU 烘焙的依据 |

## 档案：同步与取证记录

`records/` 下记录某一 commit、环境或用户回执。与姊妹分支
`TaCZ_Refabricated_Unofficial` 的四轮同步取证按时间编号：

- `REFAB_SYNC_01A04E96_20260830.md` / `_R2_20260830.md` / `_R3_20260831.md` /
  `_R4_20260831.md` —— 一轮一份，含「搬了什么 / 不搬什么 + 理由 / 回单」；
- `REFAB_SCOPE_PIP_SYNC_20260830.md`、`REFAB_SCOPE_PIP_FPS_DECAY_20260829.md`
  —— PIP 同步取舍与帧率衰减调查；
- `SCOPE_MASK_HULL_SLOPESPACE_20260827.md`、`SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md`、
  `SCOPE_RING_IRIS_OVERLAY_20260830.md` —— 目镜掩码与光影的三份取证；
- `SERVER_TEST_*.md`、`BRANDING.md`、`RELEASE_CHECKLIST_26_2_R1.md` —— 实测回执、
  品牌决策与 R1 归档清单。

若版本仅重新定名，应在记录中追加命名说明，不应伪造当时实际运行的版本字符串。

## 给姊妹分支的回单

`handoff/` 下是**写给对方维护者**的文档：只讲机制与证据，不贴代码。对方侧是
Fabric 分支，加载器表面不同不代表游戏语义不同 —— 回单一律写清「这条是加载器差异
还是真功能差异」。

## 工程与 CI

`ci/` 存放 workflow 的仓库内副本（与 `.github/workflows/` 逐字一致），便于在
`.github/` 之外评审配方。

| 配方 | 作用 |
|---|---|
| `compile-check.yml` | 编译验证闭环；日志经 Contents API 写回 `build-reports/compile-java.log` |
| `changelog.yml` | **收尾用**：按提交前缀生成 CHANGELOG 条目草稿（见下） |

## 收尾流程（每版必做）

1. 跑 `bash scripts/check_release_consistency.sh --strict`，版本号三处必须一致；
2. 生成条目草稿：
   `bash scripts/generate_changelog.sh <since-ref> --version 1.1.8+neoforge.26.2.R2`
   （或在 Actions 里手工触发 `changelog` workflow，产物可下载）；
3. **人工核对草稿** —— 脚本只按 `feat/fix/docs/...` 前缀归类，不负责判断机制描述
   是否准确，也不负责补「谁在什么时候实机验证过」；
4. 并入 `CHANGELOG.md`，并把 `RELEASE_CHECKLIST.md` H 节的收尾项勾掉。

写不下的结论才进 `records/`；**禁止把未实机验证的结果写成 PASS。**

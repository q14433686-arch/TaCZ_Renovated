# TaCZ: Renovated 26.2 文档索引

当前版本：`1.1.8+neoforge.26.2.0.R1`（Unreleased candidate）。

文档按用途分为以下几类。不要把冻结记录当成当前状态，也不要把工作包过程写回根 README。

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
| [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) | R1 发布前逐项检查；未全部关闭不得发布 |
| [`../CHANGELOG.md`](../CHANGELOG.md) | 对外版本变更；当前条目保持 Unreleased |
| [`../AGENTS.md`](../AGENTS.md) / [`../CHARTER.md`](../CHARTER.md) | AI/人类协作规则与洁净室红线 |

## 26.2 工作包证据

- `WP262_0_EVIDENCE.md`：transfer API 前置卫生。
- `WP262_1_EVIDENCE.md`：26.2 build skeleton 与版本来源。
- `WP262_2_EVIDENCE.md`：非渲染 API、R1 多人修复回流与 AT。
- `WP262_3_EVIDENCE.md`：Feature Rendering、ocular mask、Iris 与 Vulkan。
- `WP262_4_EVIDENCE.md`：可选 Mod artifact/API 证据。
- `WP262_5_EVIDENCE.md`：发布准备与阻塞项。

`WP01`–`WP07` 是 26.1.2 历史证据，仅用于谱系和回归定位。

## 冻结记录

`records/` 下文件记录某一 commit、环境或用户回执，完成后原则上不改写结论：

- 26.1.2 R1 LAN / dedicated / gun-pack 实测；
- R1 → 26.2 必要代码修复回流；
- 26.2 LR 内置层前滚、API descriptor 与待测边界（`LR_R1_SYNC_26_2_20260822.md`）；
- LR 合入前的 26.2 L0-L3 用户回执；
- 过程 changelog 草稿与 handoff。

若版本仅重新定名，应在记录中追加命名说明，不应伪造当时实际运行的版本字符串。

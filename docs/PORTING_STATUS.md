# 移植状态

目标版本：Minecraft **26.2** + NeoForge **26.2.0.64**（release）。
当前源码版本：**1.1.8+neoforge.26.2.0.R1**。
状态：**未发布 R1 候选；定名前同代码 L0-L3 用户 PASS，R1 metadata 产物待快速复核。**

> 最后更新：2026-08-21。本文只记录诚实状态；README 不作为逐包进度日志。

## 26.2 工作包

| 工作包 | 已落地 | 尚缺验收 |
|---|---|---|
| WP-262-0 transfer 卫生 | `IItemHandler` for-removal 调用迁到 `ResourceHandler<ItemResource>`；删除死屏幕 | R1 最终 compile warning 复核 |
| WP-262-1 构建骨架 | MC 26.2 / NF 26.2.0.64 / Java 25；定名前同代码 build/专服 PASS | R1 版本展开后的 L0/L1 快速复核 |
| WP-262-2 非渲染 | Gui/HUD/AT；R1 network/getName 回流；定名前同代码 L0-L3 用户 PASS | L2.5 单独确认 |
| WP-262-3 渲染 | stage-boundary ocular mask、Feature/PiP/Gizmo/hand API、Iris bridge；编译通过 | OpenGL/Iris/Vulkan 完整 GPU 矩阵 |
| WP-262-4 可选兼容 | 26.2 坐标重钉、Carry On 2.11、FPM/NEA dormant bridge、矩阵文档 | `COMPATIBILITY.md` 全部游戏内项目 |
| WP-262-5 发布准备 | README/CHANGELOG/LICENSES/状态文档 | **发布 jar 与源码包被上述构建/实测闸门阻塞** |

证据：`docs/WP262_0_EVIDENCE.md` 至 `docs/WP262_5_EVIDENCE.md`。

首次 JDK 25 build 暴露的 9 个编译错误、离屏 scope-mask 与 26.1.2 R1 修复均已进入
当前代码。用户已对定名前同代码候选报告 build、L0-L3 **PASS**；随后只将版本 metadata
从开发标签改名为 `1.1.8+neoforge.26.2.0.R1`。冻结回执见
`docs/records/SERVER_TEST_20260821_262_R1.md`。逻辑结论不变，但最终 R1 jar 的版本展开需复核。

## 版本基线

- **26.2 R1（当前工作树）**：最初从 Beta-1 前滚，现已 cherry-pick 26.1.2 R1 的三组
  必要代码修复；不是重写。
- **26.1.2 R1（完整功能基线）**：LAN、真实专服与枪包专项实测通过；记录已同步到
  `docs/records/`。其 PASS 不能自动继承为 26.2 PASS。
- **Beta-1 / r1–r30**：历史开发基线，不再作为 26.2 完整起点。

## 当前明确边界

### 图形后端

- OpenGL ocular mask：stage-boundary target / body-reticle-viewmodel clip 已接入，**未 GPU 实测**。
- OpenGL + Iris 1.11.2：linked fragment / per-draw uniform bridge 已接入，**未 GPU 实测**。
- Vulkan：设置 `config/fml.toml: earlyWindowControl=false` 后用户启动 **PASS**；低倍准星
  mask 报告 FAIL，已拆分 reticle/body mask 状态修复，当前 HEAD 待复测。
- 其他 shader replacement / Aperture：没有已核 bridge 时未掩码回退。

### 可选 Mod

所有发布文件、source commit、pin 与未实测标记见根目录
[`COMPATIBILITY.md`](../COMPATIBILITY.md)。当前没有任何 26.2 可选兼容项被标记为用户 PASS。

### LRTactical 内置

仍继承 26.1.2 R1（自 Beta-1 起）的撤回决定：本仓库不含其四类基础物品和行为。相关历史与重启条件
见 `docs/WP07_LRTACTICAL_PLAN.md`。这次 26.2 前滚没有重新引入 LRTactical。

## 发布阻塞项

在以下项目全部完成前，仍不得发布 R1 或声称移植完成：

1. R1 产物：重跑 build + L0，并快速确认 Mod List 版本与 `Done`；
2. 枪包：单独确认 L2.5 默认包/第三方包、双端不对称安装、`/tacz reload` 与 F3+T；
3. 客户端：补齐无可选 Mod 的完整枪械/资源回归记录；
4. GPU：OpenGL、Iris、Vulkan scope-mask 矩阵；
5. `COMPATIBILITY.md` 可安装项目逐行用户实测或继续明确“未实测”；
6. 发布 jar 的 metadata/license 与对应源码 tag/归档一致性最终复核。

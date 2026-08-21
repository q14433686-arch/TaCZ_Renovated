# 移植状态

目标版本：Minecraft **26.2** + NeoForge **26.2.0.64**（release）。
当前源码版本：**1.1.8+neoforge.26.2.0.r0**。
状态：**未发布候选；scope-mask 替换后的当前 HEAD 待重新构建，运行矩阵未完成。**

> 最后更新：2026-08-21。本文只记录诚实状态；README 不作为逐包进度日志。

## 26.2 工作包

| 工作包 | 已落地 | 尚缺验收 |
|---|---|---|
| WP-262-0 transfer 卫生 | `IItemHandler` for-removal 调用迁到 `ResourceHandler<ItemResource>`；删除死屏幕 | 当前 HEAD 重新编译 |
| WP-262-1 构建骨架 | MC 26.2 / NF 26.2.0.64 / Java 25 / r0 / 官方 MDK 对齐 | 当前 HEAD `build`；`runServer` Mod List / `Done` |
| WP-262-2 非渲染 | Gui screen 重组、文本颜色、HUD loader event、AT 最小化、common descriptor | 专服枪包装载数字与 26.1.2 对比 |
| WP-262-3 渲染 | 26.2 stage-boundary ocular mask、Feature/PiP/Gizmo/hand API、Iris bridge | 当前 HEAD 编译；OpenGL/Iris/Vulkan GPU 矩阵 |
| WP-262-4 可选兼容 | 26.2 坐标重钉、Carry On 2.11、FPM/NEA dormant bridge、矩阵文档 | `COMPATIBILITY.md` 全部游戏内项目 |
| WP-262-5 发布准备 | README/CHANGELOG/LICENSES/状态文档 | **发布 jar 与源码包被上述构建/实测闸门阻塞** |

证据：`docs/WP262_0_EVIDENCE.md` 至 `docs/WP262_5_EVIDENCE.md`。

2026-08-21 的首次用户 JDK 25 `gradlew build` 暴露 9 个源码错误；commit `15e4a35`
修复后，用户对 commit `c40dab9` 报告 `compileJava` / `build` **PASS**。随后按用户裁决，
当前分支把 GL-only depth-aperture 整体替换为 refab 26.2 离屏 ocular mask；这项变更使旧
build PASS 不再覆盖当前 HEAD。新实现证据与重跑矩阵见 `docs/WP262_3_EVIDENCE.md`。

## 版本基线

- **26.2 r0（当前工作树）**：从 26.1.2 Beta-1 前滚；不是重写。
- **26.1.2 Beta-1（历史稳定出发点）**：工作包①–⑥功能基线；其用户 PASS 不能自动
  继承为 26.2 PASS。
- r1–r30：26.1.2 开发历史，详见一期证据文档与 git 历史。

## 当前明确边界

### 图形后端

- OpenGL ocular mask：stage-boundary target / body-reticle-viewmodel clip 已接入，**未 GPU 实测**。
- OpenGL + Iris 1.11.2：linked fragment / per-draw uniform bridge 已接入，**未 GPU 实测**。
- Vulkan：用户启动在 GLFW window-surface 创建阶段 **FAIL**，尚未进入首帧/TACZ mask；
  日志指向隐式 Vulkan layers/驱动/渲染 Mod 隔离项，不能记作 scope-mask 运行结果。
- 其他 shader replacement / Aperture：没有已核 bridge 时未掩码回退。

### 可选 Mod

所有发布文件、source commit、pin 与未实测标记见根目录
[`COMPATIBILITY.md`](../COMPATIBILITY.md)。当前没有任何 26.2 可选兼容项被标记为用户 PASS。

### LRTactical 内置

仍维持 26.1.2 Beta-1 的撤回决定：本仓库不含其四类基础物品和行为。相关历史与重启条件
见 `docs/WP07_LRTACTICAL_PLAN.md`。这次 26.2 前滚没有重新引入 LRTactical。

## 发布阻塞项

在以下项目全部完成前，仍不得发布 r0 或声称移植完成：

1. JDK 25：对 scope-mask 替换后的当前 HEAD 重跑 `clean compileJava` 与 `build`；
2. 专服：Mod List 可见 tacz、服务器 `Done`、默认/第三方枪包装载数字回归；
3. 客户端：无可选 Mod 的基础枪械/工作台/网络/资源回归；
4. GPU：OpenGL、Iris、Vulkan scope-mask 矩阵；
5. `COMPATIBILITY.md` 可安装项目逐行用户实测或继续明确“未实测”；
6. 发布 jar 内 jar-in-jar、metadata、license、源码 tag/归档一致性复核。

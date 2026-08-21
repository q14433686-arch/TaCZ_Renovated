# 移植状态

目标版本：Minecraft **26.2** + NeoForge **26.2.0.64**（release）。
当前源码版本：**1.1.8+neoforge.26.2.0.r0**。
状态：**未发布候选；生产 JDK 25 编译/构建已由用户报告 PASS，运行矩阵未完成。**

> 最后更新：2026-08-21。本文只记录诚实状态；README 不作为逐包进度日志。

## 26.2 工作包

| 工作包 | 已落地 | 尚缺验收 |
|---|---|---|
| WP-262-0 transfer 卫生 | `IItemHandler` for-removal 调用迁到 `ResourceHandler<ItemResource>`；删除死屏幕；用户 JDK 25 编译 PASS | removal/deprecation warning 明细仍可留档 |
| WP-262-1 构建骨架 | MC 26.2 / NF 26.2.0.64 / Java 25 / r0 / 官方 MDK 对齐；用户 `build` PASS | `runServer` Mod List / `Done` |
| WP-262-2 非渲染 | Gui screen 重组、文本颜色、HUD loader event、AT 最小化、common descriptor | 专服枪包装载数字与 26.1.2 对比 |
| WP-262-3 渲染 | 26.2 pipeline/Feature/PiP/Gizmo/hand API；GL depth-aperture；Vulkan 降级；编译 PASS | OpenGL/Iris/Vulkan GPU 矩阵 |
| WP-262-4 可选兼容 | 26.2 坐标重钉、Carry On 2.11、FPM/NEA dormant bridge、矩阵文档 | `COMPATIBILITY.md` 全部游戏内项目 |
| WP-262-5 发布准备 | README/CHANGELOG/LICENSES/状态文档 | **发布 jar 与源码包被上述构建/实测闸门阻塞** |

证据：`docs/WP262_0_EVIDENCE.md` 至 `docs/WP262_5_EVIDENCE.md`。

2026-08-21 的首次用户 JDK 25 `gradlew build` 暴露 9 个源码错误；commit `15e4a35`
按真实编译器反馈修复后，用户已报告请求的 `compileJava` / `build` 重跑 **PASS**。
修复与 descriptor 见 `docs/WP262_2_EVIDENCE.md`；该 PASS 不外推到专服、GPU 或可选 Mod。

## 版本基线

- **26.2 r0（当前工作树）**：从 26.1.2 Beta-1 前滚；不是重写。
- **26.1.2 Beta-1（历史稳定出发点）**：工作包①–⑥功能基线；其用户 PASS 不能自动
  继承为 26.2 PASS。
- r1–r30：26.1.2 开发历史，详见一期证据文档与 git 历史。

## 当前明确边界

### 图形后端

- OpenGL depth-aperture：代码与 classfile API 已迁移，**未 GPU 实测**。
- OpenGL + Iris 1.11.2：API/shader target 已核，**未 GPU 实测**。
- Vulkan：只实现“不调用 GL + 未掩码瞄具降级”，**未启动实测**。
- Aperture：未接入。

### 可选 Mod

所有发布文件、source commit、pin 与未实测标记见根目录
[`COMPATIBILITY.md`](../COMPATIBILITY.md)。当前没有任何 26.2 可选兼容项被标记为用户 PASS。

### LRTactical 内置

仍维持 26.1.2 Beta-1 的撤回决定：本仓库不含其四类基础物品和行为。相关历史与重启条件
见 `docs/WP07_LRTACTICAL_PLAN.md`。这次 26.2 前滚没有重新引入 LRTactical。

## 发布阻塞项

JDK 25 `compileJava` / `build` 已由用户报告 PASS；在以下其余项目全部完成前，仍不得发布
r0 或声称移植完成：

1. 专服：Mod List 可见 tacz、服务器 `Done`、默认/第三方枪包装载数字回归；
2. 客户端：无可选 Mod 的基础枪械/工作台/网络/资源回归；
3. GPU：OpenGL、Iris、Vulkan 降级矩阵；
4. `COMPATIBILITY.md` 可安装项目逐行用户实测或继续明确“未实测”；
5. 发布 jar 内 jar-in-jar、metadata、license、源码 tag/归档一致性复核。

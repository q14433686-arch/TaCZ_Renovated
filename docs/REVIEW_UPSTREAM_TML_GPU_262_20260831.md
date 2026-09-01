# 上游 26.2 线 TML GPU 提交审查笔记（移植源审计）

> 适用版本：Minecraft 1.21.11 + NeoForge 21.11.x。
> 本线内置 TacZ Mesh Loader 的 GPU 路径按 26.2 线 `8191f6b` / `0ea0fb6` / `9f7412e`
> 机械移植到 1.21.11 改名映射（见 `PolyMeshGpuRenderer` 类注）。移植时逐 commit 读了
> 这三个提交的 diff，本文记录其中影响本线落地形状的审查结论。时点：2026-08-31。

## A2：失败降级只置内存标志，不回写配置

26.2 线的第 1 步沿用了 `MeshyConfig.GPU_BAKING.set(false)` 作为「GPU pass 失败 →
退回 collector」的降级手段。本线**不这么做**，理由两条：

1. **绘制线程里改配置可能触发磁盘写**：GPU 绘制发生在渲染线程，
   `set(false)` 走配置系统的写回链路，等于在帧内做 I/O；
2. **用户重启后会看到「GPU 烘焙自己关了」**：一次瞬时失败（例如纹理未加载完）
   被持久化成配置变更，用户下一次启动仍在 collector 路上，且找不到原因。

本线落地形状：

- 手部路径：失败只置**会话级内存标志** `gpuDisabledThisSession = true`
  （`PolyMeshGpuRenderer#renderAtHandFlush` 的 catch 块），不回写任何配置；
- 世界路径：一开始就是**分表 + 阈值语义**（`renderAtWorldFlush`），
  失败同样只影响当帧/当会话，不进配置；
- 日志只打一条 error（含 `irisFlush=` 上下文），随后整个会话静默走 collector，
  不再逐帧刷屏。

## 与 A2 同源的日志纪律（本线一并落地）

- 纹理解析失败按**每张纹理一条**去重（`loggedTextureFailures`），
  不做一次性闩锁：旧实现会把一次瞬时取空变成整会话 EMISSIVE；
- 首帧绘制日志（`loggedFirstDraw` / `loggedFirstWorldDraw` /
  `loggedScopeWorldDraw`）各只打一条 info，供实机确认路径生效；
- 光影下拿不到 lightmap 不再整路退化 EMISSIVE：直接退回 collector
  （`PolyMeshGpuRenderer#gpuMasterUsable`）。EMISSIVE 管线在光影包眼里是
  「自发光、不受阴影」，不是光影下的合理降级态。

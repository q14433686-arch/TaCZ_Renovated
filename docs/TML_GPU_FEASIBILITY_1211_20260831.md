# TML GPU 烘焙可行性注记（1.21.11 线）

> 适用版本：Minecraft 1.21.11 + NeoForge 21.11.x。
> 本文是内置 TacZ Mesh Loader 性能边界的依据文档（`TaczPolyMeshGunModel` 类注的
> 「性能边界（如实声明）」指向本文 §5）。时点：2026-08-31。

## §1 背景

内置移植分三步：第 0 步 collector-only（能用、有预算闸门）；第 1 步无光影第一人称
GPU 静态烘焙；第 2 步 v2 光影下把 pass 画进 Iris 的手部 flush（见
`docs/TML_GPU_STEP2_HANDFLUSH_20260831.md`）；第 3 步世界语境 GPU 路。

## §2 collector 路径的成本模型

collector 路径每帧对每个可见 poly 顶点做一次 CPU 变换 + 逐顶点 VertexConsumer 调用。
成本与**可见顶点数**线性相关；36 万顶点级高模在第一人称**仍然有帧率成本**。
第 0 步内置只保证三件事：

1. 能用（模型解析、渲染正确）；
2. 不劣化无 mesh 场景（无 mesh 枪包时路径完全不触发）；
3. GUI / 世界语境有顶点预算闸门（`MeshGuiMaxVertices` / `MeshWorldMaxVertices`）。

## §3 GPU 静态烘焙：O(顶点) → O(骨骼)

GPU 路径把 cutout 顶点**常驻 GPU**：顶点留在骨骼本地坐标 + light 烘进顶点，
每帧只上传 **O(bones)** 的骨骼矩阵（`DynamicTransforms.ModelViewMat`），
顶点变换交给 shader。代价结构：

- 上传成本与骨骼数线性，与顶点数无关；
- VBO 常驻显存，成本与模型顶点数成正比（世界路径按量化光照档 LRU 缓存，
  见 §4）；
- translucent 骨骼不烘（混合顺序交给 collector），换弹 `additional_magazine`
  恒走 collector（矩阵语义不同且不是顶点热点）。

## §4 世界语境与光照 LRU

世界路径（`MeshGpuWorld`）让满服高模 mesh 枪可玩：每把枪每帧 O(bones) 矩阵上传，
而不是每顶点 CPU 变换。光照由「每模型小 LRU 缓存」服务：先量化光照
（block/sky 各 4 档），再按档缓存整套骨骼 VBO，`MeshGpuLightCacheSize`
（默认 4）控制档数。逐出的 VBO 走延迟释放池（`releaseDeferred`，
下一帧才 close），因为同帧内两个实体可能共享同一模型实例，当场 close 会让
帧末绘制引用已销毁的 buffer。

## §5 性能边界结论（如实声明）

- collector 路径：O(可见顶点) 每帧 CPU 成本；高模枪第一人称仍吃帧率；
- GPU 路径：O(骨骼) 每帧上传成本，VBO 常驻显存 —— **用显存换 CPU**，
  不是免费；
- 世界路径的顶点预算（`MeshWorldMaxVertices`）在 GPU 世界路存活时**不参与**
  （该路没有每顶点 CPU 成本要保护）；`MeshWorldFullDetailDistance` 内的近距离
  豁免保证眼前的枪永远画全模；
- 光影下两条 GPU 路（`MeshGpuUnderShaders` / `MeshGpuWorldUnderShaders`）
  默认开（维护者 2026-09-01 裁定）：「高模枪挡住太阳/月亮的那部分几何继承
  天体自发光亮度」是已知、可观测、可整键关闭的取舍。

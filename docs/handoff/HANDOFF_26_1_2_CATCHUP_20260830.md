# 把 26.1.2 拉到 1.21.11 的水平（遮光环 / 准星被光影后置 pass 吃掉）

日期：2026-08-30
来源：本仓 `arena/01a04ea3-tacz-renovated`（26.2）修遮光环时的跨分支取证
对象：本仓 `26.1.2` 分支（depth-aperture 架构，与 1.21.11 同架构）
结论：**有同样的问题，而且是同一个**——26.1.2 上有案例⑨ 的 `ocular_ring`，
却缺 1.21.11 上那套「最终覆盖」机制，于是光影包的后置 pass 会把目镜框与准星吃掉。

> 本会话被钉在 26.2 分支，不能动 `26.1.2`。这份文档 + §4 的提示词是给
> **开在 26.1.2 上的新会话**用的，粘贴即可开工。

---

## 1. 差距清单（逐文件对过）

| 文件 | 1.21.11 | 26.1.2 | 作用 |
|---|:--:|:--:|---|
| `BedrockAttachmentModel` 里的 `ocularRingPart`（案例⑨） | ✔ | ✔ | 物理目镜框无裁剪重画（地基已在，不用搬） |
| `ScopeLateReticleState` | ✔ | ✘ | 把准星延后到手部 pass 末段（Iris） |
| `scope_reticle_final.fsh` | ✔ | ✘ | 延后那遍用的无雾准星片元着色器 |
| `IrisHandRendererReticlePassMixin` | ✔ | ✘ | Iris 手部 pass 的延后钩子 |
| `ScopeFinalOverlayState` | ✔ | ✘ | **排队 + 在 Iris 最终合成之后重画**（准星 + 目镜框） |
| `scope_ring_final.fsh` | ✔ | ✘ | 目镜框那遍的无雾片元着色器 |
| `IrisFinalScopeOverlayMixin` | ✔ | ✘ | 挂在 `IrisRenderingPipeline#finalizeLevelRendering` TAIL |
| `ScopeDepthCopyState` 的 final-overlay 相关 uniform | ✔ | ✘ | `tacz_ScopeFinalOverlay` 等 |
| `ScopeRenderTypes` 的对应条目 | ✔ | ✘ | 上述管线的 RenderType |

姊妹仓库的同源提交（她是 Fabric，我们 NeoForge，**只抄语义不抄代码**）：
`6f2528c`（defer Iris reticle to late hand pass）、`828ba10`（preserve late reticle
depth for fog）、`2710c7c`（render reticle after Iris final composite）、
`189a1bd`（remap GlRenderPipeline info accessor）。

## 2. 26.1.2 上的症状（预期，未实测）

开光影包 + 开镜：目镜内圈那圈黑边（遮光环）和/或准星被光影包的 composite/final
pass 盖掉或糊掉。1.21.11 上 `scope_ring_final.fsh` 的头注释写的就是这个：
*"the ring is a physical 3D foreground layer submitted after Iris has finished all
composite/final passes"*。

与 26.2 那条（PIP 合成盖掉遮光环）是**同源不同形**：26.2 是「我们的合成盖掉它」，
depth-aperture 分支是「光影包的后置 pass 盖掉它」，解法都是**延后到最后再画一遍**。

## 3. 移植要点（26.1.2 ← 1.21.11）

- **同架构、同加载器**（都是本仓 NeoForge、都是 depth-aperture），所以这是本仓库里
  风险最低的一次搬运 —— 比跨仓库、跨加载器那几次都安全。
- **案例⑨ 已经在 26.1.2 上**（`BedrockAttachmentModel:137` 的 `ocularRingPart`、
  `:538` 的 `captureStandalonePart`、`:632` 的行内提交都在），所以只需要补
  「延后重画」那一层，不需要再动模型侧的摘除逻辑。
- **蓝本是现成的，不用自己摸索**：该功能在 1.21.11 上是**一次提交** `62532f1` 引入的，
  而且**之后没有任何后续修正**。相关的 15 个路径我已经摘出来存成
  [`26_1_2_catchup/final_overlay_62532f1.diff`](26_1_2_catchup/final_overlay_62532f1.diff)
  （1678 行）。**别 `git apply`** —— 两个分支的 `BedrockAttachmentModel` /
  `ScopeRenderTypes` 上下文不同，它是**蓝本不是补丁**。
- **四个版本差异核对点我已经替你查过了**，答案（含 26.1.2 上每一处的实际现状）在
  [`26_1_2_catchup/PORT_CONTRACT.md`](26_1_2_catchup/PORT_CONTRACT.md) §1.3 §2。
  三句话的总结：
  - `GlCommandEncoder` 的注入点两边**完全一致**，不用动；
  - `ScopeRenderTypes` 的 `clonePipeline` 26.1.2 上**已有**，配方照抄，但要先确认
    26.1.2 的 `DepthTestFunction` 有没有 `ALWAYS`（1.21.11 没有，所以搞了一套
    「`NO_DEPTH_TEST` 声明 + encoder mixin 手动 `GL_ALWAYS`」的别扭机制）；
  - `RenderSystem` 那 6 个符号在 26.1.2 本仓代码里**一个都没被用过**，必须对着
    `minecraft-merged-0d09a28b48-26.1.2.jar` 核，尤其 `getModelViewMatrix()`
    （26.2 上已改名 `getModelViewMatrixCopy()`）。
- **两处可能整条路走不通，先证实再动手**：26.1.2 上那个 Iris 版本里
  `IrisRenderingPipeline#finalizeLevelRendering()` 和
  `HandRenderer#renderTranslucent()` / 字段 `submitNodeCollector` 是否还在
  （1.21.11 只审计过 Iris **1.10.7**，26.1.2 上不一定是它）。详见 PORT_CONTRACT §1.1。

## 4. 【可直接粘贴】给 26.1.2 会话的提示词

```
本分支（26.1.2，depth-aperture 架构）落后于本仓 1.21.11 分支一整套「延后重画」
机制，请把 26.1.2 拉到 1.21.11 的水平。

【现成材料（26.2 分支上已经替你备好，先读这两个）】
docs/handoff/26_1_2_catchup/PORT_CONTRACT.md      —— 逐文件/逐 API 的移植契约
docs/handoff/26_1_2_catchup/final_overlay_62532f1.diff —— 1.21.11 的原始改动集（15 路径）
docs/handoff/26_1_2_catchup/README.md             —— 索引与用法
注意：diff 是蓝本不是补丁，别 git apply；两边的 BedrockAttachmentModel /
ScopeRenderTypes 上下文不同。四个版本差异核对点 PORT_CONTRACT §2 已经查过了，
不用再 grep，但里面标「核 jar」的仍须你对着
minecraft-merged-0d09a28b48-26.1.2.jar 核一遍。

【差距（已逐文件对过，两边都是本仓 NeoForge、同架构）】
26.1.2 缺、1.21.11 有：
  ScopeLateReticleState、scope_reticle_final.fsh、IrisHandRendererReticlePassMixin
  ScopeFinalOverlayState、scope_ring_final.fsh、IrisFinalScopeOverlayMixin
  ScopeDepthCopyState 里 final-overlay 相关的 uniform（tacz_ScopeFinalOverlay 等）
  ScopeRenderTypes 里对应的管线条目
26.1.2 已有、不用搬：BedrockAttachmentModel 里的 ocularRingPart（案例⑨ 地基）。

【为什么要搬】
26.1.2 上有物理目镜框（ocular_ring）却没有「最终覆盖」，开光影包开镜时，
目镜内圈那圈黑边（遮光环）和/或准星会被光影包的 composite/final pass 盖掉或糊掉。
1.21.11 上 scope_ring_final.fsh 的头注释写的就是这件事：
"the ring is a physical 3D foreground layer submitted after Iris has finished all
composite/final passes"。
这与 26.2 分支上刚修完的那个 bug 同源不同形 —— 那边是「PIP 的合成盖掉遮光环」
（已实机 PASS，见 docs/records/SCOPE_RING_IRIS_OVERLAY_20260830.md），
解法都是「延后到最后再画一遍」。

【搬法】
从本仓 1.21.11 分支搬语义（不要照抄代码，注意版本差异）；姊妹仓库
TaCZ_Refabricated_Unofficial 的同源提交可作为语义参照：6f2528c（defer Iris
reticle to late hand pass）、828ba10（preserve late reticle depth for fog）、
2710c7c（render reticle after Iris final composite）、189a1bd（remap
GlRenderPipeline info accessor）。

【四个版本差异核对点（动手前先 grep 确认）】
1. RenderPipeline 的 info 访问器（姊妹 189a1bd 修的就是它）；
2. GlCommandEncoder 的 depth-copy 注入点；
3. ScopeRenderTypes 的管线配方常量；
4. RenderSystem 的投影/模型视图方法名 —— 26.2 上 getModelViewMatrix() 已改名为
   getModelViewMatrixCopy()，26.1.2 上叫什么都请先确认。

【要求】
按仓库规矩：先取证再动手；源码级移植要自审并标注未验证项；不要写 PASS；
等用户实机反馈再收口。搬完请给出复测清单（开光影 / 不开光影 × 有准星 / 无准星
× 含 ocular_ring 的镜 / 不含的镜）。
```

## 5. 验收清单

1. 不开光影开镜：与改动前逐位一致（回归）。
2. 开光影开镜：目镜框与准星都在，且**在光影雾效之上**（不被糊）。
3. 含 `ocular_ring` 骨骼的镜（默认枪包 14 个中高倍镜）逐个看黑边完整。
4. 第三人称 / 腰射 / GUI 预览：不受影响（延后机制只在第一人称开镜 + Iris 时排队）。

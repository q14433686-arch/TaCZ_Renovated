# 光影下开镜帧率持续衰减（Scope PIP 二次渲染）—— 调查过程与修复

日期：2026-08-29（调查）/ 2026-08-30（本仓同步落地）
作者：姊妹分支（`q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `26.2(main)`，
commit `052e600` 及之前的调查）＋ 本仓整理
状态：**根因与修复属姊妹分支实机取证（VisualVM GC Root），本仓已同步修复代码，
但本仓未编译、未实机。** 禁止写 PASS。
下一步：本地开 `ScopePipEnable` + `ScopePipIsolatePipeline` + 光影，按 §5 复测；
若衰减仍在，说明本仓的保留链条另有分叉，回到 §2 的取证流程。

---

## 1. 症状签名

1. 触发点 = **第一次开镜**（哪怕一瞬），不是进世界；
2. 推进量 ∝ **累计开镜帧数**（不是墙钟，也不是开镜会话次数 —— 见 §4）；
3. 空闲时不恢复，隔一会再开镜从上次的位置继续掉；
4. 地板 ~7 FPS；重进存档重置全过程；
5. 仅 `ScopePipIsolatePipeline=true`（二次渲染 + 独立 Iris 管线 + 光影）时出现；
6. 卸载 Voxy 后现象不变（用户已排除 Voxy）。

---

## 2. 已排除（姊妹分支多轮探针 + 用户实验）

| 嫌疑 | 探针 | 结论 |
|---|---|---|
| 瞄具管线每帧重建 | scope 管线实例身份计数 | 无果 |
| 预热慢路径每帧重跑 | probeSlowPathRuns | 无果 |
| Iris `pipelinesPerDimension` 膨胀 | map 大小 | 无果 |
| 激活 SSBO 数量/字节 | ACTIVE_BUFFERS 计数 | 无果 |
| Blaze3D 保留集合 / SodiumWorldRenderer 集合 | 反射扫描集合字段 | 无果 |
| Voxy `doTraversal` / 视口切换 | 计时 + 计数 | 无果（且卸载后复现） |
| 第二遍渲染是否被闸门拦下 | lastBlockedGate | 无果 |
| **GPU 显存耗尽** | — | 16G 卡吃满 14G 不现实；真爆显存在这边是 `GpuOutOfMemoryException` 直接崩，不会优雅降到 7fps 稳住 |

---

## 3. 取证（决定性的一步）

`/spark heapsummary` 采三拍（开镜前 / 开镜后 / 闲置 2 分钟后），差值的类指纹：

| 类 | 每 pass 沉积率 |
|---|---|
| `ItemStackRenderState`（+ 其 `LayerRenderState`） | ~3.7 |
| `BedrockRenderSnapshot$DrawCommand`（**TaCZ 枪模快照绘制命令**） | ~3.7 |
| `ModelFeatureRenderer$Submit` | ~3.1 |
| `org.joml.Matrix4f` / `Matrix3f` | ~7.7 / ~4.2 |
| `Optional` | ~7.5 |
| `SlimeRenderState` | ~0.67 |

三条读数一起定性：

- 沉积率 ∝ 开镜帧数（按住 60s 的沉积 ≈ 5×1s 的 12 倍 ≈ 帧数比 12 倍）；
- 跨闲置**不回收**（#3−#2 ≈ 0）；
- 沉积的是**每次 scope pass 的提交节点及其整张载荷图**（DrawCommand 经
  `collector.submitCustomGeometry` 的闭包挂在 Submit 上；渲染状态经 26.2 的
  render state 挂在 Submit 上）。

VisualVM 「Show nearest GC root」定案持有者：

```
IrisRenderingPipeline -> shadowRenderer -> submitNodeStorage
                      -> SimpleFeatureRenderPhase -> batches        （实例 17,444，占 100% 沉积）
```

---

## 4. 根因与修复（`052e600`，本仓已同步）

**根因**：`SimpleFeatureRenderPhaseMixin` 之前**无差别**对所有发生在
`insideScopeLevelRender` 期间的 `sortInto` 阻止 `clear()`。而 Iris 的
`ShadowRenderer` 拥有一个**独立的** `SubmitNodeStorage`，它只在镜内那一遍里跑，
主画面从不跑。于是 Iris 的阴影提交队列被永久禁止清空：每开镜一帧沉积 ~3.7 个
Submit/DrawCommand、永不释放，还要每帧重复遍历上万节点 —— CPU/GPU 阴影开销一起爆。

**修复**：只有**主画面**需要把提交节点留到合成阶段；Iris 阴影那份必须照常清空。

| 文件 | 改动 |
|---|---|
| `mixin/client/LevelRendererAccessor.java` | 暴露 `LevelRenderer.submitNodeStorage` |
| `mixin/client/FeatureRenderDispatcherMixin.java` | 跟踪「当前正在 prepare 的是哪个 `SubmitNodeStorage` 实例」 |
| `client/render/scope/ScopePipRenderer.java` | 新增 `shouldPreserveSubmits()`：仅当（正在镜内渲染）且（当前准备的就是 `mc.levelRenderer.submitNodeStorage`）时才保留 |
| `SimpleFeatureRenderPhaseMixin` / `TranslucentFeatureRenderPhaseMixin` | `tacz$keepSubmitsForTheMainPass` 改问 `shouldPreserveSubmits()` |

---

## 5. 本仓复测协议（给实机那一轮）

1. 进单人世界，装 Complementary 系光影，`ScopePipEnable=true`、
   `ScopePipIsolatePipeline=true`；
2. 记第一次开镜的 FPS；
3. 连续开镜约 60 秒（或快速点射 5 次 × 1 秒），每 15 秒记一次 FPS；
4. 闲置 2 分钟不开镜，再开镜记 FPS。

**判读**：修复生效 = 步骤 3 的 FPS 基本平、步骤 4 与步骤 2 持平。
若仍衰减：先确认 `shouldPreserveSubmits()` 的分支真的走到了（本仓的
`LevelRenderer.submitNodeStorage` 字段名是**未验证**的，混入失败会表现为
`clear()` 依旧被无差别拦下），再回到 §3 的堆图取证。

---

## 6. 相关

- 同步总记录：[`REFAB_SCOPE_PIP_SYNC_20260830.md`](REFAB_SCOPE_PIP_SYNC_20260830.md) §4
- 姊妹分支原文（未同步，属其分支）：`docs/SCOPE_PIP_FPS_DECAY_INVESTIGATION_2026_08_29.md` @ `fcaa2b8`
- 按用户裁决（`diagnostics = fix_plus_doc`）：**只同步修复与这份调查记录**，
  那批实验装置（`ScopePipResourceProbe`、`ScopePipDebugGpuMem`、
  `ScopePipReleaseIdlePipeline` + `ScopePipIdleReleaseDelayFrames`）**不进本仓**。

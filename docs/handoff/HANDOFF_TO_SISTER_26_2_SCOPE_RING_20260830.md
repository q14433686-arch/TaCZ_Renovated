# 给姊妹项目 26.2 分支的提示词：遮光环被镜内合成盖掉

日期：2026-08-30
来源：本仓（TaCZ_Renovated，`arena/01a04ea3-tacz-renovated`）实测确认的病灶与修法
对象：`TaCZ_Refabricated_Unofficial` 的 `26.2(main)` 分支（掩码 + PIP 架构，与我们同构）
状态：**本仓已实机 PASS**（用户 2026-08-30 复测）；她们那边尚未修

> 我们与她是两套加载器（本仓 NeoForge / 她 Fabric），**不要让她抄我们的代码**。
> 下面这份东西刻意只给「病灶 + 证据行 + 26.2 的 API 差异 + 两个坑」，
> 机制让她从**她自己仓库的 1.21.11 分支**搬 —— 那里早就有同一套解法。

---

## 1. 症状与复现条件

开镜内画中画（PIP）+ 开光影包，**对着光源开镜**时，目镜的**遮光环**（她那边叫
「目镜内圈黑色遮光环」，模型骨骼 `ocular_ring`）变成半透明 —— 镜筒内那一圈黑的
没了，透出放大的世界。

三条缺一不可：`ScopePipEnable` 开 + `ScopePipAllowShaderPacks` 开（她那边的
`AllowShaderPacks` 默认 false，必须显式打开）+ 光影包生效。无光影时完全正常。

她仓库里这个问题的登记处：`COMPAT_AND_ROADMAP.md` 案例③「目镜内黑边（遮光环）
被不正确裁切」，以及案例⑨（`fd8d1bf`）—— 但**登记的是另一半**（被掩码裁），
本次这个是「被合成盖」，两者表现一样、成因不同。

## 2. 真因：光影下「合成 vs 手持」的先后关系是反的

| 路径 | 合成时机 | 与手持的先后 |
|---|---|---|
| 无光影 | `compositeAtPhaseBoundary()`：`renderAllFeatures` 里 `executeSolid()` **之前** | 合成 → 手持 ⇒ 手持盖在合成之上 ✔ |
| 有光影 | `compositeAfterLevelUnderShaders()`：`LevelRenderer#render` 调用**之后** | 手持 → 合成 ✘ |

Iris 把整个手部 pass 搬进了 `LevelRenderer#render` 内部，而「最终画面」要等整条
Iris 管线收工才存在，所以 PIP 的合成只能排在 `LevelRenderer#render` 的返回处
（她自己 `captureScene` 那段注释就写了这件事）。于是**合成把刚刚画好的物理目镜框
整片盖掉**。

被盖掉的正是案例⑨（她 2026-08-12 `fd8d1bf`，PIP 之前）用「无裁剪重画」救回来的
那部分：`ocular_ring` 的内圈与目镜投影（掩码）重叠。无光影时它画在合成之后、能
盖住合成多铺出来的那一圈；光影下这个保护整个失效。

为什么「对着光」才看得见：露出来的是**放大的世界拷贝**，世界本身暗时与黑环无异。

## 3. 她自己仓库里现成的机制（1.21.11 分支 commit `2710c7c`）

不用我们提供方案 —— 她 1.21.11 分支上早就解决过同一类问题（遮光环被光影包的后置
pass 盖掉），四个文件搬迁即可：

- `ScopeFinalOverlayState`（排队 + 延后重画，含 `queueOcularRing` 与
  `FINAL_OCULAR_RING_ORDER`）
- `scope_ring_final.fsh`（entity 片元路径去掉 `apply_fog()`；注释原话：
  *"the ring is a physical 3D foreground layer submitted after Iris has finished all
  composite/final passes"*）
- `IrisFinalScopeOverlayMixin`（挂在 `IrisRenderingPipeline#finalizeLevelRendering` TAIL）
- `BedrockAttachmentModel` 里 `queueOcularRing` 的调用点

## 4. 26.2 的三处 API 差异 —— 照抄 1.21.11 会编译不过

这三条是本仓用 NeoForge 官方 26.2 迁移指南 + rendering/feature 文档逐条核过、
并由用户本地编译器验过的：

1. **不要自建 `FeatureRenderDispatcher`**：26.2 的构造函数改成吃 `RenderBuffers`
   （1.21.11 那版是 7 个参数：`nodes, blockRenderer, bufferSource, atlasManager,
   outlineBufferSource, crumblingBufferSource, font`）。26.2 用官方配方：
   `Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(storage)`
2. **不要调 `SubmitNodeStorage#endFrame()`**：26.2 里它与 `clear()` 一起被移除，
   `renderAllFeatures` 自己收尾。
3. **刷新点不能是 `finalizeLevelRendering` TAIL**：那一步跑在
   `LevelRenderer#render` 内部，**早于** PIP 的合成，挂在那里等于没延后。
   必须挂在她自己的 `compositeAfterLevelUnderShaders()` 之后（同一个注入点内）。

## 5. 两个坑（本仓第 1 轮就是栽在这上面）

**坑 A · 方法名**：`RenderSystem.getModelViewMatrix()` 在 26.2 **不存在**
（那是 1.21.11 的名字）。26.2 叫 **`getModelViewMatrixCopy()`**。

**坑 B · 捕获时机**：手持的**投影与模型视图**必须在**阶段边界**
（`renderAllFeatures` 里 `executeSolid()` 之前、与她画掩码同一个位置）取，
**不能**在 `submit` 阶段取 —— submit 时 `RenderSystem` 里挂的还是**世界**那套矩阵，
取到它目镜框会整个飘出画面。几何快照的顶点已套过 poseStack（世界坐标）、
节点 pose 是单位矩阵，落点完全取决于绘制那一刻这两个矩阵。
（只取切片对象、不要 `map()` 读回内容：那 64 字节在光影下不可读。）

## 6. 两个「别做」

- **别把 ring-final 管线注册给 Iris 的 HAND 程序**（她的
  `assignScopePipelineToHand`）：注册了它就会被塞回光影管线，既拿不到无雾语义，
  又会被想躲开的那些后置 pass 再盖一次。
- **别无条件延后**：只在「光影 + PIP 合成确实走 Iris 那条路」时排队
  （等价于她的 `wantsIrisComposite()` 为真）。其余路径（无光影 / 未开 PIP /
  关着 `AllowShaderPacks` / 第三人称）必须逐位保持原样。

另外：队列要在帧首清空（她的 `beginFrame`，挂在 `GameRenderer#extract` HEAD），
否则某一帧没走到刷新点时残留快照会画到下一帧。

## 7. 验收清单

1. 无光影开镜：与改动前逐位一致（回归）。
2. 光影 + PIP + 对着光源开镜：遮光环是不透明黑环（主要症状消失）。
3. 目镜框位置/大小正确（验坑 B）。
4. 目镜框可见（若整片不见 → 深度被判遮挡，给这条管线关掉深度测试即可；
   本仓沿用 vanilla entity cutout 的深度测试是**对的**，实测没被遮挡）。
5. 日志出现一次「Queued … ocular ring …」与「Drew … after the PIP composite」。

## 8. 已知缺口

同一机理下，**蚀刻/发光准星在光影 + PIP 时同样会被合成盖住**（它也在手持那一遍画）。
本仓这次只修了遮光环（用户报的就是这个），准星待确认；若确认受影响，同一套
`ScopeFinalOverlayState` 再加一路 `queueReticle` 即可（order 要比目镜框小，
让她用她 1.21.11 的 `FINAL_RETICLE_ORDER = 20_000` / `FINAL_OCULAR_RING_ORDER = 20_001`，
准星先画、目镜框后画）。

---

## 9. 【可直接粘贴】给她 agent 的提示词

```
你的 26.2 分支上有一个实测确认的渲染 bug，请自行定位并修复（不要照搬外部代码，
机制请从你自己仓库的 1.21.11 分支搬）。

【症状】
开镜内画中画（ScopePipEnable=true）+ 开光影包（ScopePipAllowShaderPacks=true）+ 第一人称，
对着光源开镜时，目镜的遮光环（模型骨骼 ocular_ring，COMPAT_AND_ROADMAP 案例③ 里
叫「目镜内圈黑色遮光环」）变成半透明 —— 镜筒内那一圈黑的没了，透出放大的世界。
无光影时完全正常。三条开启条件缺一不可。

【真因（已由邻链实测确认）】
光影下「合成 vs 手持」的先后关系是反的：
  无光影：compositeAtPhaseBoundary() 跑在 renderAllFeatures 里 executeSolid() 之前
          = 合成画在手持【之前】⇒ 手持（含案例⑨ fd8d1bf 无裁剪重画的 ocular_ring）
            盖在合成之上，一切正常；
  有光影：Iris 把整个手部 pass 搬进了 LevelRenderer#render 内部，最终画面要等整条
          Iris 管线收工才存在，所以 compositeAfterLevelUnderShaders() 只能排在
          LevelRenderer#render 的返回处 = 合成画在手持【之后】⇒ 把刚画好的物理
          目镜框整片盖掉。
被盖掉的正是案例⑨（你 2026-08-12 fd8d1bf，PIP 之前）救回来的那部分：ocular_ring
的内圈与目镜投影（掩码）重叠。为什么对着光才看得见：露出来的是放大的世界拷贝，
世界本身暗时与黑环无异。

【机制来源：你自己的 1.21.11 分支 commit 2710c7c】
「render reticle after Iris final composite」——ScopeFinalOverlayState（含
queueOcularRing / FINAL_OCULAR_RING_ORDER）、scope_ring_final.fsh（entity 片元路径
去掉 apply_fog）、IrisFinalScopeOverlayMixin、BedrockAttachmentModel 的调用点。
那条分支用它解决「遮光环被光影包的后置 pass 盖掉」，本次是同源形态。

【26.2 的三处 API 差异 —— 照抄 1.21.11 会编译不过（已由邻链编译器验过）】
1. 不要自建 FeatureRenderDispatcher：26.2 的构造函数改成吃 RenderBuffers。
   用官方配方：Minecraft.getInstance().gameRenderer.featureRenderDispatcher()
   .renderAllFeatures(storage)。
2. 不要调 SubmitNodeStorage#endFrame()：26.2 里它与 clear() 一起被移除。
3. 刷新点不能是 finalizeLevelRendering TAIL —— 那一步跑在 LevelRenderer#render
   内部，早于 PIP 的合成，挂在那里等于没延后。必须挂在
   compositeAfterLevelUnderShaders() 之后（同一个注入点内）。

【两个坑（邻链第 1 轮就栽在这上面）】
A. RenderSystem.getModelViewMatrix() 在 26.2 不存在（那是 1.21.11 的名字）；
   26.2 叫 getModelViewMatrixCopy()。
B. 手持的投影与模型视图必须在【阶段边界】（与画掩码同一个位置，executeSolid()
   之前）取，不能在 submit 阶段取 —— submit 时 RenderSystem 里挂的还是世界那套
   矩阵，取到它目镜框会整个飘出画面。几何快照顶点是已套 poseStack 的世界坐标、
   节点 pose 是单位矩阵，落点完全取决于绘制那一刻这两个矩阵。
   只取切片对象，不要 map() 读回（那 64 字节光影下不可读）。

【两个「别做」】
- 别把 ring-final 管线注册给 Iris 的 HAND 程序（assignScopePipelineToHand）：
  注册了就被塞回光影管线，既拿不到无雾语义又会被后置 pass 再盖一次。
- 别无条件延后：只在「光影 + PIP 合成确实走 Iris 那条路」时排队（等价于
  wantsIrisComposite() 为真）；其余路径（无光影 / 未开 PIP / 关着
  AllowShaderPacks / 第三人称）必须逐位保持原样。队列要在帧首清空（beginFrame），
  否则某帧没走到刷新点时残留快照会画到下一帧。

【验收】
1) 无光影开镜与改动前逐位一致；2) 光影+PIP+对着光开镜，遮光环是不透明黑环；
3) 目镜框位置/大小正确（验坑 B）；4) 目镜框可见（若整片不见 → 深度被判遮挡，
   给这条管线关掉深度测试即可）。

【已知缺口】同一机理下蚀刻/发光准星在光影+PIP 时同样会被盖住（它也在手持那一遍
画）。先只修遮光环，修完请实测准星是否受影响；若受影响，同一套
ScopeFinalOverlayState 再加一路 queueReticle（order 20_000，比目镜框 20_001 小，
准星先画、目镜框后画）。

【要求】按你仓库的规矩：先取证（把上述判断在你自己的代码里逐条对上再动手），
源码级移植要自审并明确标注未验证项，不要写 PASS，等用户实机反馈再收口。
```

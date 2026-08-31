# 内置 TacZ Mesh Loader [TML] —— 安全子集（第 0 步）+ GPU 静态烘焙（第 1 步）

> **本仓移植说明**（NeoForge 26.2 / `TaCZ_Renovated`）：本文随姊妹分支
> `TaCZ_Refabricated_Unofficial` `arena/01a04e96` 同步，分两段落地：
> - **第 0 步**（安全子集）← 她的 `8c6ad27` + `f70867d`；
> - **第 1 步**（GPU 静态烘焙，见 §2.5）← 她的 `8191f6b` / `0ea0fb6` / `6e275d0` /
>   `9f7412e` 四笔的**最终形态**（即 `f70867d→9f7412e` 的聚合差）。不逐笔照搬：
>   后三笔全是对第一笔的修正，逐笔搬会把已经修掉的 bug 原样搬回来。
>
> 代码适配：去掉 Fabric 的 `@Environment`；`ForgeConfigSpec` → `ModConfigSpec`；
> 缓存失效监听器走 NeoForge 的 `AddClientReloadListenersEvent`；目镜框挂点用本仓的
> `ScopeFinalRingOverlay`（姊妹侧叫 `ScopeFinalOverlayState`）。配置面按维护者硬性
> 惯例**同时接 TOML 与 Cloth**。她侧 `withinContextBudget` 的世界全细节距离那段
> **没搬**——本仓已有 `PolyRenderPolicy.withinFullDetailDistance`。

> 代码移植自 [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
> `1.21.1_fabric` v0.1.7，GPL-3.0。不是官方 TacZ 附属。
>
> **状态：源码 + CI 编译通过（`bf2a16f` / `833d8ac` 均 BUILD SUCCESSFUL）；
> 第 1 步（GPU 烘焙）的 §5.2 第 8–12 条由维护者 2026-08-31 实机复测全部通过。**
> 未实测的部分（非第一人称各场景的帧率收益、ttf/unihex 字体路径）仍记 UNVERIFIED ——
> 按 AGENTS.md §2，不是我自己跑出来的结果不写 PASS，是谁跑的写清楚。
>
> 路线图见 [`TML_PERF_DIRECTIONS_2026_08_29.md`](TML_PERF_DIRECTIONS_2026_08_29.md)。
> 第 0 步：从干净基线重新内置「不含 GPU 赌注」的部分。
> 第 1 步：GPU 静态烘焙（§2.5），同步她四笔的最终形态。

## 0. 与四个关闭 PR 的关系（为什么这是第五次、以及第 0 步为什么砍掉了 GPU）

| 版本 | 结局 | 教训（本轮如何处置） |
|---|---|---|
| PR #33 | 关 | GPU 画在世界 pass + 不可信矩阵 + `visitBones` skip 剪子树 → **本轮无 GPU 路径，问题不存在** |
| PR #69 | 关 | 光影一开整条回退 CPU；声称做了的代码没做 → 本轮如实声明 CPU 路径是常态而非回退 |
| PR #70 | 关 | 全局 WORLD_DRAWS 表泄漏进世界 pass；弹匣没接 `IMirrorGeometry` → **无 GPU 表；弹匣链路照搬 v4 已修正的架构（见 §2）** |
| PR #71/#72 | 关 | v4 架构收敛但被要求从干净基线重做 → 本轮**逐文件对照 HEAD（含 #77/#82 之后的懒加载改动）重新落地**，只保留三轮教训打磨过的安全子集 |

维护者关闭 #72 的意见是「不应以重做名义复用已关闭的分支」。本轮的处理方式：
不 cherry-pick、不合并任何关闭分支；以关闭分支为**参考资料**逐文件审计后在
当前 HEAD（`fcaa2b8`，含 PR #82 的 PIP 修复与懒加载重构）上重写落地，
每个 mixin 注入点、每个反射字段名都对照当前 HEAD 源码逐一核实过
（`ClientAttachmentIndex` 在 #72 之后新增了 warmUp/懒加载路径，
注入点 `checkTextureAndModel`/`checkLod` 仍是模型装载的唯一入口，语义未变）。

## 1. 本轮包含什么 / 不包含什么

### 包含（安全子集）

- **poly_mesh 解析与渲染**：枪 / 配件 / 弹药（物品、掉落实体、抛壳）/ 方块，
  全部走 26.2 的 `SubmitNodeCollector.submitCustomGeometry` 延迟提交路径，
  submit 当刻冻结骨骼矩阵快照（与 `BedrockRenderSnapshot` 同一理由）。
- **geo JSON 解析缓存**（修复用户 2026-08-25 log 实证的双遍解析）：
  按 geo 路径缓存共享网格数据，资源重载时整体失效；统计日志按 geo 去重。
- **顶点预算闸门**：GUI/FIXED/HEAD 超 `MeshGuiMaxVertices` 只画立方体；
  第三人称/掉落物/展示框超 `MeshWorldMaxVertices` 同理；另有距离闸门。
  **近距离全模豁免**（`MeshWorldFullDetailDistance`，默认 16 格）：该距离内的
  世界语境 poly 无条件画全模，世界预算只保护远处/密集场景——否则无 LOD
  低模的高模枪（如 36 万顶点级枪包）在玩家眼前的第三人称/掉落物/展示台上
  会整层消失只剩立方体。枪包若在 display JSON 里提供了 `lod` 字段，
  TACZ 本体的 LOD 选择逻辑优先生效（`GunLodRenderDistance` 控制），
  该豁免只兜底「没有 LOD 可退」的枪包。
- **弹匣双通道**：主遍历 exclude `additional_magazine` 子树；立方体弹匣走
  26.2 原生 `IMirrorGeometry`；poly 弹匣在 `additional_magazine.visible` 时
  按该节点变换补画（与上游 TML `renderSubtreeDirect` 同构）。
- **阴影 pass 默认跳过 poly**（`MeshPolyInShadow=false`）：立方体已提供影子形状，
  光影下省一半顶点成本。
- **加载告警**：超 `MeshMaxModelVertices` 的模型加载时警告枪包作者。

### 明确不包含（后续步骤，见路线图）

- ~~**GPU 静态烘焙 / 逐骨骼 VBO**~~ —— 已落地，见 §2.5。注意它**只覆盖第一人称
  手部 pass**：世界 / 掉落物 / GUI / 阴影恒走 collector。36 万顶点级高模的
  **第一人称**成本才有量级改善，其它视角一分没减。这是如实声明。
- 她侧最初的「光影下 `assignPipeline(HAND)`」形态：**没有搬**。裸 GPU pass 会绕过
  光影拦截，改用她 `6e275d0` 之后的 vanilla `RenderType` 路线（§2.5 第 5 条）。
- 姿态缓存 / 三角形配对（路线图方向 2）。
- mesh 目镜（上游 TML 同样不支持：ocular 物体必须用立方体）。

## 2. 弹匣链路（关 PR #70 的架构缺口，本轮的处理）

26.2 的换弹弹匣：`BedrockGunModel` 把 `additional_magazine` 的 FunctionalRenderer
设为返回 `IMirrorGeometry`（指向 `magazine` 节点），快照遍历器原生处理立方体镜像。

poly 部分：`TaczPolyMeshGunModel#submit` 里
1. 主遍历 `setExcludeSubtree(additional_magazine)`——否则换弹中它会出现在两个位置；
2. `super.submit` 照常（立方体 + IMirrorGeometry）；
3. 主 poly 快照提交（含 `magazine`）；
4. `additional_magazine.visible` 时，把该节点到根的变换链乘进新 PoseStack，
   `captureSubtree(mirrorRoot=true)` 补画 `magazine` / `additional_magazine`
   的 poly（mirrorRoot=true = 根骨骼自身变换不再套用，因为已在变换链里）。

## 2.5 GPU 静态烘焙（第 1 步，`PolyMeshGpuRenderer`）

**做了什么**：静态骨骼的顶点按**骨骼本地坐标**一次烘进常驻 VBO
（`DefaultVertexFormat.ENTITY`，光照档量化后烘进 UV2），此后每帧只写
**O(骨骼)** 次 `DynamicTransforms`，而不是 O(顶点) 次 CPU 变换 + VertexConsumer。
36 万顶点 / ~40 骨骼的高模，第一人称每帧的矩阵写次数从 36 万降到 40 量级。

**生效条件**（`shouldSubmitGpu()`，四条全满足才走 GPU，否则静默回 collector）：

1. `MeshGpuBaking=true`；
2. **现在就在手部 pass 里** —— 判据是 `ScopeMaskRenderer.isInHandPass()`，
   **不是** `transformType.firstPerson()`；
3. **不在镜内那一遍**（`ScopePipRenderer.isInsideScopeLevelRender()` 为 false）；
4. 烘焙成功（见第 4 条的部分失败规则）。

**绘制点**：`FeatureRenderDispatcherMixin` 在 `PreparedFrame.executeSolid()`
**之后**注入（`shift = AFTER`）——不在任何 render pass 内，且立方体深度已写入；
与目镜掩码是同一个安全边界。帧表在 `GameRendererMixin#extract` HEAD 与掩码同点归零。

### 关 PR #33/#69/#70/#71-72 的教训，逐条落地

1. **HAND 表不用 `transformType.firstPerson()` 判**：26.2 里 `renderItemInHand`
   与镜内重放都走那段，用它判会导致世界 pass 收不到、或镜内那一遍串进来。
   用 `isInHandPass()` 才是「手上这一遍」的准确语义。
2. **绘制时自乘 `RenderSystem.getModelViewMatrixCopy()`**（她 `0ea0fb6`）：
   collector 路径是 `MV_draw × pose_submit` 两层，GPU 路径原先只写了后者，
   表现为**枪朝向恒指北、不随视角转**。字节码依据：`RenderType.prepare()`
   内部 `getModelViewMatrixCopy() -> writeDynamicTransforms(mv)`。
3. **换弹 `additional_magazine` 恒走 collector**：它走 `mirrorRoot` 矩阵，
   与主遍历的矩阵语义不同，烘进 VBO 会错位（`submitAdditionalMagazinePoly`
   在 GPU 分支之外，无条件执行）。
4. **失败即整体回退**：任何异常 → 本会话停用 GPU 路径并把 `MeshGpuBaking` 翻 off；
   部分骨骼烘失败（`bakeBone` 返回 null）→ **整个模型**回 collector。
   半 GPU 半 collector 的 cutout 集合无法对账（哪根骨骼谁画的说不清），二选一。

### 光影与重烘

5. **光影下默认走 vanilla `RenderType`**（她 `6e275d0`）：`RenderType.prepare()`
   + `PreparedRenderType.drawFromBuffer()`，用 `RenderTypes.entityCutout`，
   让 Iris 按 HAND program 接管。**裸 GPU pass 会绕过光影拦截**，所以
   `MeshGpuUnderShaders` 默认 false，且它是诊断开关、不是常规选项。
6. **重烘规则**：光照档变化（`quantizeLight` 档位变了）触发重烘，**节流 1 秒**；
   但**光影包开关翻转导致的世代号不匹配立即重烘**，不受节流约束
   （她 `9f7412e`）—— Iris 激活与否会改变实体顶点格式的写出布局，
   旧 VBO 在新管线下属性错位就是模型拉伸，一帧都不能再画。
   模型重载走 `loadPolyMesh` 开头的 `releaseBaked()`。

### 2.6 手部路径的六条后续修复（2026-08-31，同步她 08-31）

第 1 步实机 PASS 之后，她那边又修了几条落在**同一条手部路径**上的问题，本仓已同步
（提交 `2a408c7`）。其中第 1 条是**已经带病发船的真 bug**：

1. **光影下反光的光源关系错乱（法线弹栈时序）**：`prepare()` 之后、`drawFromBuffer()` 之前就弹了 MV 栈。光影包的 `gl_NormalMatrix` 是 Iris 在
   **绘制执行那一刻**从 `RenderSystem` MV 栈顶取的逆转置（Iris 26.2
   `ExtendedShader.iris$setupState` 源码实读），不走 `prepare()` 快照 —— 弹早了
   ⇒ 栈顶只剩 MV_draw ⇒ 顶点法线（骨骼本地系）丢了 `pose_bone` 的旋转层。
   **位置一直是对的**（ModelViewMat 走 `prepare()` 快照），所以肉眼容易漏，
   症状只表现为「反光/光照的方向不对」。弹栈移到 `drawFromBuffer()` 之后。
2. `LinkageError` 也要接：Iris/Sodium 升级后签名变了抛的是 `NoSuchMethodError`
   （Error 不是 Exception），漏接 = 崩游戏而不是回退 collector。
3. GPU 失败**不再回写配置**（原来会把 `MeshGpuBaking` 翻成 false 写回去）：
   渲染线程写配置文件既不安全，也会把一次瞬时故障固化成持久设置。只置会话标志。
4. 逐帧比对 `ENTITY.getVertexSize()`：stride 一变就整代失效（原来只认光影开关翻转）。
5. 手部消费点带 `RenderSystem.outputColorTextureOverride` 时跳过并清表（防别的 mod）。
6. **退化面不写零法线**：零面积面的叉积是 (0,0,0)，写进顶点后 `normalize(0)` = NaN
   ⇒ 光影下随机高光/黑点；退化为枪包自带法线，再不行取 (0,1,0)。

> 复测重点（装光影包）：**反光方向是否与立方体部件一致**——第 1 条修的就是这个，
> 之前位置正确所以看不出来。

## 3. 枪包怎么用

display JSON：

```json
{
  "model_type": "mesh",
  "model": "mypack:gun/mygun_geo",
  "texture": "mypack:gun/uv/mygun",
  "animation": "mypack:mygun"
}
```

并提供 `assets/mypack/geo_models/gun/mygun_geo.json`（Meshy 插件导出的
poly_mesh geo）。`model_type: "mesh"` 只对枪本身必需；配件/弹药/方块只要
模型旁存在同名 geo 就会替换。目镜物体不支持 mesh（与上游 TML 相同）。

`fabric.mod.json` `provides: ["taczmeshloader"]`——依赖外置 TML 的枪包
在本 mod 下视为依赖满足。

## 4. 配置（`tacz-client.toml` 的 `[mesh_loader]`）

| 键 | 默认 | 含义 |
|---|---|---|
| `MeshEnable` | true | 总开关（关掉后仅立方体渲染，行为同无 TML） |
| `MeshPolyInShadow` | false | 阴影 pass 是否画 poly |
| `MeshMaxRenderDistance` | 48 | 世界 poly 距离（0=不限） |
| `MeshPolyInPreview` | true | GUI/FIXED/HEAD 是否画 poly |
| `MeshGuiMaxVertices` | 65536 | GUI 顶点预算（0=不限） |
| `MeshWorldMaxVertices` | 120000 | 第三人称/掉落物顶点预算（0=不限） |
| `MeshWorldFullDetailDistance` | 16 | 该距离（格）内世界 poly 免顶点预算画全模（0=关闭豁免；已接 Cloth Config 界面） |
| `MeshMaxModelVertices` | 120000 | 加载时告警阈值（不影响渲染） |
| `MeshLogStats` | true | 加载统计日志 |
| `MeshGpuBaking` | true | 第一人称手部 pass 的骨骼静态烘焙（§2.5）；关掉 = 全程 collector |
| `MeshGpuUnderShaders` | false | 装了光影时仍强走裸 GPU pass（**诊断用**开关，非常规选项） |

两个 GPU 键都有真实消费点（`PolyMeshGpuRenderer#shouldSubmitGpu`），
TOML + Cloth + 中英语言三处齐备，不是「没人读的配置」。

## 5. 验证清单

### 5.1 编译（CI 闭环）

沙箱无 JDK 且 Maven CDN 不可达（2026-08-29 复测：pypi/npm/GitHub 主域可达，
Adoptium/Maven Central/镜像站全部 000）。编译验证走 `compile-check.yml`
CI 闭环：push 触发 → Actions 跑 `./gradlew compileJava` →
日志 commit 回推分支 → 沙箱 `git pull` 读取。

### 5.2 实机（本地）

1. **无 mesh 枪包回归**：行为应与改动前一致（默认包全立方体，mixin 注入点
   都是 TAIL + geo 存在性检查，无 geo 时零行为差异）。
2. `model_type: mesh` + geo：第一人称可见、贴图正确；日志出现
   `poly_mesh stats for ... N bones, M vertices`（每 geo 只一行——缓存生效）。
3. F5 / 掉落物 / JEI / 展示框：位置与投影正确（本轮全走 collector，
   不存在 #70 的世界 pass 泄漏形态）。
4. 换弹：枪上弹匣与手里弹匣都在（纯 mesh 弹匣尤其要看）；换弹全程无双影。
5. 高模包（duyupack 级）：JEI 打开一屏图标——应看到
   `poly preview suppressed in GUI` 且不卡死。
6. 光影（Complementary 系）：poly 枪身正常照明（走的是 vanilla
   entityCutout 提交，Iris 按 HAND program 处理，与立方体同一路径）；
   阴影里枪影仍在（立方体提供）。
7. 资源重载（F3+T）：poly 仍正常（解析缓存失效并重建）。
8. **GPU 路径**（默认 `MeshGpuBaking=true`）：第一人称持枪时日志出现一次
   `GPU-baked N bones (M vertices) for ... at quantized light 0x...`；
   在同一把枪上来回走动（光照档变化）**不应每帧刷这一行**——1 秒节流生效。
9. **GPU 路径的朝向**：第一人称左右转身 / 抬头低头 / 蹲下 / 开镜，枪必须跟着视模走。
   若出现「枪朝向恒指北、不随视角转」，说明第 2 条那层 `getModelViewMatrixCopy()`
   又丢了。
10. **GPU 路径下的换弹**：枪上弹匣与手里弹匣都要在，且位置与
    `MeshGpuBaking=false` 时一致（`additional_magazine` 恒走 collector，是故意的）。
11. **光影开关翻转**（Iris + Complementary，`MeshGpuUnderShaders` 保持 false）：
    进入 / 退出光影包时模型**不得拉伸错位**，且开关瞬间日志应重新出现一次
    `GPU-baked`（世代号不匹配立即重烘）。
12. **降级通道**：`MeshGpuBaking=false` 时，画面必须与第 1 步合并前完全一致。

### 5.3 已知边界（如实）

- **GPU 路径只覆盖第一人称手部 pass**：世界 / 掉落物 / GUI / 展示框 / 阴影恒走
  collector，那些场景的 O(顶点) 成本一分没减（只有 §1 的闸门和缓存级削减）。
- **第 1 步的实机状态**（2026-08-31 回填）：维护者复测 §5.2 第 8–12 条**全部通过**
  （GPU baked 日志节流正常、朝向随视模、换弹双弹匣位置正确、Iris 光影翻转不拉伸、
  `MeshGpuBaking=false` 与合并前一致）。这条结论来自维护者实测，不是本 sandbox 跑出来的。
- **仍未量化**：世界 / 掉落物 / GUI 走 collector 的**帧率收益数字**没人测过；
  高模（36 万顶点级）第一人称的 fps 对比也还没有数字，只有「成本从 O(顶点) 降到
  O(骨骼)」这个机制性结论。这是 §5.2 之外的一条空档。
- 36 万顶点级高模第一人称**仍有帧率成本**（每帧 O(顶点) CPU 变换 +
  逐顶点 VertexConsumer 调用）。这是路线图第 1/2 步要解决的，本轮不解决。
  （第 1 步已落地后，这句话只在 `MeshGpuBaking=false` 或非第一人称时成立。）
- PIP 二次渲染（`ScopePipRerender=true`）时镜内那遍会重放 collector 回调，
  poly 成本 ×2。降级方案在路线图方向 3，待镜内行为实机确认后做。

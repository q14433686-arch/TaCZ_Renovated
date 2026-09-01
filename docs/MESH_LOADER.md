# 内置 TacZ Mesh Loader [TML] —— 安全子集（第 0 步）+ GPU 静态烘焙（第 1/2 步）

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
> **状态：源码 + CI 编译通过（`bf2a16f` / `833d8ac` / `2a408c7` / `ba59ff5`
> 均 BUILD SUCCESSFUL）；第 1 步（GPU 烘焙）的 §5.2 第 8–12 条由维护者
> 2026-08-31 实机复测全部通过；第 2 步（世界语境烘焙，`ba59ff5`）已实装，
> 维护者 **2026-09-01 实机 PASS**（§5.4 九项矩阵未逐条回报）。**
> 未实测的部分（第 2 步全部、非第一人称各场景的帧率收益、ttf/unihex 字体路径）
> 仍记 UNVERIFIED ——
> 按 AGENTS.md §2，不是我自己跑出来的结果不写 PASS，是谁跑的写清楚。
>
> 路线图见 [`investigations/TML_PERF_DIRECTIONS_2026_08_29.md`](investigations/TML_PERF_DIRECTIONS_2026_08_29.md)。
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

## 2.7 世界语境 GPU 烘焙（第 2 步，`MeshGpuWorld` 默认开，**未实机**）

第 1 步只解决第一人称。多人场景下「每帧每枪 O(顶点)」仍在：别人手里的枪、掉落物、
展示框、展示台雕像全走 collector 的 CPU 顶点变换。第 2 步让这些语境**共用同一套
常驻骨骼 VBO**，每把枪每帧只往 `WORLD_DRAWS` 登记 O(骨骼) 个矩阵。

### 消费点为什么不是 `renderAllFeatures`（她用字节码取证证明，本仓沿用）

26.2 的世界实体 pass **根本不经过** `renderAllFeatures`：`LevelRenderer.render` 的
帧图 lambda **直调** `PreparedFrame.executeSolid`（偏移 177）。而 `renderLevel`
偏移 560 那次 `renderAllFeatures` 是收尾调用，**此时 MV 栈已 pop 回单位阵** ——
在那里画 = 丢掉相机旋转整层 = 枪固定在视角空间（她实测复现）。

⇒ 世界表挂在 `PreparedFrameSolidMixin`（`executeSolid` RETURN），调用者由
`LevelRendererWorldPassMixin`（`LevelRenderer#render` 的 HEAD/RETURN 括号）区分：
只有世界帧图那一类落在括号内，手部 / GUI / 收尾三类都在括号外，一律拒收。
此时 MV 栈顶恰为 viewRotation，与手部两层变换完全同构。

> 这与第 1 步当年「丢 MV_draw 层」是同一个病，只是丢法不同：当年是没乘，
> 这次是在栈已经空了的地方乘。

### 本仓唯一需要自己设计的表皮：Screen 提取窗口追踪

她用 Fabric 的 `ScreenEvents.beforeExtract/afterExtract` 精确框住 Screen 提取窗口。
**不能用**「有菜单开着」或时间戳窗口判定 —— 那会「玩家一开背包，地上/别人手里的
全部 mesh 枪瞬间跌回 collector」，上游 TML 记载过同款事故（本仓 `RenderDistance.
isGuiRender()` 正是那种时间戳窗口，第 2 步因此不拿它当闸门）。

NeoForge 没有等价事件，本仓改为 mixin 注入 vanilla 的 `Screen#extractRenderState`
（`ScreenExtractMixin`）。两处关键取舍：

- **挂点存在性自证**：`GunRefitScreen extends Screen`（直接继承）覆写了
  `extractRenderState(GuiGraphicsExtractor, int, int, float)` 并调 `super.…`
  ⇒ 该方法在 `Screen` 上必然存在且可注入，不是猜的；
- **深度计数而不是布尔**：子类覆写里调 `super.extractRenderState(...)` 时，
  super 那次的 RETURN 会先触发，布尔会被它清零、外层剩下的提取阶段就漏了。

### 光照、额度与失效

- **光照档 LRU**（`MeshGpuLightCacheSize`，默认 4）：同屏不同光照的枪各用各的档；
  逐出的 VBO **延迟一帧释放**（本帧绘制表可能还引用它）；
- **每帧烘焙额度**（`MeshGpuBakeBudgetPerFrame`，默认 4）：病理场景（同帧光照档数
  超容量）回退 collector，而不是逐帧「逐出—重烘」打摆；额度与缓存容量解耦
  （缓存 = 显存开销，额度 = 每帧 CPU/上传开销，A6）；
- **分表禁用**：世界 GPU 失败**不再拖垮已实测过的手部路径**（A2），
  两者各有独立的会话标志；
- 世代号失效链路与第 1 步共用（光影翻转 / stride 变化）。

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
| `MeshGpuWorld` | **true** | 世界语境（第三人称/掉落物/展示框/展示台）GPU 烘焙（§2.7，需 `MeshGpuBaking`） |
| `MeshGpuLightCacheSize` | 4 | 每枪模保留的世界烘焙光照档数（LRU，1–16） |
| `MeshGpuBakeBudgetPerFrame` | 4 | 每帧最多执行多少次世界烘焙（1–64；超出当帧回退 collector） |

两个 GPU 键都有真实消费点（`PolyMeshGpuRenderer#shouldSubmitGpu`），
TOML + Cloth + 中英语言三处齐备，不是「没人读的配置」。

## 2.8 2026-09-02 轮：Fabric 26.2 线（`arena/01a05e3e`，tip `dee2578d`）实质改动同步

上轮取货点 `bf5bc5a`（R4，2026-08-31）。本轮逐 commit 核对她 `bf5bc5a..tip` 的
6 笔实质提交（+ 探针/文档），移植 5 项、判 4 项不适用。完整对照与回执见
`docs/records/REFAB_SYNC_0105E3E_R5_20260902.md`。

1. **纹理必须在 render pass 之外解析**（她 `99b15b28` 第 1 件，26.1.2 线
   `2ae4c29` 实机踩坑）：`TextureManager.getTexture` 对未加载纹理是懒加载
   （`CommandEncoder.writeToTexture`），pass 开着时上传被拒 ⇒ 全 GPU 提交的枪
   每个可见部件都没 collector 兄弟先请求贴图，我们的 pass 就是第一请求者 ⇒
   贴图永远加载不上 + 每帧报错 + 枪面紫黑（26.1.2 duyupack kar98un 复现）。
   `drawList` 现在在 `createRenderPass` 之前预解析 `viewsByTexture`；
   解析失败按纹理去重打日志（全 GPU 枪失败时逐帧重试，不去重会刷屏）。
   本仓 `resolveTextureView` 与她基线**逐字相同**，同病同修。
2. **预热窗口同样挡 `allChanged`**（她 `99b15b28` 第 2 件之保险带一，
   26.1.2 线 `d3f0fdc` 实机 ESC 崩溃链）：`LevelExtractorScopePassMixin` 的
   取消闸从「仅镜内那一遍」扩到「镜内那一遍 **或** `isBuildingScopePipeline()`
   预热构建窗口」。窗口极窄（一次 `preparePipeline` 调用），误伤一次真实重载
   的概率可忽略，cancel 本身无害（它刷新的全局方块 id 状态主管线早已设好）；
   取消的 reload 从未执行，Voxy 不会收到通知，无需补偿。
   **保险带二不移植**（`releaseScopePipelineIfPresent` 的拒释放熔断 +
   `VoxyScopePipelineCompat.isForeignVoxyBoundTo`）：本仓**从未引入**姊妹线那套
   「空闲释放瞄具管线」实验入口（见 `IrisScopePipelineCompat` 头注释移植说明
   第 2 条，R4 前已裁定「探针不是修复」），没有可熔断的对象。
3. **开镜 mesh 枪身目镜裁剪**（她 `7227ff99` 捎带的 5.2-bis 第 9 项，26.1.2
   线 `ee77059` 点名的同款缺口）：collector 提交的枪身经
   `ScopeBodyRenderTypes.clipForViewmodel` 换成 SCOPE_MASK 管线，GPU 手部表
   画的 mesh 枪身却走自己的管线、从不经过那次替换 ⇒ mesh 枪管穿进镜内画面。
   修法按**本仓掩码语义**（非 26.1.2 深度孔径架构）：
   - 无光影裸 pass：新 `LIT_CLIPPED_PIPELINE`（`core/scope_body` + SCOPE_MASK，
     pass 内直接绑掩码，NEAREST 采样同 `ScopeMaskTextureHandle` 的理由）；
   - 光影 RenderType 路线：手部表的 `entityCutout` 过一遍
     `clipForViewmodel`（与 collector 枪身**同一份**替换；scope_body_clipped
     的 Iris 链路已被立方体枪身实证）；
   - 两路共用 `maskReadyForViewmodel(true)` 判据 ⇒ 与立方体裁剪同开同关，
     不出现「立方体裁了 mesh 没裁」的分叉；世界表不裁（世界枪本就该出现在
     镜内画面里）。
   配套：`ScopeBodyRenderTypes` 加 `maskSamplerLayout()`/`maskSamplerName()`
   两个只读出口（同一 layout 实例 = 同一 sampler 名）。
4. **开镜距离补偿**（她 `08869095`）：两道距离闸门（`MeshMaxRenderDistance`
   48 / `MeshWorldFullDetailDistance` 16）按裸眼距离调参，但提交每帧只在
   extract 阶段过一次、镜内那遍复用同一批节点 ⇒ 4x 镜下 48 格上限观感只剩
   12 格 ⇒ 举镜看到的掉落物/第三人称 mesh 枪几乎必然是立方体（实机回报
   「二次渲染镜头里还是未烘焙」）。「多远该有细节」本质是角尺寸判定：
   现闸门阈值乘以 `ScopePipRenderer.currentDetailZoom()`
   （`1+(zoom-1)·progress`，随开镜进度渐变、收镜回 1，经典变焦与 PIP 皆适用），
   带 `Throwable` 守卫（scope 线故障绝不连坐 mesh 闸门）。
5. **PIP 二次渲染：镜内那遍世界表「各自登记、各自画、画完即清」**
   （她 `3151adcd` → `dc24a2b7`，先「可观测」后按实机改判移植姊妹线修法；
   与 1.21.11 `237dc153` / 26.1.2 `db360639` 同因同修）：
   - **错判记录（不要重蹈）**：`3151adcd` 先裁定「26.2 的世界提交只发生在
     extract 阶段一次、镜内那遍只是重画同一批节点」并加了哨兵日志；用户实机
     latest.log 把哨兵行打了出来 ⇒ **镜内那遍确实在重新提交**。错在把
     「extract 产出**提交节点**（每帧一次）」误读成「extract 完成**模型
     提交**」——把节点画出来的那一步（枪模 `submit`，即
     `shouldSubmitGpuWorld` 的调用点）在**每一遍** `LevelRenderer#render`
     各跑一次。
   - 修法：`shouldSubmitGpuWorld` 删除镜内拒收（原位留说明 + log-once 播报）；
     `renderWorldAfterSolid` 镜内那遍**画完即清表**（镜内有自己的表；不清则
     主遍把镜内登记的条目再叠画一遍——白付一倍顶点、半透明骨骼叠加加倍），
     `worldDrawnThisFrame` 仍只在主遍置位；首画日志改报真实表名
     （hand/world），世界表在自定义 pass 上的首画单独记一次。

**判为不适用（本线架构差异，全部读码核实）**：

- **FCAP 保存断桥桥接**（她 `7227ff99` 的 `ConfigPersist` +
  `LoadingConfigEvent.track` + Cloth `savingRunnable` 改 `ConfigPersist::saveAll`）：
  那是 **Fabric Config API（FCAP）26.x** 的断桥（FCAP 的 `ConfigValue.set` 只写
  内存、FCAP 兼容层 `ForgeConfigSpec.save()` 恒 no-op，必须显式调
  `LoadedConfig.save()`）。本线是**原生 NeoForge `ModConfigSpec`**：
  `save()` = `checkNotNull(loadedConfig) → loadedConfig.save()`（night-config
  落盘，已核 NeoForge 源码 + 本线 `MenuIntegration` 的
  `setSavingRunnable` 早已对 `CommonConfig`/`ClientConfig` 同款调用且经 R2
  实机），`/tacz overwrite` 那条绕过面板的入口上一轮已补显式
  `spec.save()`。无 FCAP ⇒ 无此病。
- **`tacz:nbt` 注册一等 ingredient**（她 `61345c58` 新增
  `TaczNbtIngredient`，Fabric `CustomIngredient`）：本线（与 1.21.11 姊妹线
  同判定）不注册新类型，`RecipeCompat` 把 `tacz:nbt` 改写为已注册的
  `tacz:partial_nbt`（`strict = !partial`，`items` 字符串→数组），
  `PartialNbtIngredient` 的 strict/partial 双语义 + 带 NBT 的
  `display()` 与她的 `TaczNbtIngredient` **逐语义等价**（已对读两实现）。
  她在提交信息里称「NeoForge 家族继承上游 tacz:nbt 注册」——与本线不符
  （本线 `ModRecipe` 只注册 `partial_nbt`），但改写路径已全覆盖；若将来野包
  出现 `neoforge:ingredient_type: tacz:nbt` 原生写法，再补注册别名（记录在案）。
- **空闲释放的拒释放熔断**（保险带二，见上第 2 条）。
- **日志级别差异**：她 `7227ff99` 把 `GunSmithTableIngredient` 解析失败记
  `LOGGER.error`，本线（随 1.21.11 姊妹线）为 `LOGGER.warn`——同一修复的
  级别取舍，两条 NeoForge 线一致，保留 `warn`，不跟 Fabric 线改。

### 5. 验证清单

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
13. **全 GPU 枪的贴图**（§2.8 第 1 件）：每个可见部件都走 GPU 表的枪
    （duyupack kar98un 级）贴图正常、不紫黑，latest.log 不逐帧刷
    `Failed to resolve texture view`（失败按纹理只记一次）。
14. **开镜 mesh 枪身裁剪**（§2.8 第 3 件）：开镜时 mesh 枪管不穿进镜内画面
    （与立方体枪身同开同关：`MeshGpuBaking=false` 时行为不变）；松开右键
    枪身完整；低倍 sight 的 reticle-only 掩码不啃枪身；光影下同一行为。
15. **开镜距离补偿**（§2.8 第 4 件）：4x/8x 开镜看 30-100 格外的掉落
    mesh 枪 → 应为高模（补偿前同距离同镜头是立方体）；收镜后远处枪恢复原
    距离行为；帧率无异常（只是让更多枪走已烘焙路径，O(骨骼)）。
16. **PIP 镜内世界高模**（§2.8 第 5 件）：`ScopePipRerender=true` + 4x 以上
    开镜，视野里放一把超 `MeshWorldMaxVertices` 的 mesh 枪 → 镜内那遍也是
    高模（不再是立方体），主画面不出现双影/叠画（镜内表画完即清）；日志各
    出现一次 `GPU world mesh pass active inside the scope PIP re-render pass`
    与 `World mesh submits are produced inside the scope PIP re-render pass
    on 26.2 too`。
17. **预热窗口重载闸**（§2.8 第 2 件）：首次开镜（预热刚跑完）附近改区块
    视距 / F3+A，主画面远景不错乱、无 `Tried to use destroyed RenderTargets`
    类崩溃；日志若打印 `Suppressed a full renderer reload … prewarm build`
    属预期。

### 5.3 已知边界（如实）

- **第 2 步之前**的旧结论（已作废）：「GPU 路径只覆盖第一人称手部 pass，世界 /
  掉落物 / 展示框恒走 collector」。`ba59ff5` 之后世界语境也走 GPU（§2.7），
  **只剩 GUI/Screen 预览、translucent 骨骼、GPU 失败回退**三类还在 collector。
- **第 1 步的实机状态**（2026-08-31 回填）：维护者复测 §5.2 第 8–12 条**全部通过**
  （GPU baked 日志节流正常、朝向随视模、换弹双弹匣位置正确、Iris 光影翻转不拉伸、
  `MeshGpuBaking=false` 与合并前一致）。这条结论来自维护者实测，不是本 sandbox 跑出来的。
- **仍未量化**：第 1/2 步的**帧率收益数字**一个都没有 —— 只有「成本从 O(顶点)
  降到 O(骨骼)」这个机制性结论。多人满屏高模枪的 fps 对比是第 2 步最该出数字的
  地方，至今无人跑过（§5.4 第 8 项）。
- 36 万顶点级高模第一人称**仍有帧率成本**（每帧 O(顶点) CPU 变换 +
  逐顶点 VertexConsumer 调用）。这是路线图第 1/2 步要解决的，本轮不解决。
  （第 1 步已落地后，这句话只在 `MeshGpuBaking=false` 或非第一人称时成立。）
- PIP 二次渲染（`ScopePipRerender=true`）时镜内那遍会重放 collector 回调，
  poly 成本 ×2。降级方案在路线图方向 3，待镜内行为实机确认后做。

### 5.4 世界 GPU 烘焙的验证矩阵（第 2 步）

维护者 **2026-09-01 报告实机 PASS**，但未逐条回报下面九项。
`MeshGpuWorld` 默认开，所以发版前若要拿这一条当依据，需要补齐逐条结果
（尤其第 4、6、8 项）。异常时先 `MeshGpuWorld=false` 确认是否由第 2 步引起，再回报。

1. 无光影：掉落一把高模 mesh 枪 → 位置/贴图/光照正确，日志出现 `GPU world-baked …`；
2. 第三人称（F5 或第二个客户端）：手持高模枪正确，换弹/开火动画正常（逐骨骼矩阵
   天然跟随动画）；
3. 展示台雕像 / 物品展示框：位置与投影正确；
4. **开背包 / 枪匠桌**：GUI 预览照常（collector），**同屏世界里的 mesh 枪不消失也
   不掉帧** —— 这条专门验证 `ScreenExtractMixin` 的窗口是否框对（开背包若全场景
   跌回 collector，说明窗口开太大，等同上游那起事故）；
5. 明暗差异场景（洞口 / 火把旁）放多把枪：各枪光照正确，日志烘焙次数收敛
   （不逐帧重烘）；
6. 光影：世界 mesh 枪照明与立方体一致（gbuffers_entities 接管）—— **风险最高**，
   异常时 `MeshGpuWorld=false` 回退并回报；
7. 开镜（PIP）：镜内那遍世界枪仍在（不消失、不双影），**且镜内也是高模**
   （超 `MeshWorldMaxVertices` 的枪在镜内不应退化成立方体 —— §2.8 第 5 件）；
8. **多人满屏高模枪的 fps 对比**（`MeshGpuWorld` 开/关）—— 第 2 步最该出数字的一条，
   至今无人跑过；
9. 光影开关翻转：世界枪不拉伸（世代号失效链路与第 1 步共用）。

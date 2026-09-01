# 内置 TacZ Mesh Loader（poly_mesh + GPU 烘焙）

适用版本：Minecraft 1.21.11 + NeoForge 21.11.x（本页只描述本 1.21.11 线）。
本页是内置 TacZ Mesh Loader（包名 `cn.sh1rocu.tacz.compat.meshloader`，下称 TML）
的说明文档：出处与许可、配置键默认值、法线/绕序（下游审查 A10）与家族分歧注记、
待实机清单。

> **状态（如实声明）**：TML / GPU 烘焙 / 镜内裁切 / 光影下烘焙在本线**尚未实机验证**
> （仅 CI 编译门绿）。任何「已可用 / 已修复」的结论都以本文末尾的待实机清单
> 勾选为准；未勾选前不构成可用性声明（AGENTS §2）。

## §1 这是什么

内置 TML 给枪包里的 `poly_mesh`（高模网格）部件提供两条渲染路：

1. **collector 路**：pose 烘进顶点、逐顶点提交（第 0 步，始终可用，是 GPU 路的
   兜底）；
2. **GPU 静态烘焙路**：顶点常驻 VBO（骨骼本地系 + light 烘进顶点），每帧上传
   O(bones) 骨骼矩阵（第 1/2 步手部、第 3 步世界语境）。

两条路都只替换「高模」层；原版 Bedrock 立方体模型照常渲染，mesh 枪包缺失时
整条路径不触发。性能边界见 `docs/TML_GPU_FEASIBILITY_1211_20260831.md`。

## §2 许可与出处

- 移植自 **VellEagle/TacZMeshLoader `1.21.1_fabric` v0.1.7**（GPL-3.0；
  **不是**官方 TaCZ 附属项目）。每个移植文件的类注都带这句出处。
- 本仓 `LICENSES.md` 有对应条目：`cn/sh1rocu/tacz/compat/meshloader` 20 个源文件，
  仅代码、不打包原作美术/音频。
- mesh 家族（TML 谱系、poly_mesh 谱系等）成员之间存在许可与法线/绕序假设上的
  分歧；本移植跟随 VellEagle/TacZMeshLoader v0.1.7 的语义，再叠加 26.2 线
  下游审查（A10 等）采纳的现代修复形态（见 §6）。再分发时以上游仓库与
  GPL-3.0 文本为准。

## §3 架构要点

- **两张绘制表**：`HAND_DRAWS`（第一人称手部）与 `WORLD_DRAWS`（世界语境），
  各在自己的 flush 处消费；GUI / Screen 内嵌预览 / 镜内那遍 / 阴影 pass 的
  submit 在**提交侧**被闸门拒收，不会泄漏进世界 pass（关 PR #33/#69/#70/#71
  的「帖图不对」bug 正是提交侧没有语境闸门）。
- **绘制点**：手部在 `ItemInHandRenderer#renderHandsWithItems` 的 RETURN
  （= 当次 flush 紧后），世界在 `FeatureRenderDispatcher#renderAllFeatures`
  返回处 —— 字节码审计见 `docs/TML_GPU_STEP2_HANDFLUSH_20260831.md`。
- **钩子存活证明**：只有上一帧真的跑过 flush 钩子才允许跳过 collector；
  mixin 失效 → 下一帧自动回 collector，不会枪体消失。
- **静默回退**：GPU pass 失败只置会话级内存标志，不回写配置、不逐帧刷屏
  （见 `docs/REVIEW_UPSTREAM_TML_GPU_262_20260831.md` A2）。
- **纹理解析在 pass 体外**：`TextureManager#getTexture` 对未加载纹理的同步
  加载会在 pass 内抛「Close the existing render pass」，因此纹理视图整批先解析。

## §4 配置键与默认值

TOML 段 `mesh_loader`（`tacz-client.toml`），Cloth 面板路径
`config.tacz.client.render.mesh_*`（en/zh 语言键齐备）。**改动生效方式**：
多数即时生效；`MeshPoly*` 三个法线/绕序键需要资源重载（F3+T）后重新解析模型。

| TOML 键 | Cloth 路径 | 默认 | 说明 |
|---|---|---|---|
| `MeshEnable` | `mesh_enable` | `true` | 总开关：poly_mesh 渲染。关闭后 mesh 枪只有立方体层（与未装 TML 相同） |
| `MeshPolyMirrorReverseWinding` | `mesh_poly_mirror_reverse_winding` | `true` | A10 修复：镜像（奇数次轴翻转）时反转发射绕序，见 §6。F3+T |
| `MeshPolyInvertNormals` | `mesh_poly_invert_normals` | `false` | 诊断用全局法线取反。高光仍在错误一侧时试。F3+T |
| `MeshPolyPreferPackNormals` | `mesh_poly_prefer_pack_normals` | `false` | 逐顶点消费枪包平滑法线（否则逐面平坦法线）。F3+T |
| `MeshPolyIlluminatedRealSky` | `mesh_poly_illuminated_real_sky` | `false` | `_illuminated` 骨骼光影下 sky 用环境真值，见 §5.8/§5.9 |
| `MeshPolyInShadow` | `mesh_poly_in_shadow` | `false` | 阴影 pass 是否画 poly（光影才有意义；省半帧顶点成本） |
| `MeshMaxRenderDistance` | `mesh_max_render_distance` | `48.0` | 世界语境最大渲染距离（格）；0 = 无限。第一人称恒全模 |
| `MeshPolyInPreview` | `mesh_poly_in_preview` | `true` | GUI / FIXED 预览语境是否画 poly |
| `MeshLogStats` | `mesh_log_stats` | `true` | 模型加载时打一行统计（骨骼/顶点/半透明/自发光/弹匣） |
| `MeshGpuBaking` | `mesh_gpu_baking` | `true` | GPU 静态烘焙总闸（第一人称手部）。失败自动回 collector |
| `MeshGpuUnderShaders` | `mesh_gpu_under_shaders` | `true` | 光影下手部 GPU 路。**默认开**（维护者 2026-09-01 裁定） |
| `MeshGpuWorld` | `mesh_gpu_world` | `true` | 世界语境 GPU 路（需 `MeshGpuBaking`） |
| `MeshGpuWorldUnderShaders` | `mesh_gpu_world_under_shaders` | `true` | 光影下世界 GPU 路。**默认开**（同上裁定） |
| `MeshGpuLightCacheSize` | `mesh_gpu_light_cache_size` | `4`（1–16） | 每模型量化光照档 LRU 容量（显存语义；上游缓存 8 档未量化） |
| `MeshGpuBakeBudgetPerFrame` | `mesh_gpu_bake_budget` | `4`（1–64） | 每帧世界烘焙额度（CPU/上传语义；与 LRU 容量解耦，A6） |
| `MeshGuiMaxVertices` | `mesh_gui_max_vertices` | `65536` | GUI 语境顶点预算；超出画立方体或枪包 LOD。0 = 无限 |
| `MeshWorldMaxVertices` | `mesh_world_max_vertices` | `120000` | 世界语境顶点预算；同上。GPU 世界路存活时不参与 |
| `MeshWorldFullDetailDistance` | `mesh_world_full_detail_distance` | `16.0` | 此距离内世界语境恒画全模（无视预算）。0 = 无豁免 |
| `MeshMaxModelVertices` | `mesh_max_model_vertices` | `120000` | 加载时软警告阈值（只打日志，不改渲染）。0 = 不警告 |

## §5 行为注记

### §5.1 语境闸门

第一人称恒全模；世界语境受 `MeshMaxRenderDistance` 与 `MeshWorldMaxVertices` 约束，
`MeshWorldFullDetailDistance` 内豁免预算；GUI / FIXED / HEAD 受 `MeshPolyInPreview`
与 `MeshGuiMaxVertices` 约束；阴影 pass 由 `MeshPolyInShadow` 决定；光影状态由
`ShaderStateTracker` 每帧采样缓存。

### §5.2 开镜角尺寸补偿

两道距离闸门都按**裸眼**距离调参，但开镜把远处物体的角尺寸放大 Z 倍
（4x 镜下 48 格观感只剩 12 格）。`PolyRenderPolicy#detailZoom()` 把阈值乘上
当前放大倍数（`ScopePipRenderState.currentDetailZoom()`），随开镜进度渐变、
收镜回 1；经典整屏变焦与 PIP 二次渲染都适用。补偿在**提交侧**做
（镜内那遍复用 extract 阶段的同一批提交节点，闸门只在提交时过一次）。

### §5.3 镜内目镜裁剪（联动镜内裁手）

手部 GPU 表在「本帧确有完整目镜掩码周期 + 当前倍率不低于低倍底线」时，
lit 批次换用孔外剔除变体并绑定两份实时深度拷贝（`mesh_entity_scope_clip.fsh`）。
其余情况（世界表 / GUI / 无镜 / 掩码失效 / 低倍镜）一律普通管线 ——
**失败语义 = 未裁剪外观，不会更糟**。

### §5.4 弹匣（关 PR #70 的教训）

上游在 `loadPolyMesh` 后把 `additional_magazine` 的 FunctionalRenderer 包一层；
关 PR #70 没接这条链路，纯 mesh 枪的弹匣会丢。本仓：exclude
`additional_magazine` 子树避免主遍历画错位置 → 立方体 + `IMirrorGeometry`
照常 → 主 poly（含 `magazine`）走 collector → `additional_magazine.visible`
时在该节点变换下补交 poly（与上游 `renderSubtreeDirect` 同构）。

### §5.5 顶点预算与降级

超出预算的模型画立方体层（或枪包自带 LOD 模型）。预算只保护远处/密集场景，
近距离由 `MeshWorldFullDetailDistance` 豁免（见 §5.1）。

### §5.6 日志判据（实机确认用）

- `loggedFirstDraw` / `loggedFirstWorldDraw`：首帧 GPU 路各一条 info；
- `loggedScopeWorldDraw`：**镜内那一遍（PIP 二次渲染）首次吃上世界 GPU 表**时
  一条 info —— 供实机确认开镜路径生效；
- 纹理失败每张纹理只报一条（去重集合，不再逐帧刷屏）；
- 加载统计一条/模型（`MeshLogStats`）。

### §5.7 延迟释放池

世界 LRU 逐出的 VBO 先进延迟释放池、下一帧才 close：同帧内两个实体共享同一
模型实例时，当场 close 会让帧末绘制引用已销毁的 buffer。

### §5.8 `_illuminated` 自发光骨骼与 `MeshPolyIlluminatedRealSky`

骨骼名以 `_illuminated` 结尾的部件按 block=15 且 sky=15 烘焙（与
`BedrockPart#render`、`PolyMeshModel.FULL_BRIGHT = 15728880` 同值）：无光影下
原版光照图把两列相乘，sky=0 会全黑，所以「永远看得见」必须两边拉满。
但光影包把 sky 分量读成「这块表面看得见天空」：常亮 15 = 告诉光影包
太阳/月亮永远照得到我 ⇒ 枪身按天空亮度被照明，屋顶墙都遮不住。
`MeshPolyIlluminatedRealSky` 打开且装光影包时，sky 用环境真值、block 仍保 15
（洞里照样看得见，但不再声称晒得到太阳）；无光影下逐字保持上游行为。

### §5.9 「近乎全黑」回归的真因（为什么 `MeshPolyIlluminatedRealSky` 默认关）

历史症状：镜像 + 光影包下整枪近乎全黑。**早期假设**是 §5.8 的 sky 分量问题，
`MeshPolyIlluminatedRealSky` 就是按那个假设写的（所以它的注释说「written
against an early reading of the shader report」）。**真因**后来查明是别的：
绕序反转后，poly 的 collector 路径走 `entityCutout`（1.21.11 该管线默认剔除
背面），被剔的正是朝外面 → 近乎全黑。修复 = 四条 collector 提交路径改
`entityCutoutNoCull`（GPU 管线本就 `withCull(false)`），反转不再吞面。
`MeshPolyIlluminatedRealSky` 因此保持 opt-in，直到有人实机确认它更好看。

### §5.10 光影下两条 GPU 路（默认开）与静默回退

`MeshGpuUnderShaders` / `MeshGpuWorldUnderShaders` 默认 **true**（维护者裁定，
对齐 26.2 R3 定稿与 26.1.2）：常驻 VBO 在光影下的收益胜过每帧 CPU 重变换；
「高模枪挡住太阳/月亮的那部分几何继承天体自发光亮度」是已知、可观测、
可整键关闭的取舍，不再作为默认关的理由。两条路各自：
pass 开在 Iris 自己的 flush 内（gbuffer 内、`IrisApi.assignPipeline(HAND)` /
`ENTITIES` 登记）；需要经审计的 Iris 1.10.x，flush 钩子不活则拒收提交、
枪保持 collector 路；失联/异常时各自静默回 collector；光影下拿不到 lightmap
直接回 collector（`gpuMasterUsable`），**不再**整路退化 EMISSIVE
（那条管线在光影包眼里是「自发光、不受阴影」）。

## §6 法线/绕序（下游审查 A10）与家族分歧注记

mesh 家族不同成员对 **Blender/Blockbench 导出 OBJ 的法线与面绕序**存在不同
假设；A10 病根：Y 镜像翻转每个面的正反，烘焙出的朝外法线与
`gl_FrontFacing` 矛盾 —— 光影包写 `normal *= gl_FrontFacing ? 1 : -1` 时
高光贴到错误一侧。

本线按 26.2 `bb6fcb61` 采纳的现代形态落地（维护者当面指令取该形态）：

- 镜像（奇数次轴翻转）时**反转发射绕序**（`MeshPolyMirrorReverseWinding`
  默认 **true**），使变换后绕序叉积与烘焙法线一致；
- 退化面优先退回枪包法线、再退化为确定方向 `(0,1,0)`，**绝不写零向量**
  （杜绝 NaN 高光）；
- `MeshPolyPreferPackNormals` 逐顶点消费枪包平滑法线（上游恒平坦着色，
  打开不构成对上游的偏离——上游同一分支被常量编译掉了）；
- 配套：四条 collector 提交路径改 `entityCutoutNoCull`（见 §5.9）。

**已知分歧注记**：26.2 线 `98298fa6` 最终把该键默认又调回 false（理由是
「真实枪包绕序本就与镜像一致」）。本线按维护者指令取 true；若实机表明需要
跟随 26.2 终态，仅需改 `MeshyConfig` 一个默认值（注释已写明可回退）。
枪包作者自测：绕序已与镜像一致的自制包可关此键；Blockbench/Blender 导出时
分别试「翻转 Z / 翻转法线」组合，记下哪种与游戏内一致后按 README 反馈格式回报。

## §7 已知限制与待实机清单

**已知限制（设计使然，非缺陷）**：

- translucent 骨骼不烘进 GPU（混合顺序交给 collector）；
- 换弹 `additional_magazine` 恒走 collector（矩阵语义不同且非顶点热点）；
- 世界 GPU 路存活时 `MeshWorldMaxVertices` 不参与（该路无每顶点 CPU 成本）；
- 法线/绕序三键需要 F3+T 才重解析模型。

**待实机清单（未验证，勾选前不得宣称已可用）**：

- [ ] GPU 烘焙第 1/2/3 步在真实客户端上的路径生效（判据：`loggedFirstDraw` /
      `loggedFirstWorldDraw` 日志各一条；光影下 `gbuffers_hand` 内正确受光）；
- [ ] 光影两键默认开实机观感（含「遮挡日月处继承天体亮度」取舍的观感复核）；
- [ ] A10 修复实机：光影包下高光侧正确；`MeshPolyMirrorReverseWinding`
      开/关两态与 Blockbench 导出组合的对照（§6）；
- [ ] 镜内裁切联动（§5.3）：开镜时手部 mesh 枪被目镜孔径裁剪；
- [ ] 开镜角尺寸补偿（§5.2）：4x 镜下远距离掉落物/第三人称 mesh 枪仍是
      高模（`loggedScopeWorldDraw` 应出现一条 info）；
- [ ] 静默回退：断开 flush 钩子（如拔掉 Iris 或改映射）后下一帧自动回
      collector，枪体不消失；
- [ ] PIP / 镜内画中画相关配置键实机（见
      `docs/records/SYNC_SIBLING_0105DB2_20260901.md` §4）；
- [ ] `MeshGpuBakeBudgetPerFrame` 在跨光照档场景不出现逐帧重烘抖动。
- [ ] 验证通过后把截图/日志证据放入 `docs/records/`，并回来勾选本清单。

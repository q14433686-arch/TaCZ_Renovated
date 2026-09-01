# 内置 TacZ Mesh Loader [TML] —— NeoForge 26.1.2 移植

> **来源与许可**：代码移植自 [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
> `1.21.1_fabric`（**GPL-3.0**），经姊妹项目
> [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
> 26.1.2 线（`arena/01a05db3`，含该线对上游的全部 26.1.2 修正）再移植到本仓
> （NeoForge 26.1.2）。全链公开源码、可审计；GPL-3.0 义务见
> [`../LICENSE`](../LICENSE) / [`../LICENSES.md`](../LICENSES.md)。
> 各源文件头保留「移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)」声明。
>
> **状态（2026-09-02，如实声明）**：
> - **编译级完成**：CI `compile-check` 在 `89db8081` 上 BUILD SUCCESSFUL。
> - **齐平自查通过**：`python3 docs/check_mesh_config_parity.py` → 19 TOML ↔ 19 Cloth ↔
>   38 语言键，键/字段绑定/默认值/区间/en·zh 全对齐。
> - **实机已验证（维护者本机）**：2026-08-31 – 09-01 多轮实机——高模枪包
>   duyupack kar98un 的贴图错误 / “Close the existing render pass…” 刷屏（v2 修复）、
>   Sodium 下 PIP rerender 投影错位（v3 修复）、2026-09-01 光影首测（ComplementaryUnbound
>   r5.8.1）的 PIP 冻结 / 遮光罩累积 / ESC 崩溃（RawOutput.log）/ 镜内残影、PIP 二次渲染
>   中镜内高模枪打成立方体（v5 修复）等，均由实机发现并修复；取证出处见
>   `docs/publish/RELEASE_NOTES_26_1_2_R2.md` §已核验 与各移植记录。
> - **§5 分项未逐条留档**：主体已实机，但下面矩阵的每一项结论仍需逐条补记
>   （没验的写「待实机」，本文不写 PASS）。
>
> 姊妹线（Fabric 26.1.2）对应文档为
> [`docs/MESH_LOADER.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/01a05db3/docs/MESH_LOADER.md)
> ——其中 §1「18 项」、§2「scope PIP 无镜内二次渲染」、§6 边界「镜内画但不清表」三处
> 在姊妹线自己的后续提交（`091dd5ec` / `db360639`）落地后**已过时**；本仓版按下文
> 现状改写，不再逐句照搬。

## 0. 这是什么

`poly_mesh` 内置加载器：枪包 geo.json 的骨骼可以带 `poly_mesh` 数组，
按骨骼本地坐标解析成静态网格并延迟提交渲染。三层能力：

1. **collector 安全子集**（第 0 步）：`SubmitNodeCollector.submitCustomGeometry`
   延迟提交，submit 当刻冻结骨骼矩阵快照；geo 解析缓存；顶点预算闸门；
   弹匣双通道（`IMirrorGeometry` + `additional_magazine` 补画）；半透明骨骼拆分。
2. **GPU 静态烘焙**（第 1/2 步，第一人称手部）：顶点留在骨骼本地坐标、
   光照量化后烘进顶点，常驻 VBO；每帧只上传 O(骨骼) 个矩阵；
   绘制点在「手部 geometry flush 之后」。
3. **世界语境 GPU**（第 3 步）：第三人称手持 / 掉落物 / 展示框 / 雕像。
   提交侧闸门拒收 GUI / Screen 提取 / 阴影 / 手部语境；镜内那一遍（PIP 二次渲染）
   **放行**（见 §2.3）；按量化光照档做 LRU 缓存 + 每帧烘焙额度；失败半径分表
   （世界挂了不影响手部）。

## 1. 配置（`tacz-client.toml` 的 `[mesh_loader]`，19 项）

| 键 | 默认 | 说明 |
|---|---|---|
| `MeshEnable` | `true` | 总开关；关掉 = 行为等价于没装 |
| `MeshPolyMirrorReverseWinding` | `false` | 镜像时反转绕序。**保持关**（1211 实机否证；本仓消费层/自建管线均不剔面，见 §4） |
| `MeshPolyInvertNormals` | `false` | 全局取反烘焙法线 |
| `MeshPolyPreferPackNormals` | `false` | 用枪包逐顶点法线（平滑着色）；需 F3+T 生效 |
| `MeshPolyIlluminatedRealSky` | `false` | 光影下 `_illuminated` 骨骼的 sky 用环境真值（block 仍 15） |
| `MeshPolyInShadow` | `false` | 阴影 pass 是否画 poly（独立需求，别和 GPU 混） |
| `MeshMaxRenderDistance` | `48.0`（0..1e6） | 世界语境 poly 最大距离，0=无限；**开镜时按 `ScopePipRenderState#currentDetailZoom()` 放大**（§3.1） |
| `MeshPolyInPreview` | `true` | GUI/FIXED/HEAD 预览语境是否画 |
| `MeshLogStats` | `true` | 模型加载统计日志（按 geo 去重） |
| `MeshGpuBaking` | `true` | GPU 静态烘焙总闸；失败自动回 collector |
| `MeshGpuUnderShaders` | **`true`** | 光影下手部也走常驻 VBO（R3 定稿；§3 的亮度继承是已知取舍） |
| `MeshGpuWorld` | `true` | 世界语境也走常驻 VBO |
| `MeshGpuWorldUnderShaders` | **`true`** | 光影下世界也走常驻 VBO（R3 定稿；同 §3 取舍） |
| `MeshGpuLightCacheSize` | `4`（1..16） | 世界 LRU 的光照档容量（**显存语义**） |
| `MeshGpuBakeBudgetPerFrame` | `4`（1..64） | 每帧世界烘焙额度（**CPU/上传语义**，与 LRU 容量独立；v5 新增） |
| `MeshGuiMaxVertices` | `65536`（0..1e7） | GUI 预览顶点预算，超出只画立方体 |
| `MeshWorldMaxVertices` | `120000`（0..1e7） | 世界语境顶点预算；`MeshWorldFullDetailDistance` 内全豁免 |
| `MeshWorldFullDetailDistance` | `16.0`（0..1024） | 近距全模豁免距离，0=关；开镜时同样 ×currentDetailZoom() |
| `MeshMaxModelVertices` | `120000`（0..1e7） | 单模型顶点告警线（不改变渲染） |

19 项全部接进 Cloth 局内面板（Render 配置页），三方齐平由
`docs/check_mesh_config_parity.py` 把关。

## 2. 26.1.2 的消费点（本仓 v4/v5 定稿）

完整移植链路见 [`PORT_01a05170_TO_NEOFORGE_26_1_2_20260901.md`](PORT_01a05170_TO_NEOFORGE_26_1_2_20260901.md)（v1–v5）。
要点：

- **手部**：`ItemInHandRenderer#renderHandsWithItems` 尾部自己就是
  `renderAllFeatures()` + `endBatch()`；GPU 手部表在该方法 RETURN 处消费
  （`ItemInHandRendererMixin`，`require=0`）。Iris 26.1 只是把这两个调用换成
  `HandRenderer#endRender()`、从同一个方法进来 —— 注入点不变。
- **世界**：26.1.2 的世界 feature flush 在主 pass 的 `renderSolidFeatures` RETURN
  处消费（`FeatureRenderDispatcherMixin`，`require=0`），消费窗口由
  `GameRendererMixin` 对 `LevelRenderer.renderLevel` 调用的 `@Redirect` +
  try/finally 圈定（`setLevelRenderActive`）。`renderItemInHand` 开头的预 flush 与
  GUI 调用点被 `inHandPass` / `levelRenderActive` / `ScreenRenderTracker` 门正确拒收。
- **顶点格式**：26.1.2 常量是 `DefaultVertexFormat.ENTITY`；Iris 26.1 的
  `MixinRenderPipeline#iris$change` 在光影 + level 渲染期间换成扩展实体格式 ⇒
  烘焙格式「问绘制端当刻的 getter」+ 世代号同时认「光影开关翻转」与「消费格式变化」。
- **lightmap**：`LightTexture` 类消失；`GameRenderer#lightmap()` 直接给
  `GpuTextureView`；`pack(II)I` 不存在 ⇒ 按消费端公式内联
  `packLight(block, sky) = (block<<4) | (sky<<20)`。

### 2.3 镜内那一遍（PIP 二次渲染）—— v5 修正

26.1.2 的 `LevelRenderer#renderLevel` **每调用一次就重新提交一遍世界几何**：
`extractLevel` 每帧只跑一次、产出「逐帧状态袋」（`LevelRenderState`），真正写进
`SubmitNodeStorage` 的提交发生在**每一遍** render 阶段（镜内那一遍跑完才重跑
`extractLevel` 补状态）。因此：

- `shouldSubmitGpuWorld` **不再拒收镜内那一遍**（v5 移除该分支）——否则镜内只能回
  collector + 顶点预算，远处高模世界枪被预算打成立方体（「镜内不烘焙」的根因）；
- `renderAtWorldFlush` 在镜内那一遍**照常画、画完即清表、不记 `worldConsumedFrame`**
  （主画面那一遍是另一次独立提交，必须允许再次消费），并 log-once
  `GPU world mesh pass active inside the scope PIP re-render pass; drawing N ...`；
- 主画面那一遍随后会重新提交一份全新的表，两遍内容一致、不叠加。

**残留风险（姊妹未一并修）**：镜内那一遍的 flush 钩子某帧没跑时，已登记条目会
留到主画面那一遍叠画 —— 不属于本文档范围，出现即报。

## 3. 光影下的两条键默认值（R3 定稿：默认开）

1211 维护者实机：开着 `MeshGpuUnderShaders` / `MeshGpuWorldUnderShaders` 时，
高模枪挡住太阳/月亮的那部分几何会「继承」天体的自发光亮度，只有把这两键关掉才消失。
**本线 R3 定稿改回默认开**：常驻 VBO 在光影下的收益仍胜过每帧 CPU 重变换，亮度继承
是已知、可观测、随时可以整键关闭回退的取舍。连带修掉的缺陷：以前「拿不到 lightmap」
会一次性闩锁、把整条路永久退化到 EMISSIVE 管线；现在是每帧重试 + 光影下真取不到就
整条拒收回 collector（`gpuMasterUsable()` + `worldSubmitBlocker()` 原因串）。

26.1.2 上这两键的对应实测**还没做**（§5.2/§5.4 复测矩阵第一优先项）。

### 3.1 开镜距离补偿与低倍率闸门（v5 新增）

- **距离补偿**：`MeshMaxRenderDistance` / `MeshWorldFullDetailDistance` 原本按
  <b>裸眼</b>距离判定，而开镜把远处物体的角尺寸放大了 Z 倍（4× 镜下 48 格观感只剩
  12 格）。两道闸门在提交侧乘 `ScopePipRenderState#currentDetailZoom()`
  （`1 + (zoom-1)·progress`，随开镜进度渐变、收镜回 1）——经典整屏变焦与 PIP 两种
  模式都适用。
- **低倍镜不裁**：mesh GPU 手部批次的目镜孔径裁剪（`mesh_entity_scope_clip`）另加
  倍率下限 `ScopeRenderTypes#magnificationSupportsLensClip()`（阈值
  `ScopePipMinMagnification`，默认 4×）。低倍镜（含组合镜低倍档）没有放大画面可让位，
  裁掉的是枪身自己 = 枪上破洞。手臂/火光/枪身/配件同一条线，见
  [`SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)。

## 4. poly 绕序 × 背面剔除

- 26.1.2 上 poly collector 路径用的 `RenderTypes.entityCutout(texture)` 底层是
  `RenderPipelines.ENTITY_CUTOUT`，字节码实证该管线显式 `.withCull(false)`
  ⇒ **消费层不剔背面**：1211 那套「镜像位置但不反转绕序 ⇒ 整枪变黑」的解释在
  26.1.2 不成立；不自洽数据只会以「高光偏一侧」的更轻形态出现。自建 GPU 管线两条
  也都 `withCull(false)`。
- 枪包 `poly_mesh` 的绕序约定（从外看 CCW 还是 CW）需要真实枪包统计，**未测**。
- 本仓决定：**维持与上游一致、只记录不修**（`MeshPolyMirrorReverseWinding` 默认关，
  与 1211 实机否证一致）。

## 5. 实机复测矩阵（主体已实机；分项按步走，逐条补记结论）

### 5.1 第 0 步（collector 安全子集）
- [ ] mesh 枪包在背包 / 手持 / 掉落物 / 展示框都画得出来；
- [ ] 高面数枪按预算降级（日志有降级行）；
- [ ] 关 `MeshEnable` 行为等价于没装；
- [ ] `tacz.mesh.mixins.json` 4 条全应用（启动日志无 `Invalid mixin`）。

### 5.2 第 1/2 步（第一人称常驻 VBO，`MeshGpuBaking=true`）
- [ ] 第一人称枪与 collector 路径逐像素一致（同光照档 / 同缩放 / 同俯仰摆动）；
- [ ] 换弹 / 开火 / 检视连打：无双影、无残影；
- [ ] `GPU baked … bones` 只出现一次，之后每帧无日志；
- [ ] F3+T ×5：显存不单调增长；
- [ ] 光影下（`MeshGpuUnderShaders=true`）：位置朝向随相机正确变化、明暗变化照明跟着变、
      挡天体不继承自发光、Iris 卸载/换包/F3+T 只回 collector 不崩不黑屏。

### 5.3 第 3 步（世界语境，`MeshGpuWorld=true`）
- [ ] 他人手持的 mesh 枪随相机正确移动（「钉在视角方向 / 转身漂」= 变换取自错误时刻）；
- [ ] 近处高模枪不因预算整把消失；日志出现 `GPU world-baked N bones …`；
- [ ] 光照边界上一排掉落枪：`GPU world-baked` 只在前两次是 info 级；
- [ ] 开背包 / 枪匠台 / 热栏：世界里不多画、GUI 内不少画；
- [ ] 光影组合（`MeshGpuWorldUnderShaders=true`）：`Assigned mesh_entity_world to the Iris
      ENTITIES program.` + 夜晚变暗、进照明块变亮 + 5.2 的「挡天体不继承自发光」；
- [ ] 任一组合没生效时先查 `GPU world submit refused: <原因>` 行。

### 5.4 v5 新增验收项
- [ ] **PIP 二次渲染**（`ScopePipEnable` + `ScopePipRerender`）：开镜后镜内世界
      mesh 枪是<b>高模</b>（日志出现 `GPU world mesh pass active inside the scope PIP
      re-render pass; drawing N ...`），退镜仍正常；主画面与镜内不叠加不残影；
- [ ] **开镜距离补偿**：4× 镜下 48 格外的第三人称/掉落物 mesh 枪仍拿到 poly 细节
      （对照旧行为 = 立方体）；收镜后远处立即回立方体；
- [ ] **镜内裁手**：高倍镜 + PIP 关（或开）→ 目镜圈内无手臂/袖子，日志出现一次
      `In-scope arm clipping engaged`；
- [ ] **低倍镜豁免**：红点/全息/2×/3× 开镜 → 手臂、火光、枪身、配件全部正常绘制
      （无破洞）、无 `engaged` 日志；组合镜低倍档同、切高倍档恢复裁切；
- [ ] **烘焙额度**：`MeshGpuBakeBudgetPerFrame=1` 且同屏 >1 把世界枪 → 日志出现一次
      `World bake budget (1 per frame) exhausted; ...`，余枪回 collector 且不崩；
- [ ] Iris 光影下整族：2026-09-01 实机首测（ComplementaryUnbound r5.8.1）覆盖
      PIP 冻结 / 遮光罩累积 / ESC 崩溃 / 镜内残影等并已修复；mesh GPU 光影下
      的天体亮度继承、换包 / F3+T 回 collector 等分项仍待逐条补记。

### 5.5 收尾
- [ ] 本文件状态块改写成实机结论（没验的写「待实机」）；
- [ ] `python3 docs/check_mesh_config_parity.py` 保持 0 退出。

## 6. 设计不变量（从上游/1211 继承，改前先读）

1. `require=0` + 安全回退：注入失败的后果是「走 collector」，绝不是少画或崩。
2. 存活证明用**帧号比对**（钩子本帧/上一帧真跑过才允许跳过 collector）。
3. 变换取自**消费时刻**：顶点烘骨骼本地 pose，绘制时乘当刻
   `RenderSystem.getModelViewMatrix()`。
4. 烘焙不绑瞬间：世界路径按量化光照档 LRU（`MeshGpuLightCacheSize`）+
   每帧额度（`tryReserveBake`，`MeshGpuBakeBudgetPerFrame`）+ 延迟释放池；
   世代号同时认光影翻转与格式变化。**额度与容量是两个旋钮，别合并。**
5. 失败半径 = 一张表：世界 30 次连败只关世界；`catch (Exception | LinkageError)`；
   **绝不**在渲染路径里写配置。
6. 光影下兜底不得换照明语义：lightmap 取不到 ⇒ 每帧重试 + 光影下整条拒收。

边界（别当 bug 修）：半透明部件与弹匣永远走 collector；GUI/预览/阴影在提交侧拒收；
**镜内那一遍照常提交、照常画、画完即清表**（v5；主画面会重新提交自己那份）。

## 7. 本仓明确不做的事

- 不做 `Lightmap` 自定义烘焙/纹理拷贝（走 `RenderSystem.bindDefaultUniforms`
  + 现有光照贴图）。
- 不把半透明部件、弹匣搬进常驻 VBO。
- 不在世界路径用 `IrisProgram.HAND`（手部专项，世界用会串）。
- 不把手部与世界两张表合成一张（两个消费时刻的渲染状态不同）。
- 不在渲染路径写配置文件。
- `MeshPolyInShadow` 保持 false。
- 不「顺手修」绕序（见 §4）。

## 8. 枪包怎么用（枪包作者看这里）

geo.json 里 `poly_mesh` 数组挂在骨骼上，支持 `normalized_uvs`；骨骼名含
`translucent` 走半透明提交、以 `_illuminated` 结尾按自发光光照烘焙。枪本体在
display JSON 用：

```json
{
  "model_type": "mesh",
  "model": "mypack:gun/mygun_geo",
  "texture": "mypack:gun/uv/mygun",
  "animation": "mypack:mygun"
}
```

`model_type: "mesh"` 只对枪本身必需；配件 / 弹药 / 方块只要模型旁存在同名 geo 就会
替换。目镜物体不支持 mesh（与上游 TML 相同的限制：ocular 必须用立方体）。
改用法线/绕序相关开关后需 F3+T 重载资源才生效（值在模型解析期读一次）。

> 姊妹线 `fabric.mod.json` 的 `provides: ["taczmeshloader"]` 是 **Fabric 依赖标识**；
> 本仓为 NeoForge，没有等价 `provides`。依赖外置 TML（Fabric modid `taczmeshloader`）
> 的枪包在 NeoForge 下的表现出**未验证**——如遇「依赖不满足」类提示，属预期差异，
> 请按此条反馈。

## 9. 弹匣链路（架构约束记录）

`BedrockGunModel` 把 `additional_magazine` 的 FunctionalRenderer 设为返回
`IMirrorGeometry`（指向 `magazine` 节点）。poly 部分（`TaczPolyMeshGunModel#submit`）：
主遍历排除 `additional_magazine` 子树 → `super.submit` 照常 → 主 poly 快照提交 →
`additional_magazine.visible` 时补画该节点的 poly（`captureSubtree(mirrorRoot=true)`）。
半透明部件与弹匣永远走 collector（§6 边界）。

## 10. 背景文档（姊妹侧证据链；本仓未整搬，按需跳转）

- 姊妹 TML 主文档（Fabric 26.1.2）：
  [`docs/MESH_LOADER.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/01a05db3/docs/MESH_LOADER.md)
- 26.1.2 移植取证与修正：
  [`docs/TML_GPU_PORT_2612_20260901.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/01a05db3/docs/TML_GPU_PORT_2612_20260901.md)
- 手部/世界 1.21.11 字节码取证链：
  [`docs/TML_GPU_STEP2_HANDFLUSH_20260831.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/01a05db3/docs/TML_GPU_STEP2_HANDFLUSH_20260831.md)
- 对 26.2 分支同款实现的审查（A2/A4/A6/A9/A10 与本实现不变量对应）：
  [`docs/REVIEW_UPSTREAM_TML_GPU_262_20260831.md`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/blob/01a05db3/docs/REVIEW_UPSTREAM_TML_GPU_262_20260831.md)

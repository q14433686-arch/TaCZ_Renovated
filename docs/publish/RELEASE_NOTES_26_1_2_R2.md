# TaCZ: Renovated — Minecraft 26.1.2 / NeoForge（R2）

> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。
> 本移植的问题请提交到本仓库，不要打扰原作者。**
>
> 本公告覆盖自 **R1-hotfix**（2026-08-27，tag `26.1.2_R1_HOTFIX`）以来的全部实质改动。
> 实质改动 = 姊妹 Fabric 渲染线（`01a05170` v1–v4 + `01a05db3` v5）的等价移植
> （10 个源码/文档/CI 提交）+ 版本号晋升；另有 CI 自动 ci-log 提交，不属于人工内容。

## 环境

- Minecraft：**26.1.2**
- NeoForge：**26.1.2.x**（开发基于 26.1.2.97）
- Java：**25+**
- Mod：**`1.1.8+neoforge.26.1.2.R2`**
- 必需前置：**无**

不同 Minecraft 版本的文件不能混用。

## 本次变化

### 新增

- **内置 TacZ Mesh 加载器（TML）**：枪包可在 geo.json 骨骼上携带 `poly_mesh`
  网格（枪本体 `"model_type": "mesh"`；配件/弹药/方块「同名 geo 存在即替换」），
  由本 mod 直接解析渲染。
  - collector 安全子集：延迟提交、骨骼矩阵快照、顶点预算、弹匣双通道、半透明拆分；
  - GPU 静态烘焙：第一人称手部常驻 VBO（O(骨骼) 矩阵/帧）；
  - 世界语境 GPU：量化光照档 LRU 缓存 + 每帧烘焙额度
    （`MeshGpuBakeBudgetPerFrame`，与 LRU 容量**独立**）+ 延迟释放；
    世界表 30 次连败只关世界路径，手部不受牵连；
  - 光影下两条 GPU 键**默认开**（R3 定稿：常驻 VBO 收益 > 已知的天体亮度继承取舍）；
  - 19 项配置全部接入 Cloth「渲染」页；`MeshEnable=false` = 回退纯立方体，等价于未装。
- **瞄具画面内画中画（PIP）**：除经典整屏变焦外新增两种模式（`ScopePip*` 键，默认关，
  即默认仍是经典整屏变焦）：
  - PIP 重投影：镜片显示复用已渲染帧的放大重投影，镜外保持 1×；
  - PIP 二次渲染：窄 FOV **再渲染一次世界**进镜片（原生分辨率，成本 = 一帧完整世界渲染），
    支持隔帧重渲（RerenderInterval）、重投影倍率渐变；
  - 光影（Iris）下：独立管线隔离、阴影分辨率缩放、空闲释放、终局合成与 final overlay；
    Sodium 地形投影快照（fix 视差）；Voxy 第二渲染栈（反射接入，无声降级）；
  - 掩码周期帧戳 fail-closed：周期被否决的帧跳过合成、绝不贴陈旧截图。
- **镜内裁手**：第一人称手臂在目镜孔径内 discard（`RenderHelper` 动态代理把手臂的
  `entityTranslucent` 换成孔径裁剪版，复用火光管线）；枪身/配件/火光/镜内文字/mesh GPU
  枪身的孔径裁剪齐备。
- **低倍镜豁免**：低于 `ScopePipMinMagnification`（默认 **4×**）的瞄具（含组合镜低倍档）
  **一律不裁**手臂/火光/枪身/配件/mesh GPU 枪身 —— 没有放大画面可让位，裁掉只会破洞。
- **tacz:nbt 配方材料**：工作台配方支持 `type: "tacz:nbt"`（TaCZPackUpgrader 形态），
  以及无 `type` 的 `{item + nbt}` 隐式写法；统一按本仓 NeoForge 原生 `tacz:partial_nbt`
  自定义材料解析（宽松子集匹配，枪械物品必带弹药/开火模式等额外字段）
  —— 材料格不再显示裸枪、匹配也不再退化成「任意同物品枪械」。
- **开镜距离补偿**：世界 poly 的两道距离闸门（`MeshMaxRenderDistance` /
  `MeshWorldFullDetailDistance`）在提交侧乘 `currentDetailZoom()`（随开镜进度渐变、
  收镜回 1）——4× 镜下 48 格的上限不再只剩 12 格观感。
- **CI 基建**：`compile-check-2612.yml`（编译门禁 + 编译日志回推）。

### 修复

- **PIP 二次渲染中世界 mesh 枪不烘焙**：26.1.2 的每一遍 `renderLevel` 都会重新提交
  一份世界几何；原先提交侧拒收镜内那遍，导致镜内高模枪只能回 collector + 顶点预算
  被打成立方体（「镜内立方体、主画面正常」）。现在镜内那遍照常提交、照常画、画完即
  清表、不占主遍消费标志，并 log-once 供实机确认。
- **低倍镜「镜内破洞」**：枪身/配件的孔径裁切原先与倍率无关，低倍镜下镜片挖出来的是
  未放大的背景 = 枪上破洞；经 `AIM_CLIP_START` 后镜片本体移入 depth writer 的性质
  更正（那一刀与手/火光同为「给镜内画面让位」），纳入低倍镜豁免。
- **旧配方 NBT 静默丢弃**：无 `type` 的 `{item + nbt}` 此前被丢掉 nbt，显示裸枪且匹配
  放宽；现改写为 partial_nbt 语义并打日志。自定义材料解析 catch 加 `LinkageError`
  （serializer 缺失不再无声消失），错误日志带归一化原文。
- **烘焙额度与 LRU 容量耦合**：额度原取 `Math.max(4, 容量)`，一个旋钮当两个用；
  现独立（默认 4，1–64），「省显存调容量到 1」不再白顶 4 次额度。
- **worldZoomForcedToOne 死路**：镜内那遍被任何原因拒掉时，原逻辑仍把世界压成 1×、
  合成又不跑 ⇒ 内外一起 1X 且不自愈；现加 `scopePassRunnable()` 判据，拒了就退回
  重投影/整屏变焦。
- **PIP 纹理视图在 render pass 体内解析**：懒加载纹理的 `registerAndLoad` 会在 pass 内
  抛“Close the existing render pass…”；改为 pass 外整批解析（pass 体内不变量）。
- **Sodium 地形投影快照**：镜内窄遍会污染 Sodium 的区块 uniform，主遍地形沿用镜内
  投影（地形不跟 FOV 变化/视差错位）；改为逐遍快照 + finally 还原 + 重置上传闸。
- **镜内文字状态无法区分**：`ScopeTextSubmitter` 提交成功打 log-once（与回退 vanilla
  的静默分支区分开，验收矩阵可判）。

### 文档、许可与版本

- 新增 `docs/MESH_LOADER.md`（TML 主文档：来源/许可、19 项配置、26.1.2 消费点与
  复测矩阵、设计不变量、§8 枪包作者用法）与
  `docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`（镜内裁手 + 低倍率豁免 + 验收清单）；
  `PORT_..._20260901.md` 追加 v1–v5 移植记录；README / CHANGELOG / LICENSES.md /
  三站文案同步。
- **TML 来源与许可**：内置 TML 移植自
  [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
  `1.21.1_fabric`（**GPL-3.0**），经姊妹项目中转；各源文件头保留移植声明。
  TML 作者的许可允许纳入本 GPL 项目并再分发，但**不构成授权背书**——上游问题请回
  TML 仓库，不要要求 TML 作者为本端口提供支持。
- 版本号：`1.1.8+neoforge.26.1.2.R1-hotfix` → **`1.1.8+neoforge.26.1.2.R2`**
  （`1.1.8` SemVer 核心不变，枪包 `>=1.1.8` 依赖检查不受影响）。

## 已核验（只列实际执行并有记录的结果）

- CI `compile-check` / `consistency`：各轮 **success**（本线首次 v5 编译失败
  `206d8e0` 已由 `89db8081` 修复，失败/成功均留档）。
- 本地脚本：`check_lang_keys`（320 键）、`check_mesh_config_parity`（19 项齐平）、
  `check_mixin_registration`（44 注册/42 类）、`check_release_consistency.sh --strict` 全过。
- **运行期行为全部未实机** —— 本仓无实机环境，以下一律按「待实机」对待，
  本文不写任何 PASS（下节列完整清单）。

## 已知边界

- **TML 运行行为未实机**（collector/GPU 手部/GPU 世界/光影组合全在
  `docs/MESH_LOADER.md` §5 复测矩阵）；`MeshGpuUnderShaders`/`MeshGpuWorldUnderShaders`
  的光影照明等价性未实机（R3 默认开是已知取舍）。
- **PIP 全部模式未实机**：重投影 / 二次渲染 / 光影下（Iris 独立管线 / 终局合成 /
  final overlay / Voxy 反射桥）。Iris 1.11.x 门（`supportsFinalScopeOverlay`）只做了
  源码级核对。
- **镜内裁手未实机**；关键前置「`RenderTypes.entityTranslucent` 按贴图 memoize」是
  26.2 侧字节码实读结论，本仓未 javap 复核 —— 若未 memoize，效果等于没做且不报错
  （日志仍会打 `engaged`），复测第 1 条专门盯这个。
- **低倍率豁免 / 开镜距离补偿 / tacz:nbt 实机配方解析**：未实机。
- 半透明部件与弹匣永远走 collector；GUI/预览/阴影在提交侧拒收世界表（设计边界）。
- 明确依赖 TacZ:Arcana 的内容不受支持；LRTactical 是部分兼容框架，不含 `flash_shield`
  或原作完整美术资源。
- 面板服、代理网络和混合服务端不在统一保证范围。

## 安装与枪包

将 jar 放入 `mods/`。现代枪包放入 `.minecraft/tacz/`（zip 或解压目录均可，
`gunpack.meta.json` 必须位于包根）；旧布局枪包备份后放入 `tacz_backup/` 并执行
`/tacz convert`。联机时枪包需服务端和客户端同时安装；服务端 `/tacz reload`，
客户端 F3+T。TML 的 `fabric.mod.json provides: ["taczmeshloader"]` 为 Fabric 依赖
标识，本 NeoForge 构建没有等价物。

## 链接

- [源码](https://github.com/q14433686-arch/TaCZ_Renovated)
- [问题反馈](https://github.com/q14433686-arch/TaCZ_Renovated/issues)
- [Fabric 姊妹项目](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated)
- [原始 TaCZ](https://github.com/MCModderAnchor/TACZ)
- [直接上游](https://github.com/Sh1roCu/TACZ-Refabricated)
- [NeoForge 移植骨架参考（GPL-3.0）](https://github.com/MUKSC/TACZ-1.21.1)
- [内置 TML 上游（GPL-3.0）](https://github.com/VellEagle/TacZMeshLoader)

代码 GPL-3.0-only（含内置 TML 移植）；默认枪包资源 CC BY-NC-ND 4.0；其他组件见
[LICENSES.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSES.md)。

---

## 英文短版（CurseForge / Modrinth 文件 Changelog）

```markdown
## TaCZ: Renovated 1.1.8+neoforge.26.1.2.R2

**Minecraft 26.1.2 · NeoForge 26.1.2.x · Java 25+ · No required dependencies**

### Changes
- Built-in TacZ Mesh Loader (TML, ported from VellEagle/TacZMeshLoader 1.21.1_fabric,
  GPL-3.0): poly_mesh geo parsing for guns/attachments/ammo/blocks, collector-safe
  deferred submits with vertex budgets, first-person resident-VBO GPU baking, world-context
  GPU baking with quantized-light LRU + independent per-frame bake budget
  (MeshGpuBakeBudgetPerFrame), and 19 config entries wired into the Cloth "Render" page.
- Scope picture-in-picture: reprojection mode and second-world-render mode (native lens
  resolution, optional rerender interval, zoom ramp), plus Iris pipeline isolation,
  shadow scale, idle release, final composite/overlay and Sodium terrain-projection
  snapshotting; scope PIP is off by default (classic whole-screen zoom remains default).
- In-scope clipping for the first-person arms (collector proxy) and a low-power exemption
  for arms/flash/body/attachments/mesh GPU below ScopePipMinMagnification (default 4x).
- tacz:nbt gun-smith materials (and implicit {item + nbt} legacy form) mapped to the
  native tacz:partial_nbt custom ingredient; LinkageError-aware resolution logging.
- ADS distance compensation for the two mesh world distance gates.
- Fix: world mesh guns now bake inside the scope PIP re-render pass (26.1.2 resubmits
  world geometry per renderLevel pass); fix: legacy NBT ingredients no longer silently
  drop their NBT; fix: bake budget decoupled from LRU capacity; fix: worldZoomForcedToOne
  no longer dead-locks the view at 1x; fix: PIP texture views resolve outside the render
  pass; fix: Sodium chunk uniforms reset per pass.

### Verification status
- CI compile-check and consistency: success. **No runtime test has been performed yet.**
  TML behavior, all PIP modes, arm clipping, the low-power exemption, ADS distance
  compensation and tacz:nbt parsing are NOT TESTED in-game; see docs/MESH_LOADER.md §5
  and docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md §4 for the in-game checklist.
- Iris shader-pack combos remain untested (same status as the sibling line).

### Known boundaries
- Files for other Minecraft releases are not interchangeable; back up worlds and gun
  packs before upgrading. LRTactical remains a partial framework (no flash_shield, no
  original art); Arcana-dependent content is unsupported. Semi-transparent parts and
  magazines always use the collector path.
```

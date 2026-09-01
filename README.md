<p align="center"><img src="src/main/resources/icon.png" width="128" alt="TaCZ: Renovated"></p>

# [UNOFFICIAL] TaCZ: Renovated — Minecraft 26.1.2 / NeoForge
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1663324_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1663324.svg)](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZ_Renovated/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZ_Renovated/releases)

> **Unofficial NeoForge port of TaCZ (Timeless & Classics Guns: Zero) for Minecraft 26.1.2.
> Open source, auditable GPL lineage. Not an official TaCZ release; not reviewed or
> endorsed by the TACZ Dev Team. GPL-3.0.**

> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。**

本仓库把 TaCZ 移植到 **Minecraft 26.1.2 NeoForge**。游戏语义来自姊妹项目
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
（Fabric 26.x / 1.21.11 移植）的 26.1.2 分支；本仓库当前源码版本为
**`1.1.8+neoforge.26.1.2.R2`**。

[问题反馈](https://github.com/q14433686-arch/TaCZ-Renovated/issues)
· [姊妹项目（Fabric）](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
· [原始 TaCZ 项目](https://github.com/MCModderAnchor/TACZ)

### 选择你的版本 / Pick your version

| Minecraft | 加载器 | 状态 |
|---|---|---|
| **26.1.2** | NeoForge | **本仓库默认分支**（`1.1.8+neoforge.26.1.2.R2` 源码，基于 R1 回传 26.2 最新修复；[CurseForge 1663324](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)） |
| **26.2** | NeoForge | [`26.2` 分支](https://github.com/q14433686-arch/TaCZ-Renovated/tree/26.2)（`1.1.8+neoforge.26.2.R1` 已发布） |
| **1.21.11** | NeoForge | [`1.21.11` 分支](https://github.com/q14433686-arch/TaCZ-Renovated/tree/1.21.11)（`1.1.8+neoforge.1.21.11.R1` 已发布） |
| 26.2 / 26.1.2 / 1.21.11 | Fabric | 由[姊妹项目](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)提供 |

---

## 1. 支持环境

| 项目 | 26.1.2 要求 |
|---|---|
| Minecraft | **26.1.2** |
| 加载器 | **NeoForge 26.1.2.x**（release 通道；开发基于 26.1.2.97） |
| Java | 游戏侧随 NeoForge 安装器（源码构建需 JDK 25） |
| 硬依赖 | **无**（不需要 Fabric API / Forge Config API Port，配置走 NeoForge 原生） |
| 本 mod | **`1.1.8+neoforge.26.1.2.R2`** |

可选集成（Cloth Config 图形配置、Iris 光影、Player Animation Library 第三人称动画、
Controllable、Shoulder Surfing、JEI/REI、Carry On、FirstPerson Model）的
**验证版本号与状态矩阵**见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。

本仓库只提供 NeoForge 构建，不能与 Fabric 版 TaCZ 混装。

---

## 2. 项目范围

本仓库包含：

- TaCZ 的 NeoForge 26.1.2 端口及随上游带来的默认枪包；
- 为 26.x API 与 NeoForge 事件面改写的注册、网络、资源加载、GUI 和渲染接线；
- 若干可选模组的兼容接线。

**LRTactical 内置框架**（R1 起）：throwable/melee/detonator/consumable 四类物品与
五类投掷行为，依赖 `lrtactical` 的内容包完整可用。范围界定：flash_shield 未含；
原作美术零打包，道具模型/贴图由内容包提供，无内容包时显示原版占位模型。
（开发史：WP07 撤回 → WP-LR2 重启并通过单机+专服验收，
见 [`docs/records/LR2_INVENTORY.md`](docs/records/LR2_INVENTORY.md)。）

**内置 Mesh 加载器（TML）**（R1 后随渲染线一并移植）：枪包可在 geo.json 骨骼上携带
`poly_mesh` 网格（`"model_type": "mesh"`），由本 mod 直接解析渲染，带第一人称 /
世界语境的 GPU 静态烘焙 + 目镜孔径裁剪；详见 [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md)
与其 §8「枪包怎么用」。配置位于 `tacz-client.toml` 的 `[mesh_loader]` 段（19 项，
全部接进局内「渲染」页）；把 `MeshEnable` 关掉即回退到纯立方体外观，行为等价于未装。
**光影下两条 GPU 开关默认开**（R3 定稿的已知取舍，见 MESH_LOADER.md §3）；运行期行为
在 26.1.2 上**尚未实机验证**，问题请按 MESH_LOADER.md §5 的复测矩阵反馈。

**镜内画中画（PIP）与镜内裁切**（v4/v5）：除经典整屏变焦外，还提供重投影 PIP 与
「二次渲染」PIP 两种瞄具模式，以及镜内文字 / 手臂 / 火光 / 枪身的孔径裁剪与
低倍镜豁免；详见 [`docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)。

这不代表本项目是 TaCZ 或 LRTactical 的官方版本，也不代表所有第三方枪包、
战术装备包或 shader pack 都已经兼容。TML 自身的来源与许可见
[`docs/MESH_LOADER.md`](docs/MESH_LOADER.md) 开头与 §7。

---

## 3. 瞄具渲染：深度孔径 + 可选 PIP，不是「只有一种」

本端口的瞄具底层仍是 **depth-aperture（深度孔径）**：在绘制边界备份/恢复深度并做目镜
孔径拷贝。在此之上提供三种镜内显示模式（`RenderConfig` 的 `ScopePip*` 键，默认关闭，
即经典整屏变焦）：

| 模式 | 机制 | 默认 |
|---|---|---|
| 经典整屏变焦 | 不开 PIP：整屏 FOV 收窄，镜片处仅做孔径裁切 | **开**（`ScopePipEnable=false` 即此模式） |
| PIP 重投影 | 镜片显示复用已渲染帧的放大重投影，镜外保持 1× | 关 |
| PIP 二次渲染 | 用窄 FOV **再渲染一次世界**进镜片（原生分辨率，成本 = 一帧完整世界渲染；`ScopePipRerender`） | 关 |

代码中的 `PictureInPictureRenderer` **仅用于枪械工作台的 GUI 模型预览**，与上述瞄具 PIP
无关。装有 Iris 时 PIP 走单独的管线/终局合成接线（反射接入，见
[`docs/SCOPE_PIP_RERENDER_IRIS_PORT_2612_20260901.md`](docs/SCOPE_PIP_RERENDER_IRIS_PORT_2612_20260901.md)），
不装 Iris 则不加载；其他 shader pack 仍可能改写自定义管线的最终效果，不保证一致。

**开镜时的周边裁切**（手臂/火光/枪身/配件/mesh GPU 枪身，depth-aperture 版）与
**低倍镜豁免**（< `ScopePipMinMagnification` 默认 4× 一律不裁）见
[`docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)。

---

## 4. 安装

1. 安装 Minecraft 26.1.2 与 NeoForge 26.1.2.x；
2. 把本 mod 的 `.jar` 放入 `.minecraft/mods/`（无必装前置）；
3. 启动游戏。首次启动会把默认枪包解压到 `.minecraft/tacz/`。

不要只看文件名中的 `1.1.8`：还必须核对 Minecraft 版本与加载器。

---

## 5. 第三方枪包

### 加载目录

现代枪包放在 **`.minecraft/tacz/`**：

```text
.minecraft/
├── mods/
│   └── tacz-....jar
├── tacz/
│   ├── some_pack.zip
│   └── another_pack/
└── tacz_backup/
```

zip 可以直接加载，也可以解压为目录。无论哪种形式，包根目录都必须有
`gunpack.meta.json`（zip 时必须位于压缩包根部，不能多套一层目录）。

### 旧包转换

以**包结构**（而非"适用于 1.20"之类标签）判断是否需要转换。旧布局包放进
`.minecraft/tacz_backup/`，然后在游戏内执行：

```text
/tacz convert
```

转换器不保证自动修复所有旧资源、配方或脚本差异，请保留原包备份并检查日志。

### 版本约束

本仓库的完整版本号 `1.1.8+neoforge.26.1.2.R2` 中，`1.1.8` 是 SemVer 核心，
`+` 之后是构建元数据，不参与版本先后比较——因此枪包常见的 `tacz >= 1.1.8` 谓词照常通过。
一个枪包最终是否通过检查，取决于它写下的完整谓词，不能笼统理解为"所有旧包都兼容"。

### 联机（专用服务器）注意

- 枪包需要**双端安装**：服务端 `tacz/` 提供逻辑（数值/配方经网络同步），
  客户端 `tacz/` 提供显示资产（模型/贴图/音效/语言文件）。
- 只装服务端：枪能用但客户端显示紫黑方块、名字为原始翻译键；只装客户端：无效果。
- 服务端加包后 OP 执行 `/tacz reload` 即可生效并全员重同步；
  **客户端**新增的包按 **F3+T** 重载资源即可加载，无需重启游戏。

### 不受支持的内容

- 明确依赖 **TaCZ:Arcana** 的内容包：本仓库不提供 Arcana，也未实现其 API
  （Arcana 官方发布为 1.20.1 Forge，姊妹项目 2026-08-12 核对）。
- LRTactical 的 flash_shield（独立子系统 + 原作 ARR 美术，未移植，与姊妹项目同边界）。

紫黑贴图或模型缺失不能直接证明"依赖 Arcana"，也可能是目录层级、资源路径、
版本谓词或包不完整造成的。

---

## 6. 当前已知边界

- **联机**：局域网与**真实专用服务器**（生产 jar 部署 + 双客户端）均已实测通过
  （2026-08-21，R1）；混合服（Youer/Arclight 等）、代理网络（Velocity）、
  面板服等形态未测试。测试预案与形态矩阵见
  [docs/DEDICATED_SERVER_TEST.md](docs/DEDICATED_SERVER_TEST.md)。
- 启动日志中原版 `RecipeManager` 对工作台配方报 `empty ingredients` 警告：无害，
  实际合成走 mod 内部管线。
- 可选 mod 的逐项状态（含**明确不适配**的 Just Zoom、无 NeoForge 版的 Zoomify 等）
  见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。
- **TML / PIP / 镜内裁切全部未实机**：本仓没有运行环境，v5 之前的渲染线亦然。
  已知取舍与复测清单见
  [docs/MESH_LOADER.md](docs/MESH_LOADER.md) §3/§5 与
  [docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md) §4。
- **Iris 光影下是同一未实机状态**：姊妹线多处标注未实机，本仓承接同一状态，不写 PASS。
- **目录层级 `recipe/` 与 `recipes/`**：26.x 数据包布局（vanilla registry 读 `recipe/`）；
  旧枪包的 `recipes/` 由 PackMapping 重映射、`recipes→recipe` 兼容，详见
  [docs/PORT_01a05170_TO_NEOFORGE_26_1_2_20260901.md](docs/PORT_01a05170_TO_NEOFORGE_26_1_2_20260901.md)（v1-v5）。

---

## 7. 许可与来源

本仓库**不只有一套许可**：

- 本端口与 TaCZ 上游代码：**GPL-3.0**（发布二进制必须随附完整对应源码）；
- 默认枪包资源：**CC BY-NC-ND 4.0**（沿用上游声明）；
- 随 jar 打包的 LuaJ：MIT；commons-math3：Apache-2.0；
- **内置 TML（Mesh 加载器）**：移植自
  [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
  `1.21.1_fabric`（**GPL-3.0**），经姊妹项目 TaCZ_Refabricated_Unofficial 26.1.2 线
  中转；各源文件头保留移植声明，完整来源/许可/边界见
  [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md) 开头与 §7。TML 作者的 GPL-3.0 许可
  允许本仓库将其源码纳入本 GPL 项目并再分发，但**不构成**任何授权背书——上游问题
  请回 [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader) 仓库，
  不要要求 TML 作者为本端口提供支持；
- 其他第三方库与外部内容包可能有各自许可。

详见 [`LICENSE`](LICENSE) 与 [`LICENSES.md`](LICENSES.md)。代码许可不会自动覆盖美术资源。

谱系（全链公开源码，可审计）：

- 游戏语义主线：MCModderAnchor/TACZ → Sh1roCu/TACZ-Refabricated →
  [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) → 本仓库；
- 渲染层（TML 上游）：[VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
  （GPL-3.0，经上述姊妹线中转）；
- 加载器习语参考（辅，未采用其渲染代码）：MUKSC/TACZ-1.21.1。

本项目按"原样"提供，不附带担保。请勿把本移植的问题提交给 TaCZ 或 LRTactical 原作者。

---

## 8. 从源码构建

需要 JDK 25：

```bash
./gradlew build
```

产物位于 `build/libs/`。开发环境与项目规则见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)，
AI 协作者请先读 [`AGENTS.md`](AGENTS.md)。版本历史见 [CHANGELOG.md](CHANGELOG.md)。

---

## 9. 反馈

请在[本仓库 Issues](https://github.com/q14433686-arch/TaCZ-Renovated/issues)提交：

1. 完整 `logs/latest.log`（崩溃再附 crash report）；
2. Minecraft、NeoForge 与本 mod 的完整版本；
3. 第三方枪包和可选模组的名称与版本；
4. 是否能在"仅本 mod"的最小环境复现；
5. 使用 shader pack 时，注明 Iris 与 shader pack 的具体版本；
6. **混合服（Youer/Arclight/Mohist 系）或代理网络（Velocity 等）上的问题，
   请先在原生 NeoForge 专用服务器复现后再提交**——混合核会改写网络与事件底层，
   无法复现的问题恕不受理。

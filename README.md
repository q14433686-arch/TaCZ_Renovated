<p align="center">
  <img src="src/main/resources/icon.png" alt="TaCZ: Renovated" width="240">
  <br>
  <a href="https://www.curseforge.com/minecraft/mc-mods/tacz-renovated"><img src="https://cf.cdn.curseforge.com/mc-mods/691781/new_logo_1756467997960.png" alt="CurseForge" height="30"></a>
  <a href="https://github.com/q14433686-arch/TaCZ_Renovated"><img src="https://img.shields.io/badge/GitHub-q14433686-arch%2FTaCZ--Renovated-24292e?logo=github&logoColor=white" alt="GitHub" height="20"></a>
</p>

# [UNOFFICIAL] TaCZ: Renovated — Minecraft 26.2 / NeoForge

> **English**: This project is an **unofficial community port/derivative** of the TaCZ: Reborn series. It is **not** the official TaCZ, and it is **not** produced, endorsed, or supported by the original TaCZ team (Xqyao / Shuairon / Yumeko). It is provided **as-is** with no warranty of any kind; use at your own risk.
>
> **中文**：本项目是 TaCZ: Reborn 系列的**非官方社区移植/衍生作品**，**不是官方 TaCZ**，与 TaCZ 原团队（Xqyao / Shuairon / Yumeko）**无关**，也**未获得**其认可或支持。本项目**按“原样”提供**，不提供任何明示或暗示的担保，使用风险自负。

本仓库是把 `26.1.2` 线 R1 基线社区移植到 **Minecraft 26.2（NeoForge）** 的工作线。
当前构建 `1.1.8+neoforge.26.2.R2`（R2 候选，待发布命令；`R1` / `R1-hotfix` 已发布）。
游戏语义（瞄具渲染、PIP、mesh 裁剪等）以 Fabric 26.2 姊妹线
（`q14433686-arch/TaCZ_Refabricated_Unofficial`）为权威 —— 每轮同步逐 commit
核对本线基线后等价移植，对照与「搬了什么 / 不搬什么 + 理由」记录在
[`docs/records/`](docs/records/)。

### 选择你的版本 / Pick your version

| MC / Loader | 线 | 当前状态 |
|---|---|---|
| **26.2 / NeoForge（本仓库，`26.2` 分支）** | [`26.2`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/26.2) | `R1` / `R1-hotfix` 已发布；**R2 推进中**（LRTactical、TML、PIP —— 见 [CHANGELOG](CHANGELOG.md)） |
| 26.1.2 / NeoForge | [`26.1.2`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/26.1.2) | `R1` / `R1-hotfix` 已发布 |
| 1.21.11 / NeoForge | [`1.21.11`](https://github.com/q14433686-arch/TaCZ_Renovated/tree/1.21.11) | `R1` / `R1-hotfix` 已发布 |
| Fabric 姊妹项目 | [`q14433686-arch/TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) | 1.21.11 Fabric 主线 + 26.2 Fabric 工作线（本仓库游戏语义权威线） |

## 1. 支持环境

- Minecraft **26.2**
- NeoForge **`26.2.0.64`**（钉选构建，见 `gradle.properties`）
- Java **25**
- **无硬依赖**（LesRaisins-Tactical 功能已内置到本 Mod，不可分离）

可选 Mod 集成（Cloth Config、PAL、Controllable、SSR、JEI/REI、Iris、Carry On、
First-person Model、Punchy! 等）逐项状态见 [COMPATIBILITY.md](COMPATIBILITY.md)。

> **Vulkan** 后端：请在 `neoforge.mods.toml` 中加 `earlyWindowControl=true`，
> 否则瞄具管线可能因初始化时序问题无法启用。

## 2. 项目范围

基础面为 26.1.2 R1 线的移植（枪械系统、枪包、工作台、网络等），R2 在其上加入
三个功能块：

### 2.1 内置 LRTactical

[LesRaisins-Studios/LesRaisins-Tactical-Equipements](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements)
（LR）的游戏功能（投掷物 / 近战 / 起爆器 / 消耗品四类物品与对应客户端反馈）
**内置**于本 Mod：只带代码（109 个源文件），原作 ARR 美术**不打包**，也不是官方
LR 内容包。范围与边界（含明确排除的 `flash_shield`）见
[COMPATIBILITY.md](COMPATIBILITY.md) §内置 LRTactical。

### 2.2 内置 TacZ Mesh Loader [TML] —— 高模 mesh 枪模

移植自 [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
（`1.21.1_fabric` v0.1.7，GPL-3.0，**不是**官方 TaCZ 附属），经姊妹线同步中转，
源文件头保留移植声明。它带来：

- 枪包 `geo.json` 加 `poly_mesh`（挂骨骼）、显示 json 声明
  `"model_type": "mesh"` → 以高模 mesh 绘制（枪包用法见
  [docs/MESH_LOADER.md](docs/MESH_LOADER.md) §3）；
- **第 0 步**安全子集（CPU 路径，无 GPU 赌注）+ **第 1 步**GPU 静态烘焙
  （第一人称视角）+ **第 2 步**世界语境烘焙（世界掉落 / 第三人称 mesh 枪），均已实装；
- **开镜孔径裁剪**：镜体、准星、镜内视模（含 mesh 枪身 / 配件，与 cube 枪身同开同关）
  全部按 26.2 离屏目镜掩码裁到镜口 —— 含 2026-09-02 修复的「mesh 枪身裁剪判据
  时序 bug（高模枪身从未被孔径裁掉）」，全程记录见
  [docs/records/BUG_MESHGUNBODY_SCOPE_CLIP_RERENDER_20260902.md](docs/records/BUG_MESHGUNBODY_SCOPE_CLIP_RERENDER_20260902.md)；
- GPU 烘焙在**光影下默认开**（取舍：Complementary 系光影下 GPU 静态光照非动态，
  换取世界语境 mesh 枪稳定；`gpu_under_shaders=false` 可关闭）。

配置位于 `tacz-client.toml` 的 `[mesh_loader]` 段，**同时全部接进局内 TACZ 设置
「渲染」页**（游戏内 T 键 / Mods 菜单 → Timeless and Classics Guns → 渲染，需要
Cloth Config；未装时该入口给下载提示；两处改的是同一份配置）。
`mesh_enable=false` 时整个 loader 不生效（纯 cube 枪模）。

> **实机验证**（维护者回报）：第 0/1/2 步均 PASS（2026-08-31 / 09-01；第 2 步世界
> 语境矩阵未逐条回报）；帧率收益与 ttf/unihex 字体路径仍 UNVERIFIED。权威口径与
> 验证清单见 [docs/MESH_LOADER.md](docs/MESH_LOADER.md)。

### 2.3 高倍镜：PIP 镜内画中画（picture-in-picture）

高倍开镜时，可以把高倍视野只画在**镜内**（镜外保持裸眼视野），而不是整屏缩放。
三种渲染模式：

| 模式 | 配置 | 每帧额外成本 |
|---|---|---|
| **经典整屏变焦（默认）** | `ScopePipEnable = false` | 无（整屏按瞄具缩放） |
| PIP · 重投影 | `ScopePipEnable = true`，`ScopePipRerender = false` | 一次全屏拷贝 |
| PIP · 二次渲染 | 上两行 + `ScopePipRerender = true` | **一帧完整世界渲染**（等价多画一遍世界） |

通用规则：倍率低于 `ScopePipMinMagnification`（默认 **4×**）自动退回整屏变焦
（低倍镜整屏变焦观感更好，也省成本）；镜内视模按孔径裁剪（含 mesh 枪身，§2.2）；
装 Iris 时镜内画面走单独管线、终局合成后并入主画面（反射接入，不装 Iris 则不加载）。

**PIP 已可作为局内配置选项开启**：游戏内设置页（T 键 / Mods 菜单 → TACZ → 渲染）
直接暴露 14 个 PIP 项中的 10 个 —— 「瞄具画中画（PIP）」开关、二次渲染模式、
最低倍率、镜内分辨率/阴影缩放、二次渲染间隔（光影下降开销）、光影放行、管线隔离、
世界变焦比例、锐化；开关与保存后对开镜行为即时生效。其余 4 项为 debug/进阶项，
完整清单见 [RenderConfig](src/main/java/com/tacz/guns/config/client/RenderConfig.java)。

枪械工作台 GUI 里的瞄具预览是独立的 `PictureInPictureRenderer`（GUI 内静态预览），
与游戏内管线无关。

## 3. 安装

1. 下载 jar（CurseForge，或自行从源码构建，§6）放入 `mods/`。
2. 本 Mod **没有必装前置** —— 单独把 TaCZ 放进 `mods/` 即可运行（Cloth Config 等
   均为可选集成，见 §2 / [COMPATIBILITY.md](COMPATIBILITY.md)）。
3. 首次启动会自动把默认枪包解压到 `.minecraft/tacz/`。
4. 环境要求见 §1（MC 26.2 + NeoForge `26.2.0.64` + Java 25）。

## 4. 第三方枪包

- 放入 `.minecraft/tacz/` 目录：枪包 zip 直接丢进 `tacz/` 即可加载（推荐，
  删除更方便）；或手动解压（gunpack zip 没有额外层级，解压出来的内容直接放）。
- **旧枪包目录升级？** 旧 `tacz/` 会被自动备份到 `tacz_backup/`，之后可用
  `/tacz convert` 把内容迁移到新布局。
- **联机枪包版本约束**：枪包用 `supported_mod_versions`（SemVer）声明支持的 Mod
  版本区间；联机服务端对两端校验，不匹配**不允许开火**。
- **联机同步**：两端需装同一枪包（或重载后同步），`/tacz reload` 重载枪包；
  游戏内 `F3+T` 可强制即时重载。
- **mesh 枪包**：`"model_type": "mesh"` + `geo.json` 的 `poly_mesh`，见
  [MESH_LOADER.md](docs/MESH_LOADER.md) §3。
- **不受支持**：依赖 Arcana 的枪包；LR 原作 `flash_shield`。

## 5. 可选 Mod

Cloth Config、PAL、Controllable、SSR、JEI/REI、Iris、Carry On、First-person
Model、Punchy!（First-person Model / Not Enough Animations 当前无 NeoForge 26.2
文件，兼容桥保持 dormant）。逐项坐标与核验状态：
[COMPATIBILITY.md](COMPATIBILITY.md)。

## 6. 开发与验证

- **JDK 25**；构建用仓内 Gradle 9.2.1 / ModDevGradle 2.0.144
  （[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)）。
- **源码权威**：游戏语义以 Fabric 26.2 姊妹线为权威；每轮同步逐 commit 核对，
  「搬 / 不搬 + 理由」记录在 `docs/records/`（AGENTS.md §0）。
- **验证纪律**（[AGENTS.md](AGENTS.md)）：CI 编译门 ≠ 实机 PASS；实机结论必须写清
  谁在哪个 commit 上跑的。当前候选的真实状态与剩余闸门见
  [docs/PORTING_STATUS.md](docs/PORTING_STATUS.md)，发布前逐项检查见
  [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)。

## 7. 枪包版本约束

Mod 的 id 是 `tacz`，版本号形如 `1.1.8+neoforge.26.2.R2`。

- **`1.1.8` 是 Mod 自身的版本**，不代表 Minecraft 版本。它是 Mod 自己的 SemVer
  版本号，延续原版 TaCZ 系列的版本线。
- **`+neoforge.26.2.R2` 是 build metadata** —— `+` 是 SemVer 标准的
  build-metadata 分隔符，标识加载器（`neoforge`）、Minecraft 版本（`26.2`）与
  发布标签（`R2`）。**SemVer build metadata 不参与版本排序**：`>=1.1.8` 与
  `>=1.1.8+neoforge.26.2.R2` 等价，比较时忽略 `+` 之后的部分。
- 因此枪包声明 `supported_mod_versions: ">=1.1.8"` 即可匹配本 Mod 任意
  `1.1.8+...` 构建。

## 8. 文档

| 文档 | 内容 |
|---|---|
| [COMPATIBILITY.md](COMPATIBILITY.md) | 可选 Mod 与图形后端的逐项状态 |
| [LICENSES.md](LICENSES.md) | 代码、资源与依赖许可（含 TML 来源） |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更（R2 候选内容） |
| [docs/README.md](docs/README.md) | 完整文档索引（用户 / 开发 / 子系统 / 档案） |
| [docs/MESH_LOADER.md](docs/MESH_LOADER.md) | TML：机制、配置表、枪包用法、验证清单 |
| [docs/GUNPACKS.md](docs/GUNPACKS.md) | 枪包双端安装、重载、版本谓词 |
| [docs/DEDICATED_SERVER_TEST.md](docs/DEDICATED_SERVER_TEST.md) | 专服搭建与 L0–L4 多人验收 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 构建与运行、源码权威、验证纪律 |
| [docs/PORTING_STATUS.md](docs/PORTING_STATUS.md) / [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | 当前状态与发布闸门 |
| [`docs/records/`](docs/records/) | 同步取证记录、瞄具 / PIP / Iris 调查记录（冻结档案） |

## 9. 许可与来源

本仓库**不是一个单一许可**：

- **端口与上游代码**：GPL-3.0。**发布二进制必须随附对应源码**（GPL-3.0 义务）。
- **默认枪包（原版枪模资源：模型 / 贴图 / 音效）**：CC BY-NC-ND 4.0（**非商业**、
  禁止演绎）—— 随 jar 分发，因此本 Mod jar 整体按非商业再分发处理（与原版 TaCZ
  同一约束）。
- **内置 LRTactical**：仅代码，GPL-3.0
  （[LesRaisins-Studios/LesRaisins-Tactical-Equipements](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements)）；
  原作美术不打包（ARR）。
- **TML（TacZ Mesh Loader）**：移植自
  [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
  `1.21.1_fabric` v0.1.7（GPL-3.0），经姊妹线中转；源文件头保留移植声明
  （详见 [docs/MESH_LOADER.md](docs/MESH_LOADER.md) 开头与
  [LICENSES.md](LICENSES.md)）。TML 作者的 GPL-3.0 允许代码被纳入与再分发，
  但**不构成对本移植的授权背书或担保** —— 上游问题请到
  VellEagle/TacZMeshLoader 仓库提出，**请勿要求 TML 作者为本移植提供支持**。
- **其他第三方库**（LuaJ、commons-math3、SimpleBedrockModel 等）：各自许可，
  见 [LICENSES.md](LICENSES.md)。

谱系：MCModderAnchor/TACZ → Sh1roCu/TACZ-Refabricated →
q14433686-arch/TaCZ_Refabricated_Unofficial → 本仓库。加载器习语参考
MUKSC/TACZ-1.21.1（未采用其渲染代码）。

本项目**按“原样”提供**，不提供任何明示或暗示的担保。

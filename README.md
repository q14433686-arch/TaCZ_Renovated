# [UNOFFICIAL] TaCZ: Renovated — Minecraft 26.1.2 / NeoForge

> **Unofficial NeoForge port of TaCZ (Timeless & Classics Guns: Zero) for Minecraft 26.1.2.
> Open source, auditable GPL lineage. Not an official TaCZ release; not reviewed or
> endorsed by the TACZ Dev Team. GPL-3.0.**

> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。**

本仓库把 TaCZ 移植到 **Minecraft 26.1.2 NeoForge**。游戏语义来自姊妹项目
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
（Fabric 26.x / 1.21.11 移植）的 26.1.2 分支；本仓库当前源码版本为
**`1.1.8+neoforge.26.1.2.Beta-2`**。

[问题反馈](https://github.com/q14433686-arch/TaCZ-Renovated/issues)
· [姊妹项目（Fabric）](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
· [原始 TaCZ 项目](https://github.com/MCModderAnchor/TACZ)

### 选择你的版本 / Pick your version

| Minecraft | 加载器 | 状态 |
|---|---|---|
| **26.1.2** | NeoForge | **本仓库**（Beta-1；Releases 发布前请从源码构建） |
| **26.2** | NeoForge | 筹备中（[移植工单](docs/PORT_262_BRIEF.md)） |
| 26.2 / 26.1.2 / 1.21.11 | Fabric | 由[姊妹项目](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)提供 |

---

## 1. 支持环境

| 项目 | 26.1.2 要求 |
|---|---|
| Minecraft | **26.1.2** |
| 加载器 | **NeoForge 26.1.2.x**（release 通道；开发基于 26.1.2.97） |
| Java | 游戏侧随 NeoForge 安装器（源码构建需 JDK 25） |
| 硬依赖 | **无**（不需要 Fabric API / Forge Config API Port，配置走 NeoForge 原生） |
| 本 mod | **`1.1.8+neoforge.26.1.2.Beta-2`** |

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

**与 Fabric 姊妹项目的关键差异**：本仓库**未内置 LRTactical 兼容框架**
（曾于开发期引入，因未定位的启动崩溃撤回；决策与踩坑记录见
[`docs/records/WP07_LRTACTICAL_PLAN.md`](docs/records/WP07_LRTACTICAL_PLAN.md)）。
依赖 `lrtactical` 的枪包可以装载、枪械部分可用，但近战/投掷物/引爆器/消耗品等
LR 道具**不可用**。

---

## 3. 瞄具渲染：深度孔径，不是 PIP

**本端口的瞄具不是 Picture-in-Picture，不会为镜片再渲染一次世界。**

26.1.2 线采用 **depth-aperture（深度孔径）** 方案：在绘制边界备份/恢复深度并做目镜
孔径拷贝，镜片后看到的仍是同一次世界渲染——没有第二台相机，没有第二次 `renderLevel`。
代码中的 `PictureInPictureRenderer` **仅用于枪械工作台的 GUI 模型预览**，与瞄具无关。

装有 Iris 时走单独的 HAND shader 接线（反射接入，不装 Iris 则完全不加载）；
其他 shader pack 仍可能改写自定义管线的最终效果，不保证一致。

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

本仓库的完整版本号 `1.1.8+neoforge.26.1.2.Beta-2` 中，`1.1.8` 是 SemVer 核心，
`+` 之后是构建元数据，不参与版本先后比较——因此枪包常见的 `tacz >= 1.1.8` 谓词照常通过。
一个枪包最终是否通过检查，取决于它写下的完整谓词，不能笼统理解为"所有旧包都兼容"。

### 不受支持的内容

- 明确依赖 **TaCZ:Arcana** 的内容包：本仓库不提供 Arcana，也未实现其 API
  （Arcana 官方发布为 1.20.1 Forge，姊妹项目 2026-08-12 核对）。
- 依赖 **lrtactical** 的 LR 道具部分（见第 2 节）。

紫黑贴图或模型缺失不能直接证明"依赖 Arcana"，也可能是目录层级、资源路径、
版本谓词或包不完整造成的。

---

## 6. 当前已知边界

- **联机**：局域网与**真实专用服务器**（生产 jar 部署 + 双客户端）均已实测通过
  （2026-08-21，Beta-2）；混合服（Youer/Arclight 等）、代理网络（Velocity）、
  面板服等形态未测试。测试预案与形态矩阵见
  [docs/DEDICATED_SERVER_TEST.md](docs/DEDICATED_SERVER_TEST.md)。
- LRTactical 未内置（第 2 节）。
- 启动日志中原版 `RecipeManager` 对工作台配方报 `empty ingredients` 警告：无害，
  实际合成走 mod 内部管线。
- 可选 mod 的逐项状态（含**明确不适配**的 Just Zoom、无 NeoForge 版的 Zoomify 等）
  见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。

---

## 7. 许可与来源

本仓库**不只有一套许可**：

- 本端口与 TaCZ 上游代码：**GPL-3.0**（发布二进制必须随附完整对应源码）；
- 默认枪包资源：**CC BY-NC-ND 4.0**（沿用上游声明）；
- 随 jar 打包的 LuaJ：MIT；commons-math3：Apache-2.0；
- 其他第三方库与外部内容包可能有各自许可。

详见 [`LICENSE`](LICENSE) 与 [`LICENSES.md`](LICENSES.md)。代码许可不会自动覆盖美术资源。

谱系（全链公开源码，可审计）：

- 游戏语义主线：MCModderAnchor/TACZ → Sh1roCu/TACZ-Refabricated →
  [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) → 本仓库；
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
5. 使用 shader pack 时，注明 Iris 与 shader pack 的具体版本。

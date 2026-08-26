<p align="center"><img src="src/main/resources/icon.png" width="128" alt="TaCZ: Renovated"></p>

# TaCZ: Renovated — NeoForge 26.2
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1663324_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1663324.svg)](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZ_Renovated/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZ_Renovated/releases)

> **非官方移植。请勿向 MCModderAnchor / Serene Wave Studio 报告本移植的问题。**
>
> 当前源码版本：**`1.1.8+neoforge.26.2.R1`**；状态：**R1 已发布**（2026-08-22，
> [GitHub Release `26.2_R1`](https://github.com/q14433686-arch/TaCZ-Renovated/releases/tag/26.2_R1) /
> [CurseForge 1663324](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)）

从 NeoForge 26.1.2 R1 前滚到 Minecraft 26.2 的 TaCZ 社区移植。modId 保持
`tacz`，枪包的 `tacz >= 1.1.8` 依赖检查继续有效。

## 1. 支持环境

| 组件 | 版本 / 状态 |
|---|---|
| Minecraft | **26.2** |
| NeoForge | **26.2.0.64** release |
| Java | **25** |
| 本 Mod | **1.1.8+neoforge.26.2.R1** |
| Gradle / ModDevGradle | 9.2.1 / 2.0.144 |
| 专用服务器与基础多人 | 暂无bug |
| LRTactical 内置层 | 已前滚；26.1.2/26.2 源基线单机/专服测试pass  |
| 第三方枪包专项 | L2.5 与 LR 内容包均待确认 |
| GPU / 可选 Mod | 逐项状态见 [`COMPATIBILITY.md`](COMPATIBILITY.md) |

冻结测试记录 [`SERVER_TEST_20260821_262_R1.md`](docs/records/SERVER_TEST_20260821_262_R1.md)
只覆盖 LR 合入前的 26.2 核心候选。当前 R1 已增加 LRTactical 内置层，必须重新执行 build、
L0-L3、L2.5 与 LR 专项；26.1.2 的 LR PASS 只作为源基线证据。

## 2. 已实现内容与明确边界

- 完整基础枪械、弹药、配件、工作台、枪包加载、网络同步与多人转播。
- R1 基线修复已回流：EMPTY Draw optional codec、recipe-filter/attachment-tag 同步、
  dedicated-safe `getName` 与 Iris already-assigned 处理。
- 26.2 Feature Rendering、PiP 枪械预览、shape outline 与第一/第三人称提交路径。
- 离屏 ocular mask：镜身/视模/火光在镜内 discard，准星反向约束在镜内；低倍 sight
  使用 reticle-only mask，高倍 scope 使用 full-viewmodel mask。
- **OpenGL**：普通 ocular-mask 路径已实现；完整画面矩阵仍待最终确认。
- **OpenGL + Iris 1.11.x**：HAND pipeline 分类与 linked-fragment mask bridge 已实现，未标 PASS。
- **Vulkan**：普通 mask 使用 `TextureTarget`/`RenderPass`，不直接调用 GL。NeoForge#3230
  要求客户端先在 `config/fml.toml` 设置 `earlyWindowControl=false`；启动已获用户 PASS，
  scope-mask 视觉矩阵仍待确认。
- **LRTactical**：已内置 throwable / melee / detonator / consumable、五类投掷行为、
  LR index/recipe/script 装载与同步、tooltip/HUD/分类冷却反馈层；不含 flash_shield，也不
  打包原作 ARR 美术。26.2 API 已静态前滚，当前实机矩阵尚未执行。
- **Aperture / 未核 shader replacement**：没有稳定 bridge 时走未掩码安全回退。

未实际执行的项目不会写成兼容或 PASS。

## 3. 安装

1. 安装 Minecraft 26.2、NeoForge 26.2.0.64 与 Java 25。
2. 将构建产物 `tacz-1.1.8+neoforge.26.2.R1.jar` 放入实例 `mods/`。
3. 客户端若使用 Vulkan，将 `config/fml.toml` 中的 `earlyWindowControl` 改为 `false`。
4. 服务端搭建与完整 L0-L4 流程见
   [`docs/DEDICATED_SERVER_TEST.md`](docs/DEDICATED_SERVER_TEST.md)。

LuaJ 与 Commons Math 已通过 jar-in-jar 打包，不需要玩家另外安装。

## 4. 枪包

- 服务端把枪包放在服务器根目录的 `tacz/`；客户端仍需在实例 `tacz/` 放同一包，
  以提供模型、贴图、声音和语言资源。
- 服务端 common 数据重载：`/tacz reload`。
- 客户端运行中新增本地枪包：按 **F3+T** 重载资源。
- 只装服务端时允许逻辑数据同步，但客户端会缺显示资源；只装客户端时服务端不认识包内 id。
- 版本谓词必须以 `tacz >= 1.1.8` 等正常 SemVer 形式书写。
- 依赖 `lrtactical` 的内容包可使用内置四类承载物品与行为；仍需双端安装显示/数据资产，
  且 flash_shield 内容不在本次范围内。

详见 [`docs/GUNPACKS.md`](docs/GUNPACKS.md) 与 L2.5 测试章节。

## 5. 可选 Mod

已核公开 artifact/API 的项目包括 Cloth Config、PAL、Controllable、Shoulder Surfing、JEI、
REI、Iris 与 Carry On。构建通过不等于游戏内兼容；逐项版本、缺口与测试矩阵见
[`COMPATIBILITY.md`](COMPATIBILITY.md)。

First-person Model 与 Not Enough Animations 在核验日没有 NeoForge 26.2 发布文件；源码中的
反射桥仅为 dormant 预留，不作为可安装兼容宣传。Punchy! 2.7d 有 NeoForge 26.2 文件，本仓
用可选 mixin 在持枪时让出其手臂/位移层；未实机，不标 PASS。

## 6. 开发与验证

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew build --no-configuration-cache
./gradlew runServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
bash scripts/check_release_consistency.sh --strict
```

26.1+ 游戏本体未混淆：**不要配置 mappings、Parchment 或 Yarn**。开发规则、证据层级与
洁净室边界见 [`AGENTS.md`](AGENTS.md) 和 [`CHARTER.md`](CHARTER.md)。

## 7. 版本约束

`1.1.8` 是 SemVer core；`+neoforge.26.2.R1` 是 build metadata，不参与
`>=1.1.8` 的优先级比较。

**禁止**改成 `1.1.8-neoforge...`：`-` 会产生低于正式 `1.1.8` 的 pre-release，导致部分
枪包依赖检查静默失败。

## 8. 文档

- [文档索引](docs/README.md)
- [开发指南](docs/DEVELOPMENT.md)
- [当前状态与发布闸门](docs/PORTING_STATUS.md)
- [兼容矩阵](COMPATIBILITY.md)
- [专服与多人测试](docs/DEDICATED_SERVER_TEST.md)
- [枪包指南](docs/GUNPACKS.md)
- [R1 发布检查清单](docs/RELEASE_CHECKLIST.md)
- [更新日志](CHANGELOG.md)
- [许可证清单](LICENSES.md)

## 9. 许可与反馈

```

- 代码：GPL-3.0-only；发布二进制时必须同步提供对应源码。
- 原版资源：CC BY-NC-ND 4.0。
- 本移植问题请提交到本仓库，不要打扰原作者。
- 禁止下载、反编译或参考 CurseForge `tacz-port`（guilhermez1989）的 jar。

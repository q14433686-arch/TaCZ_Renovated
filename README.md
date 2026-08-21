# TaCZ: Renovated — NeoForge 26.2

> **非官方移植。请勿向 MCModderAnchor / Serene Wave Studio 报告本移植的问题。**
>
> 当前源码版本：**`1.1.8+neoforge.26.2.0.R1`**；状态：**Unreleased R1 candidate**

从 NeoForge 26.1.2 R1 前滚到 Minecraft 26.2 的 TaCZ 社区移植。modId 保持
`tacz`，枪包的 `tacz >= 1.1.8` 依赖检查继续有效。

## 1. 支持环境

| 组件 | 版本 / 状态 |
|---|---|
| Minecraft | **26.2** |
| NeoForge | **26.2.0.64** release |
| Java | **25** |
| 本 Mod | **1.1.8+neoforge.26.2.0.R1** |
| Gradle / ModDevGradle | 9.2.1 / 2.0.144 |
| 专用服务器与基础多人 | L0-L3 用户 PASS（版本定名前同代码候选） |
| 第三方枪包专项 | L2.5 待单独确认 |
| GPU / 可选 Mod | 逐项状态见 [`COMPATIBILITY.md`](COMPATIBILITY.md) |

R1 只改版本 metadata 与文档，不改上述已测逻辑；但最终 R1 jar 的文件名、mods.toml 展开和
Mod List 版本仍应补跑一次 L0/L1 快速确认。冻结测试记录：
[`SERVER_TEST_20260821_262_R1.md`](docs/records/SERVER_TEST_20260821_262_R1.md)。

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
- **LRTactical**：仍维持 26.1.2 R1 的撤回决定；本仓库不包含其四类基础物品与行为。
- **Aperture / 未核 shader replacement**：没有稳定 bridge 时走未掩码安全回退。

未实际执行的项目不会写成兼容或 PASS。

## 3. 安装

1. 安装 Minecraft 26.2、NeoForge 26.2.0.64 与 Java 25。
2. 将构建产物 `tacz-1.1.8+neoforge.26.2.0.R1.jar` 放入实例 `mods/`。
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

详见 [`docs/GUNPACKS.md`](docs/GUNPACKS.md) 与 L2.5 测试章节。

## 5. 可选 Mod

已核公开 artifact/API 的项目包括 Cloth Config、PAL、Controllable、Shoulder Surfing、JEI、
REI、Iris 与 Carry On。构建通过不等于游戏内兼容；逐项版本、缺口与测试矩阵见
[`COMPATIBILITY.md`](COMPATIBILITY.md)。

First-person Model 与 Not Enough Animations 在核验日没有 NeoForge 26.2 发布文件；源码中的
反射桥仅为 dormant 预留，不作为可安装兼容宣传。

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

`1.1.8` 是 SemVer core；`+neoforge.26.2.0.R1` 是 build metadata，不参与
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

## 9. 谱系、许可与反馈

```text
MCModderAnchor/TACZ                         1.20.1 Forge 官方源
        ├── Sh1roCu/TACZ-Refabricated      1.21.1 Fabric
        │       └── TaCZ_Refabricated_Unofficial 26.2（游戏语义权威）
        └── MUKSC/TACZ-1.21.1              1.21.1 NeoForge（仅加载器习语）
                                └── TaCZ: Renovated 26.1.2 R1 → 26.2 R1
```

- 代码：GPL-3.0-only；发布二进制时必须同步提供对应源码。
- 原版资源：CC BY-NC-ND 4.0。
- 本移植问题请提交到本仓库，不要打扰原作者。
- 禁止下载、反编译或参考 CurseForge `tacz-port`（guilhermez1989）的 jar。

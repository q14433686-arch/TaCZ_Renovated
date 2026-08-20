# TaCZ NeoForge 26.1.2（非官方移植）

> **非官方。请勿向原作者（MCModderAnchor / Serene Wave Studio）报 bug。**
> 首发目标：**Minecraft 26.1.2 + NeoForge 26.1.2.x（release 通道）**。
> 1.21.11 与 26.2 不在本阶段范围内。

Timeless and Classics Zero 的 NeoForge 26.1.2 移植。公开源码、谱系可审计。当前处于 **工作包⑥完成（26.1.2 首发）**：depth-aperture 瞄具、第一人称 Feature Rendering、可选 Iris ShaderCompat。

## 谱系（GPL-3.0）

```
MCModderAnchor/TACZ                 1.20.1 Forge 官方源
        │
        ├── Sh1roCu/TACZ-Refabricated          1.21.1 Fabric
        │         └── q14433686-arch/TaCZ_Refabricated_Unofficial
        │                   26.x Fabric（本移植的游戏语义权威）
        │
        └── MUKSC/TACZ-1.21.1 (neoforge/1.21.1)
                  1.21.1 NeoForge（加载器习语权威；禁止抄其渲染）
                            │
                            └── 本仓库  NeoForge 26.1.2
```

衍生合法，义务是：发布二进制必须同步提供完整对应源码，保留原作者版权声明。

- 代码：GPL-3.0-only
- 资源：CC BY-NC-ND 4.0（原版资产许可，沿用上游）

## 工作包进度

| # | 工作包 | 状态 |
|---|---|---|
| ① | 构建骨架（ModDevGradle + 空 mod） | 完成（`./gradlew build` + `runServer` Mod List 可见 `tacz`） |
| ② | 注册与数据层 | 完成（物品/方块/创造标签/RecipeCompat codec；`tacz:workbench_a` 与 `tacz:modern_kinetic_gun` 已进注册表） |
| ③ | 网络与同步 | 完成（PayloadRegistrar 全量注册；C2S 开火/换弹转播 S2C；专用服务端可启动） |
| ④ | 非渲染游戏逻辑 | 完成（弹道/枪包/IGunOperator mixin；专用服务端冒烟） |
| ⑤ | 渲染层 | 完成（depth-aperture、第一人称 SubmitNodeCollector、BER） |
| ⑥ | 兼容层（ShaderCompat） | 完成（可选 Iris mixin + 反射；无 Iris 不加载） |

规则见 [`CHARTER.md`](CHARTER.md)。API 证据见 [`docs/WP01_EVIDENCE.md`](docs/WP01_EVIDENCE.md)。

## 开发环境

- JDK **25**（Minecraft 26.1.2 / NeoForge 官方要求）
- Gradle Wrapper 9.2.1（仓库自带）
- **不要配置 mappings / parchment / Yarn**。26.1+ 游戏本体未混淆。

```bash
./gradlew build
./gradlew runClient
```

## 版本号

`mod_version` 必须写成 `1.1.8+neoforge...`（SemVer **build metadata**）。

**禁止**写成 `1.1.8-neoforge...`（那是 pre-release，会导致枪包的 `>=1.1.8` 检查静默失败）。

## 洁净室

禁止接触 CurseForge 项目 `tacz-port`（作者 guilhermez1989）的 jar。不下载、不反编译、不参考。

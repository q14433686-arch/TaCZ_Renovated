# WP-11211-0/1 证据：1.21.11 构建环境与依赖钉版

> 2026-08-22 联网核实。全部结论以当日的活动仓库/API 实测为准，非训练数据记忆。
> 每项给出来源层级：① maven metadata / API 直接返回；② 官方仓库钉版；③ 姊妹项目实践。

## 一、工具链（①② 双源一致）

| 项 | 值 | 证据 |
|---|---|---|
| NeoForge | **21.11.45** | maven.neoforged.net `net/neoforged/neoforge/maven-metadata.xml`：21.11 线 release 构建为 21.11.42 / 21.11.44 / 21.11.45（**21.11.43 不存在**）；官方 `NeoForgeMDKs/MDK-1.21.11-ModDevGradle` 的 gradle.properties 同样钉 `neo_version=21.11.45`（①） |
| Minecraft | 1.21.11（**混淆**版本） | 与 26.1+ 不同，官方映射由 MDG 自动接线（①） |
| ModDevGradle | **2.0.144**（不变） | 官方 MDK-1.21.11 的 build.gradle 钉同一版本（②）；本仓库已是 2.0.144 |
| Gradle wrapper | 9.2.1（不变） | 本仓库 gradle-wrapper.properties |
| JDK | **21** | MDK 注释 "Mojang ships Java 21 to end users in 1.21.11"（②）；本仓库 toolchain 25→21 |
| minecraft_version_range | `[1.21.11]` | MDK 同款（②），延续本仓库精确区间纪律 |

## 二、兼容依赖钉版（1.21.11 NeoForge）

| 依赖 | 钉版 | 来源与证据 | 与 26.1.2 的变化 |
|---|---|---|---|
| JEI | `27.30.0.76` | maven.blamejared.com `mezz/jei/jei-1.21.11-neoforge/maven-metadata.xml`（①） | 26.1.2 用 29.29.0.77；姊妹 Fabric 1.21.11 用 27.23.0.71（同 27.x 线） |
| REI | `21.11.816` | maven.shedaniel.me `me/shedaniel/RoughlyEnoughItems-neoforge/maven-metadata.xml` 21.11 线最新（①）；姊妹 Fabric 同钉 21.11.816（③） | **坐标源切换**：CurseMaven file id → maven.shedaniel.me 坐标（免 file id） |
| Architectury | `19.0.1` | maven.architectury.dev `architectury-neoforge/maven-metadata.xml`：19.x 线仅 19.0.1（①）；姊妹同钉（③） | 26.1.2 用 20.0.6 |
| Cloth Config | `21.11.153` | maven.shedaniel.me `cloth-config-neoforge/maven-metadata.xml` 21.11 线最新（①） | 26.1.2 用 26.1.154；姊妹 Fabric 同钉 21.11.153（③） |
| PAL | **1.1.9**（NeoForge 变体 jar） | Modrinth `player-animation-library` 1.21.11：`PlayerAnimationLibNeoforge-1.1.9+mc.1.21.11.jar`（①）。1.1.10 已于 2026-08-18 发布，仍钉 1.1.9 以对齐姊妹已验证的 API 面（③） | 26.1.2 用 1.2.5 merged jar；**API 差异**：姊妹记录 1.1.9 的 `get3DTransform` 返回 `PlayerAnimBone`（1.2.5 返回 void）→ compat 代码适配 |
| Controllable | **0.25.8** | MrCrayfish/Controllable GitHub release `v1.21.11-0.25.8`，资产 `controllable-neoforge-1.21.11-0.25.8-signed.jar`（①）。无 Modrinth 项目（API 404） | 26.1.2 用 0.26.0（**向下**跨版本，符号待编译期核） |
| Shoulder Surfing | **1.21.11-5.0.10**（NeoForge） | Modrinth `shoulder-surfing-reloaded` 1.21.11 neoforge 最新，发布于 2026-08-07（①） | 26.1.2 用 5.0.10 同版号线 |
| Carry On | 2.9.2（仅运行时验证用） | Modrinth `carry-on` 1.21.11 neoforge：`carryon-neoforge-1.21.11-2.9.2.jar`（①）；姊妹同用 2.9.2（③）。**无编译期依赖**：本仓库 carryon mixin 走反射 + mixin plugin 门控（CarryOnReflection / CarryOnCompatMixinPlugin） | 运行时矩阵项，非 compileOnly |
| Iris | 1.10.7+mc1.21.11（NeoForge，仅运行时验证用） | Modrinth `iris` 1.21.11 neoforge：`iris-neoforge-1.10.7+mc1.21.11.jar`（①）。**无编译期依赖**：IrisDepthRestoreShaderMixin 用 `@Mixin(targets=...)` 字符串目标（javadoc 自证），plugin 门控 | 运行时矩阵项 |
| Sodium | 0.8.13 稳定 / 0.8.14-beta（NeoForge，仅验证用） | Modrinth `sodium` 1.21.11 neoforge（①） | 可选验证 profile |
| Zoomify | —（**无 NeoForge 1.21.11 构建**） | Modrinth `zoomify` 1.21.11 neoforge = 0 版本（①） | 维持现状 no-op（AGENTS §2 口径：no-op 不写成兼容） |

**结论：八项可钉（含三项 libs/ 落盘），零项需挂起。**

## 三、libs/ 落盘记录（escape hatch）

| 文件 | 来源 URL |
|---|---|
| `libs/PlayerAnimationLibNeoforge-1.1.9+mc.1.21.11.jar` | Modrinth CDN（player-animation-library，neoforge 1.1.9 primary 文件） |
| `libs/controllable-neoforge-1.21.11-0.25.8-signed.jar` | github.com/MrCrayfish/Controllable/releases/download/v1.21.11-0.25.8/ |
| `libs/ShoulderSurfing-NeoForge-1.21.11-5.0.10.jar` | Modrinth CDN（shoulder-surfing-reloaded，1.21.11-5.0.10+neoforge primary 文件） |

远程坐标不可用原因（写入 build.gradle 注释）：CurseMaven file id 离线不可解析；
Modrinth maven 无法表达版本串中的 `+`（Gradle 会解析为动态版本）。三个 fileTree
escape hatch 缺失时改为 `GradleException` 直接给出下载指引，杜绝静默拿到 26.1.2 旧 jar。

## 四、姊妹项目资产复用（③）

已拉取至本地工具目录（不提交仓库）：`classify_errors.py`、`migrate_family1.py`、
`migrate_gui_overrides.py`、`migrate_dynamic_item_model.py`、`verify_mixin_targets.py`、
`verify_shader_imports.py`、`port-1.21.11-error-families.json`（refab@1.21.11 docs/）。
WP-11211-2 编译收敛时按本仓库包结构适配后使用。

## 五、refmap 不需要（NeoForge 侧机制，2026-08-22 核实）

- NeoForge 官方 21.11 发布博文（neoforged.net/news/21.11release/）：「NeoForge 21.11
  continues to be built on top of an obfuscated Minecraft … **we already removed
  obfuscation of mods from our toolchain two years ago**」。
- 即：**mod 以官方（named）映射分发，运行期由 NeoForge 处理游戏侧混淆**——jar 不带
  refmap、mixin 目标写官方名是正确形态。与姊妹 Fabric 线需要 Loom refmap 的机制不同
  （Fabric 以 intermediary 分发）。
- 本线构建产物已验证：class 文件引用官方名、无 refmap，专服 runServer 运行期
  mixin/AT 全部生效（PORT_11211_COMPILE_RECORD.md 第四节）。

## 六、本仓库既有资产核验

- 四份 mixin json 的 compatibilityLevel：tacz/iris 已 JAVA_21；carryon 为 JAVA_25
  → **已改 JAVA_21**（1.21.11 运行于 Java 21）；lrtactical 为 JAVA_17（≤21，兼容，不动）。
- Iris / CarryOn mixin 均不 import 目标 mod 类型（字符串 @Mixin target + 反射 +
  plugin 门控），**无需新增 compileOnly**——运行时兼容在 WP-11211-5 验证。
- mods.toml 的 `[[dependencies]]` 走模板占位符，无硬编码版本。

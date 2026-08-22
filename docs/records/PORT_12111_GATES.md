# WP-12111-0/1 证据记录：1.21.11 回移前置闸门与构建骨架

> 执行日期：2026-08-22。工单：`docs/PORT_12111_BRIEF.md`。
> 冻结快照性质：记录本轮每条钉版决定的证据来源；后续轮次修订时新增条目，不回改本文。

---

## 0. 沙箱执行约束（如实申报）

本工作沙箱的网络出口仅放行 GitHub（api.github.com / github.com / codeload.github.com）。
以下主机**实测 TLS 不可达**（2026-08-22，`curl` 全返回 `SSL_ERROR_SYSCALL`）：

- `maven.neoforged.net`（NeoForge 构件）、`maven.parchmentmc.org`、`api.modrinth.com`、
  `services.gradle.org`（Gradle 发行包）、`libraries.minecraft.net` / `piston-meta.mojang.com`
  （MC 构件）、`repo.maven.apache.org`、`cursemaven.com`、`files.minecraftforge.net`、
  `maven.fabricmc.net`、`ldtteam.jfrog.io`；
- 且沙箱无 JDK（`java`/`javac` 均不存在）、无 sudo。

**结论：本沙箱无法运行任何 Gradle 任务。** 本轮所有构建配置只能静态编写，首次
`./gradlew help` / `compileJava` 必须在下游有完整网络的机器上执行（见第 5 节 runbook）。
所有钉版数字均来自 GitHub 可达的一手源，见下。

---

## 1. 构建骨架钉版（全部 GitHub 可达一手源，2026-08-22 抓取）

| 项 | 钉版 | 证据 |
|---|---|---|
| NeoForge | **21.11.45**（release 通道） | 官方 [MDK-1.21.11-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle) `gradle.properties`：`neo_version=21.11.45`（main 分支，2026-08-22 抓取）。1.21.11 线属 release 通道、目标为混淆版 1.21.11：NeoForge 官方发布页 [NeoForge 21.11 for Minecraft 1.21.11](https://neoforged.net/news/21.11release/)（2025-12-09） |
| Minecraft / 范围 | `1.21.11` / `[1.21.11]` | 同上两份证据；另 [neoforged/NeoForge](https://github.com/neoforged/NeoForge) 分支 `1.21.11` 的 `gradle.properties`：`minecraft_version=1.21.11`、`java_version=21`（2026-08-22 抓取） |
| Parchment | `1.21.11` / `2025.12.20` | MDK-1.21.11 `gradle.properties`：`parchment_minecraft_version=1.21.11`、`parchment_mappings_version=2025.12.20` |
| ModDevGradle | `2.0.144` | MDK-1.21.11 `build.gradle` 插件行；与本仓库 26.1.2 基线同版本，无需换 |
| Gradle wrapper | `9.2.1` | MDK-1.21.11 `gradle/wrapper/gradle-wrapper.properties` 与本仓库现有 wrapper 完全一致 |
| JDK | 21 | MDK-1.21.11 `build.gradle` 注释「Mojang ships Java 21 to end users in 1.21.11」+ NeoForge 分支 `java_version=21` |
| settings.gradle | 不变 | MDK-1.21.11 与本仓库现有内容一致（gradlePluginPortal + foojay 1.0.0） |
| mixin AP / refmap | **无需手写接线** | MDG 2.x 自动接线：MDG 仓库 testproject 自带 mixin（`testproject/.../mixins/BlockPosMixin.java` + `testmod.mixins.json`）且构建文件无任何 annotationProcessor 配置（2026-08-22 树检查） |
| `neoforge.mods.toml` 键位 | 现有模板已兼容 | MDK-1.21.11 模板与本仓库模板同构：`license` 顶层、`[[mods]]`、`[[mixins]]`、`[[accessTransformers]]`、`[[dependencies.${mod_id}]]`（neoforge/minecraft）——无需结构性改动 |

## 2. vendored jar 字节码核验（WP-12111-0 闸门）

`libs/` 两个 jar 均为 26.1.2 纪元随迁的本地文件，用 Python 读 class 头（2026-08-22 实测）：

| jar | 首个 class major | 对应 Java | 结论 |
|---|---|---|---|
| `commons-math3-3.6.1.jar` | 49 | Java 5 | ✅ Java 21 可加载，不用换 |
| `luaj-jse-3.0.1.jar` | 47 | Java 3（1.3 时代字节码） | ✅ 同上 |

## 3. mixin 配置 compatibilityLevel

| 配置 | 26.1.2 值 | 1.21.11 动作 | 依据 |
|---|---|---|---|
| `tacz.mixins.json` | JAVA_21 | 不变 ✅ | 1.21.11 运行在 JVM 21 |
| `tacz.iris.mixins.json` | JAVA_21 | 不变 ✅ | 同上 |
| `lrtactical.mixins.json` | JAVA_17 | 不变 ✅ | JAVA_17 ≤ 21，合法 |
| `tacz.carryon.mixins.json` | **JAVA_25** | **改 JAVA_21** | 1.21.11 的 sponge-mixin 无 JAVA_25 枚举值（姊妹项目阶段 1 同结论：1.21.11 加载的 sponge-mixin 0.17.3+mixin.0.8.7 支持到 JAVA_21） |

## 4. 访问转换器第一轮（WP-12111-1）

保留 5 条、删 1 条。姊妹项目 1.21.11 分支对其同名 AW 五目标已 **javap 逐符号核实**
（`docs/PORT_1_21_11_PHASE1.md`「AW 五个目标逐一核验」表）：

| 目标 | 姊妹项目 1.21.11 javap 结论 | 本仓库动作 |
|---|---|---|
| `RenderType <init>(String;RenderSetup)V` | ✅ private，描述符完全一致 | 保留；WP-12111-2 复验 |
| `MultiPlayerGameMode#ensureHasSentCarriedItem` | ✅ private，存在 | 保留；复验 |
| `Minecraft#startUseItem` | ✅ private，存在 | 保留；复验 |
| `LivingEntity#jumping` | ✅ protected，存在 | 保留；复验 |
| `Player#canCriticalAttack(Entity)Z` | ✅ private（26.1.2 是包级私有），AT 依然必要 | 保留；复验 |
| `RenderPipelines#register` | ❌ 26.x 专属，1.21.11 无此类 | **删除**（瞄具管线随 §4-E 重写） |

## 5. 可选集成 1.21.11 线钉版（证据层级标注）

| 集成 | 钉版 | 证据层级 |
|---|---|---|
| JEI | `jei-1.21.11-neoforge:27.23.0.71` | ③ 姊妹项目 1.21.11 gradle.properties `jei_version=27.23.0.71`（2026-08-13 对活动仓库解析 200）——**Fabric 变体**；NeoForge 同线为推断，libs/ 兜底 |
| REI | `roughly-enough-items-neoforge:21.11.816` | ③ 姊妹项目 `rei_version=21.11.816`（Fabric）；NeoForge 同线推断，libs/ 兜底 |
| Architectury | `19.0.1` | ③ 姊妹项目 `architectury_version=19.0.1`（2026-08-13 解析 200） |
| Cloth | `cloth-config-neoforge:21.11.153` | ③ 姊妹项目 `cloth_config_fabric=21.11.153`（Fabric 坐标）；NeoForge 同线推断，libs/ 兜底 |
| PAL | `1.1.9`（**降版**） | ③ 姊妹项目 `player_animation_lib=1.1.9`（Modrinth，解析 200）；26.1.2 用 1.2.5 → `compat/playeranimator/**` API delta 待核 |
| Controllable | `1.21.11-0.25.8`（**降版**） | ① GitHub release 实证（2026-08-22）：`MrCrayfish/Controllable` tag `v1.21.11-0.25.8`，资产 `controllable-neoforge-1.21.11-0.25.8-signed.jar`。26.1.2 用 0.26.0 → `compat/controllable/**` API delta 待核 |
| Shoulder Surfing | `1.21.11-5.0.10+neoforge` | ③ 姊妹项目 `shoulder_surfing_version=9T2YSavE`（Modrinth 版本 id，对应 `1.21.11-5.0.10+fabric`）；NeoForge 后缀为推断，libs/ 兜底 |
| Iris | `1.10.7+mc1.21.11-neoforge`（仅客户端 dev 档案） | ③ 姊妹项目 `iris_curse_file=7805348`（`1.10.7+mc1.21.11` Fabric）；Iris mixin 全部字符串 targets，**无编译依赖**；NeoForge 变体存在性待验证 |
| Carry On | `2.9.2` 线（运行时） | ③ 姊妹项目 R2 钉 Carry On 2.9.2（Fabric）；本仓库 CarryOn mixin 全部字符串 targets，**无编译依赖**；R2 兼容逻辑回哺待后续包 |

> 证据层级：① = 一手（GitHub release 资产名 / 官方仓库文件，2026-08-22 抓取）；
> ③ = 姊妹项目 1.21.11 分支钉版（其 2026-08-13 对活动仓库解析记录）。
> 层级 ③ 的 NeoForge 变体坐标属**同线推断**，下游机器上首次构建若解析失败，
> 按 `build.gradle` 注释把对应 jar 丢进 `libs/` 即可（兜底优先于坐标）。

## 6. 下游机器 runbook（WP-12111-2 开工第一步）

```bash
# 1) JDK 21 环境（foojay resolver 会补 toolchain）
java -version                          # 期望 21.x（Gradle 9.2.1 本身要求 JVM 17+）

# 2) 构建骨架验证（会拉取 MC/NeoForge/Parchment 工件）
./gradlew help                         # 期望 BUILD SUCCESSFUL；确认 1.21.11 named jar 就位

# 3) 依赖解析验证（§5 坐标逐个 200；失败的丢 libs/ 兜底）
./gradlew dependencies --configuration compileClasspath

# 4) 首轮编译错误基线（预期 ~11 个错误族，见 PORT_12111_BRIEF.md §4）
./gradlew compileJava 2>&1 | tee docs/port-12111-compile-01.log

# 5) 后续：移植姊妹项目 docs/verify_mixin_targets.py 后，每次改 mixin 跑一遍
```

## 7. 本轮未做（如实申报）

- 未运行任何 Gradle 任务（沙箱无 JDK + 构件源不可达，见 §0）；
- 业务源码迁移（GUI 族、Feature Rendering 回退、瞄具管线、mixin 目标重验等）
  全部在 WP-12111-2/3，未开始；
- 未推送任何远程分支。

## 8. 追记（WP-12111-2 执行中，2026-08-22）：三条工单假设被姊妹 1.21.11 定稿推翻

1. **1.21.11 原生有 Feature Rendering**：姊妹定稿仍用 `SubmitNodeCollector` /
   `RenderTypes` / `submitCustomGeometry`；26.1 新增的只是 `RenderPipeline` 状态对象层。
   PORT_12111_BRIEF §4-C 已更正——不需要 33 文件的 MultiBufferSource 回退。
2. **NeoForge 21.11 原生触发 `ViewportEvent.ComputeCameraAngles`**（neoforged/NeoForge
   `1.21.11` 分支 `patches/net/minecraft/client/Camera.java.patch`：`Camera#setup` 内
   `NeoForge.EVENT_BUS.post(new ComputeCameraAngles(...))` + `setRotation(yaw, pitch, roll)`），
   `ComputeFov` 同样原生触发（ClientHooks.java:362）。因此 CameraMixin 已删除、
   GameRendererMixin 不需要 getFov hook（Fabric 侧才需要）。这与 26.1.2 基线注释
   「RenderFrameEvent is already fired by NeoForge ClientHooks」同一性质。
3. **本仓库 25 个已注册 mixin 中绝大多数与姊妹 1.21.11 定稿签名逐字一致**（diff 实测：
   仅 GlCommandEncoder（+GL_ALWAYS 方案，已移植）与 GameRendererMixin（Fabric 独有
   FOV hook，不需要）两处有差）；PIP 两文件与姊妹定稿逐字节一致（无需改）；
   LR 侧仅 `GuiGraphicsExtractorMixin`→`GuiGraphicsMixin`（目标
   `itemCooldown`→`renderItemCooldown`，javap 核实）一处。

WP-12111-2 剩余主体 = scope 包 + Iris 层整体采纳（见 PORT_12111_BRIEF §4-E 修订版），
连同爆头盒 Gizmo 修复与 `RenderTypes` 常量核验，归入下一回合。

## 9. 姊妹参考文件镜像

本沙箱不持久化 /tmp；WP-12111-3 需要的姊妹 1.21.11 定稿文件（scope 包 9 文件、
IrisCompat legacy/newly、2 个 iris mixin、GameRendererMixin/GlCommandEncoder 参考、
migrate 脚本、错误族 JSON、树清单）已镜像到 `/home/user/.ref-sister12111/`
（git 之外、snapshot 之内），下一回合直接消费。

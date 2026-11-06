# 上游 TaCZ 1.2.0 变更覆盖面审计（2026-11-07）

> 性质：**只做源码比对与覆盖面盘点，不含任何移植代码**。本文所有数字来自
> `MCModderAnchor/TACZ` 公开仓库的 tag 比对，可用文末命令复现。
> 结论不构成任何「已验证」声明：本仓当前 HEAD 未改一行代码，仍是
> `1.1.8+neoforge.26.2.R3-hotfix4`。

## 0. 比对口径

| 项 | 值 |
|---|---|
| 上游仓库 | [`MCModderAnchor/TACZ`](https://github.com/MCModderAnchor/TACZ) |
| 基线 tag | `1.1.8-hotfix`（`b43eb84`，2026-05-25）—— 本仓移植所依据的 1.1.8 系列末版 |
| 目标 tag | `1.2.0-forge-mc1.20.1`（`618c927`，2026-11-05 发布）—— **内容主线** |
| 参考 tag | `1.2.0-neoforge-1.21.1`（`7f5b112`，同日发布）—— 内容同源，另加 1.21.1 平台层 |
| 本仓对照 | 当前工作树（`arena/88c6f5d1-tacz-renovated`，`1.1.8+neoforge.26.2.R3-hotfix4`） |

1.2.0 是 1.1.8 之后的第一个 feature 版本，距 `1.1.8-hotfix` 约 5.5 个月：
**246 个非合并提交 + 4 个合并提交**，`changelogs/changlog-1.2.0.md` 是官方口径。

## 1. 总量

| 维度 | 1.1.8-hotfix | 1.2.0 | 变化 |
|---|---|---|---|
| 全仓文件 | 4,187 | 5,328 | +1,141 |
| Java 文件 | 652 | 799 | +147 |
| Java 行数 | 58,926 | 76,507 | **+17,581（+30%）** |
| 资源文件 | 3,508 | 4,497 | +989 |
| 资源体积 | 77.0 MB | 98.2 MB | **+21.2 MB** |
| 网络消息 | 31 | 42 | **+11** |

`git diff --numstat 1.1.8-hotfix 1.2.0-forge-mc1.20.1`：**2,132 文件 / +470,503 / −44,032**。
其中 `src/main/resources/assets` 一项就占 1,737 文件 / +450,470（默认枪包的美术与数据），
`src/main/java` 是 368 文件 / +19,221 / −1,643。

## 2. 功能块（按对移植的影响排序）

### 2.1 新建方块与容器系统（全新子系统，12 文件 / +1,798 行 Java）

六个新方块 + 三个新方块实体：

- `gun_pegboard` / `glow_gun_pegboard`（可连接、转角、双面、染色、放置预览，独立 `PegboardShape`）；
- `gun_display_case`（可连接、可上锁的展示柜）；
- `ammo_storage` / `_gold` / `_diamond`（大容量、可升级、可搬运、与弹药盒互转）。

配套：4 个 GUI 屏幕/菜单（`AmmoStorageScreen/Menu/Inventory`）、3 个新配方类型或配方
（`AmmoStorageUpgradeRecipe`、`GunPegboardDyeRecipe`）、4 个交互事件、
`GunDisplayCaseSlotHelper` / `GunPegboardSlotHelper`、11 条网络消息、6 个 blockstates、
~40 个方块模型。

**对本线的意义**：这部分**几乎没有 26.2 渲染纪元包袱**（除方块实体渲染器），
是「纯逻辑 + 26.2 标准 GUI/容器 API」的移植，属于性价比最高的一块。

### 2.2 开镜（ADS）表现体系重做

- `ads_path` / `ads_fov` 曲线化：`AdsCurve` / `AdsPath` / `AdsFov` / `AdsPreset`（5 个新 POJO）；
- **共享预设机制**：`assets/<ns>/ads_presets/<name>.json`，新增 `docs/ads-presets.md` 说明文档，
  默认枪包 67 把常规枪引用 `tacz:renew`，M320 / RPG7 / Minigun 走 `tacz:legacy`；
- 26 种缓动曲线（`Easing`）、`clamp` 语义、单枪覆盖优先级；
- 开镜时间/FOV 的平滑器从此可分通道调。

### 2.3 瞄具镜片与热成像（渲染前沿，风险最高）

`client/renderer/scope/` 新增 4 文件 / **961 行**：

- `ScopeLensRenderer`（511 行）：屏幕空间透镜 pass，边缘畸变、色散、色调、暗角，
  多 sampler + 十几个 uniform；
- `ScopeThermalRenderer`（302 行）：白热/黑热/铁弓三种热成像模式；
- `ScopeLensContour`（147 行）：从 ocular 生成距离场轮廓；
- 新 shader 13 个文件（`scope_lens*.vsh/fsh`、`scope_thermal_mask*`）。

依赖链：`TextureTarget` + `ShaderInstance` + `RegisterShadersEvent` + **裸 GL**
（`GlStateManager` / `GL11` / `GL13` / `GL30`）+ `CameraStateCache`（SBM）。
配套 mixin：`EntityRenderDispatcherMixin`（WrapOperation 抓实体顶点喂热成像）、
`RenderSystemAccessor`（`shaderLightDirections`）。

**这是 1.2.0 与 26.2 底座冲突最集中的一块**：本仓 26.2 线已把瞄具管线整体迁到
`RenderPipeline` / `GlCommandEncoder` / Feature Rendering（见
`client/render/scope/ScopePipRenderer` 等），上游这套是 **1.21.1 stencil+立即模式纪元的写法**。
按 `AGENTS.md` §3.8，机制归属上属于「上游纪元」的必须**按 26.2 机制重做**，不是照搬。

### 2.4 枪口特效换成 SnowStorm 粒子（依赖升级驱动）

- `MuzzleParticleManager`（585 行，全新）、`ThirdPersonMuzzleParticleManager`（155 行）、
  `HitSurfaceParticleSpawner`（270 行）、`HitBlockEffectHandler`（102 行）；
- 配方侧：`particle_definitions/muzzle_flash.particle.json`、`muzzle_flash_smoke.particle.json`；
- 配件新增 `flash_hider` / `smoke_hider` / `smoke_speed_scale` / `smoke_count_scale` /
  多方向 `SmokeDirectionData`；枪械 display 新增 `initial_speed_scale` / `smoke_particle_count`；
- 第一人称开火时短暂照亮手部模型（`flash_light_time`）；
- 动画新增 `particle_effects` 关键帧（`ParticleEffectKeyframes` + 序列化器，
  仅枪口位置，不支持 locator）。

**根因是依赖升级**：SimpleBedrockModel `2.2.2 → 2.6.2`，MAE `1.1.2 → 1.1.4`。
本仓当前**只有两个 SBM 接口的桩**（`IFPAnimationInstance`、`IFPGeoItemRenderer`），
上游 1.2.0 用到 SBM 的 **25 个类**（v1 particle 全家桶 + v2 mesh/baked 全家桶），
涉及 17 个文件 / 3,845 行。这块**不能照抄，必须先把 SBM 依赖关系理清**：

- v1 particle（snowstorm）→ 只要接进来，`MuzzleParticleManager` 可以基本原样搬；
- v2 mesh（`BakedBedrockModel` / `WorldMeshRenderer` / `MeshCachePolicy`）→ 是 26.2
  渲染底座重写，与上游 1.21.1 的 `MultiBufferSource` 提交模型**语义不同**，需重做。

### 2.5 批量渲染加速（VBO）

- `ExternalGunItemVboRenderer`、`DisplayGunMeshGroup` / `DisplayGunGeometry`（各 184 行）、
  `DisplayGunModelInstance`、`ExternalGunAppearance` / `ExternalGunModel`；
- 覆盖第三人称、掉落物、展示柜三种静态场景，缓存策略 `MeshCachePolicy(300s, 128, 512MB)`；
- 配置开关 `EnableExperimentalDisplayGunVbo` / `EnableStaticItemGunVbo`。

与 2.4 同一依赖（SBM v2）。对本线的意义是**性能**而非功能，可最后做。

### 2.6 枪包同步协议重写（服务端/网络，收益明确）

`ServerMessageSyncGunPack` 由「一条大包」改成 **GZIP 压缩 + 256 KiB 分片 + UUID 事务**：

- 新文件 `resource/network/GunPackSync.java`（110 行，纯 JDK，无 MC 依赖）；
- 服务端每次重载只压缩一次（`createMessages`），所有玩家复用；
- 客户端 `Receiver` 收齐最后一片才解压、校验（重复 ID / 类型 / 尾部数据），
  之后一次性 `ClientIndexManager.reload()`；
- 连接切换清理（`clearPendingSync`）、`NetworkHandler.VERSION` `1.0.5 → 1.0.14`。

**这一块 26.2 复用成本极低**：`GunPackSync` 完全平台无关，协议层只是
`FriendlyByteBuf` 读写；修的是「枪包多到无法进服」的实际故障。属于**建议优先做**的一块。

### 2.7 内置/可选配件与槽位适配器

- `builtin_attachments` 从 `Map<Type, ResourceLocation>` 变为 `BuiltInAttachment`
  （列表 + 默认 + 展示 + 是否显示图标），新增 `BuiltInAttachmentSerializer`
  兼容**字符串 / 数组 / 对象**三种写法；
- 新索引：`slot_adapters`（6 个 riser + `SlotAdapterData`）、`attachment_variants`
  （`VariantData` + `ClientAttachmentVariantIndex` / `CommonAttachmentVariantIndex`）；
- 新网络消息：`ClientMessageSetAttachmentVariant`、`ClientMessageSetSlotAdapter`、
  `ClientMessageInstallVirtualAttachment`；
- `TimelessAPI` 新增 6 个公开查询入口；
- 枪包 index：attachments 99 → 128，guns 54 → 70。

### 2.8 卸弹、后坐力推挤、爬行、索敌

- 卸弹：`UnloadKey`（默认 `Alt+R`）、`LocalPlayerUnload` / `LivingEntityUnload` /
  `GunAmmoUnloadHelper`、`ClientMessagePlayerUnloadGun` + `ServerMessageGunUnload`、
  枪械 `allow_ammo_unload` 开关、`m870_unload_*` 脚本；
- 后坐力推挤：`GunRecoilPushData` + `RecoilPushModifier`（站/空中/蹲/趴四套参数）；
- 爬行：不再要求手持枪械（`EnableCrawlWithoutGun`）、俯仰角可配
  （`CrawlMaxUpAngle` / `CrawlMaxDownAngle`）、修复「强制保持爬行」与“按住爬行”；
- 索敌：`GunSoundAngerEvent`，开枪惊动中立/敌对生物，消音器免除，可配置关闭。

### 2.9 输入统一（键鼠 + 手柄 + 修饰键）

`client/input/` 12 个文件几乎全部重写（+305/−321），新增 `GunInputHandler`（123 行）
统一收口，`InputEvent.Key/MouseButton` 监听全部改为从 `GunInputHandler` 分发；
`MouseHandlerMixin` 重做爬行俯仰钳制。**本线已有自己的键位处理，需逐个对齐语义**。

### 2.10 其他

- 创造模式枪包筛选侧栏（`CreativePackFilters` 272 行 + `CreativeModeInventoryScreenMixin`）；
- JEI 查询（配件/弹药兼容 + 工作台配方转移）10 文件 / +699 行，`AmmoQueryCategory` 重做；
- 成就树 23 个 advancement + 2 个 trigger（`GunRefitTrigger` / `TargetHitTrigger`）；
- 音效：`SoundData`（音量/音高/随机浮动/引用原版音效）；
- 脚本沙箱：`GunScriptGuard`（服务端脚本异常不再崩服）、`AttachmentScriptManager`
  （`Modifier` 的 `function` 字段预编译，`ModifierTypeAdapterFactory`）；
- 配置项新增 **26 个**（见 §4.3）；
- 枪包与资源：新枪 16 把、新配件 29 个、新弹药 `.300 BLK`；24 个新 Lua 脚本；
  新 `default_state_machine_v2.lua`（802 行），旧状态机保留以兼容第三方枪包。

## 3. 1.21.1 平台层（与我们无关，但要认出来别误抄）

`1.2.0-neoforge-1.21.1` 相对 `1.2.0-forge-mc1.20.1` 多出 27 个文件，都是平台适配：

- **枪包版本覆盖层**：`GunPackOverlayResolver`、`OverlayPackResources`、`DelegatingPackResources`，
  默认枪包拆成 1.20.1 主包 + `compat_1_21_1/` 覆盖层（226 个文件，绝大多数是
  `forge:`→`c:` 标签与 `recipes/`→`recipe/` 目录名）；
- **旧枪包转换工具**：`/tacz convert universal` + `resource/convert/` 7 个文件；
- **数据组件原生实现**：`GunItemDataAccessor` 直接用 `DataComponents.CUSTOM_DATA`
  （本仓 26.2 已经在用同一套组件，`ItemNbtUtils` 已封装）；
- Iris 兼容、EMI 兼容、`NBTIngredient`、`PlayerAnimatorEasing`。

对本线的提示：**26.2 也属于「1.21+ 数据格式」纪元**，枪包最终会以「1.20.1 主包 + 版本覆盖层」
的形式发布。是否采纳覆盖层机制是**产品决策**（影响所有第三方枪包），需单独定。

## 4. 与本仓 26.2 线的交集分析（本审计的核心）

### 4.1 重合度

| 指标 | 数值 |
|---|---|
| 本仓 Java 文件 | 866（其中与上游 1.1.8 同名 609，本仓自有 257，上游有而我们没搬 43） |
| 上游 1.2.0 改动 Java 文件 | 368（修改 221 / 新增 147） |
| **「我们也改过 且 上游也改」——冲突热点** | **185 / 368（50.3%）** |
| 直接改（无冲突） | 183 文件 / 14,572 行 |
| 冲突热点涉及的上游改动 | 6,292 行 |
| 冲突热点中本仓 26.2 的改写量 | **+8,932 / −5,709** |

本仓 609 个同源文件里，**438 个（72%）已经被 26.2 移植改过**——这不是「拉个 diff 就能合并」
的状态，任何按文件覆盖的做法都会把 26.2 的 API 迁移成果冲掉。

### 4.2 冲突最重的文件（上游改动量降序）

| 上游改动 | 本仓 26.2 改写 | 文件 |
|---|---|---|
| +220/−42 | +83/−55 | `client/resource/GunDisplayInstance` |
| +185/−63 | **+962/−561** | `client/model/BedrockAttachmentModel` |
| +197/−20 | +73/−32 | `client/gui/GunRefitScreen` |
| +193/−0 | +3/−3 | `client/resource/pojo/display/gun/MuzzleFlash` |
| +126/−67 | +132/−38 | `client/renderer/item/GunItemRendererWrapper` |
| +173/−17 | **+328/−216** | `client/gui/GunSmithTableScreen` |
| +156/−4 | +19/−19 | `client/resource/index/ClientAttachmentIndex` |
| +103/−23 | +52/−20 | `client/event/FirstPersonRenderGunEvent` |
| +104/−20 | +29/−39 | `network/message/ServerMessageSyncGunPack` |
| +94/−7 | +81/−47 | `client/model/functional/AttachmentRender` |

按包统计冲突热点：`client` 79、`resource` 20、`api` 17、`util` 10、`entity` 9、
`config` 8、`mixin` 8、`compat` 7、`network` 7。

### 4.3 我们明显落后的地方（与 1.2.0 无关，但会被 1.2.0 放大）

- **配置项缺 26 个**：`SyncConfig` 7（爬行角度上下限、无枪爬行、标靶板黑白名单、
  空中腰射散布、命中方块特效广播距离）、`RenderConfig` 11（VBO 两个、镜片、热成像、
  命中粒子三项、枪口烟雾、创造侧栏、受伤晃动倍率、第三人称副手隐藏）、
  `GunConfig` 3（枪声索敌距离）、`AmmoConfig` 5（穿透方块、可破坏方块与硬度阈值、
  队伍免伤、伤害忽略名单）。
- **网络消息 31 vs 42**：差的 11 条正是 1.2.0 新增的（弹药存储 5、虚拟配件 1、
  变体/适配器 2、卸弹 2、命中方块 1）。
- **上游 43 个 1.1.8 文件从未搬**：KubeJS 兼容 18 个、Oculus 3 个、
  Accelerated Rendering 4 个、OptiFine 1 个、PlayerAnimator 动画层 5 个。
  其中 **12 个文件在 1.2.0 里又被改了**（`ARCompatImpl`、`OculusCompat` 系、
  `playeranimator/animation/*`、`mixin/client/ar/BedrockPartMixin`）——
  这些是「先补基座、再跟新」的二段活。
- **默认枪包仍是 1.1.8 内容**：3,322 文件 vs 4,194（1.20.1）/ 4,420（1.21.1 含覆盖层）。

### 4.4 26.2 特有的额外摩擦（上游两平台都没有）

| 摩擦点 | 说明 |
|---|---|
| 渲染纪元 | 上游镜片/热成像 = stencil + 立即模式 + 裸 GL；26.2 = `RenderPipeline` / `GlCommandEncoder` / Feature Rendering。68 个改动文件里 11 个引 `ShaderInstance`/`RegisterShadersEvent`、9 个裸 GL、64 个 `MultiBufferSource`/`RenderType`/`PoseStack` |
| SBM v2 | 上游依赖 2.6.2 的 v2 mesh/baked API；本仓只有 2 个接口桩，SBM 在 26.2 是否可用需先验证（这决定 2.4/2.5 能否落地） |
| 平台适配 | 368 个改动文件中只有 147 个在 1.20.1/1.21.1 两平台字节相同（12,879 行）；另 221 个上游自己还要做一次 1.20.1→1.21.1 适配，我们等于要跟着做第二次到 26.2 |
| Iris/Oculus | 上游 1.2.0 把光影兼容**改为 Iris**、删掉 Oculus；本仓已有自己的 Iris 桥（Fix-A），需要判断保留/替换 |
| 数据组件 | 上游 1.21.1 才原生用 `DataComponents`；本仓 26.2 已在用，这块**反而领先**，不必跟 |

## 5. 建议的落地顺序（5 个批次，按「收益 ÷ 26.2 风险」排）

| 批次 | 内容 | 理由 |
|---|---|---|
| **B1 服务端/协议层** | `GunPackSync` 压缩分片、11 条新网络消息、配置项 26 个、卸弹、后坐力、索敌、脚本沙箱、`BuiltInAttachment` 变体 | 几乎不碰渲染纪元；修的是「枪包多进不了服」等硬故障；可独立验收 |
| **B2 世界内容层** | 6 个新方块 + 3 个方块实体 + 弹药存储 GUI/容器 + 3 个配方 + 成就树 | 26.2 提供标准容器/GUI API，主要是逻辑移植；与新网络消息天然配套 |
| **B3 枪包内容同步** | 默认枪包 1.1.8→1.2.0（3,322→4,194 文件）、16 把新枪、29 个配件、24 个 Lua、`ads_presets` | 纯数据/美术，可与 B1/B2 并行；**但要先定「是否采纳覆盖层格式」** |
| **B4 表现层（先非图形）** | ADS 曲线预设、输入统一 `GunInputHandler`、JEI 查询、创造侧栏 | 有 26.2 对应 API，工作量集中在语义对齐 |
| **B5 图形前沿（最后做/单独立项）** | 镜片与热成像、SnowStorm 枪口粒子、SBM v2 批量渲染 | 必须先解决 SBM 依赖与 26.2 渲染管线；按 `AGENTS.md` §3.8 归入「按 26.2 机制重做」，不得折算成移植遗漏 |

B5 的前置阻塞项（建议先单独验证，再做任何图形移植）：

1. SBM 是否已有 26.x 版本 / 能否自行适配到 26.2 的渲染底座；
2. 26.2 下 `TextureTarget` + 后处理 pass 的等价写法（本仓 PIP 已趟过一遍，可复用）；
3. 热成像需要的「实体顶点抓取」在 Feature Rendering 下是否仍有对应注入点。

## 6. 明确不承诺的部分

- 本文**只比对源码**，没有编译、没有实机、没有任何 PASS 结论。
- 上游 1.2.0 官方 changelog 里的「修复」项不做逐条背书；
  本仓若要写进 `CHANGELOG.md`，必须自己实测或按 §3.8 归类。
- 上游 1.2.0 存在**行为不兼容变更**（`builtin_attachments` 格式、`Modifier` 预编译、
  输入分发、Iris 替换 Oculus），第三方枪包与附属 Mod 的兼容性未评估。
- 默认枪包升级为大版本内容替换，与「只搬代码不搬资源」的旧节奏不同，需要单独过一遍
  `docs/GUNPACKS.md` 的双端安装与版本谓词说明。

## 7. 复现命令

```bash
git clone --filter=blob:none https://github.com/MCModderAnchor/TACZ.git tacz-upstream
cd tacz-upstream
git diff --numstat 1.1.8-hotfix 1.2.0-forge-mc1.20.1
git diff --name-only 1.1.8-hotfix 1.2.0-forge-mc1.20.1 -- src/main/java
git diff --name-only 1.2.0-forge-mc1.20.1 1.2.0-neoforge-1.21.1 -- src/main/java   # 平台层
git show 1.2.0-forge-mc1.20.1:changelogs/changlog-1.2.0.md
```

冲突热点（185 个文件）的判定：对本仓与上游 1.1.8 同名文件做逐文件行级 diff，
取「本仓已有本地改动」与「上游 1.2.0 改动」的交集；判定脚本未入库（一次性分析）。

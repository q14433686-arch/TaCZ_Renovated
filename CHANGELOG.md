# 更新日志

版本号格式：`1.1.8+neoforge.26.1.2.<标签>`。`+` 之后是 SemVer build metadata，
因此枪包的 `tacz >= 1.1.8` 依赖检查照常通过（**禁止**改用 `-`，那是 pre-release，会静默不满足 `>=1.1.8`）。

## 1.1.8+neoforge.26.1.2.R2 — 2026-09-02

> R2 = R1-hotfix 之后回传的 26.2 修复 + 姊妹渲染线（v1–v5：TML Mesh 加载器 / PIP 二次渲染 /
> 镜内裁切与低倍率豁免 / tacz:nbt 材料 / 开镜距离补偿）。**其中 TML/PIP/镜内裁切尚未实机
> 验证**，见 [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md) 与
> [`docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)。

### 新增

- **内置 Mesh 加载器（TML）**：枪包可在 geo.json 骨骼上携带 `poly_mesh` 网格
  （`"model_type": "mesh"`），由本 mod 直接解析渲染；带第一人称 / 世界语境的 GPU
  静态烘焙、按光照档 LRU 缓存 + 每帧烘焙额度、目镜孔径裁剪与低倍镜豁免。移植自
  VellEagle/TacZMeshLoader（GPL-3.0，经姊妹项目中转）；配置见 `tacz-client.toml`
  `[mesh_loader]`（19 项，Cloth「渲染」页同步），说明与复测矩阵见
  [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md)。**运行期未验证**，实机前请按其中 §5 逐条测。
- **镜内画中画（PIP）与镜内裁切**：除经典整屏变焦外，新增 PIP 重投影与 PIP 二次渲染
  两种瞄具模式（`RenderConfig` 的 `ScopePip*` 键，默认关）；镜内文字 / 手臂 / 火光 /
  枪身 / 配件的孔径裁剪，以及低于 `ScopePipMinMagnification`（默认 4×）时不裁的
  低倍镜豁免。机制与验收见
  [`docs/SCOPE_PIP_RERENDER_IRIS_PORT_2612_20260901.md`](docs/SCOPE_PIP_RERENDER_IRIS_PORT_2612_20260901.md)、
  [`docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)
  与 [`docs/MESH_OCULAR_CLIP_IRIS_ROUTE_20260901.md`](docs/MESH_OCULAR_CLIP_IRIS_ROUTE_20260901.md)。
  **同上，运行期未验证**。
- **tacz:nbt 配方材料**：工作台配方支持 `type: "tacz:nbt"`（`TaCZPackUpgrader` 形态）
  以及无 `type` 的 `{item + nbt}` 隐式写法，统一按本仓 NeoForge 原生
  `tacz:partial_nbt` 自定义材料解析（宽松子集匹配；旧的静默丢弃 nbt 行为已修，
  材料格不再显示裸枪）。

### 修复

- **LR 长按右键的「幽灵使用」**：手雷 / 闪光弹 / 消耗品用完还没松手时，原版输入循环
  会立刻再开一次使用（`Minecraft#startUseItem`），导致进度条空读一遍、姿势与进度
  定格卡住。新增 `UsePressGate`（一次按压只消耗一次使用，右键抬起才解锁）+
  `MinecraftUseRestartMixin`（`startUseItem` HEAD 取消），并新增 `StuckUseRecovery`
  兜底：可预燃投掷物若陷进服务端不存在的使用状态，超过最长预燃 + 20 tick 余量后
  本地 `stopUsingItem()` 自行恢复（不发包、不误触发投掷）。
- **LR 冷却两端分叉**：`ThrowableItem#use` / `ConsumableItem#use` 改为两端都查各自的
  冷却表（服务端 `SERVER_COOL_DOWNS` / 客户端 `CLIENT_COOL_DOWNS`），不再「客户端
  一律乐观放行」——消除「服务端在冷却中、客户端却凭空起一轮使用」的状态分叉；
  服务端仍是投掷/消耗的唯一权威。
- **耳鸣消声注入点**：从 `SoundEngine#calculateVolume(SoundInstance)` 搬到
  `AbstractSoundInstance#getVolume()`。26.x 引擎里 `play()` 不经过外层重载
  （refab 对 26.2 字节码核对），旧注入点只压到「可 tick 音效」与改滑条重算的那批，
  表现为「有时闷有时不闷」；新注入点覆盖新播放 / tick 更新 / 改滑条三条路径，
  耳鸣声以 `instanceof` 豁免，不再依赖 `SoundSource` 类别（保持 `PLAYERS`）。

### 资源

- 新增 `assets/lrtactical/sounds.json`（顶层无 `_comment`，逐字节照抄 refab；顶层
  注释键会让引擎整体反序列化失败，耳鸣声 `NOT_STARTED` 的根因）。
- 新增 `sounds/stun_ringing.ogg`（28566 B）、`textures/mob_effect/deafened.png`（302 B）、
  `textures/mob_effect/blinded.png`（188 B）—— 之前缺文件，效果图标显示为紫黑块、
  耳鸣声无音源。
- 新增 `scripts/verify_lr_assets.py`（`--strict` 校验 sounds.json 结构 / ogg / 效果图标）
  与 `scripts/gen_effect_icons.py`（自绘 18×18 图标，不依赖 Pillow）。
- `DeafenState#tick` 接住 `SoundManager#play` 的返回结果，非 `STARTED` 时 WARN 一次，
  消息内列出三个已知坑（sounds.json 顶层坏键 / ogg 缺失 / 音量滑条为 0），
  避免「耳鸣不响却无日志」的排查盲区。

### 品牌

- 新增本仓库原创 `icon.png` / `logo.png`（青色四段瞄具环 + 铜色 R），并写入
  `neoforge.mods.toml` 的 `logoFile`。未使用官方 TaCZ 图标（CC BY-NC-ND 4.0
  禁止再创作），也与 Fabric 姊妹项目那套官方原图区分开。

### 文档

- 重写 CurseForge、Modrinth 与 MC 百科发布正文：按项目级长期说明与版本级
  Changelog 分层，删除冗长、重复及过度承诺的内容。
- MC 百科文案改为可直接使用的 BBCode，并为本项目 CurseForge 页面（Project ID
  1663324）、原始项目、直接上游、源码、Release、Issues 与许可文件补齐跳转链接。
- `docs/publish/RELEASE.md` 改为覆盖 26.2、26.1.2、1.21.11 的内部更新规范，明确
  活文档 → Release notes → 平台 Changelog 的同步顺序及“测试结论不得跨分支继承”。
- 新增 [`docs/MESH_LOADER.md`](docs/MESH_LOADER.md)（TML 主文档：来源/许可、19 项
  配置、26.1.2 消费点与 v5 修正、复测矩阵、设计不变量、枪包作者 §8）与
  [`docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md`](docs/SCOPE_ARM_CLIP_26_1_2_2026_09_02.md)
  （镜内裁手 + 低倍镜豁免 + 验收清单）；`PORT_..._20260901.md` 追加 v5 补丁段。
- README §2/§3/§6/§7 与 [`LICENSES.md`](LICENSES.md) 同步：项目范围补 TML/PIP，
  §3 由「不是 PIP」改写为「三类模式」，§6 补未实机边界，§7 补 TML 的
  VellEagle/GPL-3.0 来源与“不构成授权背书、上游问题回 TML 仓库”声明。

## 1.1.8+neoforge.26.1.2.R1-hotfix — 2026-08-27

> 版本号写法自 `R1.hotfix` 统一为 `R1-hotfix`（连字符风格，与 refab 来源分支的
> `R2-hotfix2` 一致；`-` 位于 `+` 之后的 build metadata 内部，不构成 pre-release）。

R1 后从 26.2 分支回传的修复与功能跟进（Iris 高倍镜裁剪 / ADS bob-scale 尝试
已在 26.2 上游实证失败后回退，本版本不含）。

### 修复

- **LR 投掷物**：站立不动时每 tick 向所有 LR 动画状态机广播 `INPUT_IDLE`，与官方手雷脚本里
  用作取消拔销的字面量 `"idle"` 撞名，导致静止拉栓反复抖动、走动反而正常。移动输入
  现只发给近战，匹配官方 LR `ClientEventsHandler#tickAnimation` 语义。
- **LR 投掷物**：可烹饪手雷（cookable）在满引信（remaining = 0）时实体首 tick 因
  `life > 0` 判断跳过 `onDeath` 导致不爆炸；条件改为 `life >= 0`，C4 等
  `life_time = -1` 的遥控物仍然永生。
- **LR 投掷物**：使用进度条（`UsingProgressOverlay`）分母按 90% 引信长度算，导致满烹饪
  时进度条永远到不了头；改为完整 `lifeTime`，匹配手中即引爆的行为。
- **LR 近战**：`MeleeDisplay` record 缺失 `display_offset` 字段声明，而 `create()`
  已读取 `pojo.displayOffset`，从源码构建会直接 `compileJava` 失败。
- **Punchy 兼容**：Punchy 在第一人称独立叠加手臂骨骼与行走/冲刺/视角滞后矩阵，
  覆盖了 TaCZ/LR 已烘焙好的枪+手动画。新增 5 个 `@Pseudo` mixin + `PunchyCompatMixinPlugin`
  （仅当 Punchy 在加载列表时启用），让 TaCZ 手持模型走 Punchy 支持的物品黑名单/
  让步路径，普通物品仍由 Punchy 接管。
- **烟雾粒子**：烟雾弹粒子硬编码全亮 `0xF000F0`，夜战时烟幕自身发光不自然；改按官方
  0.4.3 采环境光（天光/块光最低各 2，两者都 ≤2 时再扫六邻格取较大值）。

### 新增

- **LR 0.4.3 跟进**：display JSON 新增 `display_offset` 与 `entity_transform` 字段，
  近战/投掷物第一人称及飞行实体姿态按内容包定义应用变换；投掷物实体无 display 时
  沿用旧占位姿态。
- **LR 消耗品**：新增 `ConsumableItemRenderer`、`ConsumableDisplayInstance`、
  `ConsumableDisplayManager`、`ConsumableAnimationStateContext`，消耗品物品在内容包
  提供 display JSON 时走 Bedrock/Lua 第一人称渲染（服务端效果与 R1 一致）；
  `HasCustomDisplayProperty`、`LrClientAssetsManager`、`LrTacticalAPI` 同步扩展。

## 1.1.8+neoforge.26.1.2.R1 — 2026-08-22

首个发布版。三条战线在同一版本收口，全部经用户实机验收
（LAN 双轮 + 专用服务器 L2/L3 + 枪包专项 + LR 单机/专服专项，
records/SERVER_TEST_*、records/LR2_INVENTORY.md 全程台账）。

### 更名

- **项目更名：TaCZ: Renovated**（原"TaCZ NeoForge 26.1.2（非官方移植）"）。
  只改显示名，**modId 仍为 `tacz`**，枪包兼容不受影响。
  决策记录：`docs/records/NAMING_DECISION.md`。

### 新增：LRTactical 内置层（WP-LR2）

- throwable/melee/detonator/consumable 四类基础物品、五类投掷行为
  （explode/sticky/smoke/stun/effect-cloud）、数据装载与网络同步
  （独立载荷通道 `lr1`）、反馈层（三类 tooltip / 使用进度 HUD / 分类冷却遮罩）。
- 依赖 `lrtactical` 的内容包**完整可用**。
- 范围界定：flash_shield 未含（独立子系统 + ARR 美术，与姊妹项目同边界）；
  原作美术零打包，道具模型/贴图由内容包提供；无内容包时显示原版占位模型。
- 实现要点：LR init 包全量 DeferredRegister 重写（根治 WP07 A 类注册时序坑，
  当年 r30 的未定位启动崩溃未复现——E-13 闭案）；本仓补齐 NeoForge 路径必需的
  `items/*.json` 与 `particles/smoke_cloud.json`（本仓实现所需，非上游缺陷）。

### 修复（联机战役，Beta-1 → R1）

- **专服致命**：四个物品类（枪/弹药/配件/工作台）的 `getName` 覆写调用 client 索引，
  `/give` 等服务端路径触发即 `NoClassDefFoundError` 崩服（26.1 起 NeoForge 不再按
  `@OnlyIn` 剥离成员，上游祖传写法失效）。改走 common 索引。
- **联机致命**：`ServerMessageGunDraw` 空 ItemStack 编码崩溃——加入/空手切枪把视野内
  所有玩家踢下线。改 `ItemStack.OPTIONAL_STREAM_CODEC`（上游 1.21.1 与 refab 同款）。
- **联机功能**：RECIPE_FILTER 与 ATTACHMENT_TAGS 漏出网络同步包——联机客户端方块索引
  全部解析失败（工作台不可用）、配件允装判断静默失效。接回 `registerNetwork`。
- **Iris**：Iris 1.11.3 已自动分类 entity 管线时不再误报 WARN，保留 Iris 分类。
- **构建**：mods.toml 模板注释中的字面量 dollar-brace 炸毁 `generateModMetadata`。

### 文档

- 文档体系对齐姊妹项目规范：README 重写、`AGENTS.md`、一致性自检脚本、
  专用服务器测试预案（`docs/DEDICATED_SERVER_TEST.md`，L0-L4 + L2.5 枪包专项）。
- 联机枪包指引：双端安装职责、服务端 `/tacz reload`、客户端新增包按 F3+T 重载。

### 已知事项

- Fabric 姊妹项目的 getName 模式待其作者顺手核查（NeoForge 侧已实证，Fabric 未实测）。
- 面板服/代理网络/混合服未测试（L4 矩阵在案，非阻塞项）。

## 1.1.8+neoforge.26.1.2.Beta-1 — 2026-08-21

首个稳定基线。此前的 r0–r30 为开发迭代，历史见 git log，各阶段验收证据见 `docs/records/`。

### 包含

- 完整枪械玩法：物品/方块/配方注册、网络同步、弹道、枪包装载
  （默认枪包：枪械 54、弹药 24、配件 99、方块 4、配方 182）
- 渲染：depth-aperture 瞄具、第一人称 Feature Rendering、工作台 BER
- 可选 Iris 光影兼容（无 Iris 时不加载任何相关代码）
- 可选 mod 兼容：Cloth Config、Player Animation Library、Controllable、
  Shoulder Surfing Reloaded、JEI / REI、Carry On、FirstPerson Model
  （矩阵见 `docs/COMPATIBILITY.md`）

### 相对 r30 的变更

- **移除**：LRTactical 内置框架（r26 立项，三轮修复后仍有未定位的启动崩溃，撤回；
  决策与踩坑记录见 `docs/records/WP07_LRTACTICAL_PLAN.md`）。
  枪包依赖检查对 `lrtactical` 的软放行保留。

### 已知问题

- 依赖 `lrtactical` 的枪包：枪械可用，LR 道具（近战/投掷/引爆器/消耗品）不可用。
- 启动日志中原版 `RecipeManager` 对工作台配方有 `empty ingredients` 警告——无害，
  实际合成走 mod 内部管线。

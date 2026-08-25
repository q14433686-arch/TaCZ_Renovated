# 更新日志

版本号格式：`1.1.8+neoforge.<mc>.<标签>`。`+` 后是 SemVer build metadata，不参与
`>=1.1.8` 排序；禁止改用 `-neoforge...` pre-release。

## Unreleased — 1.1.8+neoforge.26.2.R1

### 目标环境

- Minecraft 26.2
- NeoForge 26.2.0.64 release
- Java 25
- Gradle 9.2.1 / ModDevGradle 2.0.144

### 品牌

- 接入 26.1.2 定稿的原创品牌标：`icon.png`（512² 方形头像）与 `logo.png`
  （1280×360 Mods 横幅），青色四段瞄具环 + 铜色 R。`neoforge.mods.toml` 写入
  `logoFile="logo.png"` 与 `logoBlur=false`；用 `scripts/generate_branding.py`
  重新生成，逐字节与 26.1.2 一致。不使用官方 TaCZ 图标（CC BY-NC-ND 4.0
  禁止再创作），决策快照见 `docs/records/BRANDING.md`。

### 修复

- 修复 26.2 首次生产编译暴露的 FOV event、HUD tick、AvatarRenderer descriptor 与三个
  transformed member AT 问题。
- 回流 26.1.2 R1 多人修复：`ServerMessageGunDraw` 的可空栈改 optional stream codec，
  防止首次/空手切枪的 tracking 广播踢出玩家。
- 将 `AttachmentsTagManager` 与 `RecipeFilterManager` 接入 network-cache listener，恢复
  RECIPE_FILTER / ATTACHMENT_TAGS 的联机同步。
- 四个双端 `Item#getName(ItemStack)` 改用 common index，避免 dedicated `/give` 路径加载
  client 类并崩服。
- Iris 已自动分类 pipeline 时，将 `Shader already assigned` 视为成功而不是兼容失败。
- 修复 `neoforge.mods.toml` 注释中的未知 dollar-brace 被 Groovy template engine 求值的问题。
- 低倍 sight 的 reticle containment 与 full-viewmodel clipping 拆分：低倍使用
  reticle-only mask，高倍使用完整镜身/枪身/配件/火光 mask。
- 安装 Punchy! 时右手脱离枪身、枪+手臂整体摆幅过大：按姊妹项目语义接入可选 mixin，
  持枪期间让 Punchy 的独立手臂与位移矩阵让出给 TACZ 第一人称状态机。

### 新增：LRTactical 内置层

- 从 26.1.2 R1 稳定尖端前滚 throwable / melee / detonator / consumable 四类基础物品、
  explode / sticky / smoke / stun / effect-cloud 五类投掷行为、LR 数据/配方/Lua 装载与
  `lr1` payload 同步。
- 接入三类 tooltip、使用进度 HUD、id-keyed 分类冷却遮罩、耳鸣压音量、实体渲染与
  26.2 item-model 分流；无内容包时使用 vanilla 占位资源。
- 26.2 专项修正：受保护 `SimpleParticleType` 构造、即时药效方法拼写、Java 25 mixin、
  `GuiGraphicsExtractor` / `SoundEngine` 注入点复核与 `Player#canCriticalAttack` AT。
- 不含 flash_shield，不打包 LRTactical 原作 ARR 美术。26.1.2 源基线已有单机/专服 PASS，
  但 26.2 LR 实机矩阵仍待执行。

### 变更

- 从包含多人修复与 LRTactical 的完整 NeoForge 26.1.2 R1 稳定基线前滚到 26.2；不是重写。
- 将已 removal 的 `IItemHandler` 路径迁到事务式 `ResourceHandler<ItemResource>`。
- 当前 screen 访问迁到 `Minecraft.gui`；HUD、文本颜色、PiP、shape outline、hand API 与
  Feature Rendering 对齐 26.2。
- 用 refab 26.2 已验证的阶段边界离屏 ocular mask 取代临时 OpenGL raw-depth 方案：
  convex-hull fill、sight/scope 分组、`ocular_ring` 普通重画、反向准星裁切、视模与火光裁切。
- 普通 mask 只使用 `TextureTarget` / `RenderPass` backend 抽象，可进入 OpenGL/Vulkan。
- Iris 改用 HAND pipeline 分类、linked-fragment dormant branch 与逐 draw uniform/texture binding。
- 版本 metadata 定名为 `1.1.8+neoforge.26.2.R1`。

### 可选兼容

- 重钉 Cloth Config、PAL、Controllable、Shoulder Surfing、JEI、REI、Architectury 的 26.2
  artifact。
- Carry On 对齐 2.11 的 `ItemStackTemplate#create()` 渲染路径。
- First-person Model / Not Enough Animations 只有反射 handoff 预留；核验日没有 NeoForge
  26.2 发布文件，不作为可安装兼容宣传。
- Punchy!：持有带模型的 TACZ/LR viewmodel 时走其官方 blacklist 让出路径，取消独立手臂
  与 walk/sprint/camera-lag 叠层；普通物品仍由 Punchy 控制。未实机。
- ImmediatelyFast hook 为明确 no-op；Accelerated Rendering 强制关闭；Aperture 未硬依赖接入。

### 移除

- 删除无引用的 `GunPackProgressScreen`。
- 删除旧 `IItemHandler` family 使用。
- 删除 raw-depth scope 状态机、GL encoder mixin、旧 shader 与 private `RenderType` 构造器 AT。

### 验证

- 用户对 **LR 合入前** 的 26.2 核心候选报告 JDK 25 build 与专服/多人 L0-L3 PASS；
  冻结记录：`docs/records/SERVER_TEST_20260821_262_R1.md`。
- 26.1.2 R1 的 LR 层已有用户单机与专服 PASS；该结果只作为源基线，不外推到 26.2。
- NeoForge Vulkan 需在 `config/fml.toml` 设置 `earlyWindowControl=false` 绕过仍开放的
  NeoForge#3230 ELS 问题；关闭后用户报告 Vulkan 启动 PASS。
- 当前 LR-integrated R1 只完成源码/API 静态前滚，尚无 JDK 25 生产构建或实机 PASS。

### 发布前仍需完成

- 当前 LR-integrated R1 jar 的 clean build、L0 与 Mod List/`Done` 复核；
- LR 单机与生产专服专项（同步、实体 tracking、冷却、近战、烟雾/闪光）；
- L2.5 第三方枪包及 LR 内容包明确确认；
- OpenGL / Iris / Vulkan 完整 GPU scope-mask 矩阵；
- 可选 Mod 逐项用户结果；
- metadata、license、source tag 与 source archive 最终一致性检查。

本条目必须保持 **Unreleased**，直到检查清单关闭并收到项目发起人的明确发布命令。

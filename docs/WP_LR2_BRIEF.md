# WP-LR2 工单：LRTactical 内置层重启（26.1.2 线）

> **✅ 已完成（2026-08-22）**：LR2-0..8 全部关闭，单机 + 专用服务器验收 PASS，
> 并入 **R1** 发布（发起人决定：未发布过的版本线不递增标签，R1 = 首发全量）。执行台账：`records/LR2_INVENTORY.md`。本工单转为历史文档。
>
> 2026-08-21 立案。前置评估：`records/LR_RESTART_ASSESSMENT.md`（结论：做）。
> **硬约束**：本会话只有这一条分支、PR #6 只合一次——LR 全部工作在
> `arena/01a023bf-...` 分支上完成后随 PR 一并入主线。
> **回退点**：R1 = 提交 `b9de5e0`。若按第 7 节判据放弃，`git revert` 区间即可，
> R1 本体不受污染。

## 1. 目标与范围（沿用 WP07 决策，一字未变的部分不重议）

- **形态 B**：LR 框架内置进 tacz 主 mod（refab 同款），不另立 mod；
- 范围：throwable / melee / detonator / consumable 四类基础物品，
  explode / sticky / smoke / stun / effect-cloud 五类投掷行为，
  LR 数据装载（index/data/recipe/recipe_filter/scripts），
  反馈层三件套（三类 tooltip、使用进度 HUD、分类冷却遮罩）；
- **排除**：flash_shield（独立子系统 + ARR 美术，上游 refab 亦排除）；
- **红线**：ARR 美术零打包；内容包自带获准分发的资产。

## 2. 侦察定数（2026-08-21 拉取 refab `26.1.2` 分支文件树清点）

- LR 代码 **104 个 java**（与 WP07 记载吻合）：client 26、item 24、api 15、
  init 8、entity 7、util 6、resource 4、network 4、inventory 3、mixin 2、
  capability 2、event 1、effect 1、EquipmentMod 1；
- LR 资源约 **39 个文件，全部 json/lua**（lang 2、scripts 5、display 2、
  data 2、index 14、recipe 13、recipe_filters 1）——ARR 零打包红线已由上游满足；
- mixin 两个（`client/GuiGraphicsExtractorMixin` 冷却遮罩、`client/SoundEngineMixin`）
  + 独立 `lrtactical.mixins.json`——与 WP07 D-10 结论一致。

## 3. 权威与边界

| 来源 | 用途 |
|---|---|
| refab `26.1.2` 分支 `me.xjqsh.lrtactical.*` | **语义唯一权威**（行为、数据结构、反馈层逻辑） |
| 本仓库现行 `init/` / `compat/` / 网络层 | 加载器习语权威（DeferredRegister、事件面、payload 写法照自己抄） |
| `records/WP07_LRTACTICAL_PLAN.md` 第三节 | 13 条踩坑地图 + Fabric→NeoForge 映射表（C 表已验证，直接执行） |
| 原作 LesRaisins-Tactical-Equipements | 仅语义仲裁，不抄任何 API 表面 |

## 4. 阶段切分（每阶段一个提交组，独立可回退）

| 阶段 | 内容 | 验收 |
|---|---|---|
| **LR2-0 清点冻结** | 拉全 refab 26.1.2 的 104 文件与资源清单，逐文件标注：直接平移 / 需映射改写（对照 C 表）/ 需重写（init 包）。产出 records 清单 | 清单入 records，无"未分类"文件 |
| **LR2-1 init 重写** | LR 全部注册改 **DeferredRegister**（物品/实体类型/音效/菜单等按清点结果），静态字段一律 supplier 化；mod 构造期零 vanilla 注册表写入（根除 A-1/A-2） | 编译过；`grep` 证明 LR 无构造期 `new Item` |
| **LR2-2 逻辑层** | item/entity/api/util/resource/network 迁移。执行 C 映射表；网络消息逐条过"EMPTY 栈"纪律（D-11 协变、B-8 readMap 显式 lambda 照办） | 编译过；消息 codec 清单入 records |
| **LR2-3 客户端/反馈层** | 实体渲染器（`EntityRenderersEvent`）、tooltip（`RegisterClientTooltipComponentFactoriesEvent`）、HUD（`RegisterGuiLayersEvent`）、冷却遮罩 mixin（独立 `lrtactical.mixins.json`）、物品模型（B-5 走事件注册）。渲染器注册放 `FMLClientSetupEvent.enqueueWork` 之后（r29 教训） | 编译过；mixin json 三件套齐 |
| **LR2-4 接线与资源** | GunMod/GunModClient 接线、AT 补 `Player#canCriticalAttack`（B-6）、mods.toml `[[mixins]]`、资源迁入 | L0 静态自检（jar 含 lrtactical 资源与 mixin json） |
| **LR2-5 E-13 猎杀** | 沙盒 L1 `compileJava`+可行冒烟 → 用户 `runClient` 启动 → **拿崩溃日志定位**（本阶段唯一目标；r30 差的就是这份日志） | 客户端进主菜单 + 进存档不崩 |
| **LR2-6 单机功能** | refab 反馈层实测清单 1-5 + 用户的 LR 枪包实用（手雷可扔、有效果） | 用户 PASS |
| **LR2-7 专服 LR 专项** | L2 部署 + 投掷物 tracking 同步、冷却消息、近战事件、LR 包全流程（上游从未做过的验证，完成后回哺 refab） | log 归档 + 用户 PASS |
| **LR2-8 收版 R2** | `mod_version` → `R2`、CHANGELOG、README 项目范围段改写（"未内置"→范围界定）、COMPATIBILITY lrtactical 行、PR #6 body 追加 | `--strict` 过、全链接核验过 |

## 5. 版本策略（两次一致性同步，不多不少）

1. ~~LR2-1 动工时~~ **LR2-4 摘除 sourceSets 排除项时**：`mod_version` →
   `1.1.8+neoforge.26.1.2.LR-dev`（一次 README/CHANGELOG 同步）。
   修订理由（LR2-0 执行时定）：适配期间 LR 源码被 build.gradle 排除，
   构建行为 = R1，提前改串反而制造"同串不同物"；LR 真正进入编译的那一刻
   才需要可辨识的版本串。
2. LR2-8 验收后：→ `R2`（第二次同步）。中途**不再**递增标签。

## 6. 高危点执行卡（13 条踩坑 + R1 战役新增纪律）

- **A 类（注册冻结）**：LR2-1 根除；`EntityType.Builder.build` 可任意时机（A-3）；
  自建静态 map 不受限（A-4）。
- **B 类**：ID_MAPPER 私有 → 事件注册（B-5）；AT 补行（B-6）；
  `AddServerReloadListenersEvent` 双参（B-7）；readMap 显式 lambda（B-8）；
  `SimpleParticleType` 26.1.2 可直接 new（B-9，26.2 前滚时再改）。
- **D 类**：独立 mixin 配置（D-10）；StreamCodec 协变（D-11）；
  重载方法引用显式 lambda（D-12）。
- **R1 战役新增（WP07 当年不知道的）**：
  1. **LR 的 24 个 item 类必查 getName/tooltip 类覆写**——若 refab 原文用
     client 索引，**不照抄**，直接按我们 `3b19477` 的 common 模式写
     （refab 自己都还没修这个）；
  2. LR 网络消息 ItemStack 字段逐个问"会不会 EMPTY"；
  3. LR 若有数据管理器需网络同步，接 `registerNetwork` 不是裸 `register.accept`
     （R1 同步缺口的教训）；
  4. `@OnlyIn`/`@Environment` 一律视为装饰，按无注解审查。
- **C 映射表**（WP07 第三节）逐行执行，特别是 `IEntityWithComplexSpawn`（投掷物
  spawn 数据）与 `AttackEntityEvent` 适配、挥臂拦截 mixin 需同步移植
  （C 表末行明示主 mod 缺该 mixin）。

## 7. 放弃判据（先说好，免得沉没成本绑架）

LR2-5 若 **3 轮日志往返**仍无法定位启动崩溃，或定位后发现需要重构主 mod
核心（超出 LR 范围）：停止、`git revert` LR 区间、把新增诊断信息追记进
WP07 文档、版本串回 R1。R1 的发布不被 LR 绑架。

## 8. 协调

- **26.2 前滚（01a023e5）**：基线**钉死在 R1 = `b9de5e0`**，不要跟分支尖端
  （HANDOFF_262_SYNC 已同步更新）。LR 稳定成 R2 后另行通知他平移。
- PR #6：LR 完成后追加 body 段落再由发起人合并。

## 9. 节奏预估

LR2-0…4 约 2 个 AGENT 会话（纯沙盒工作）；LR2-5…7 取决于你的 3-5 轮实测；
LR2-8 半小时。第一份要你出手的东西是 **LR2-5 的启动日志**。

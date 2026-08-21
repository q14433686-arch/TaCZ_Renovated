# LR2-0 清点冻结 + LR2-1/2 进度记录（2026-08-21/22）

> WP-LR2 工单的执行清单。来源：refab `26.1.2` 分支克隆（本日）。
> **安全阀**：`build.gradle` 已对 `me/xjqsh/**` 设 sourceSets 排除——
> 适配期间本分支构建行为 = R1；LR2-4 接线时摘除。

## 体量定数

- 代码 104 java（copy 完成）；资源 39 个全 json/lua（copy 完成，含
  `lrtactical.mixins.json`）。
- 分类：**85 个 VERBATIM**（零 Fabric/垫片 import，原样平移）+ 19 个 ADAPT。

## ADAPT 处置台账

### ✅ 已完成（本记录时点）

| 文件 | 处置 |
|---|---|
| `init/ModItems` | DeferredRegister.Items 重写（A-1 根治）；消费方需 `.get()`，跟改进行中 |
| `init/ModEffects` | DeferredRegister；DeferredHolder 即 Holder，消费方零改动 |
| `init/ModParticleTypes` | DeferredRegister；`new SimpleParticleType(true)` 直构（B-9） |
| `init/ModEntities` | DeferredRegister.Entities + supplier 包 TYPE（A-3 习语） |
| `init/ModCreativeTabs` | DeferredRegister<CreativeModeTab> 重写；`.get()` 已跟改 |
| `init/ModCapabilities` | PlayerTickEvent.START→NeoForge Pre；AFTER_RESPAWN→PlayerRespawnEvent（无旧玩家引用，remove(new) 按网络 id 等价命中，适配注释在文件内） |
| `EquipmentMod` | 重写为 `register(IEventBus)` 接线：5 个 DR + 载荷事件 + 近战 AttackEntityEvent 适配 + 三索引 reload 监听 + OnDatapackSyncEvent 同步 |
| `network/LrNetworkHandler` | PayloadRegistrar（registrar 版本 `lr1`）；发送复用 tacz NetworkHandler |
| `network/ServerMessageSyncLrPack` | readMap/writeMap 显式 lambda（B-8）；S2C 经 LrClientBridge（dedicated 常量池零客户端类型） |
| `network/ServerMessageCustomCooldown` | 同上桥接 |
| `network/ClientMessagePrepareMeleeAttack` | IPayloadContext + enqueueWork，服务端直处理 |
| （新增）`network/LrClientBridge` | tacz ClientPacketBridge 同模式反射桥 |
| （新增）`client/network/LrClientPacketHandlers` | S2C 客户端落地（memory connection 检查 + 索引应用 + 冷却表） |

### ✅ 第二批完成（2026-08-22）——原 13 文件待适配清单全部关闭

| 文件 | 需要的改动 |
|---|---|
| `entity/ThrowableItemEntity` | IEntityAdditionalSpawnData 垫片 → 原生 `IEntityWithComplexSpawn`（C 表） |
| `client/init/ModEntitiesRender` | EntityRendererRegistry → `EntityRenderersEvent.RegisterRenderers`；ModEntities 消费点 `.get()` |
| `client/event/LrTickAnimationEvent` | RenderTickEvent 垫片 → `RenderFrameEvent`（C 表）；重载方法引用显式 lambda（D-12） |
| `client/input/MeleeAttackKeys` | InputEvent 垫片 → NeoForge `InputEvent`；键位注册 → `RegisterKeyMappingsEvent` |
| `client/overlay/UsingProgressOverlay` | HudElementRegistry → `RegisterGuiLayersEvent` |
| `client/overlay/BlindnessOverlay` | 同上 |
| `client/particle/SmokeCloudParticle` | 粒子提供器注册 → `RegisterParticleProvidersEvent`；SMOKE_CLOUD 消费点 `.get()` |
| `client/resource/LrClientAssetsManager` | IdentifiableResourceReloadListener → `AddClientReloadListenersEvent`（PAL 同款，C 表） |
| `client/renderer/entity/ThrowableEntityRenderer` | @Environment 清除 + 消费点核查 |
| `client/renderer/item/LrDynamicItemModel` | 物品模型注册 → `RegisterItemModelsEvent`（B-5） |
| `client/audio/DeafenState` / `StunRingingSound` | @Environment 清除；SoundEngine 交互核查 |
| `api/animation/BaseAnimationStateContext` | @Environment 清除 |

### ✅ 接线完成（LR2-4，2026-08-22）

GunMod 构造器调 `EquipmentMod.register(modEventBus)`；GunModClient 客户端注册链；
mods.toml `[[mixins]]` 加 `lrtactical.mixins.json`；AT 补 `Player#canCriticalAttack`
（B-6）；`ModItems.THROWABLE/MELEE/CONSUMABLE/DETONATOR` 全消费点 `.get()` 扫尾
（约 10 处）；摘除 build.gradle 排除项。

## 纪律执行状态

- LR item 类 getName 审查：**待 LR2-2 逐文件过**（ThrowableItem/MeleeItem/
  ConsumableItem 含 getName 相关代码，必须按 `3b19477` common 模式核）；
- 网络消息 EMPTY 审查：三条消息均无 ItemStack 字段 ✅；
- `@Environment` 视为装饰：已在 ModCapabilities 落例，其余待清；
- 版本策略修订：LR-dev bump 从"LR2-1 动工时"**推迟到 LR2-4 摘除排除项时**——
  排除期构建行为 = R1，提前改串反而制造"同串不同物"。工单 §5 以本条为准。

## 第二批执行摘要（2026-08-22）

- 垫片替身：`LrItemRendererRegistry`（原 BuiltinItemRendererRegistry，零加载器依赖直迁）、
  `IMoveDistTracker`（迁入 LR 包；**暂无 mixin 实现方**，BaseAnimationStateContext 走
  无插值回退——LR2-6 观感确认后再决定是否补 mixin）、`ILrItemExtension`
  （getCustomRenderer 惰性安全型 + tacz$onEntitySwing 默认不触发，挥臂抑制暂缺=化妆级）。
- `@Environment` 全树清除；ThrowableItemEntity → 原生 IEntityWithComplexSpawn
  （去 getAddEntityPacket 覆写，buf → RegistryFriendlyByteBuf）。
- MeleeAttackKeys → 原生 InputEvent.MouseButton.Post + NetworkHandler.sendToServer；
  LrTickAnimationEvent → RenderFrameEvent（tacz TickAnimationEvent 同习语）；
  LrClientAssetsManager → AddClientReloadListenersEvent 双参注册（PAL 同款）；
  LrDynamicItemModel/HasCustomDisplayProperty → B-5 事件注册；
  ModEntitiesRender 重写为事件形态；新增 LrClientEvents 订阅类（tick 双通道 +
  渲染帧 + 鼠标输入 + 六个注册事件）。
- `.get()` 扫尾清零（4 实体 getDefaultItem、DETONATOR 三处、SMOKE_CLOUD addParticle）。
- **getName 纪律审查结论**：LR 三个 item 的 getName 走 LrTacticalAPI → LR
  CommonAssetsManager（common 路径 + 网络同步），**非** tacz 旧 client 索引病灶，
  免改；LrTacticalAPI 的 display 查询方法引用 client 类，属惰性安全型
  （服务端只执行 common 方法），备案。
- 接线四处：GunMod ctor（EquipmentMod.register）、GunModClient enqueueWork
  （registerItemRenderers，r29 时序）、mods.toml `[[mixins]]`、AT 补
  canCriticalAttack（B-6）。安全阀已摘除，`mod_version` → **LR-dev**。
- 遗留观察项：ThrowableItemEntity 顶部未用 import（FriendlyByteBuf/Packet 等，
  无害）；LR mixin 两个（GuiGraphicsExtractor/SoundEngine）为 VERBATIM 迁入，
  目标为 vanilla 类，LR2-5 启动时首验。

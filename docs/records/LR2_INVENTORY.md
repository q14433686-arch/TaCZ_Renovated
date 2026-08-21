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

### ⏳ 待适配（下一轮，13 文件）

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

### 待接线（LR2-4）

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

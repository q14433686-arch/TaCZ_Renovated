# 工作包⑦：LRTactical 内置 —— 已立项、已实施、**已撤回**

> 状态：**撤回**（Beta-1 起）。本文件保留决策记录、撤回原因与全部踩坑点，
> 供将来（上游先行或人力充足时）重启此工作包使用。
>
> 时间线：2026-08-21 立项（r26）→ 三轮修复（r26→r30）→ 用户决定撤回（Beta-1）。

## 一、决策记录（保留，将来重启时仍然有效）

- **形态 B**：LR 兼容框架内置进 tacz 主 mod（refab 26.1.2 同款），不另立独立 mod。
  理由：refab 的 LR 代码（`me.xjqsh.lrtactical.*`，104 文件）深度引用 tacz 内部类，
  独立成 mod 需先造插件 SPI；NeoForge 无 `provides` 字段，独立 mod 无额外机制收益。
- **许可红线**：LR 代码公开（GPL-3.0，经 refab 谱系可审计）允许移入；
  **美术 ARR 绝不打包**（refab 内置的 31 个资源全为 json/lua，零美术，已核）；
  flash_shield 不移植、不注册空壳。
- 原作：`LesRaisins-Studios/LesRaisins-Tactical-Equipements`（Programmer xjqsh，Artist LeComte）。

## 二、撤回原因

r26 起 LR 框架引发**三轮构建/启动失败**（详见下节踩坑表），r30 修复全部已知问题后
用户复测**仍然崩溃**，且该次崩溃的日志未上传、崩溃点未诊断。项目优先稳定性，
决定撤回全部 LR 代码，留待上游先行或完成崩溃定位后再重启。

撤回范围（Beta-1）：`me/xjqsh` 104 文件、`assets|data/lrtactical` 31 资源、
`lrtactical.mixins.json`、GunMod/GunModClient/ClientSetupEvent 三处接线、
accesstransformer.cfg 的 `Player#canCriticalAttack` 行、mods.toml 的 `[[mixins]]` 声明。
**保留**：`GunPackLoader` 对 `lrtactical` 的软 provides（枪包依赖检查放行，独立无害）。

## 三、踩坑点全记录（26.1.2 NeoForge 移植 LR 的已知雷区）

> 每一条都来自 r26-r30 的真实构建/运行日志。重启时**先读本节**。

### A. 注册表冻结（最致命，启动即崩）

1. **mod 构造期注册表已冻结**（26.1 与 1.20.1 老 Forge 相反）。`Item.<init>` 内部
   立即 `createIntrusiveHolder` 写注册表 —— 在 `@Mod` 构造器里 `new Item(...)` 直接抛
   `IllegalStateException: Registry is already frozen`（r28 崩溃实证）。
   **一切 vanilla 注册表写入（含物品构造）必须在 `RegisterEvent` 窗口或 DeferredRegister 内。**
2. 注册窗口填充的静态字段在 **mod 构造期读到的是 null**：GunModClient 构造器里调
   `registerItemRenderers()`（内部 `ModItems.MELEE instanceof IItem`）会静默跳过、
   渲染器永远不注册。此类调用必须挪到 `FMLClientSetupEvent.enqueueWork` 之后（r29 修复）。
3. `EntityType.Builder.build(ResourceKey)` **不写注册表**，可在任意时机构建——
   实体类保留静态 `TYPE` 字段是安全的，只要把它包进 supplier（`() -> GrenadeEntity.TYPE`
   延迟类加载到 RegisterEvent，主 mod `ModEntities` 习语）。
4. `ModCustomTypes` 式**自建静态 map**（非 vanilla 注册表）不受冻结限制，构造期安全。

### B. 26.1.2 私有化 / 签名坑

5. `ItemModels.ID_MAPPER` 与 `ConditionalItemModelProperties.ID_MAPPER` 均已 **private**
   （Fabric 侧 refab 能直接 put，NeoForge 侧不行）→ 必须走
   `RegisterItemModelsEvent#register(Identifier, MapCodec)` 与
   `RegisterConditionalItemModelPropertyEvent#register(Identifier, MapCodec)`。
6. `Player#canCriticalAttack(Entity)` **包级私有**（vanilla 26.1）→ NeoForge 侧需
   accesstransformer.cfg：`public net.minecraft.world.entity.player.Player canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z`
   （refab 用 Fabric accesswidener 开的同一条）。
7. `AddServerReloadListenersEvent#addListener` 是**双参** `(Identifier, PreparableReloadListener)`
   （SortedReloadListenerEvent 系），不是单参。
8. `FriendlyByteBuf.readMap/writeMap` 在 NeoForge 下**方法引用必歧义**
   （`IFriendlyByteBufExtension` 扩展重载 + `readUtf()` 无参重载使方法引用同时匹配
   StreamDecoder 与 BiFunction）→ 必须显式 lambda：
   `buf.readMap(b -> b.readIdentifier(), b -> b.readUtf())` /
   `buf.writeMap(m, (b, k) -> b.writeIdentifier(k), (b, v) -> b.writeUtf(v))`。
9. `SimpleParticleType` 构造器 26.1.2 可直接 new（r28 编译实证）；26.2 为 protected
   （refab 注释，届时需换工厂/匿名子类）。

### C. Fabric→NeoForge 机制映射（已验证的正确对应）

| Fabric（refab） | NeoForge 26.1.2 |
|---|---|
| `provides: ["lrtactical"]` | 无等价物；GunPackLoader 软特判（枪包依赖检查放行） |
| `@Environment(EnvType.CLIENT)` | 删除（靠注册路径隔离；dedicated server 不触碰 client 类） |
| `IEntityAdditionalSpawnData`（自建+spawn packet 覆写） | 原生 `IEntityWithComplexSpawn`（无需覆写 getAddEntityPacket，buf 类型 RegistryFriendlyByteBuf） |
| `PayloadTypeRegistry` + 双端 receiver 三段式 | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` 一体注册 |
| `AttackEntityCallback`（返回 FAIL） | `AttackEntityEvent` + `setCanceled(true)` 适配器 |
| `PlayerTickEvent.START`（自建 mixin 事件） | `net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre` |
| `ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS` | `OnDatapackSyncEvent`（`getRelevantPlayers()`） |
| `ServerPlayerEvents.AFTER_RESPAWN` | `PlayerEvent.PlayerRespawnEvent`（**无旧玩家引用**，传 null） |
| `EntityRendererRegistry` / `ParticleProviderRegistry` | `EntityRenderersEvent.RegisterRenderers` / `RegisterParticleProvidersEvent#registerSpriteSet` |
| `HudElementRegistry.addLast` | `RegisterGuiLayersEvent#registerAboveAll` |
| `RenderTickEvent`（simplebedrockmodel） | `RenderFrameEvent`（partialTick 用 `getGameTimeDeltaPartialTick(false)`；无 `renderTickTime` 字段） |
| `IdentifiableResourceReloadListener#getFabricDependencies`（reload 顺序） | 无等价物；靠 `AddClientReloadListenersEvent` 注册顺序承载（弱保证） |
| accesswidener | accesstransformer.cfg |
| `cn.sh1rocu...IItem.tacz$onEntitySwing`（其 LivingEntityMixin 驱动） | **注意：我们主 mod 没有该 mixin，接口保留 default 也不会被调用**——重启时需同步移植 refab 的 `LivingEntityMixin` 挥臂拦截或放弃该行为 |

### D. 其他

10. LR 的 mixin（GuiGraphicsExtractor/SoundEngine）需**独立 mixin 配置文件**
    （包前缀不同），并在 neoforge.mods.toml 加 `[[mixins]]` ——已验证的形态。
11. 网络消息的 `StreamCodec<FriendlyByteBuf,T>` 可直接用于
    `PayloadRegistrar`（协变合法），无需改成 RegistryFriendlyByteBuf。
12. 重载方法引用挂事件监听（`LrTickAnimationEvent::tickAnimation` 双重载）需
    显式类型 lambda，否则 `addListener` 泛型推断失败。

### E. 未诊断

13. **r30（修复 A/B 全部已知问题后）用户复测仍崩溃**，日志未上传。
    重启前必须先拿到该日志（或重新构建复现）定位——候选嫌疑：
    RegisterEvent 窗口内字段填充的其它时序消费者、LR mixin 与 26.1.2 NeoForge
    环境的兼容性、或 A 节尚未覆盖的第 4 个注册路径。

## 四、重启前置条件

1. 拿到 r30 级别代码的崩溃日志并定位（E-13）；
2. 优先考虑**改用 DeferredRegister 完整重写 init 包**（而非静态字段+窗口填充的
   折中方案），从根上消除时序类隐患；
3. 重读第三节映射表；软 provides 无需重建（一直在）。

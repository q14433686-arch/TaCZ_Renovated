package me.xjqsh.lrtactical;

import me.xjqsh.lrtactical.event.MeleeAttackHandler;
import me.xjqsh.lrtactical.init.ModCapabilities;
import me.xjqsh.lrtactical.init.ModCreativeTabs;
import me.xjqsh.lrtactical.init.ModCustomTypes;
import me.xjqsh.lrtactical.init.ModEffects;
import me.xjqsh.lrtactical.init.ModEntities;
import me.xjqsh.lrtactical.init.ModItems;
import me.xjqsh.lrtactical.init.ModParticleTypes;
import me.xjqsh.lrtactical.network.LrNetworkHandler;
import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LRTactical（LesRaisins Tactical Equipements）内置层 —— NeoForge 26.2。
 *
 * <h2>移植来源与授权</h2>
 * <ul>
 *   <li>原作：{@code LesRaisins-Studios/LesRaisins-Tactical-Equipements}
 *       —— Programmer {@code xjqsh}，Artist {@code LeComte}，代码 GPL-3.0；</li>
 *   <li>NeoForge 稳定基线：TaCZ: Renovated 26.1.2 R1 的 WP-LR2 实现；</li>
 *   <li>26.2 游戏语义：refab（TaCZ_Refabricated_Unofficial）{@code 26.2(main)}；
 *       Fabric API 表面不进入本实现。</li>
 * </ul>
 *
 * <h2>【重要】本移植不包含原作美术资源</h2>
 * 原作声明 {@code Art Assets: All Rights Reserved}——只移植代码（GPL-3.0 允许），
 * 不打包贴图/模型/音效。内容完全数据驱动：代码注册四个基础物品，具体道具来自
 * 内容包的 {@code data/<ns>/index/*}。flash_shield 未移植（与 refab 同边界）。
 *
 * <h2>接线方式（与 refab 的 Fabric 显式 init 链不同）</h2>
 * {@code GunMod} 构造器调用 {@link #register(IEventBus)}：DeferredRegister 挂
 * mod 总线（注册发生在 RegisterEvent 窗口——WP07 坑 A-1 的根治），
 * 事件监听挂 game 总线。不再需要"触发静态初始化"的空 init() 链。
 */
public final class EquipmentMod {
    public static final String MOD_ID = "lrtactical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private EquipmentMod() {
    }

    /** 由 {@code GunMod} 构造器调用（WP-LR2 唯一接线点，common 侧）。 */
    public static void register(IEventBus modEventBus) {
        // --- 注册表（mod 总线，RegisterEvent 窗口执行） ---
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticleTypes.PARTICLE_TYPES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        // --- 网络载荷（RegisterPayloadHandlersEvent，mod 总线） ---
        modEventBus.addListener(LrNetworkHandler::register);

        // --- 自建静态 map 类型表（WP07 坑 A-4：不受注册表冻结限制，构造期安全） ---
        ModCustomTypes.init();

        // --- 冷却/连招 tick 驱动与重生清表（game 总线，见 ModCapabilities 注释） ---
        ModCapabilities.init();

        // --- 近战攻击入口：AttackEntityCallback(FAIL) → AttackEntityEvent.setCanceled ---
        //     （WP07 C 映射表已验证的对应；服务端权威判定，客户端仅拦截原版攻击）
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent event) -> {
            InteractionResult result = MeleeAttackHandler.onAttackEntity(
                    event.getEntity(), event.getEntity().level(), InteractionHand.MAIN_HAND,
                    event.getTarget(), null);
            if (result == InteractionResult.FAIL) {
                event.setCanceled(true);
            }
        });

        // --- 服务端数据重载：LR 三个索引管理器（与 tacz CommonAssetsManager 同事件同总线） ---
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> {
            event.addListener(id("throwable_index"),
                    CommonAssetsManager.get().getThrowableIndexManager());
            event.addListener(id("melee_index"),
                    CommonAssetsManager.get().getMeleeIndexManager());
            event.addListener(id("consumable_index"),
                    CommonAssetsManager.get().getConsumableIndexManager());
        });

        // --- 登入/重载时同步索引给客户端（与 tacz onDatapackSync 同钩子；
        //     R1 教训：LR 索引只在服务端加载，不同步则联机客户端"找不到手雷、名字裸键"） ---
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) ->
                event.getRelevantPlayers().forEach(LrNetworkHandler::syncToPlayer));

        LOGGER.info("LRTactical built-in layer (NeoForge 26.2 R2 candidate) registered");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

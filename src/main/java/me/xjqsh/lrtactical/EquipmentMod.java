package me.xjqsh.lrtactical;

import me.xjqsh.lrtactical.init.ModCapabilities;
import me.xjqsh.lrtactical.init.ModCreativeTabs;
import me.xjqsh.lrtactical.init.ModCustomTypes;
import me.xjqsh.lrtactical.init.ModEntities;
import me.xjqsh.lrtactical.init.ModItems;
import me.xjqsh.lrtactical.network.LrNetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LRTactical（LesRaisins Tactical Equipements）的非官方 26.1.2 NeoForge 移植。
 *
 * <h2>移植来源与授权</h2>
 * <ul>
 *   <li>原作：{@code LesRaisins-Studios/LesRaisins-Tactical-Equipements}
 *       —— Programmer {@code xjqsh}，Artist {@code LeComte}，代码 GPL-3.0；</li>
 *   <li>本包移植自 TaCZ_Refabricated_Unofficial 26.1.2 的内置版本（语义权威）。</li>
 * </ul>
 *
 * <h2>【重要】本移植<b>不包含</b>原作的美术资源</h2>
 * 原作 readme 明确声明 {@code Art Assets: All Rights Reserved}，
 * 因此本移植<b>只移植代码（GPL-3.0 允许）</b>，不打包、不分发原作的贴图 / 模型 / 音效。
 * 内容完全由数据驱动：代码注册 throwable / melee / consumable / detonator 四个基础物品，
 * 具体内容来自数据包中的 {@code data/<ns>/index/*}。flash_shield 未移植。
 *
 * <h2>与本仓库主体（TACZ）的关系</h2>
 * 本包是<b>附属模块代码</b>，与 {@code com.tacz.guns} 并列，依赖 TACZ 的 API。
 * 由 TACZ 主入口 {@code GunMod} 构造期调用 {@link #init(IEventBus)}。
 */
public final class EquipmentMod {
    public static final String MOD_ID = "lrtactical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private EquipmentMod() {
    }

    /**
     * 附属模块的统一初始化入口（NeoForge 版）。
     *
     * <p>必须在 mod 构造期调用（注册窗口未关闭）：物品 / 实体 / 粒子 / 效果 /
     * 创造标签用 vanilla {@code Registry.register}（26.1 NeoForge 构造期合法）；
     * 网络 / 实体渲染器走 mod bus 事件；游戏事件挂 game bus。
     *
     * <p>{@link ModItems} 必须在 {@link ModCreativeTabs} 之前 ——
     * 标签页的 {@code icon} 与 {@code displayItems} 会引用物品实例。
     */
    public static void init(IEventBus modEventBus) {
        // 26.1：所有 vanilla 注册表的写入都必须在 RegisterEvent 窗口内执行
        //（mod 构造期注册表已冻结，new Item()/Registry.register 会直接抛
        // "Registry is already frozen"）。各 init 类挂监听、窗口内填充静态字段。
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        // 粒子类型注册表两端都要有（服务端 addParticle 也要能查到该类型）
        me.xjqsh.lrtactical.init.ModParticleTypes.register(modEventBus);
        // 状态效果注册表两端都要有（服务端施加效果、客户端查询效果）
        me.xjqsh.lrtactical.init.ModEffects.register(modEventBus);
        // ModCustomTypes 是自建静态 map（非 vanilla 注册表），构造期类加载安全
        ModCustomTypes.init();
        ModCreativeTabs.register(modEventBus);

        // 近战攻击入口：把左键攻击接到 IMeleeWeapon#performAttack（服务端权威，客户端拦截原版攻击）。
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent event) ->
                me.xjqsh.lrtactical.event.MeleeAttackHandler.onAttackEntityNeoForge(event));

        // 冷却计时器的每 tick 驱动（缺它则冷却永不结束）。
        ModCapabilities.init();

        // 数据包重载时加载 index/throwable|melee|consumable/*.json —— 与 TACZ 自身
        // 的 CommonAssetsManager 走同一个服务端 reload 事件，时机一致。
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> {
            // SortedReloadListenerEvent#addListener 是双参 (Identifier, listener)——
            // 与主 mod CommonAssetsManager#reloadAndRegister 的 keyOf 习语一致。
            var manager = me.xjqsh.lrtactical.resource.CommonAssetsManager.get();
            event.addListener(manager.getThrowableIndexManager().ID, manager.getThrowableIndexManager());
            event.addListener(manager.getMeleeIndexManager().ID, manager.getMeleeIndexManager());
            event.addListener(manager.getConsumableIndexManager().ID, manager.getConsumableIndexManager());
        });

        // 网络层：载荷 + 收发器一并注册（两端都会执行）。
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            PayloadRegistrar registrar = event.registrar(LrNetworkHandler.VERSION);
            LrNetworkHandler.register(registrar);
        });

        // 玩家登入 / 数据包重载时把索引同步给客户端（联机时客机索引为空会表现为
        // 「创造栏找不到手雷、名字显示成『投掷物』」）——TACZ 侧 OnDatapackSync 同一个钩子。
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) ->
                event.getRelevantPlayers().forEach(LrNetworkHandler::syncToPlayer));

        LOGGER.info("LRTactical (unofficial NeoForge 26.1.2 port) initialized");
    }
}

package me.xjqsh.lrtactical.network;

import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * LRTactical 的网络层（NeoForge 版）。
 *
 * <p>结构对齐本仓库 {@code com.tacz.guns.network.NetworkHandler} 的习语：
 * 载荷与收发器在 mod bus 的 {@code RegisterPayloadHandlersEvent} 里经
 * {@link PayloadRegistrar} 一并注册（playToServer / playToClient），
 * 不再有 Fabric 三段式的“类型注册 / 接收器注册”分离。
 */
public final class LrNetworkHandler {
    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath(
            me.xjqsh.lrtactical.EquipmentMod.MOD_ID, "main");
    public static final String VERSION = "1.0.0";

    private LrNetworkHandler() {
    }

    /**
     * 载荷注册 —— 由 GunMod 构造期挂到 mod bus 的 RegisterPayloadHandlersEvent。
     * 两端的编解码与收发器都在这里注册，专用服务器同样会执行。
     */
    public static void register(PayloadRegistrar registrar) {
        // S2C：索引同步 + 自定义冷却
        registrar.playToClient(ServerMessageSyncLrPack.TYPE, ServerMessageSyncLrPack.CODEC,
                ServerMessageSyncLrPack::handle);
        registrar.playToClient(ServerMessageCustomCooldown.TYPE, ServerMessageCustomCooldown.CODEC,
                ServerMessageCustomCooldown::handle);
        // C2S：近战攻击请求
        registrar.playToServer(ClientMessagePrepareMeleeAttack.TYPE, ClientMessagePrepareMeleeAttack.CODEC,
                ClientMessagePrepareMeleeAttack::handle);
    }

    /**
     * 把当前索引发给某个玩家。
     *
     * <p>由 NeoForge 的 {@code OnDatapackSyncEvent} 触发（玩家登入与数据包重载都会发），
     * 与 TACZ 侧 {@code CommonAssetsManager#onDatapackSync} 挂同一个钩子。
     */
    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ServerMessageSyncLrPack(
                CommonAssetsManager.get().getThrowableIndexManager().getNetworkCache(),
                CommonAssetsManager.get().getMeleeIndexManager().getNetworkCache(),
                CommonAssetsManager.get().getConsumableIndexManager().getNetworkCache()));
    }

    public static void syncCooldown(ServerPlayer player, Identifier id, int duration) {
        PacketDistributor.sendToPlayer(player, new ServerMessageCustomCooldown(id, Math.max(0, duration)));
    }

    /** 客户端上行快捷方式（近战攻击请求）。 */
    public static void sendToServer(ClientMessagePrepareMeleeAttack payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}

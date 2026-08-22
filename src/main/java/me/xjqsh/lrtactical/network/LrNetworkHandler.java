package me.xjqsh.lrtactical.network;

import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * LRTactical 网络层（NeoForge 26.1.2，WP-LR2）。
 *
 * <h2>与 refab（Fabric 三段式）的对应</h2>
 * Fabric 的 registerPayloads / registerC2SPackets / registerS2CPackets 三段
 * 在 NeoForge 合并为一次 {@link RegisterPayloadHandlersEvent} 注册——
 * {@code playToClient/playToServer} 同时声明编解码与处理器，双端一致，
 * 不存在"只注册一端"的 Unknown payload id 风险（WP07 C 映射表）。
 *
 * <p>S2C 处理经 {@link LrClientBridge} 反射进 client 包——dedicated 常量池
 * 不得出现客户端类（WP03 教训，与 tacz 主 mod ClientPacketBridge 同模式）。
 *
 * <p>发送复用 {@code com.tacz.guns.network.NetworkHandler#sendToClientPlayer}。
 */
public final class LrNetworkHandler {
    /** LR 载荷独立版本串；与 tacz 主 registrar 互不影响。 */
    private static final String VERSION = "lr1";

    private LrNetworkHandler() {
    }

    /** mod 总线：{@code EquipmentMod#register} 挂接。 */
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(ServerMessageSyncLrPack.TYPE,
                ServerMessageSyncLrPack.CODEC, ServerMessageSyncLrPack::handle);
        registrar.playToClient(ServerMessageCustomCooldown.TYPE,
                ServerMessageCustomCooldown.CODEC, ServerMessageCustomCooldown::handle);
        registrar.playToServer(ClientMessagePrepareMeleeAttack.TYPE,
                ClientMessagePrepareMeleeAttack.CODEC, ClientMessagePrepareMeleeAttack::handle);
    }

    /**
     * 把当前索引发给某个玩家。由 {@code OnDatapackSyncEvent} 触发
     * （登入与数据包重载，与 tacz 侧同钩子，时机已被 R1 联机实测验证）。
     */
    public static void syncToPlayer(ServerPlayer player) {
        com.tacz.guns.network.NetworkHandler.sendToClientPlayer(new ServerMessageSyncLrPack(
                CommonAssetsManager.get().getThrowableIndexManager().getNetworkCache(),
                CommonAssetsManager.get().getMeleeIndexManager().getNetworkCache(),
                CommonAssetsManager.get().getConsumableIndexManager().getNetworkCache()), player);
    }

    public static void syncCooldown(ServerPlayer player, Identifier id, int duration) {
        com.tacz.guns.network.NetworkHandler.sendToClientPlayer(
                new ServerMessageCustomCooldown(id, Math.max(0, duration)), player);
    }
}

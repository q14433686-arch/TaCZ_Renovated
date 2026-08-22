package me.xjqsh.lrtactical.network;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * 把服务端加载的 LRTactical index 原始 JSON 同步给客户端（WP-LR2 NeoForge 适配）。
 *
 * <p>读写用显式 lambda 而非方法引用——WP07 坑 B-8：NeoForge 的
 * {@code IFriendlyByteBufExtension} 扩展重载使 {@code FriendlyByteBuf::readUtf}
 * 类方法引用产生歧义，必须写成 lambda。
 *
 * <p>S2C 落地经 {@link LrClientBridge}；本类常量池零客户端类型（dedicated 安全）。
 */
public class ServerMessageSyncLrPack implements CustomPacketPayload {
    public static final Identifier PACKET_ID =
            Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, "s2c_sync_lr_pack");
    public static final CustomPacketPayload.Type<ServerMessageSyncLrPack> TYPE =
            new CustomPacketPayload.Type<>(PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncLrPack> CODEC =
            StreamCodec.ofMember(ServerMessageSyncLrPack::write, ServerMessageSyncLrPack::new);

    private final Map<Identifier, String> throwableIndex;
    private final Map<Identifier, String> meleeIndex;
    private final Map<Identifier, String> consumableIndex;

    public ServerMessageSyncLrPack(Map<Identifier, String> throwableIndex,
                                   Map<Identifier, String> meleeIndex,
                                   Map<Identifier, String> consumableIndex) {
        this.throwableIndex = throwableIndex;
        this.meleeIndex = meleeIndex;
        this.consumableIndex = consumableIndex;
    }

    public ServerMessageSyncLrPack(FriendlyByteBuf buf) {
        this(buf.readMap(b -> b.readIdentifier(), b -> b.readUtf()),
                buf.readMap(b -> b.readIdentifier(), b -> b.readUtf()),
                buf.readMap(b -> b.readIdentifier(), b -> b.readUtf()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeMap(this.throwableIndex, (b, k) -> b.writeIdentifier(k), (b, v) -> b.writeUtf(v));
        buf.writeMap(this.meleeIndex, (b, k) -> b.writeIdentifier(k), (b, v) -> b.writeUtf(v));
        buf.writeMap(this.consumableIndex, (b, k) -> b.writeIdentifier(k), (b, v) -> b.writeUtf(v));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 应用到本地索引管理器——全 common 类型，供 client 处理器回调。 */
    public void applyToLocalIndexes() {
        CommonAssetsManager.get().getThrowableIndexManager().fromNetwork(this.throwableIndex);
        CommonAssetsManager.get().getMeleeIndexManager().fromNetwork(this.meleeIndex);
        CommonAssetsManager.get().getConsumableIndexManager().fromNetwork(this.consumableIndex);
    }

    public static void handle(ServerMessageSyncLrPack message, IPayloadContext context) {
        context.enqueueWork(() -> LrClientBridge.invoke("onSyncLrPack",
                new Class[]{ServerMessageSyncLrPack.class}, message));
    }
}

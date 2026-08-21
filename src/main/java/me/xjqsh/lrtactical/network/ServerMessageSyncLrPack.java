package me.xjqsh.lrtactical.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;

/** 把服务端加载的 LRTactical index 原始 JSON 同步给客户端。 */
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

    public static void handle(ServerMessageSyncLrPack message, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = (LocalPlayer) context.player();
            boolean remoteConnection = player.connection.getConnection() != null
                    && !player.connection.getConnection().isMemoryConnection();
            if (!remoteConnection) {
                return;
            }
            CommonAssetsManager.get().getThrowableIndexManager().fromNetwork(message.throwableIndex);
            CommonAssetsManager.get().getMeleeIndexManager().fromNetwork(message.meleeIndex);
            CommonAssetsManager.get().getConsumableIndexManager().fromNetwork(message.consumableIndex);
        });
    }
}

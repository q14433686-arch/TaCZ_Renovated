package me.xjqsh.lrtactical.network;

import me.xjqsh.lrtactical.EquipmentMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 按 id 键控的分类冷却同步（只发所属玩家）。WP-LR2 NeoForge 适配：
 * S2C 落地经 {@link LrClientBridge}，本类常量池零客户端类型。
 */
public record ServerMessageCustomCooldown(Identifier id, int duration) implements CustomPacketPayload {
    public static final Identifier PACKET_ID = Identifier.fromNamespaceAndPath(
            EquipmentMod.MOD_ID, "s2c_custom_cooldown");
    public static final CustomPacketPayload.Type<ServerMessageCustomCooldown> TYPE =
            new CustomPacketPayload.Type<>(PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, ServerMessageCustomCooldown> CODEC =
            StreamCodec.ofMember(ServerMessageCustomCooldown::write, ServerMessageCustomCooldown::new);

    public ServerMessageCustomCooldown(FriendlyByteBuf buffer) {
        this(buffer.readIdentifier(), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeIdentifier(id);
        buffer.writeVarInt(Math.max(0, duration));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerMessageCustomCooldown message, IPayloadContext context) {
        context.enqueueWork(() -> LrClientBridge.invoke("onCustomCooldown",
                new Class[]{ServerMessageCustomCooldown.class}, message));
    }
}

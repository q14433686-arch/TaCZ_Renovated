package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessageCraft implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageCraft> TYPE = new CustomPacketPayload.Type<>(
        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "client_craft")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageCraft> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, message -> message.recipeId,
        ByteBufCodecs.INT, message -> message.menuId,
        ClientMessageCraft::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final Identifier recipeId;
    public final int menuId;

    public ClientMessageCraft(Identifier recipeId, int menuId) {
        this.recipeId = recipeId;
        this.menuId = menuId;
    }

    public static void handle(ClientMessageCraft message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer entity = (ServerPlayer) context.player();
            if (entity.containerMenu.containerId == message.menuId) {
                GunMod.LOGGER.debug("WP③ C2S craft menu={} recipe={}", message.menuId, message.recipeId);
            }
        });
    }
}

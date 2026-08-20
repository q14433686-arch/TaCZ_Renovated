package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessagePlayerShoot implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerShoot> TYPE = new CustomPacketPayload.Type<>(
        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "client_player_shoot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessagePlayerShoot> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, message -> message.timestamp,
        ByteBufCodecs.FLOAT, message -> message.chargeProgress,
        ClientMessagePlayerShoot::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 这里的 timestamp 应该是基于 base timestamp 的相对值
     */
    private final long timestamp;
    private final float chargeProgress;

    public ClientMessagePlayerShoot(long timestamp) {
        this(timestamp, 0f);
    }

    public ClientMessagePlayerShoot(long timestamp, float chargeProgress) {
        this.timestamp = timestamp;
        this.chargeProgress = chargeProgress;
    }

    public static void handle(ClientMessagePlayerShoot message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer entity = (ServerPlayer) context.player();
            IGunOperator.fromLivingEntity(entity).shoot(entity::getXRot, entity::getYRot, message.timestamp, message.chargeProgress);
            ItemStack gun = entity.getMainHandItem();
            NetworkHandler.sendToTrackingEntityAndSelf(entity, new ServerMessageGunShoot(entity.getId(), gun));
            NetworkHandler.sendToTrackingEntityAndSelf(entity, new ServerMessageGunFire(entity.getId(), gun));
            GunMod.LOGGER.debug("WP③ C2S shoot entity={} ts={}", entity.getId(), message.timestamp);
        });
    }
}
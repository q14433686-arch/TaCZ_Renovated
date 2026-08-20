package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ClientMessageLaserColor implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageLaserColor> TYPE = new CustomPacketPayload.Type<>(
        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "client_laser_color")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageLaserColor> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.idMapper(AttachmentType::fromId, AttachmentType::ordinal), ByteBufCodecs.INT), message -> message.colorMap,
        ByteBufCodecs.BOOL, message -> message.applyGunColor,
        ByteBufCodecs.INT, message -> message.gunColor,
        ByteBufCodecs.INT, message -> message.gunSlotIndex,
        (colorMap, applyGunColor, gunColor, gunSlotIndex) -> {
            ClientMessageLaserColor message = new ClientMessageLaserColor();
            message.colorMap.putAll(colorMap);
            message.applyGunColor = applyGunColor;
            message.gunColor = gunColor;
            message.gunSlotIndex = gunSlotIndex;
            return message;
        }
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final Map<AttachmentType, Integer> colorMap = new HashMap<>();
    private boolean applyGunColor = false;
    private int gunColor = 0;

    private int gunSlotIndex = -1;

    private ClientMessageLaserColor() {
    }

    public ClientMessageLaserColor(@NotNull ItemStack gun, int gunSlotIndex) {
        this.gunSlotIndex = gunSlotIndex;
    }

    public static void handle(ClientMessageLaserColor message, IPayloadContext context) {
        context.enqueueWork(() -> GunMod.LOGGER.debug("WP③ C2S laser color slot={}", message.gunSlotIndex));
    }

}

package com.tacz.guns.network.message.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.common.GunDrawEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import com.tacz.guns.api.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ServerMessageGunDraw implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageGunDraw> TYPE = new CustomPacketPayload.Type<>(
        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "server_gun_draw")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerMessageGunDraw> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, message -> message.entityId,
        // 必须用 OPTIONAL_STREAM_CODEC：previous/current 天然可能为 ItemStack.EMPTY
        // （首次切枪无"上一把"、空手切换、丢弃手持物）。ItemStack.STREAM_CODEC 遇 EMPTY
        // 直接抛 EncoderException("Empty ItemStack not allowed")，且本消息 sendToTracking
        // 广播，一次空栈会把视野内所有玩家踢下线（2026-08-21 LAN 实测：双端断连，
        // docs/records/SERVER_TEST_20260821_LAN.md）。
        // 证据：③ refab 26.1.2 ServerMessageGunDraw#write（javadoc 详载同一回归，
        // 并逐字核对上游 1.21.1 用 OPTIONAL）；同目录 Fire/FireSelect/Melee/Reload/Shoot
        // 上游即非 OPTIONAL（必携真实枪械），不得一并改动。
        ItemStack.OPTIONAL_STREAM_CODEC, message -> message.previousGunItem,
        ItemStack.OPTIONAL_STREAM_CODEC, message -> message.currentGunItem,
        ServerMessageGunDraw::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final int entityId;
    public final ItemStack previousGunItem;
    public final ItemStack currentGunItem;

    public ServerMessageGunDraw(int entityId, ItemStack previousGunItem, ItemStack currentGunItem) {
        this.entityId = entityId;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
    }

    public static void handle(ServerMessageGunDraw message, IPayloadContext context) {
        context.enqueueWork(() -> com.tacz.guns.network.ClientPacketBridge.invoke("onGunDraw", new Class[]{com.tacz.guns.network.message.event.ServerMessageGunDraw.class}, message));
    }

}

package com.tacz.guns.client.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Single ADS mouse scale so vanilla {@code MouseHandler#turnPlayer} and Punchy's
 * own {@code LocalPlayer#turn} path cannot double-apply or skip the MDV curve.
 */
public final class AdsMouseSensitivity {
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    private AdsMouseSensitivity() {
    }

    public static void turn(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        if (Boolean.TRUE.equals(APPLYING.get())) {
            original.call(player, yaw, pitch);
            return;
        }
        APPLYING.set(true);
        try {
            ItemStack kept = KeepingItemRenderer.getRenderer().getCurrentItem();
            ItemStack mainHandItem = kept != null && !kept.isEmpty() ? kept : player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                original.call(player, yaw, pitch);
                return;
            }
            Identifier scopeId = iGun.getAttachmentId(mainHandItem, AttachmentType.SCOPE);
            if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
                scopeId = iGun.getBuiltInAttachmentId(mainHandItem, AttachmentType.SCOPE);
            }
            float zoomLevel = 1;
            if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
                zoomLevel = TimelessAPI.getGunDisplay(mainHandItem).map(GunDisplayInstance::getIronZoom).orElse(1f);
            } else {
                Optional<ClientAttachmentIndex> optional = TimelessAPI.getClientAttachmentIndex(scopeId);
                if (optional.isPresent()) {
                    float[] zoom = optional.get().getZoom();
                    if (zoom != null && zoom.length > 0) {
                        CompoundTag attachmentTag = iGun.getAttachmentTag(mainHandItem, AttachmentType.SCOPE);
                        zoomLevel = zoom[AttachmentItemDataAccessor.getZoomNumberFromTag(attachmentTag) % zoom.length];
                    }
                }
            }
            Minecraft minecraft = Minecraft.getInstance();
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            float progress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTick);
            if (progress <= 0.0f) {
                progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
            }
            double sensitivityMultiplier = ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER.get();
            sensitivityMultiplier = 1 + (sensitivityMultiplier - 1) * progress;
            double originalFov = minecraft.options.fov().get();
            double currentFov = MathUtil.magnificationToFov(1 + (zoomLevel - 1) * progress, originalFov);
            double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
            double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
            original.call(player, yaw * denominator, crawlPitch(player, pitch, denominator));
        } finally {
            APPLYING.set(false);
        }
    }

    private static double crawlPitch(LocalPlayer player, double pitch, double denominator) {
        double finalPitch = pitch * denominator;
        if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
            float playerPitch = -player.getXRot();
            if (playerPitch > 45) {
                finalPitch = Math.max(finalPitch, 0);
            }
            if (playerPitch < -30) {
                finalPitch = Math.min(finalPitch, 0);
            }
        }
        return finalPitch;
    }
}

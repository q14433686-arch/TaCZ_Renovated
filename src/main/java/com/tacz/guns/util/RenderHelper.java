package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class RenderHelper {
    private RenderHelper() {
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light) {
        if (player == null || collector == null) {
            return;
        }
        AvatarRenderer<?> avatar = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        var texture = player.getSkin().body().texturePath();
        FirstPersonAnimationCompat.beginDirectArmRender();
        try {
            if (arm == HumanoidArm.RIGHT) {
                avatar.renderRightHand(poseStack, collector, light, texture,
                        player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
            } else {
                avatar.renderLeftHand(poseStack, collector, light, texture,
                        player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            }
        } finally {
            FirstPersonAnimationCompat.endDirectArmRender();
        }
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack, int light) {
        // Legacy VertexConsumer path; Feature Rendering uses the collector overload.
    }
}

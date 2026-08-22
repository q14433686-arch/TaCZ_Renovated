package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.compat.shader.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.11 bob hooks. RenderFrameEvent is already fired by NeoForge ClientHooks
 *  (21.11 fires ViewportEvent.ComputeFov natively; Camera angles too — see Camera.java.patch). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Unique
    private boolean tacz$renderingItemInHand;

    // 1.21.11 signature (sister javap table): (float, boolean, Matrix4f) — no CameraRenderState.
    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void tacz$beginHandPass(float partialTick, boolean firstPerson, Matrix4f projection, CallbackInfo ci) {
        this.tacz$renderingItemInHand = true;
    }

    @Inject(method = "renderItemInHand", at = @At("RETURN"))
    private void tacz$endHandPass(float partialTick, boolean firstPerson, Matrix4f projection, CallbackInfo ci) {
        this.tacz$renderingItemInHand = false;
    }

    @Unique
    private boolean tacz$isItemInHandBobPass() {
        return this.tacz$renderingItemInHand || ShaderCompat.isHandRendererActive();
    }

    // 1.21.11 signature (sister javap table): (PoseStack, float) — partialTick is a direct
    // parameter here (no DeltaTracker lookup needed anymore).
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tacz$bobHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (minecraft.getCameraEntity() instanceof LocalPlayer player && !player.isDeadOrDying()) {
            if (GunHurtBobTweak.onHurtBobTweak(player, poseStack, partialTick)) {
                ci.cancel();
                return;
            }
        }

        if (this.tacz$isItemInHandBobPass()) {
            RenderItemInHandBobEvent.BobHurt event = new RenderItemInHandBobEvent.BobHurt();
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
            }
        } else {
            RenderLevelBobEvent.BobHurt event = new RenderLevelBobEvent.BobHurt();
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void tacz$bobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (this.tacz$isItemInHandBobPass()) {
            RenderItemInHandBobEvent.BobView event = new RenderItemInHandBobEvent.BobView();
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
            }
        } else {
            RenderLevelBobEvent.BobView event = new RenderLevelBobEvent.BobView();
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
            }
        }
    }
}

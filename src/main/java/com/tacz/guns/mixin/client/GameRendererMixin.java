package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.client.render.scope.ScopeMaskRenderer;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import com.tacz.guns.client.render.scope.ScopePipTrace;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.compat.iris.IrisCompat;
import com.tacz.guns.compat.shader.ShaderCompat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.2 bob hooks. RenderFrameEvent is already fired by NeoForge ClientHooks. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    /**
     * 二次渲染那一遍要用的三样东西，全部取自 GameRenderer 自己的字段。
     *
     * <p>不用 @Inject(locals = ...) 捕获 renderLevel 的局部变量：局部变量表随编译器与版本漂移，
     * 捕获式注入极脆。而这三个字段正是 vanilla 传给 LevelRenderer#render 的那几个实参的来源。</p>
     */
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private GameRenderState gameRenderState;

    @Unique
    private boolean tacz$renderingItemInHand;

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void tacz$beginHandPass(CameraRenderState cameraState,
                                    float partialTick,
                                    Matrix4fc projection,
                                    CallbackInfo ci) {
        this.tacz$renderingItemInHand = true;
        // renderAllFeatures 每帧被调用多次（世界一次、手持一次），
        // 瞄具只存在于手持那次。掩码必须只在那次绘制，否则世界那次会先把
        // target 清空，把手持那次的结果冲掉。
        ScopeMaskRenderer.setInHandPass(true);
    }

    @Inject(method = "renderItemInHand", at = @At("RETURN"))
    private void tacz$endHandPass(CameraRenderState cameraState,
                                  float partialTick,
                                  Matrix4fc projection,
                                  CallbackInfo ci) {
        this.tacz$renderingItemInHand = false;
        ScopeMaskRenderer.setInHandPass(false);
    }

    @Unique
    private boolean tacz$isItemInHandBobPass() {
        return this.tacz$renderingItemInHand || ShaderCompat.isHandRendererActive();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tacz$bobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.getCameraEntity() instanceof LocalPlayer player && !player.isDeadOrDying()) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
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
    private void tacz$bobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
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

    /**
     * 【镜内画中画 · 抓取本帧世界画面】世界画完、视模开画之前，把主画面拷一份走。
     */
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void tacz$captureSceneForScopePip(DeltaTracker deltaTracker, CallbackInfo ci) {
        ScopePipTrace.mark("VANILLA LevelRenderer#render END (anything after this draws over the finished world)");
        ScopePipRenderer.captureScene(this.minecraft);
        // 【光影路径】Iris 把手部渲染搬进了 LevelRenderer#render 内部，所以此刻整条
        // Iris 管线已经收工，主 target 里是最终画面 —— 直接在它上面做镜内放大。
        // 无光影时这一句立即返回（合成仍在阶段边界完成，那里才能让准星盖在 PIP 之上）。
        ScopePipRenderer.compositeAfterLevelUnderShaders();
    }

    /**
     * 【二次渲染模式】用窄 FOV 把世界再画一遍，插在 vanilla 主世界渲染<b>之前</b>。
     */
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void tacz$renderScopePipView(DeltaTracker deltaTracker, CallbackInfo ci) {
        ScopePipRenderer.renderScopeView(this.minecraft, this.resourcePool,
                this.fogRenderer, this.gameRenderState, deltaTracker);
        ScopePipTrace.mark("VANILLA LevelRenderer#render BEGIN (its clear pass wipes the main target)");
    }

    /**
     * 【二次渲染模式 · 输出重定向】那一遍期间，把主 target 换成离屏 target。
     */
    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void tacz$redirectMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget scopeTarget = ScopePipRenderer.redirectTarget();
        if (ScopePipTrace.enabled()) {
            ScopePipTrace.targetResolved(scopeTarget, scopeTarget != null);
        }
        if (scopeTarget != null) {
            cir.setReturnValue(scopeTarget);
        }
    }

    /**
     * 每帧唯一的「瞄具帧状态归零」点。
     */
    @Inject(method = "extract", at = @At("HEAD"))
    private void tacz$beginScopeFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        IrisCompat.beginFrame();
        ScopePipRenderer.beginFrame();
        ScopeMaskRenderer.beginFrame();
        ScopePipRenderer.prewarmShaderPipelineIfNeeded();
        ScopePipTrace.beginFrame();
    }
}

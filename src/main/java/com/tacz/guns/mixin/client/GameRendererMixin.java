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
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * 二次渲染那一遍要用的三样东西，全部取自 GameRenderer 自己的字段。
     *
     * <p>不用 {@code @Inject(locals = ...)} 捕获 {@code renderLevel} 的局部变量：
     * 局部变量表随编译器与版本漂移，捕获式注入极脆。而这三个字段正是 vanilla 传给
     * {@code LevelRenderer#render} 的那几个实参的来源。</p>
     */
    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;
    @Shadow
    @Final
    private FogRenderer fogRenderer;
    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Unique
    private boolean tacz$renderingItemInHand;

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void tacz$beginHandPass(CameraRenderState cameraState,
                                    float partialTick,
                                    Matrix4fc projection,
                                    CallbackInfo ci) {
        this.tacz$renderingItemInHand = true;
        ScopeMaskRenderer.setInHandPass(true);
        // 记下手持这一遍真正使用的投影。镜内画中画的合成要拿它把目镜的斜率空间
        // 包围盒换算成屏幕 NDC，从而给合成加一道硬件剪裁（见 ScopeMaskRenderer）。
        ScopeMaskRenderer.setHandProjection(projection);
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

    // ------------------------------------------------------------------
    // 镜内画中画（ScopePipRenderer）的三个时机
    // ------------------------------------------------------------------

    /**
     * 【重投影模式】世界画完、视模开画之前，把主画面拷一份。
     *
     * <p>再早世界还没画完；再晚拷贝里会混进枪和手，镜片里就会出现一把缩小的枪。
     *
     * <p>光影下这个注入点的含义不同：Iris 把手部渲染搬进了
     * {@code LevelRenderer#render} 内部，于是此刻整条 Iris 管线已经收工，
     * 主 target 里是最终画面 —— 合成直接在它上面做（下一句）。
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
        // 【光影路径】无光影时这一句立即返回（合成仍在阶段边界完成，
        // 那里才能让准星盖在 PIP 之上）。
        ScopePipRenderer.compositeAfterLevelUnderShaders();
    }

    /**
     * 【二次渲染模式】用窄 FOV 把世界再画一遍，插在 vanilla 主世界渲染<b>之前</b>。
     *
     * <p>与上面的拷贝注入点刻意<b>相反</b>（BEFORE 而不是 AFTER），因为两种模式的约束相反：
     * 拷贝要 AFTER（世界得先画完）；二次渲染要 BEFORE（让 vanilla 那一遍收尾，
     * 把我们可能污染到的共享状态覆盖回去）。
     *
     * <p>注入点选 {@code INVOKE + BEFORE} 而不是方法 HEAD：投影矩阵与雾缓冲都在那之后、
     * 这次调用之前才准备好，而我们两样都要用。</p>
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
        // 紧接着就是 vanilla 那一遍。有了这个界标，日志里「谁在什么阶段解析了哪个 target」
        // 就能一眼分段。
        ScopePipTrace.mark("VANILLA LevelRenderer#render BEGIN (its clear pass wipes the main target)");
    }

    /**
     * 【二次渲染模式 · 输出重定向】那一遍期间，把主 target 换成离屏 target。
     *
     * <p>刻意注入<b>方法</b>而不是用 {@code @Accessor} 改 {@code private final mainRenderTarget}
     * 字段：final 字段可能被 JIT 常量折叠，也容易被别处缓存住引用，
     * 而所有调用方取 target 走的都是这个 public 方法。</p>
     *
     * <p>{@link ScopePipRenderer#redirectTarget()} 只在那一次调用的 try/finally 窗口内
     * 返回非 null，其余任何时候都返回 null，所以对不用二次渲染的玩家就是一次 null 判断。</p>
     */
    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void tacz$redirectMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget scopeTarget = ScopePipRenderer.redirectTarget();
        // 【诊断】这里是所有渲染目标解析的必经之路 —— 打开 ScopePipDebugTrace
        // 就能看清「镜内那一遍期间谁还在解析真正的主 target」。
        if (ScopePipTrace.enabled()) {
            ScopePipTrace.targetResolved(scopeTarget, scopeTarget != null);
        }
        if (scopeTarget != null) {
            cir.setReturnValue(scopeTarget);
        }
    }

    /**
     * 每帧唯一的「瞄具帧状态归零」点。
     *
     * <p>接在 {@code extract} 的 HEAD 上，因为 {@code Minecraft#runTick} 的顺序是
     * <b>extract → render</b> —— 这是本帧最早、且一定会执行到的位置，于是本帧所有
     * 消费者（extract 里的 FOV 事件、renderLevel 里的镜内抓取、手部 pass 里的合成）
     * 看到的都是同一份定义明确的状态。</p>
     *
     * <p>绝不能放在手部 pass 里：Iris 的 {@code HandRenderer} 一帧调用两次
     * {@code renderAllFeatures}，第二次会把第一次的结果抹掉。</p>
     */
    @Inject(method = "extract", at = @At("HEAD"))
    private void tacz$beginScopeFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ScopePipRenderer.beginFrame();
        ScopeMaskRenderer.beginFrame();
        // 瞄具那套 Iris 管线在这里预热：extract 在世界渲染之前，不在任何 render pass 内，
        // 也不在镜内那一遍里 —— 是做「编译整份 shaderpack」这种重活的唯一安全位置。
        ScopePipRenderer.prewarmShaderPipelineIfNeeded();
        ScopePipTrace.beginFrame();
    }
}

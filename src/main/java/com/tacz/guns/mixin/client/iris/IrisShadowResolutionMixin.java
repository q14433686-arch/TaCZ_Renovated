package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.iris.IrisScopePipelineCompat;
import com.tacz.guns.config.client.RenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给镜内那一遍配一张更小的阴影贴图。
 *
 * <p>Iris 每遍世界渲染都要跑一次阴影通道，二次渲染模式下等于每帧画两次阴影图 ——
 * 常常是光影帧里最贵的一项。开销按<b>面积</b>走，所以 0.5 就把那一遍的阴影开销
 * 砍到约四分之一。只有镜片受影响，主画面仍是光影包自己的完整阴影图。
 *
 * <p>窗口极其关键：{@code PackShadowDirectives#getResolution()} 的返回值只在
 * <b>管线构造</b>期间被读取一次（此后由采样器一路捕获），而瞄具管线的构造只发生在
 * {@link IrisScopePipelineCompat#prewarmIfNeeded()} 打开的那一段里 —— 见
 * {@link IrisScopePipelineCompat#isBuildingScopePipeline()}。
 * 窗口外一律不干预，否则会把主画面的阴影一起改小。
 */
@Mixin(targets = "net.irisshaders.iris.shaderpack.properties.PackShadowDirectives", remap = false)
public abstract class IrisShadowResolutionMixin {

    @Unique
    private static boolean tacz$logged;

    @Inject(method = "getResolution", at = @At("RETURN"), cancellable = true, require = 0)
    private void tacz$smallerShadowMapForScopePass(CallbackInfoReturnable<Integer> cir) {
        if (!IrisScopePipelineCompat.isBuildingScopePipeline()) {
            return;
        }
        float scale = RenderConfig.SCOPE_PIP_SHADOW_SCALE == null
                ? 1.0f
                : RenderConfig.SCOPE_PIP_SHADOW_SCALE.get().floatValue();
        if (scale >= 1.0f) {
            return;
        }
        int original = cir.getReturnValue();
        // 阴影图按 2 的幂对齐更利于驱动，也避免奇怪的尺寸；至少留 256。
        int scaled = Math.max(256, Integer.highestOneBit(Math.max(1, Math.round(original * scale))));
        if (scaled >= original) {
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Scope pass gets a {}x{} shadow map instead of {}x{}. The "
                            + "shadow pass runs once per world render, so rendering twice per frame "
                            + "doubled it; cost scales with area, so this cuts that pass' shadow work to "
                            + "about {}%. Only the lens is affected.",
                    scaled, scaled, original, original,
                    Math.round(100.0 * scaled * scaled / ((double) original * original)));
        }
        cir.setReturnValue(scaled);
    }
}

package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import com.tacz.guns.compat.iris.IrisScopePipelineCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 镜内那一遍期间，把「当前维度」换成瞄具专用的那个。
 *
 * <p>Iris 按维度索引管线，于是换掉之后镜内那一遍会拿到一套<b>独立管线</b>
 * （独立的 colortex、独立的 previous 系列 uniform），时域状态与主画面彻底分开 ——
 * 否则一帧推进两次「上一帧」uniform，主画面的 TAA / 体积云 / SSGI 会全部失准。
 *
 * <p>只在 {@link ScopePipRenderer#isScopePassIsolated()} 为真时生效，
 * 且 id 的构造与预热共用 {@link IrisScopePipelineCompat} 的同一个实例 ——
 * 用两个不同的 id 会让 Iris 建出两套瞄具管线，白白多编译一次、多占一份显存。
 */
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisScopeDimensionMixin {

    @Unique
    private static boolean tacz$logged;
    @Unique
    private static boolean tacz$resolveFailed;

    @Inject(method = "getCurrentDimension", at = @At("HEAD"), cancellable = true, require = 0)
    private static void tacz$scopePassUsesItsOwnPipeline(CallbackInfoReturnable<Object> cir) {
        if (tacz$resolveFailed || !ScopePipRenderer.isScopePassIsolated()) {
            return;
        }
        Object id = IrisScopePipelineCompat.scopeDimensionId();
        if (id == null) {
            tacz$resolveFailed = true;
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Scope pass is using its own Iris pipeline "
                    + "({}:scope_pip) so its temporal state stays separate from the main view.",
                    GunMod.MOD_ID);
        }
        cir.setReturnValue(id);
    }
}

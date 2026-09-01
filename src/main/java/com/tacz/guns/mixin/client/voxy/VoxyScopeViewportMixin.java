package com.tacz.guns.mixin.client.voxy;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * 给镜内那一遍单独要一个 Voxy 视口 —— 与 Voxy 自己处理 Iris 阴影通道的机制相同。
 *
 * <h2>为什么必须分视口</h2>
 * 哪怕隔离模式下 Voxy 不绘制，它的 {@code setupViewport(...)} 依然会被调到，
 * 并且会<b>就地改写</b>视口的投影、屏幕尺寸与 frameId。
 * 用主画面那个视口去承接镜内那一遍的窄投影，就是把主画面的 LOD 状态写坏 ——
 * 那正是「第一次开镜后远景永久错乱」的成因，而且写坏之后不会自己复原。
 *
 * <p>所以无论绘不绘制，镜内那一遍都必须拿到<b>自己的</b>视口。
 */
@Mixin(targets = "me.cortex.voxy.client.core.rendering.ViewportSelector", remap = false)
public abstract class VoxyScopeViewportMixin {

    @Unique
    private static final Object TACZ_SCOPE_VIEWPORT_KEY = new Object();
    @Unique
    private static Method tacz$getOrCreate;
    @Unique
    private static boolean tacz$resolveFailed;
    @Unique
    private static boolean tacz$logged;

    @Inject(method = "getViewport", at = @At("HEAD"), cancellable = true, require = 0)
    private void tacz$scopePassGetsItsOwnViewport(CallbackInfoReturnable<Object> cir) {
        if (tacz$resolveFailed || !ScopePipRenderer.isScopePassActive()) {
            return;
        }
        try {
            Method getOrCreate = tacz$getOrCreate;
            if (getOrCreate == null) {
                getOrCreate = this.getClass().getDeclaredMethod("getOrCreate", Object.class);
                getOrCreate.setAccessible(true);
                tacz$getOrCreate = getOrCreate;
            }
            Object viewport = getOrCreate.invoke(this, TACZ_SCOPE_VIEWPORT_KEY);
            if (viewport == null) {
                return;
            }
            if (!tacz$logged) {
                tacz$logged = true;
                GunMod.LOGGER.info("[TACZ Scope] Voxy will render the scope pass into its own viewport, "
                        + "the same way it already handles the Iris shadow pass. Distant LOD terrain "
                        + "shows in the lens without disturbing the main view's LOD state.");
            }
            cir.setReturnValue(viewport);
        } catch (Throwable t) {
            tacz$resolveFailed = true;
            GunMod.LOGGER.warn("[TACZ Scope] Could not give the scope pass its own Voxy viewport; it "
                    + "will share the main one. Distant terrain may render incorrectly while aiming.", t);
        }
    }
}

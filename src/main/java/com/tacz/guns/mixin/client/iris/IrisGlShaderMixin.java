package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.iris.IrisScopeMaskPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Second Iris compile hook. HAND programs sometimes bypass {@code ShaderCreator#link}
 * and go through {@code GlShader} directly; the same fragment heuristic applies.
 */
@Mixin(targets = "net.irisshaders.iris.gl.shader.GlShader", remap = false)
public abstract class IrisGlShaderMixin {
    private static boolean tacz$loggedPatch;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, require = 0)
    private static String tacz$patchGlShaderSource(String source) {
        String patched = IrisScopeMaskPatch.patchFragmentSource(source);
        if (patched != source && !tacz$loggedPatch) {
            tacz$loggedPatch = true;
            GunMod.LOGGER.info("[TACZ Scope] Injecting dormant scope-mask branch into Iris GlShader sources.");
        }
        return patched;
    }
}

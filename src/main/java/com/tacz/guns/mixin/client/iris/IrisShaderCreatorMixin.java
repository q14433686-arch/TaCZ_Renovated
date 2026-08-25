package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.iris.IrisScopeMaskPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Patches every GLSL string argument of Iris {@code ShaderCreator#link}.
 *
 * <p>Do not pin a parameter index: Iris 1.11 builds have already moved the
 * fragment-source slot. {@link IrisScopeMaskPatch} ignores vertex sources.
 */
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.ShaderCreator", remap = false)
public abstract class IrisShaderCreatorMixin {
    private static boolean tacz$loggedPatch;

    @ModifyVariable(
            method = "link",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private static String tacz$patchLinkedSource(String source) {
        String patched = IrisScopeMaskPatch.patchFragmentSource(source);
        if (patched != source && !tacz$loggedPatch) {
            tacz$loggedPatch = true;
            GunMod.LOGGER.info("[TACZ Scope] Injecting dormant scope-mask branch into Iris ShaderCreator.link sources.");
        }
        return patched;
    }
}

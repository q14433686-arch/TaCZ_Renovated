package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.iris.IrisScopeMaskPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Catches pack-side program builds ({@code gbuffers_hand} etc.) that never pass
 * through {@code ShaderCreator#link}.
 */
@Mixin(targets = "net.irisshaders.iris.gl.program.ProgramBuilder", remap = false)
public abstract class IrisProgramBuilderMixin {
    private static boolean tacz$loggedPatch;

    @ModifyVariable(method = "begin", at = @At("HEAD"), argsOnly = true, require = 0)
    private static String tacz$patchProgramBuilderSource(String source) {
        String patched = IrisScopeMaskPatch.patchFragmentSource(source);
        if (patched != source && !tacz$loggedPatch) {
            tacz$loggedPatch = true;
            GunMod.LOGGER.info("[TACZ Scope] Injecting dormant scope-mask branch into Iris ProgramBuilder sources.");
        }
        return patched;
    }
}

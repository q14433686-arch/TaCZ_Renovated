package com.tacz.guns.compat.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tacz.guns.compat.iris.IrisCompat;
import net.minecraft.world.item.ItemStack;

/**
 * Work package ⑥ shader-pack facade.
 *
 * <p>26.2's OpenGL path talks to Iris only through this type / {@link IrisCompat}
 * (public API + optional dormant HAND-fragment mask branch). The ordinary 26.2 off-screen mask
 * itself is backend-neutral; shader replacements without a verified bridge use the unmasked
 * fallback rather than attempting loader-specific internals.</p>
 */
public final class ShaderCompat {
    private ShaderCompat() {
    }

    public static boolean isShaderPackInUse() {
        return IrisCompat.isUsingRenderPack();
    }

    public static boolean isHandRendererActive() {
        return IrisCompat.isHandRendererActive();
    }

    public static boolean isRenderingShadow() {
        return IrisCompat.isRenderShadow();
    }

    public static boolean assignPipeline(RenderPipeline pipeline, String program, String debugName) {
        return IrisCompat.assignPipelineToIris(pipeline, program, debugName);
    }

    public static void assignCommonEntityPipelinesToHandIfNeeded() {
        IrisCompat.assignCommonEntityPipelinesToHandIfNeeded();
    }

    public static boolean shouldRenderInCurrentHandPhase(ItemStack stack) {
        return IrisCompat.shouldRenderInCurrentHandPhase(stack);
    }
}

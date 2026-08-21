package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.IndexType;
import com.tacz.guns.client.render.scope.ScopeDepthCopyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Executes a pending OpenGL scope operation after the render pass/program/FBO setup and immediately
 * before the backend draw. 26.2 moved {@code IndexType} and added {@code firstInstance}.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderScopeDepthCopyMixin {
    @Inject(method = "drawFromBuffers", at = @At("HEAD"), cancellable = true, require = 1)
    private void tacz$copyScopeDepth(@Coerce Object glRenderPass,
                                     int baseVertex,
                                     int firstIndex,
                                     int indexCount,
                                     IndexType indexType,
                                     @Coerce Object glRenderPipeline,
                                     int instanceCount,
                                     int firstInstance,
                                     CallbackInfo ci) {
        if (!ScopeDepthCopyState.beforeDraw()) {
            ci.cancel();
        }
    }
}

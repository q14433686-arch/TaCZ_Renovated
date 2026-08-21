package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.client.render.scope.ScopeDepthCopyState;
import com.tacz.guns.client.render.scope.ScopeRenderTypes;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.2 replacement for the old {@code RenderType#draw -> GlCommandEncoder#drawFromBuffers}
 * scope boundary. The prepared pipeline identifies the depth operation without depending on the
 * active graphics backend's private command-encoder implementation.
 */
@Mixin(PreparedRenderType.class)
public abstract class PreparedRenderTypeScopeDepthCopyMixin {
    @WrapMethod(method = "drawFromBuffer(Lnet/minecraft/client/renderer/StagedVertexBuffer$ExecuteInfo;)V")
    private void tacz$runScopeDepthOperation(StagedVertexBuffer.ExecuteInfo info,
                                             Operation<Void> original) {
        PreparedRenderType self = (PreparedRenderType) (Object) this;
        ScopeDepthCopyState.Operation scopeOperation = ScopeRenderTypes.operationFor(self);
        if (scopeOperation == ScopeDepthCopyState.Operation.NONE || !ScopeDepthCopyState.isOpenGlBackend()) {
            original.call(info);
            return;
        }

        ScopeDepthCopyState.begin(scopeOperation);
        try {
            // The GL command encoder performs beforeDraw only after the render pass, program,
            // samplers and destination framebuffer have been bound.
            original.call(info);
        } finally {
            ScopeDepthCopyState.end();
        }
    }
}

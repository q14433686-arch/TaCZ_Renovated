package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 二次渲染（{@code ScopePipRerender}）时，天空也要画进离屏 target。
 *
 * <p>{@code SkyRenderer} 把渲染目标<b>缓存成字段</b>，而它缓存的时机并不总在
 * 我们的重定向窗口之内 —— 命中缓存时它写的是主画面而不是镜内那一遍的 target，
 * 表现为「镜内有地形和实体，但没有天空」。
 *
 * <p>这里不改动那个字段（那会波及之后的主画面），只在取值那一刻顶替：
 * {@code ScopePipRenderer#redirectTarget()} 非 null 的窗口极窄
 * （仅 {@code levelRenderer.render} 那一次调用期间），窗口外一律原样返回。
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @ModifyExpressionValue(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private RenderTarget tacz$redirectSkyTargetForScopePip(RenderTarget original) {
        RenderTarget scopeTarget = ScopePipRenderer.redirectTarget();
        return scopeTarget != null ? scopeTarget : original;
    }
}

package com.tacz.guns.mixin.client;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读 {@code PreparedFrame} 的「在用」标志。
 *
 * <p>{@code FeatureRenderDispatcher} 全程只有一个 PreparedFrame 实例，它的
 * 「在用」标志就是 {@code context != null}。镜内那一遍失败时这个标志可能留在
 * true 上，主画面那一遍会当场撞 {@code PreparedFrame already in use}。
 * 判断它才能决定要不要替失败的那一遍补一次 {@code close()}。
 *
 * @see com.tacz.guns.client.render.scope.ScopePipRenderer#consumePreparedFrameLeak()
 */
@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public interface PreparedFrameAccessor {
    @Accessor("context")
    FeatureFrameContext tacz$context();
}

package com.tacz.guns.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取 {@code LevelRenderer} 那份主画面的 {@code SubmitNodeStorage}。
 *
 * <p>镜内二次渲染期间，各 {@code FeatureRenderPhase} 在 {@code sortInto} 末尾
 * <b>不清空自己</b>，好让紧随其后的主画面那一遍还能再取一次同样的实体/方块实体。
 * 但<b>只有主画面那一份存储</b>可以被这样保留 —— 光影的阴影专用存储若在每帧
 * 阴影渲染后不清空，提交节点会随开镜帧数无限沉积并拖垮 FPS。
 *
 * @see com.tacz.guns.client.render.scope.ScopePipRenderer#shouldPreserveSubmits()
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("submitNodeStorage")
    SubmitNodeStorage tacz$getSubmitNodeStorage();
}

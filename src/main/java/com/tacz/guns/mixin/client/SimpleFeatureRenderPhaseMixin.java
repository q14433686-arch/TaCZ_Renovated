package com.tacz.guns.mixin.client;

import com.tacz.guns.client.render.scope.ScopePipRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 镜内那一遍<b>不许</b>把本帧的提交节点吃掉 —— 主画面还要用同一批。
 *
 * <h2>症状</h2>
 * 一开镜，<b>镜外</b>的实体、方块实体、名牌全部消失，而<b>镜内</b>一切正常。
 * 松开右键就恢复。
 *
 * <h2>成因（26.2 字节码实读，姊妹分支）</h2>
 * 一帧里的实体等等是在 extract 阶段被「提交」到一个 {@code SubmitNodeStorage} 里的，
 * 而 {@code LevelRenderer#render} 开头会
 * {@code featureRenderDispatcher.prepareFrame(storage) → storage.drainPhases(phase -> phase.sortInto(preparedFrame))}；
 * 而 {@code sortInto} 的<b>最后一句</b>是 {@code this.clear()}。
 * 名字里的 "drain" 是字面意思：<b>取一次就没了</b>。
 *
 * <p>而二次渲染的画中画是「同一帧里先驱动一遍 {@code levelRenderer.render} 画镜内画面，
 * 再让 vanilla 画主画面」，于是<b>第一遍把节点全取走了，第二遍拿到的是空的</b>。
 *
 * <h2>为什么可以直接不清</h2>
 * {@code sortFeatureInto} 是把节点 {@code addAll} 进 {@code PreparedFrame.allSubmits}，
 * 也就是说 <b>PreparedFrame 拿到的是一份拷贝</b>，与 phase 自己那些 list 再无瓜葛。
 * 节点本身是只读地被绘制，重复绘制没有副作用。
 *
 * <h3>为什么取消整个 sortInto 的尾巴，而不是 @Redirect 掉那个 clear()</h3>
 * 两个 phase 实现清空自己的写法不一样：本类是调自己的 {@code clear()}，
 * {@code TranslucentFeatureRenderPhase} 则是就地 {@code submits.clear()} +
 * {@code distances.clear()} 两句。但<b>两边都把清空放在方法最后</b>，
 * 所以「在第一句清空之前 cancel 掉」这一招对两边都成立。
 * 尤其对 translucent 那边很关键：它那两个 list 是平行的，只清一个会让 phase 错位。
 *
 * <h2>作用范围</h2>
 * 只在镜内那一次 {@code levelRenderer.render} 期间生效。
 *
 * @see TranslucentFeatureRenderPhaseMixin
 */
@Mixin(SimpleFeatureRenderPhase.class)
public abstract class SimpleFeatureRenderPhaseMixin {

    @Inject(
            method = "sortInto",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;clear()V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void tacz$keepSubmitsForTheMainPass(FeatureRenderPhase.Output output, CallbackInfo ci) {
        if (ScopePipRenderer.shouldPreserveSubmits()) {
            // 节点已经拷进镜内那一遍的 PreparedFrame 了，这里只是跳过「清空自己」。
            ci.cancel();
        }
    }
}

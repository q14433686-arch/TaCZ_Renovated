package com.tacz.guns.mixin.client.voxy;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 镜内那一遍不再额外跑一次 Voxy 的节点流式加载 / 驱逐 tick。
 *
 * <p>这个 tick 的语义是「每帧一次」：它按帧推进 LOD 区块的加载与驱逐。
 * 二次渲染模式下同一帧里会走两遍世界渲染，于是驱逐速率被<b>翻倍</b> ——
 * 表现为视距之外 LOD 区块闪烁、出现空洞。
 */
@Mixin(targets = {
        "me.cortex.voxy.client.core.rendering.hierachical.AsyncNodeManager",
        "me.cortex.voxy.client.core.rendering.hierachical.NodeCleaner"
}, remap = false)
public abstract class VoxyNodeTickMixin {

    @Unique
    private static boolean tacz$logged;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void tacz$noNodeTickDuringScopePass(CallbackInfo ci) {
        if (!ScopePipRenderer.isScopePassActive()) {
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Voxy's node streaming/eviction tick is limited to once per "
                    + "frame. Running it for the scope pass as well doubled the eviction rate, which "
                    + "showed up as flickering LOD blocks and holes past the vanilla render distance.");
        }
        ci.cancel();
    }
}

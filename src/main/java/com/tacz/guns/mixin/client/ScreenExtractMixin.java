package com.tacz.guns.mixin.client;

import cn.sh1rocu.tacz.compat.meshloader.render.ScreenRenderTracker;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 【本仓 NeoForge 表皮】精确框住 Screen 的提取（extract）窗口。
 *
 * <p>姊妹分支用 Fabric 的 {@code ScreenEvents.beforeExtract/afterExtract} 做同一件事，
 * NeoForge 没有等价事件，改挂 vanilla 的 {@code Screen#extractRenderState}。</p>
 *
 * <h2>挂点存在性（本仓自证，不是猜的）</h2>
 * {@code GunRefitScreen extends Screen}（直接继承 Screen）里覆写了
 * {@code extractRenderState(GuiGraphicsExtractor, int, int, float)} 并调用
 * {@code super.extractRenderState(graphics, mouseX, mouseY, partialTick)} ——
 * 该方法在 {@code Screen} 上必然存在且是本签名，注入可解析。</p>
 *
 * <p>子类覆写 + 调 super 会造成嵌套，所以追踪器用深度计数而不是布尔
 * （见 {@link ScreenRenderTracker}）。RETURN 在异常路径不触发会让计数泄漏，
 * 泄漏的后果是「世界 GPU 路径长期不开闸」= 退回 collector（安全侧），
 * 且 Screen 提取抛异常本身已是崩溃级事件，不为它再加机制。</p>
 *
 * <p>用途：世界 poly_mesh GPU 表（{@code WORLD_DRAWS}）据此拒收 GUI 内嵌 3D
 * 预览（背包人偶 / 枪匠桌预览）的提交，避免关 PR #33 的世界 pass 泄漏复刻。</p>
 */
@Mixin(Screen.class)
public abstract class ScreenExtractMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tacz$beginScreenExtract(CallbackInfo ci) {
        ScreenRenderTracker.beginExtract();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void tacz$endScreenExtract(CallbackInfo ci) {
        ScreenRenderTracker.endExtract();
    }
}

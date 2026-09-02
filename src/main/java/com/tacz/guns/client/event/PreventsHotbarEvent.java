package com.tacz.guns.client.event;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 枪械工作台/改装台是全屏界面，需要隐藏原版底部快捷栏（Hotbar）防止穿模。
 *
 * <p>1.21.11 的 GUI 架构下，原版 Hotbar 渲染钩子对应
 * {@code RenderGuiLayerEvent.Pre} + {@code VanillaGuiLayers.HOTBAR}
 * （NeoForge 21.11 @ 1.21.11 分支 {@code VanillaGuiLayers#HOTBAR} 为
 * {@code minecraft:hotbar}，{@code RenderGuiLayerEvent.Pre} 实现
 * {@code ICancellableEvent}）；本类由
 * {@link ClientGameEvents#onRenderGuiLayer} 在该层渲染前调用，
 * 返回 true 则取消该层（与本仓准星层 {@code RenderCrosshairEvent} 同款接线）。</p>
 *
 * <p><b>旧实现勘误</b>：原 {@code onRenderHotbarEvent(AtomicBoolean)} 宣称由
 * 「GuiMixin」转发——经全仓 grep 核实该 Mixin <b>不存在</b>，是幻影调用点，
 * 与本仓 26.1.2 线同款假阳性（见 26.1.2 线 WIRE 记录 §2.6）。</p>
 */
public class PreventsHotbarEvent {
    /**
     * 是否应当隐藏原版快捷栏。
     */
    public static boolean shouldHideHotbar() {
        Screen screen = Minecraft.getInstance().screen;
        // 枪械合成台界面隐藏快捷栏
        if (screen instanceof GunSmithTableScreen) {
            return true;
        }
        // 枪械改装界面隐藏快捷栏
        return screen instanceof GunRefitScreen;
    }
}

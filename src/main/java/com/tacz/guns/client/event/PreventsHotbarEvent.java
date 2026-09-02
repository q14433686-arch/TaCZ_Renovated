package com.tacz.guns.client.event;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 枪械工作台/改装台是全屏界面，需要隐藏原版底部快捷栏（Hotbar）防止穿模。
 *
 * <p>26.x 的 GUI 架构下，原版 Hotbar 渲染钩子对应
 * {@code RenderGuiLayerEvent.Pre} + {@code VanillaGuiLayers.HOTBAR}；
 * 本类由 {@link ClientGameEvents#onRenderGuiLayer} 在该层渲染前调用，
 * 返回 true 则取消该层（与本仓准星层 {@code RenderCrosshairEvent} 同款接线）。</p>
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

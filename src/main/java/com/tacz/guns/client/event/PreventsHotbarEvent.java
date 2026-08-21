package com.tacz.guns.client.event;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.concurrent.atomic.AtomicBoolean;

public class PreventsHotbarEvent {
    public static void onRenderHotbarEvent(AtomicBoolean cancelled) {
        // NeoForge's RenderGuiLayerEvent.Pre forwards this decision for each HUD layer.
        // In 26.2 the HUD lives in Hud rather than Minecraft/Gui extraction internals, so the
        // loader event is the stable cancellation surface and needs no vanilla mixin.
        Screen screen = Minecraft.getInstance().gui.screen();
        // 枪械合成台界面关闭背景
        if (screen instanceof GunSmithTableScreen) {
            cancelled.set(true);
            return;
        }
        // 枪械改装界面关闭背景
        if (screen instanceof GunRefitScreen) {
            cancelled.set(true);
        }
    }
}

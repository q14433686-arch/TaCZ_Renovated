package com.tacz.guns.compat.cloth;

import com.tacz.guns.client.gui.compat.TaczConfigHomeScreen;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/**
 * Client configuration entry point.
 *
 * <p>Landing page is NeoForge's native configuration UI (same widget style as Carry On
 * on 26.1.2), restricted to Client + Common so every listed option is editable in-game.
 * Server configs are omitted: they are world-locked / remote-disabled, and Fabric/upstream
 * Cloth never put them on the T-key screen either.</p>
 */
public final class MenuIntegration {
    private MenuIntegration() {
    }

    public static Screen getConfigScreen(@Nullable Screen parent) {
        return new TaczConfigHomeScreen(parent);
    }
}

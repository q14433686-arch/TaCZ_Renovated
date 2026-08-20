package com.tacz.guns.compat.cloth;

import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/** Cloth Config is optional. Returns null when the library is absent. */
public final class MenuIntegration {
    private MenuIntegration() {
    }

    @Nullable
    public static Screen getConfigScreen(@Nullable Screen parent) {
        return null;
    }
}

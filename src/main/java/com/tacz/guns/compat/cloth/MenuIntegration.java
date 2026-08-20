package com.tacz.guns.compat.cloth;

import com.tacz.guns.GunMod;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import javax.annotation.Nullable;

/**
 * Client configuration entry point for the NeoForge port.
 *
 * <p>The original Fabric project builds this screen through the optional Cloth Config API.
 * NeoForge 26.1.2 already provides a native configuration UI for every
 * {@code ModConfigSpec} registered by the mod, so this port must not return a null screen
 * or require a Fabric/Cloth integration just to open its own configuration.</p>
 */
public final class MenuIntegration {
    private MenuIntegration() {
    }

    @Nullable
    public static Screen getConfigScreen(@Nullable Screen parent) {
        if (GunMod.container == null) {
            return null;
        }
        return new ConfigurationScreen(GunMod.container, parent);
    }
}

package com.tacz.guns.compat.cloth;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import javax.annotation.Nullable;

/**
 * Client configuration entry point: NeoForge's native {@link ConfigurationScreen},
 * constructed exactly the way its class javadoc recommends
 * ({@code new ConfigurationScreen(container, parent)} — the same screen Carry On
 * uses on 26.1.2).
 *
 * <p>Do NOT hand-roll a {@code Screen} subclass for this again: in 26.1.2 the
 * vanilla {@code Screen#extractRenderState} default already extracts the
 * (blurred) background, and calling {@code extractBackground} manually on top
 * triggers {@code IllegalStateException: Can only blur once per frame}
 * (crash log 2026-08-21 13:21, TaczConfigHomeScreen.java:87, since removed).</p>
 *
 * <p>Lang keys already follow the native scheme ({@code tacz.configuration.title},
 * {@code tacz.configuration.section.<file>.toml[.title]}), so all existing
 * translations carry over unchanged. The native screen lists every registered
 * config type and disables SERVER/STARTUP entries with a tooltip whenever they
 * cannot be edited in the current context (e.g. while online).</p>
 */
public final class MenuIntegration {
    private MenuIntegration() {
    }

    public static Screen getConfigScreen(ModContainer container, @Nullable Screen parent) {
        return new ConfigurationScreen(container, parent);
    }
}

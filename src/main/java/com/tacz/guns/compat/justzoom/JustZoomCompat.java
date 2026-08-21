package com.tacz.guns.compat.justzoom;

import net.neoforged.fml.ModList;

/**
 * Keksuccino's Just Zoom (modid {@code justzoom}) compat.
 *
 * <p>JustZoom 2.x hooks {@code AbstractClientPlayer#getFieldOfViewModifier} (the vanilla
 * spyglass path), so its zoom is already baked into the FOV that
 * {@code ViewportEvent.ComputeFov} hands us. TACZ scope magnification would stack
 * on top of it. Mirroring the upstream Zoomify contract, while a TACZ scope is
 * engaging we divide JustZoom's modifier back out — the gun scope takes priority;
 * outside of aiming, JustZoom works untouched.</p>
 *
 * <p>Note: the API calls below are compile-only references to JustZoom's public
 * API ({@code de.keksuccino.justzoom.ZoomHandler}); no JustZoom code is
 * embedded or redistributed (upstream is DSMSLv3-licensed).</p>
 */
public final class JustZoomCompat {
    private static final String MOD_ID = "justzoom";
    private static boolean INSTALLED;

    private JustZoomCompat() {
    }

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    /**
     * The divisor to remove JustZoom's contribution from an event FOV value,
     * or {@code 1.0F} when JustZoom is absent/not zooming.
     */
    public static float getZoomFovDivisor() {
        if (INSTALLED && de.keksuccino.justzoom.ZoomHandler.isZooming()) {
            float modifier = de.keksuccino.justzoom.ZoomHandler.getFovModifier();
            if (modifier > 0.0F && Float.isFinite(modifier)) {
                return modifier;
            }
        }
        return 1.0F;
    }
}

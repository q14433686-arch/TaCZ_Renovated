package com.tacz.guns.compat.zoomify;

/**
 * Compatibility facade for Zoomify (isXander).
 *
 * <p>Investigated 2026-08: Zoomify has no NeoForge 26.2 build (CurseForge project 574741 lists mod
 * loaders Fabric/Quilt exclusively). The upstream Fabric port divides the FOV
 * by {@code Zoomify.getZoomDivisor(tickDelta)} inside its ViewportEvent.FOV
 * hook; on NeoForge that integration point does not exist because the mod
 * itself cannot be installed. This hook therefore stays a no-op.</p>
 */
public class ZoomifyCompat {
    public static void init() {
    }

    public static double getFov(double fov, float tickDelta) {
        return fov;
    }
}

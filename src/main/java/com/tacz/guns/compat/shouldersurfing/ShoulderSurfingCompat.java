package com.tacz.guns.compat.shouldersurfing;

/**
 * Optional facade for Shoulder Surfing Reloaded.
 *
 * <p><b>26.3 status: disabled, not fixed.</b> Shoulder Surfing Reloaded has no NeoForge 26.3
 * build (CurseForge project 243190's newest is the 26.2 line, verified 2026-09-21), so its
 * {@code compileOnly} coordinate is commented out in {@code build.gradle}, both
 * {@link ShoulderSurfingCompatInner} and {@code ShoulderSurfingPlugin} are excluded from the
 * source set, and {@code shouldersurfing_plugin.json} is excluded from the resource set.
 * {@link #showCrosshair()} and {@link #isInstalled()} therefore hard-return {@code false},
 * regardless of what is actually loaded.</p>
 *
 * <p>To restore once Shoulder Surfing ships a NeoForge 26.3 build:</p>
 * <ol>
 *   <li>set {@code shoulder_surfing_neoforge_file} in {@code gradle.properties} to the 26.3 file id;</li>
 *   <li>uncomment the {@code compileOnly "curse.maven:shoulder-surfing-reloaded-243190:..."} block;</li>
 *   <li>drop the two {@code com/tacz/guns/compat/shouldersurfing/...} source excludes
 *       and the {@code shouldersurfing_plugin.json} resource exclude in {@code build.gradle};</li>
 *   <li>restore the {@code ModList.isLoaded} + 5.x-API guard and the delegation below.</li>
 * </ol>
 */
public final class ShoulderSurfingCompat {
    /** Kept for the restore path; unused while the integration is disabled. */
    @SuppressWarnings("unused")
    private static final String MOD_ID = "shouldersurfing";

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        // 26.3: no Shoulder Surfing NeoForge build exists, so there is nothing to detect.
    }

    public static boolean showCrosshair() {
        // 26.3: no-op — the third-person crosshair override is inactive.
        return false;
    }

    public static boolean isInstalled() {
        // 26.3: always false; the integration is not compiled into this build.
        return false;
    }
}

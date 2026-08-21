package com.tacz.guns.compat.shouldersurfing;

import net.neoforged.fml.ModList;

public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED = false;

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID) && hasV5Api();
    }

    private static boolean hasV5Api() {
        try {
            Class.forName("com.github.exopandora.shouldersurfing.api.client.Perspective", false,
                    ShoulderSurfingCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            // Reject stale 4.x jars instead of linking against classes that only exist in 5.x.
            return false;
        }
    }

    public static boolean showCrosshair() {
        if (INSTALLED) {
            return ShoulderSurfingCompatInner.showCrosshair();
        }
        return false;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}

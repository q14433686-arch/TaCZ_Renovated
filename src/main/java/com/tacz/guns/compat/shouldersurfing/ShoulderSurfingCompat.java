package com.tacz.guns.compat.shouldersurfing;

import net.neoforged.fml.ModList;

public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED;

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean showCrosshair() {
        if (!INSTALLED) {
            return false;
        }
        try {
            Class<?> api = Class.forName("com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object camera = instance.getClass().getMethod("getCamera").invoke(instance);
            Object perspective = camera.getClass().getMethod("getPerspective").invoke(camera);
            return "SHOULDER_SURFING".equals(String.valueOf(perspective));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}

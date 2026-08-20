package com.tacz.guns.init;

import net.neoforged.fml.ModList;

public class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";
    public static final String IRIS = "iris";
    public static final String CARRY_ON_ID = "carryon";

    public static void onEnqueue() {
        checkModLoad(IRIS, com.tacz.guns.compat.iris.IrisCompat::initCompat);
    }

    public static void checkModLoad(String modId, Runnable runnable) {
        if (ModList.get().isLoaded(modId)) {
            runnable.run();
        }
    }
}

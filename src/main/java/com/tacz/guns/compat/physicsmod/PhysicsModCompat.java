package com.tacz.guns.compat.physicsmod;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.lang.reflect.Method;

/**
 * 让 Physics Mod 的物理方块在「镜内那一遍」跟着用窄投影。
 */
public final class PhysicsModCompat {

    private static final String MOD_ID = "physicsmod";
    private static final String ACCESSOR = "net.diebuddies.minecraft.LevelRendererAccessor";

    private static boolean resolved;
    private static boolean available;
    private static Method getMainRenderer;
    private static Method getStoredProjection;
    private static Method storeProjection;

    private static final Matrix4f SAVED_PROJECTION = new Matrix4f();
    private static final Matrix4f NARROW_PROJECTION = new Matrix4f();
    private static boolean projectionOverridden = false;

    private static boolean loggedFailure;

    private PhysicsModCompat() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            if (!ModList.get().isLoaded(MOD_ID)) {
                return;
            }
            Class<?> accessor = Class.forName(ACCESSOR);
            getMainRenderer = accessor.getMethod("physicsmod$getMainRenderer");
            Class<?> mainRenderer = getMainRenderer.getReturnType();
            getStoredProjection = mainRenderer.getMethod("getStoredProjectionMatrix");
            storeProjection = mainRenderer.getMethod("storeProjectionMatrix", Matrix4f.class);
            available = true;
            GunMod.LOGGER.info("[TACZ Scope] Physics Mod detected; its movable blocks will follow the "
                    + "scope pass projection.");
        } catch (Throwable t) {
            available = false;
            logOnce("resolve Physics Mod renderer", t);
        }
    }

    /**
     * 把 Physics Mod 存着的那份投影换成窄投影。
     */
    public static boolean overrideProjection(Matrix4fc narrow) {
        resolve();
        if (!available || narrow == null) {
            return false;
        }
        try {
            Object main = mainRenderer();
            if (main == null) {
                return false;
            }
            Object current = getStoredProjection.invoke(main);
            if (!(current instanceof Matrix4f live)) {
                return false;
            }
            SAVED_PROJECTION.set(live);
            NARROW_PROJECTION.set(narrow);
            storeProjection.invoke(main, NARROW_PROJECTION);
            projectionOverridden = true;
            return true;
        } catch (Throwable t) {
            projectionOverridden = false;
            logOnce("override Physics Mod projection", t);
            return false;
        }
    }

    /** 还原 {@link #overrideProjection} 换掉的那份投影。 */
    public static void restoreProjection() {
        if (!projectionOverridden) {
            return;
        }
        projectionOverridden = false;
        try {
            Object main = mainRenderer();
            if (main != null) {
                storeProjection.invoke(main, SAVED_PROJECTION);
            }
        } catch (Throwable t) {
            logOnce("restore Physics Mod projection", t);
        }
    }

    private static Object mainRenderer() throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) {
            return null;
        }
        return getMainRenderer.invoke(mc.levelRenderer);
    }

    private static void logOnce(String what, Throwable t) {
        if (!loggedFailure) {
            loggedFailure = true;
            GunMod.LOGGER.warn("[TACZ Scope] Failed to {} — Physics Mod's movable blocks will not follow "
                    + "the scope pass. Everything else is unaffected.", what, t);
        }
    }
}

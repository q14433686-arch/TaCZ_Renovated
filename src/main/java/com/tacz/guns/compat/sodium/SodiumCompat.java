package com.tacz.guns.compat.sodium;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sodium 兼容：同步它自己那份地形投影矩阵快照。
 */
public final class SodiumCompat {

    private static final String STORAGE_INTERFACE = "net.caffeinemc.mods.sodium.client.util.GameRendererStorage";
    private static final String GETTER = "sodium$getProjectionMatrix";

    private static boolean loggedFailure = false;
    private static boolean loggedSuccess = false;

    /** 改写前的原值备份；{@code null} 表示本次没有改写成功，restore 时不必做事。 */
    @Nullable
    private static Matrix4f patchedField;
    private static final Matrix4f SAVED = new Matrix4f();

    // 缓存反射句柄，避免热路径每帧反射查找与分配
    private static boolean storageResolved = false;
    @Nullable
    private static Class<?> storageClass;
    @Nullable
    private static Method getterMethod;

    private static boolean uniformManagerResolved = false;
    @Nullable
    private static Class<?> worldRendererClass;
    @Nullable
    private static Method instanceNullableMethod;
    @Nullable
    private static Field uniformBufferManagerField;
    @Nullable
    private static Method prepareFrameMethod;

    private SodiumCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("sodium");
    }

    private static void resolveStorage() {
        if (storageResolved) {
            return;
        }
        storageResolved = true;
        try {
            storageClass = Class.forName(STORAGE_INTERFACE);
            getterMethod = storageClass.getMethod(GETTER);
        } catch (Throwable t) {
            storageClass = null;
            getterMethod = null;
        }
    }

    private static void resolveUniformManager() {
        if (uniformManagerResolved) {
            return;
        }
        uniformManagerResolved = true;
        try {
            worldRendererClass = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            instanceNullableMethod = worldRendererClass.getMethod("instanceNullable");
            uniformBufferManagerField = worldRendererClass.getDeclaredField("uniformBufferManager");
            uniformBufferManagerField.setAccessible(true);
        } catch (Throwable t) {
            worldRendererClass = null;
            instanceNullableMethod = null;
            uniformBufferManagerField = null;
        }
    }

    /**
     * 把 Sodium 的地形投影快照临时换成 {@code narrow}。
     *
     * <p>必须与 {@link #restoreProjection()} 成对使用（try/finally）。
     *
     * @return 是否真的改写成功（false = Sodium 不在或反射失败，镜内地形将不跟随放大）
     */
    public static boolean overrideProjection(Matrix4fc narrow) {
        patchedField = null;
        if (!isLoaded()) {
            return false;
        }
        resolveStorage();
        if (storageClass == null || getterMethod == null) {
            return false;
        }
        try {
            Object gameRenderer = Minecraft.getInstance().gameRenderer;
            if (!storageClass.isInstance(gameRenderer)) {
                logFailureOnce("GameRenderer does not implement " + STORAGE_INTERFACE);
                return false;
            }
            Object current = getterMethod.invoke(gameRenderer);
            if (!(current instanceof Matrix4f live)) {
                logFailureOnce(GETTER + " did not return a mutable Matrix4f");
                return false;
            }
            SAVED.set(live);
            live.set(narrow);
            patchedField = live;
            if (!loggedSuccess) {
                loggedSuccess = true;
                GunMod.LOGGER.info("[TACZ Scope] Sodium terrain projection is being synced for the scope pass.");
            }
            return true;
        } catch (Throwable t) {
            logFailureOnce("reflection failed: " + t);
            return false;
        }
    }

    /** 还原 {@link #overrideProjection} 改写过的快照。没改写成功时是空操作。 */
    public static void restoreProjection() {
        if (patchedField != null) {
            patchedField.set(SAVED);
            patchedField = null;
        }
    }

    /**
     * 让 Sodium 重新上传本帧 uniform。
     */
    public static void resetChunkUniformUpload() {
        if (!isLoaded()) {
            return;
        }
        resolveUniformManager();
        if (worldRendererClass == null || instanceNullableMethod == null || uniformBufferManagerField == null) {
            return;
        }
        try {
            Object worldRenderer = instanceNullableMethod.invoke(null);
            if (worldRenderer == null) {
                return;
            }
            Object manager = uniformBufferManagerField.get(worldRenderer);
            if (manager == null) {
                return;
            }
            if (prepareFrameMethod == null) {
                prepareFrameMethod = manager.getClass().getMethod("prepareFrame");
            }
            prepareFrameMethod.invoke(manager);
            if (!loggedResetSuccess) {
                loggedResetSuccess = true;
                GunMod.LOGGER.info("[TACZ Scope] Sodium chunk uniforms will be re-uploaded for the main pass "
                        + "(this is what kept the main view on the scope's projection).");
            }
        } catch (Throwable t) {
            logResetFailureOnce(t);
        }
    }

    private static boolean loggedResetSuccess = false;
    private static boolean loggedResetFailure = false;

    private static void logResetFailureOnce(Throwable t) {
        if (loggedResetFailure) {
            return;
        }
        loggedResetFailure = true;
        GunMod.LOGGER.warn("[TACZ Scope] Could not reset Sodium's per-frame chunk uniform flag ({}). "
                + "The main view's terrain will keep rendering with the scope's projection.", t.toString());
    }

    private static void logFailureOnce(String detail) {
        if (loggedFailure) {
            return;
        }
        loggedFailure = true;
        GunMod.LOGGER.warn("[TACZ Scope] Could not sync Sodium's terrain projection ({}). "
                + "The scope's second render will show terrain at the wrong magnification.", detail);
    }
}

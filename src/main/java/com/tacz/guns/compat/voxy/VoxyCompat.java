package com.tacz.guns.compat.voxy;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * 拿到 Voxy 的 {@code VoxyRenderSystem} 实例。
 *
 * <p>取法照抄 Voxy 自己的写法（{@code IVoxyRenderSystemHolder}）：
 * <pre>
 * static VoxyRenderSystem getNullable() {
 *     var lr = (IVoxyRenderSystemHolder) Minecraft.getInstance().levelRenderer;
 *     return lr == null ? null : lr.voxy$getRenderSystem();
 * }
 * </pre>
 * 这里不去调那个接口的静态方法（接口静态方法在反射与混入上都容易踩坑），
 * 而是直接对 {@code levelRenderer} 调 {@code voxy$getRenderSystem()}。
 *
 * <p><b>移植说明</b>：随姊妹分支的镜内 PIP 一族同步而来；唯一改动是
 * {@code FabricLoader#isModLoaded} → {@code ModList#isLoaded}。
 */
public final class VoxyCompat {

    private static boolean resolved;
    private static Method getRenderSystem;

    private VoxyCompat() {
    }

    /** @return {@code VoxyRenderSystem} 实例；Voxy 不在、还没建好或取不到时返回 {@code null} */
    public static Object renderSystem() {
        if (!resolved) {
            resolved = true;
            try {
                if (ModList.get().isLoaded("voxy")) {
                    getRenderSystem = Class.forName("me.cortex.voxy.client.core.IVoxyRenderSystemHolder")
                            .getMethod("voxy$getRenderSystem");
                }
            } catch (Throwable ignored) {
                getRenderSystem = null;
            }
        }
        if (getRenderSystem == null) {
            return null;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.levelRenderer == null) {
                return null;
            }
            return getRenderSystem.invoke(mc.levelRenderer);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

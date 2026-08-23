package com.tacz.guns.compat.iris;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopeMaskRenderer;
import com.tacz.guns.client.render.scope.ScopeMaskTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Bridges TACZ's off-screen ocular mask texture into Iris shaders when a shader pack is active.
 *
 * <p>During each draw setup via {@link #applyToGlRenderPass}, this helper inspects the active
 * pipeline name from Iris' internal GlRenderPass and sets uniforms on the bound OpenGL program:
 * <ul>
 *   <li>{@code tacz_ScopeMaskMode}: 0 = off, 1 = discard inside mask (body), 2 = discard outside (reticle)</li>
 *   <li>{@code tacz_ScopeMaskSampler}: bound texture unit index pointing to the mask texture</li>
 * </ul>
 * If no mask texture is available or reflection fails, the uniform stays 0 (disabled), leaving the
 * shader pack unaffected.</p>
 */
public final class IrisScopeMaskState {
    private static final String MASK_SAMPLER = "ScopeMaskSampler";
    private static final String UNIFORM_MODE = "tacz_ScopeMaskMode";
    private static final String UNIFORM_SAMPLER = "tacz_ScopeMaskSampler";

    private static boolean loggedFailure;
    private static boolean loggedApply;

    /**
     * {@code GlRenderPass.pipeline} 字段，按 class 缓存。
     */
    private static Class<?> cachedPassClass;
    private static Field cachedPipelineField;
    private static boolean pipelineFieldResolved;

    /**
     * 「这套 GL 管线对应哪个 mode」的记忆。
     */
    private static final java.util.Map<Object, Integer> MODE_BY_PIPELINE = new java.util.IdentityHashMap<>();
    private static final int MODE_CACHE_LIMIT = 512;

    /** {@code GL_MAX_TEXTURE_IMAGE_UNITS} 是驱动常量，问一次就够。 */
    private static int cachedMaxTextureUnits = -1;

    private IrisScopeMaskState() {
    }

    /**
     * Resets the scope mask uniform on an Iris ExtendedShader program to 0 upon setup/binding.
     */
    public static void resetShaderProgram(Object shader) {
        try {
            int programId = getProgramId(shader);
            if (programId > 0) {
                int modeLocation = GL20C.glGetUniformLocation(programId, UNIFORM_MODE);
                if (modeLocation >= 0) {
                    GL20C.glUniform1i(modeLocation, 0);
                }
            }
        } catch (Throwable t) {
            logOnce("reset shader program", t);
        }
    }

    /**
     * Inspects the active GlRenderPass and injects scope-mask uniform state if supported.
     * Invoked from {@code IrisGlCommandEncoderMixin} right before executing a draw command.
     */
    public static void applyToGlRenderPass(Object glRenderPass) {
        if (!IrisCompat.isUsingRenderPack()) {
            return;
        }
        try {
            if (glRenderPass == null) {
                return;
            }
            int mode = resolveMode(glRenderPass);

            int programId = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
            if (programId <= 0) {
                return;
            }

            int modeLocation = GL20C.glGetUniformLocation(programId, UNIFORM_MODE);
            if (modeLocation < 0) {
                return;
            }

            if (mode == 0) {
                // Program has uniform location, but this draw pass does not need scope masking.
                // Reset mode to 0 so reused Iris shader programs do not leak mask state into ordinary draws.
                GL20C.glUniform1i(modeLocation, 0);
                return;
            }

            int samplerLocation = GL20C.glGetUniformLocation(programId, UNIFORM_SAMPLER);
            if (samplerLocation < 0) {
                GL20C.glUniform1i(modeLocation, 0);
                return;
            }

            int textureId = resolveMaskTextureId(glRenderPass);
            if (textureId <= 0) {
                GL20C.glUniform1i(modeLocation, 0);
                return;
            }

            if (cachedMaxTextureUnits < 0) {
                cachedMaxTextureUnits = GL11C.glGetInteger(GL20C.GL_MAX_TEXTURE_IMAGE_UNITS);
            }
            int unit = Math.max(15, cachedMaxTextureUnits - 1);
            if (!loggedApply) {
                loggedApply = true;
                GunMod.LOGGER.info("[TACZ Scope] Iris scope-mask bridge active (mode={}, textureUnit={}, textureId={}).", mode, unit, textureId);
            }
            GL20C.glUniform1i(modeLocation, mode);
            GL20C.glUniform1i(samplerLocation, unit);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureId);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        } catch (Throwable t) {
            logOnce("apply scope mask uniforms to Iris shader", t);
        }
    }

    /**
     * {@code GlRenderPass.pipeline}，字段对象按 class 缓存一次。
     */
    private static Field pipelineField(Object glRenderPass) {
        Class<?> cls = glRenderPass.getClass();
        if (cls != cachedPassClass || !pipelineFieldResolved) {
            cachedPassClass = cls;
            cachedPipelineField = null;
            for (Class<?> c = cls; c != null && cachedPipelineField == null; c = c.getSuperclass()) {
                for (String fName : new String[]{"pipeline", "renderPipeline"}) {
                    try {
                        Field f = c.getDeclaredField(fName);
                        f.setAccessible(true);
                        cachedPipelineField = f;
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
            pipelineFieldResolved = true;
        }
        return cachedPipelineField;
    }

    private static int resolveMode(Object glRenderPass) {
        try {
            if (glRenderPass == null) {
                return 0;
            }
            Field pField = pipelineField(glRenderPass);
            if (pField == null) {
                return 0;
            }
            Object glPipeline = pField.get(glRenderPass);
            if (glPipeline == null) {
                return 0;
            }
            Integer remembered = MODE_BY_PIPELINE.get(glPipeline);
            if (remembered != null) {
                return remembered;
            }
            int resolved = resolveModeUncached(glPipeline);
            if (MODE_BY_PIPELINE.size() >= MODE_CACHE_LIMIT) {
                MODE_BY_PIPELINE.clear();
            }
            MODE_BY_PIPELINE.put(glPipeline, resolved);
            return resolved;
        } catch (Throwable t) {
            logOnce("resolve scope render pass", t);
        }
        return 0;
    }

    /** 真正去问「这套管线是不是我们的镜身/准星管线」。只在每个管线实例上跑一次。 */
    private static int resolveModeUncached(Object glPipeline) {
        try {
            Object renderPipeline = invokeNoArgs(glPipeline, "info");
            if (renderPipeline == null) {
                renderPipeline = invokeNoArgs(glPipeline, "getInfo");
            }
            if (renderPipeline == null) {
                renderPipeline = readField(glPipeline, "info");
            }
            if (renderPipeline == null) {
                renderPipeline = readField(glPipeline, "renderPipeline");
            }
            if (renderPipeline == null) {
                return 0;
            }
            Object location = invokeNoArgs(renderPipeline, "getLocation");
            if (location == null) {
                location = invokeNoArgs(renderPipeline, "location");
            }
            if (location == null) {
                location = readField(renderPipeline, "location");
            }
            if (location == null) {
                return 0;
            }
            String path = location.toString();
            if (path == null) {
                return 0;
            }
            String normalized = path.toLowerCase(Locale.ROOT);
            if (normalized.endsWith("scope_body_clipped")) {
                return 1;
            }
            if (normalized.endsWith("scope_flash_translucent_clipped")
                    || normalized.endsWith("scope_flash_swirl_clipped")
                    || normalized.endsWith("muzzle_flash_translucent")
                    || normalized.endsWith("muzzle_flash_swirl")) {
                return 1;
            }
            if (normalized.endsWith("scope_reticle_clipped")
                    || normalized.endsWith("scope_reticle_emissive_clipped")
                    || normalized.endsWith("scope_reticle_emissive")) {
                return 2;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int resolveMaskTextureId(Object glRenderPass) {
        try {
            Object samplersObj = readField(glRenderPass, "samplers");
            if (samplersObj instanceof Map<?, ?> samplers) {
                Object tvs = samplers.get(MASK_SAMPLER);
                if (tvs != null) {
                    int id = getGlTextureId(tvs);
                    if (id > 0) {
                        return id;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            RenderTarget target = ScopeMaskTarget.current();
            if (target != null) {
                Object colorTex = target.getColorTexture();
                if (colorTex != null) {
                    int id = getGlTextureId(colorTex);
                    if (id > 0) {
                        return id;
                    }
                }
                Object colorTexView = target.getColorTextureView();
                if (colorTexView != null) {
                    int id = getGlTextureId(colorTexView);
                    if (id > 0) {
                        return id;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int getProgramId(Object shader) {
        try {
            if (shader == null) {
                return 0;
            }
            Method method = null;
            for (Class<?> c = shader.getClass(); c != null && method == null; c = c.getSuperclass()) {
                try {
                    method = c.getDeclaredMethod("getProgramId");
                } catch (NoSuchMethodException ignored) {
                }
                if (method == null) {
                    try {
                        method = c.getDeclaredMethod("getProgram");
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
            if (method == null) {
                return 0;
            }
            method.setAccessible(true);
            Object id = method.invoke(shader);
            if (id instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable t) {
            logOnce("resolve shader program id", t);
        }
        return 0;
    }

    private static int getGlTextureId(Object obj) {
        if (obj == null) {
            return 0;
        }
        try {
            if (obj.getClass().getSimpleName().contains("TextureViewAndSampler")) {
                Object view = invokeNoArgs(obj, "view");
                return getGlTextureId(view);
            }
            // Check method glId() / getGlId() / iris$getGlId() / id() / getId()
            for (String mName : new String[]{"glId", "getGlId", "iris$getGlId", "id", "getId"}) {
                try {
                    Method m = obj.getClass().getMethod(mName);
                    m.setAccessible(true);
                    Object id = m.invoke(obj);
                    if (id instanceof Number n && n.intValue() > 0) {
                        return n.intValue();
                    }
                } catch (Throwable ignored) {
                }
            }
            // Check method texture() / getTexture()
            for (String mName : new String[]{"texture", "getTexture"}) {
                try {
                    Method m = obj.getClass().getMethod(mName);
                    m.setAccessible(true);
                    Object tex = m.invoke(obj);
                    if (tex != null && tex != obj) {
                        int id = getGlTextureId(tex);
                        if (id > 0) {
                            return id;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            // Check field id / glId across class hierarchy
            for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
                for (String fName : new String[]{"id", "glId", "textureId"}) {
                    try {
                        Field f = c.getDeclaredField(fName);
                        f.setAccessible(true);
                        Object id = f.get(obj);
                        if (id instanceof Number n && n.intValue() > 0) {
                            return n.intValue();
                        }
                    } catch (Throwable ignored) {
                    }
                }
                for (String fName : new String[]{"texture", "tex"}) {
                    try {
                        Field f = c.getDeclaredField(fName);
                        f.setAccessible(true);
                        Object tex = f.get(obj);
                        if (tex != null && tex != obj) {
                            int id = getGlTextureId(tex);
                            if (id > 0) {
                                return id;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            logOnce("extract texture id", t);
        }
        return 0;
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void logOnce(String what, Throwable t) {
        if (!loggedFailure) {
            loggedFailure = true;
            GunMod.LOGGER.warn("[TACZ Scope] Failed to {} in Iris GL command bridge; fallback behavior active.", what, t);
        }
    }
}

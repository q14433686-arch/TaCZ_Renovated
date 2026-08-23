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
    private static final String BODY_PIPELINE = "tacz:pipeline/scope_body_clipped";
    private static final String FLASH_TRANSLUCENT_PIPELINE = "tacz:pipeline/muzzle_flash_translucent";
    private static final String FLASH_SWIRL_PIPELINE = "tacz:pipeline/muzzle_flash_swirl";
    private static final String RETICLE_PIPELINE = "tacz:pipeline/scope_reticle_clipped";
    private static final String UNIFORM_MODE = "tacz_ScopeMaskMode";
    private static final String UNIFORM_SAMPLER = "tacz_ScopeMaskSampler";

    private static boolean loggedFailure;
    private static boolean loggedApply;

    /**
     * {@code GlRenderPass.pipeline} 字段，按 class 缓存。
     *
     * <h3>为什么非缓存不可</h3>
     * {@link #applyToGlRenderPass} 挂在 {@code GlCommandEncoder.trySetup} 上，
     * 也就是<b>每一次 draw call 之前</b>都会跑一遍 —— 开着 Sodium + Iris，
     * 这是每帧成千上万次。原来那版每次都现查：
     * <pre>
     * target.getClass().getDeclaredField(name)   // 每次都新建一个 Field 副本
     * target.getClass().getMethod(name)          // 同上，且要走完整张公共方法表
     * </pre>
     * {@code getDeclaredField}/{@code getMethod} <b>每次调用都返回一份防御性拷贝</b>，
     * 于是每个 draw call 要付 5 次反射查找 + 5 次对象分配 + 5 次 setAccessible 访问检查。
     * 这笔钱与开不开镜无关，是<b>全程</b>都在付的。
     */
    private static Class<?> cachedPassClass;
    private static Field cachedPipelineField;
    private static boolean pipelineFieldResolved;

    /**
     * 「这套 GL 管线对应哪个 mode」的记忆。
     *
     * <p>一个 {@code GlRenderPipeline} 实例对应的 RenderPipeline location 是<b>固定</b>的，
     * 所以判定结果永远不变 —— 逐 draw call 重新用反射取一遍 location、
     * 再 {@code toLowerCase} 出一个新字符串来比较，纯属白花。
     * 按实例身份记住即可。
     */
    private static final java.util.Map<Object, Integer> MODE_BY_PIPELINE = new java.util.IdentityHashMap<>();
    /** 管线实例是有限的（几十个）；真出现异常增长就整体丢弃重来，避免无界增长。 */
    private static final int MODE_CACHE_LIMIT = 512;

    /** {@code GL_MAX_TEXTURE_IMAGE_UNITS} 是驱动常量，问一次就够。 */
    private static int cachedMaxTextureUnits = -1;

    private IrisScopeMaskState() {
    }

    /**
     * Inspects the active GlRenderPass and injects scope-mask uniform state if supported.
     * Invoked from {@code IrisGlCommandEncoderMixin} right before executing a draw command.
     */
    public static void applyToGlRenderPass(Object glRenderPass) {
        if (!IrisCompat.isUsingRenderPack()) {
            return;
        }
        RenderTarget maskTarget = ScopeMaskTarget.current();
        if (maskTarget == null) {
            return;
        }
        int textureId = maskTarget.getColorTextureId();
        if (textureId <= 0) {
            return;
        }

        try {
            if (glRenderPass == null) {
                return;
            }
            // 【快速路径 —— 本方法每次 draw call 都会被调到】
            //
            // mode 只可能在「本帧画了目镜掩码」的帧上变成非 0。既没开镜、上一帧也没开镜，
            // 就不存在任何需要写的 uniform，也不存在需要擦掉的残留 —— 直接回。
            //
            // 为什么「上一帧」也要算进去：Iris 把我们的 scope_body / scope_reticle 管线
            // 映射到它的 HAND 程序上，也就是<b>同一个 GL program</b> 既画镜身（mode=1）
            // 也画枪和手（mode=0）。松开右键的<b>那一帧</b>必须照常跑完整流程，
            // 把这些程序里残留的 mode 擦回 0，否则枪身会带着上一帧的裁剪继续画。
            // 擦干净之后（再下一帧起）uniform 会一直保持 0，于是可以安心早退。
            //
            // 收益：不开镜时，每个 draw call 的开销从「5 次反射 + 2 次 GL 查询」
            // 降到两次布尔读取。这条路径与开不开镜无关地跑在<b>每一帧</b>上，
            // 所以这就是「没开镜时帧数也差」的那一份。
            if (!ScopeMaskRenderer.hasMaskThisFrame() && !ScopeMaskRenderer.hadMaskLastFrame()) {
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

            // 驱动常量，问一次记住 —— 原来这一句也在逐 draw call 做 GL 查询。
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
     *
     * <p>运行期这个 class 实际上恒定，所以「上次是哪个 class」比一下就够，
     * 不必上 map。见 {@link #cachedPipelineField} 的注释。
     */
    private static Field pipelineField(Object glRenderPass) {
        Class<?> cls = glRenderPass.getClass();
        if (cls != cachedPassClass || !pipelineFieldResolved) {
            cachedPassClass = cls;
            cachedPipelineField = null;
            for (Class<?> c = cls; c != null && cachedPipelineField == null; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField("pipeline");
                    f.setAccessible(true);
                    cachedPipelineField = f;
                } catch (NoSuchFieldException ignored) {
                    // 继续往父类找
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
            Field pipelineField = pipelineField(glRenderPass);
            if (pipelineField == null) {
                return 0;
            }
            Object glPipeline = pipelineField.get(glRenderPass);
            if (glPipeline == null) {
                return 0;
            }
            // 同一个管线实例的判定结果恒定，记住即可 —— 省掉后面那四次反射
            // 与一次 toLowerCase 分配。
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
                return 0;
            }
            Object location = invokeNoArgs(renderPipeline, "location");
            if (location == null) {
                return 0;
            }
            String path = location.toString();
            if (path == null) {
                return 0;
            }
            String normalized = path.toLowerCase(Locale.ROOT);
            if (BODY_PIPELINE.equals(normalized)) {
                // 【恒为 1】镜身在孔径内 discard，于是最终画面里孔径那块就是 1× 的世界。
                //
                // 镜内的「放大」不在这里做 —— 那是
                // {@code ScopePipRenderer.compositeAfterLevelUnderShaders()} 的活：
                // 等 Iris 整条管线跑完，直接在最终画面上把孔径内那 1/Z 的小块放大铺满。
                // 而「孔径内是干净的 1× 世界、没有枪」正是这里 discard 换来的前提。
                return 1;
            }
            if (FLASH_TRANSLUCENT_PIPELINE.equals(normalized)
                    || FLASH_SWIRL_PIPELINE.equals(normalized)) {
                return 1;
            }
            if (RETICLE_PIPELINE.equals(normalized)) {
                return 2;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logOnce(String what, Throwable t) {
        if (!loggedFailure) {
            loggedFailure = true;
            GunMod.LOGGER.warn("[TACZ Scope] Failed to {} in Iris GL command bridge; fallback behavior active.", what, t);
        }
    }
}

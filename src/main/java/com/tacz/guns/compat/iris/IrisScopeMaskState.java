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
    private static boolean loggedProgramMismatch;

    /**
     * 本帧当前正在 setup 的 {@code GlRenderPass}，由 {@code IrisGlCommandEncoderMixin} 在
     * {@code GlCommandEncoder#trySetup} 的 <b>HEAD</b> 记下。
     *
     * <h3>为什么必须在 HEAD 记</h3>
     * Iris 的 {@code MixinGlCommandEncoder} 也在 {@code trySetup} 的 <b>RETURN</b> 注入，
     * 并在那里调用 {@code ExtendedShader#iris$setupState}：
     * <pre>
     * &#64;Inject(method = "trySetup", at = &#64;At("RETURN"))
     * private void iris$setupState(GlRenderPass glRenderPass, Collection&lt;String&gt; c, CallbackInfoReturnable&lt;Boolean&gt; cir) {
     *     if (glRenderPass.pipeline.program() instanceof IrisProgram is &amp;&amp; !is.iris$isSetUp()) {
     *         is.iris$setupState(glRenderPass.samplers, ...);   // ← _glUseProgram + samplers.update() + uniforms.update()
     *     }
     * }
     * </pre>
     * 也就是说「Iris 重新绑程序与采样器」和「我们写 mode」挂在<b>同一个注入点</b>上，
     * 谁先谁后完全由 mixin config 的应用顺序决定 —— 那是随已安装 mod 集合变化的，
     * 不是我们能控制的。HEAD 一定早于任何 RETURN 处理器，所以在那里抓 pass 是安全的。
     */
    private static Object currentPass;

    private IrisScopeMaskState() {
    }

    /** 记下本帧当前的 render pass。挂在 {@code trySetup} HEAD，见 {@link #currentPass}。 */
    public static void setCurrentPass(Object glRenderPass) {
        currentPass = glRenderPass;
    }

    /**
     * Iris 每做一次 {@code ExtendedShader#iris$setupState} 就调一次。
     *
     * <h2>【2026-08-27 改】不再无脑写 0</h2>
     * 旧实现在这里把 {@code tacz_ScopeMaskMode} 一律复位成 0。但
     * {@code iris$setupState} 是被 Iris 的 {@code trySetup} RETURN 处理器调起来的，
     * 而我们写 mode 的 {@link #applyToGlRenderPass} 也挂在 {@code trySetup} RETURN 上 ——
     * <b>同一个注入点的两个处理器，执行顺序由 mixin config 应用顺序决定</b>。
     * 一旦我们的处理器排在 Iris 之前，顺序就变成：
     * <ol>
     *   <li>我们写 mode = 1 / 2；</li>
     *   <li>Iris 的处理器跑 {@code iris$setupState} → {@code _glUseProgram} +
     *       {@code samplers.update()} + 本方法 → <b>mode 被写回 0</b>；</li>
     *   <li>此后同一 pass 内 {@code trySetup} 对同一条管线返回 false，
     *       我们的处理器不再被调用 —— mode 就一直是 0。</li>
     * </ol>
     * 结果：整个 pass 的镜身与准星都不裁，表现即「开光影开镜，镜内裁切直接失效」。
     * 装不装某个第三方 mod 会改变 mod 发现顺序、从而改变这两个处理器的先后，
     * 所以症状看起来像是被别的 mod「触发」的。
     *
     * <p>现在改成：在这里<b>按当前 pass 写正确的 mode</b>（非镜身/准星管线自然就是 0，
     * 防泄漏语义不变）。配合 {@link #applyToGlRenderPass} 也在 RETURN 写一次，
     * 两处谁最后跑都得到正确值 —— 与 mixin 应用顺序无关。</p>
     */
    public static void applyToShaderProgram(Object shader) {
        try {
            int programId = getProgramId(shader);
            if (programId <= 0) {
                return;
            }
            // iris$setupState 开头就做了 _glUseProgram(getProgramId())，
            // 所以这里当前程序就是它。不一致就说明调用点变了 —— 宁可不写，
            // 也不能把 A 程序的 location 写进 B 程序（glUniform1i 只作用于当前程序）。
            if (GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM) != programId) {
                if (!loggedProgramMismatch) {
                    loggedProgramMismatch = true;
                    GunMod.LOGGER.warn("[TACZ Scope] Iris program setup ran with a different program bound "
                            + "(expected={}, current={}); skipping the scope-mask uniform write for this setup.",
                            programId, GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
                }
                return;
            }
            writeScopeMaskState(programId, resolveMode(currentPass), currentPass);
        } catch (Throwable t) {
            logOnce("apply scope mask on Iris program setup", t);
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

            // 【2026-08-27 改】uniform 的写入目标只能是【当前程序】——
            // glUniform1i 作用于 glUseProgram 绑定的那个程序，而 uniform location
            // 是【按程序】分配的。旧实现在 GL_CURRENT_PROGRAM 为 0 时退回
            // 「从 glRenderPass.pipeline.program() 取 programId」，然后拿
            // 【那个程序】的 location 去调 glUniform1i —— 那是把 A 程序的
            // location 写进 B 程序（或写进空气），静默无效。现在没有当前程序就直接放弃，
            // 由 applyToShaderProgram 在 Iris 真正 setup 程序时补写。
            int programId = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
            if (programId <= 0) {
                return;
            }
            writeScopeMaskState(programId, mode, glRenderPass);
        } catch (Throwable t) {
            logOnce("apply scope mask to GL render pass", t);
        }
    }

    /**
     * 把 mode / 掩码采样器写进<b>已经绑定为当前程序</b>的 {@code programId}。
     *
     * <p>两个调用点（{@code trySetup} RETURN 与 {@code iris$setupState} RETURN）共用这一份，
     * 保证「最后跑的那个」写的是同一套状态。</p>
     */
    private static void writeScopeMaskState(int programId, int mode, Object glRenderPass) {
        int modeLocation = GL20C.glGetUniformLocation(programId, UNIFORM_MODE);
        if (modeLocation < 0) {
            // 这个程序没有被注入过 tacz 分支（绝大多数 Iris 程序都是这种），直接走人。
            return;
        }

        if (mode == 0) {
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

        int maxUnits = GL11C.glGetInteger(GL20C.GL_MAX_TEXTURE_IMAGE_UNITS);
        int unit = Math.max(15, maxUnits - 1);
        if (!loggedApply) {
            loggedApply = true;
            GunMod.LOGGER.info("[TACZ Scope] Iris scope-mask bridge active (mode={}, textureUnit={}, textureId={}).", mode, unit, textureId);
        }

        // 顺序：先写 uniform，再绑纹理，最后把活跃单元还给 0 ——
        // Iris 的 ProgramSamplers#update() 只重绑它自己那几个单元
        // （WORLD_RESERVED_TEXTURE_UNITS = {0,1,2}，其余从 3 起顺序分配），
        // 而它跑在我们之前，所以我们这一次绑定是本轮最后的写入者。
        GL20C.glUniform1i(modeLocation, mode);
        GL20C.glUniform1i(samplerLocation, unit);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureId);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
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
        // glRenderPass 可能为 null（applyToShaderProgram 在还没记下 pass 时被调用）。
        // 显式判空，而不是让 readField 抛 NPE 再被 catch 吃掉。
        if (glRenderPass != null) {
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

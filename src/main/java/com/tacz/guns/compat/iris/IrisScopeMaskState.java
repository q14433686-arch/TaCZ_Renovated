package com.tacz.guns.compat.iris;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopeMaskTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime bridge for the Iris HAND shader scope-mask integration.
 *
 * <p>This class manages the per-draw uniform state for patched Iris shaders so that
 * custom scope clipping runs accurately when scope body (mode 1) or reticle / in-scope text
 * (mode 2) passes are submitted, while all standard passes (gun body, attachments, player
 * hands, entities) are explicitly set to {@code mode = 0} on every draw call to prevent
 * uniform leakage and random clipping.</p>
 *
 * <h2>新增管线必须来这里登记</h2>
 * <p>光影下裁剪的执行者<b>不是</b>我们自己的片元着色器 —— {@code assignPipeline} 把
 * 自定义管线归入 Iris 的 HAND 程序后，光影包的手部着色器会整条替换掉我们的 fsh。
 * 真正干活的是 {@code IrisShaderCreatorMixin} 注入进光影着色器的
 * {@code tacz_ScopeMaskMode} 分支，而它的开关值由本类<b>按管线 location 查表</b>给出。
 * 于是每新增一条需要裁剪的自定义管线，都必须在本类的 {@code resolveMode} 里登一行 ——
 * 漏登 = mode 恒 0 = 分支休眠 = 光影下该管线完全不裁，且没有任何报错。</p>
 */
public final class IrisScopeMaskState {
    private static final String BODY_PIPELINE = "pipeline/scope_body_clipped";
    private static final String FLASH_TRANSLUCENT_PIPELINE = "pipeline/scope_flash_translucent_clipped";
    private static final String FLASH_SWIRL_PIPELINE = "pipeline/scope_flash_swirl_clipped";
    private static final String RETICLE_PIPELINE = "pipeline/scope_reticle_clipped";
    private static final String RETICLE_EMISSIVE_PIPELINE = "pipeline/scope_reticle_emissive_clipped";
    /**
     * 【镜内文字】与准星同侧（mode 2 = discard 镜外），把文字约束在目镜圆孔内。
     *
     * <p>这条是后补的：{@code pipeline/scope_text_clipped} 随镜内文字一案
     * （本仓 {@code 9d03659} 的移植）新增，当时没进这张表 —— 那时注释还写着
     * 「光影下掩码整体禁用、走不到这里」，而那是 Iris 桥落地<b>之前</b>的旧政策。
     * 桥落地后掩码在光影下是活的，漏登这一行的后果就是：光影下文字<b>完全不裁</b>
     * （mode 恒 0 ⇒ 注入段的分支休眠），表现即「镜内文字穿出目镜」。
     */
    private static final String TEXT_PIPELINE = "pipeline/scope_text_clipped";
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
     * Updates the active Iris shader program uniforms for the current GlRenderPass draw call.
     * If the draw call is {@code scope_body_clipped}, mode is set to 1 (discard inside the ocular).
     * If the draw call is {@code scope_reticle_clipped} or {@code scope_text_clipped},
     * mode is set to 2 (discard outside the ocular — reticle and in-scope text are both
     * constrained to the aperture).
     * Otherwise (gun body, attachments, hands, entities, particles), mode is set to 0.
     */
    public static void applyToGlRenderPass(Object glRenderPass) {
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
            Object glPipeline = readField(glRenderPass, "pipeline");
            if (glPipeline == null) {
                return 0;
            }
            Object renderPipeline = invokeNoArgs(glPipeline, "info");
            if (renderPipeline == null) {
                return 0;
            }
            Object location = invokeNoArgs(renderPipeline, "getLocation");
            if (location == null) {
                return 0;
            }
            String namespace = String.valueOf(invokeNoArgs(location, "getNamespace"));
            String path = String.valueOf(invokeNoArgs(location, "getPath"));
            if (!GunMod.MOD_ID.equals(namespace)) {
                return 0;
            }
            String normalized = path.toLowerCase(Locale.ROOT);
            if (BODY_PIPELINE.equals(normalized) || FLASH_TRANSLUCENT_PIPELINE.equals(normalized)
                    || FLASH_SWIRL_PIPELINE.equals(normalized)) {
                return 1;
            }
            // mode 2 = 只保留镜内（discard 镜外）。准星与镜内文字同侧：
            // 两者都是「浮在镜内画面之上、必须被约束在圆孔内」的一族。
            if (RETICLE_PIPELINE.equals(normalized) || RETICLE_EMISSIVE_PIPELINE.equals(normalized)
                    || TEXT_PIPELINE.equals(normalized)) {
                return 2;
            }
        } catch (Throwable t) {
            logOnce("resolve scope render pass", t);
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
            try {
                Method glIdMethod = obj.getClass().getMethod("glId");
                glIdMethod.setAccessible(true);
                Object id = glIdMethod.invoke(obj);
                if (id instanceof Number n && n.intValue() > 0) {
                    return n.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method irisGlIdMethod = obj.getClass().getMethod("iris$getGlId");
                irisGlIdMethod.setAccessible(true);
                Object id = irisGlIdMethod.invoke(obj);
                if (id instanceof Number n && n.intValue() > 0) {
                    return n.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method textureMethod = obj.getClass().getMethod("texture");
                textureMethod.setAccessible(true);
                Object tex = textureMethod.invoke(obj);
                if (tex != null && tex != obj) {
                    int id = getGlTextureId(tex);
                    if (id > 0) {
                        return id;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Field idField = obj.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(obj);
                if (id instanceof Number n && n.intValue() > 0) {
                    return n.intValue();
                }
            } catch (NoSuchFieldException ignored) {
            }
        } catch (Throwable t) {
            logOnce("extract texture id", t);
        }
        return 0;
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invokeNoArgs(Object target, String name) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static void logOnce(String action, Throwable t) {
        if (!loggedFailure) {
            loggedFailure = true;
            GunMod.LOGGER.warn("[TACZ Scope] Iris scope-mask bridge failed to {}. Scope clipping will fall back for this draw.", action, t);
        }
    }
}

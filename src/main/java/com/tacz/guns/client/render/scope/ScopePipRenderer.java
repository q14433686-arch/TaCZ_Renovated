package com.tacz.guns.client.render.scope;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.compat.iris.IrisCompat;
import com.tacz.guns.compat.iris.IrisScopePipelineCompat;
import com.tacz.guns.compat.physicsmod.PhysicsModCompat;
import com.tacz.guns.compat.sodium.SodiumCompat;
import com.tacz.guns.compat.voxy.VoxyCompat;
import com.tacz.guns.compat.voxy.VoxyScopePipelineCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;

/**
 * 瞄准镜「镜内画中画（PIP）」渲染。
 *
 * <h2>它改变了什么</h2>
 * 移植自上游的原方案里，「镜内放大」其实是<b>整屏</b> FOV 变焦
 * （{@code CameraSetupEvent#applyScopeMagnification} 压小世界 FOV），
 * 镜片只是被掏空、让已经放大的主画面原样透出来。观感上整个屏幕都在变焦，
 * 镜筒外的世界也跟着放大 —— 这不是真实瞄具的样子。
 *
 * <p>本类让镜外保持 1×，只有镜片里是放大的。
 *
 * <h2>两种模式</h2>
 * 由 {@code ScopePipRerender} 切换，合成阶段共用同一条通路：
 * <table border="1">
 *   <caption>镜内画面从哪来</caption>
 *   <tr><th></th><th>重投影（默认）</th><th>二次渲染</th></tr>
 *   <tr><td>镜内画面</td><td>主画面拷贝 + 屏幕空间等比放大</td><td>窄 FOV 真画一遍</td></tr>
 *   <tr><td>镜内分辨率</td><td>屏幕分辨率 ÷ 倍率（8× 下很糊）</td><td>原生</td></tr>
 *   <tr><td>每帧代价</td><td>一次全屏拷贝</td><td>一整遍世界渲染</td></tr>
 *   <tr><td>兼容性</td><td>只读最终颜色缓冲，天然兼容</td><td>要同步第三方渲染器的投影快照，见 {@code SodiumCompat}</td></tr>
 * </table>
 *
 * <h2>重投影模式的原理</h2>
 * 关键的几何事实（见 {@link ScopePipTarget} 的类注释）：透视投影下，
 * 「把 FOV 压窄 Z 倍」在屏幕空间<b>恒等于</b>「绕光轴把画面放大 Z 倍」。
 * 于是镜内那张图不必重新渲染，只要对已经画好的世界按
 * <pre>
 * wideUV = center + (narrowUV − center) / Z
 * </pre>
 * 重采样即可 —— 结果与「用窄 FOV 重渲一遍」<b>逐像素等价</b>（分辨率除外）。
 *
 * <h2>合成时机：掩码画完、镜身画之前</h2>
 * {@code renderItemInHand → renderAllFeatures} 的阶段边界，
 * {@link ScopeMaskRenderer#renderAtPhaseBoundary()} 之后立刻合成：
 * <pre>
 * 掩码画好          → 知道镜内是哪些像素
 * 【合成 PIP】      → 那些像素被贴上放大后的世界
 * executeSolid 起  → 镜身在镜内 discard（掩码），于是 PIP 画面留住；
 *                    准星反向裁剪（只画镜内），于是浮在 PIP 画面之上
 * </pre>
 *
 * <h2>失败即退回，永不加剧</h2>
 * 任何一环出问题都会把 {@link #failed} 置位并永久停用本特性。此后
 * {@code applyScopeMagnification} 的整屏变焦会在<b>下一帧</b>自动恢复 ——
 * 因为它每帧都重新问一次 {@link #suppressesWorldFovZoom()}，不缓存。
 *
 * <h2>移植说明（NeoForge 26.2）</h2>
 * 本类随姊妹分支 {@code TaCZ_Refabricated_Unofficial} 的 {@code 26.2(main)} 同步而来。
 * 相对姊妹分支的差异只有四处，都是为了脱离 Fabric 表面：
 * <ol>
 *   <li>去掉 {@code net.fabricmc.api.Environment}（本仓按包区分端，不靠注解）；</li>
 *   <li>换成 NeoForge 的 {@code RegisterRenderPipelinesEvent} 注册合成管线
 *       （见 {@link #registerPipeline(RegisterRenderPipelinesEvent)}），
 *       注册失败只自我停用，不影响进游戏；</li>
 *   <li>三个兼容层的 {@code FabricLoader#isModLoaded} 换成 {@code ModList#isLoaded}；</li>
 *   <li><b>未</b>同步姊妹分支那套「光影下开镜帧率衰减」的实验设施
 *       （{@code ScopePipResourceProbe}、{@code ScopePipReleaseIdlePipeline}）——
 *       那是一次未结案调查的工具，不是修复，见
 *       {@code docs/records/REFAB_SCOPE_PIP_SYNC_20260830.md}。</li>
 * </ol>
 */
public final class ScopePipRenderer {

    /** 合成用的掩码采样器名。与 {@code ScopeBodyRenderTypes} 那份同名不同 layout，互不影响。 */
    private static final String MASK_SAMPLER = "ScopeMaskSampler";

    /**
     * 合成管线：一个全屏三角形，按倍率重采样场景拷贝，按掩码 discard。
     *
     * <h3>配方来源</h3>
     * 逐项对照 vanilla {@code ENTITY_OUTLINE_BLIT}（{@code RenderPipelines.<clinit>} 实读）
     * 与它的用法 {@code RenderTarget#blitAndBlendToTexture}：
     * <pre>
     * builder(GLOBALS_SNIPPET)
     *     .withVertexShader("core/screenquad")   // 无顶点缓冲，靠 gl_VertexID 造三角形
     *     .withFragmentShader("core/blit_screen")
     *     .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
     *     .withPrimitiveTopology(TRIANGLES)
     * </pre>
     * 这里 {@code GLOBALS_SNIPPET} 是 private，但 {@code POST_PROCESSING_SNIPPET}
     * 就是 {@code builder(GLOBALS_SNIPPET).withPrimitiveTopology(TRIANGLES)} 且是 public。
     *
     * <h3>与 vanilla 的差异</h3>
     * <ul>
     *   <li>片元着色器换成我们的 {@code tacz:core/scope_pip}（重采样 + 掩码 discard）；</li>
     *   <li>多绑一个掩码采样器 layout；</li>
     *   <li>多绑 {@code DYNAMIC_TRANSFORMS}，借 {@code ColorModulator.r} 把倍率送进去
     *       —— 与 {@code ScopeMaskRenderer} 同一套路。</li>
     * </ul>
     *
     * <h3>写掩码取 {@code WRITE_COLOR} 而不是 {@code WRITE_ALL}</h3>
     * 镜内画面只该改颜色。主 target 的 alpha 通道后面还要参与 GUI/后处理的混合，
     * 顺手覆写会引入难查的偏差 —— vanilla 的 {@code ENTITY_OUTLINE_BLIT} 同样传 7。
     *
     * <h3>不声明 DepthStencilState</h3>
     * 合成是纯屏幕空间的覆盖，不参与深度测试，也不该写深度（写了会让紧接着画的
     * 准星被判成遮挡）。{@code RenderPipeline#wantsDepthTexture()} 的判据是
     * 「字段是否为 null」，所以这里必须<b>不设</b>，而不是设一个 ALWAYS_PASS。
     *
     * <h3>为什么是懒加载而不是 static final</h3>
     * 本类的静态初始化会被 {@code CameraSetupEvent} 的 FOV 事件触发 ——
     * 那是<b>每帧、且不看 PIP 开没开</b>的路径。若管线在 {@code <clinit>} 里构建，
     * 一旦构建抛异常（版本漂移、层名对不上）就是 {@code ExceptionInInitializerError}，
     * 连关着 PIP 的玩家都会被带崩。
     */
    @Nullable
    private static RenderPipeline compositePipeline;

    private static RenderPipeline compositePipeline() {
        if (compositePipeline == null) {
            BindGroupLayout maskLayout = BindGroupLayout.builder().withSampler(MASK_SAMPLER).build();
            compositePipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_pip_composite"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "core/scope_pip"))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withBindGroupLayout(maskLayout)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withColorTargetState(new ColorTargetState(
                            Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
                    .build();
        }
        return compositePipeline;
    }

    /**
     * 把合成管线交给 NeoForge 的 26.2 mod-bus 注册点。
     *
     * <p>与 {@code ScopeMaskRenderer#registerPipeline} 同一处调用。这里额外包一层
     * try/catch：注册失败只让 PIP 自我停用，绝不把进游戏一起拖下水 —— 姊妹分支上
     * 这条路径唯一的失效方式就是「管线构建抛异常」，而那不该是致命错误。
     */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        try {
            event.registerPipeline(compositePipeline());
        } catch (Throwable t) {
            failed = true;
            GunMod.LOGGER.error("[TACZ Scope] Could not register the scope PIP composite pipeline; "
                    + "scope PIP is disabled for this session. Everything else is unaffected.", t);
        }
    }

    /** 场景纹理里是否有一张可用的本帧镜内画面。 */
    private static boolean sceneCaptured = false;

    /**
     * 「{@code mainRenderTarget()} 正在被重定向」窗口，同时就是要顶上去的那个 target。
     *
     * <p>只在二次渲染模式下、{@code LevelRenderer#render} 那一次调用期间非空。
     */
    @Nullable
    private static RenderTarget redirectTarget = null;

    /** 二次渲染的投影暂存。首次使用时创建（构造需要 GPU 设备就绪）。 */
    @Nullable
    private static ProjectionMatrixBuffer projectionBuffer;
    private static final Projection PROJECTION = new Projection();
    /** 传给第三方渲染器的窄投影矩阵，复用避免每帧分配。 */
    private static final Matrix4f NARROW_MATRIX = new Matrix4f();

    /**
     * {@code GameRendererMixin} 的 {@code mainRenderTarget()} 注入点读这里。
     *
     * @return 需要顶替主 target 时返回离屏 target；其余一切时候返回 {@code null}（不干预）
     */
    @Nullable
    public static RenderTarget redirectTarget() {
        return redirectTarget;
    }

    /**
     * 是否走「二次渲染」：用窄 FOV 把世界真画一遍，而不是把主画面重投影。
     *
     * <p>这是玩家自选的实验开关，默认关闭：它要在一帧里驱动两遍世界渲染，
     * 第三方渲染器的逐帧状态同步（投影快照、区块 uniform 上传闸、视口）
     * 全靠反射兜底，缺一样就是某一类东西不跟着放大。
     */
    private static boolean rerenderMode() {
        return RenderConfig.SCOPE_PIP_RERENDER != null && RenderConfig.SCOPE_PIP_RERENDER.get();
    }

    /**
     * 是否正处在「镜内那一遍」的世界渲染调用之中（<b>仅光影下</b>为真）。
     *
     * <p>{@code IrisScopeDimensionMixin} 靠它决定要不要把「当前维度」换成瞄具专用的那个 ——
     * 换掉之后 Iris 会给这一遍配一套<b>独立管线</b>，时域状态与主画面彻底分开。
     */
    private static volatile boolean scopePassActive = false;

    /**
     * 本遍是否还额外用了「独立 Iris 管线」。
     *
     * <p>Voxy 的两条兼容策略<b>互斥</b>，取哪条正好看这个标志：
     * 隔离开 → Voxy 在镜内那一遍整体缺席（否则会用错绘制目标）；
     * 隔离关 → Voxy 照常在镜内渲染，只把它的<b>视口</b>分开。
     */
    private static volatile boolean scopePassIsolated = false;

    /** 这一遍把 Voxy 换到瞄具那套了吗（换了就必须换回来）。 */
    private static boolean voxySwapped = false;
    /** 这一遍用的那个 VoxyRenderSystem —— 换回去时必须用同一个实例。 */
    private static Object voxySystemThisPass;

    /** 供 Iris 兼容层查询：当前是不是镜内那一遍。 */
    public static boolean isScopePassActive() {
        return scopePassActive;
    }

    /**
     * 是不是正处在镜内那一次 {@code levelRenderer.render} 里面 ——
     * <b>不分光影开没开</b>。
     *
     * <p>不能复用 {@link #isScopePassActive()}：那个标志只在开光影时为真，
     * 而「这一帧的提交节点被镜内那一遍吃掉了」两条路径都有。
     */
    public static boolean isInsideScopeLevelRender() {
        return insideScopeLevelRender;
    }

    /** 严格套在镜内那一次 {@code levelRenderer.render} 外面。 */
    private static volatile boolean insideScopeLevelRender = false;

    /** 当前由 {@code FeatureRenderDispatcher.prepareFrame} 正在准备的 {@link SubmitNodeStorage}。 */
    private static volatile SubmitNodeStorage currentPreparingStorage = null;

    public static void setCurrentPreparingStorage(SubmitNodeStorage storage) {
        currentPreparingStorage = storage;
    }

    /**
     * 当前正在 drain 的 {@code FeatureRenderPhase} 是否应该跳过清空自己
     * （即保留给随后的主画面那一遍渲染）。
     *
     * <p>仅在同时满足以下条件时返回 {@code true}：
     * <ol>
     *   <li>正处于镜内二次渲染期间（{@link #insideScopeLevelRender} 为真）；</li>
     *   <li>当前正在准备的 {@link SubmitNodeStorage} <b>正是主画面的
     *       {@code LevelRenderer.submitNodeStorage}</b>。</li>
     * </ol>
     * 光影的阴影专用存储<b>绝不</b>被保留，否则提交节点会随开镜帧数无限沉积。
     *
     * @see com.tacz.guns.mixin.client.SimpleFeatureRenderPhaseMixin
     * @see com.tacz.guns.mixin.client.TranslucentFeatureRenderPhaseMixin
     */
    public static boolean shouldPreserveSubmits() {
        if (!insideScopeLevelRender) {
            return false;
        }
        SubmitNodeStorage current = currentPreparingStorage;
        if (current == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) {
            return false;
        }
        try {
            return current == ((com.tacz.guns.mixin.client.LevelRendererAccessor) mc.levelRenderer)
                    .tacz$getSubmitNodeStorage();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 供 Voxy 兼容层查询：镜内这一遍是否用了独立的 Iris 管线。 */
    public static boolean isScopePassIsolated() {
        return scopePassIsolated;
    }

    /**
     * 镜内这一遍要不要让 Voxy「别画」。
     *
     * <p>只有在<b>隔离了 Iris 管线、却没能把 Voxy 切到对应的第二套渲染栈</b>时才为真 ——
     * 那种情况下 Voxy 会用主管线的绘制目标往瞄具管线里画，结果是错乱的远景。
     * 宁可镜内没有 LOD，也不能画错。
     */
    public static boolean shouldSuppressVoxyDraw() {
        return scopePassIsolated && !voxySwapped;
    }

    /**
     * 在帧首把瞄具那套 Iris 管线预先建好，免得它落在第一次开镜的帧中途去编译。
     *
     * <p>判据与镜内那一遍一致，但<b>不看开镜进度</b> —— 预热的全部意义就是赶在开镜之前做完。
     */
    public static void prewarmShaderPipelineIfNeeded() {
        if (failed || !rerenderMode() || !isolatePipeline()) {
            return;
        }
        if (RenderConfig.SCOPE_PIP_ENABLE == null || !RenderConfig.SCOPE_PIP_ENABLE.get()) {
            return;
        }
        if (!allowShaderPacks()) {
            return;
        }
        IrisScopePipelineCompat.prewarmIfNeeded();
    }

    /** 本会话累计跑过多少次镜内那一遍（诊断用）。 */
    private static int scopePassCount = 0;

    public static int scopePassCount() {
        return scopePassCount;
    }

    /**
     * 是否给镜内那一遍配独立的 Iris 管线。
     *
     * <p>不隔离的话，Iris 那一整族「上一帧」uniform 会被一帧推进两次，
     * 主画面的时域效果（TAA、体积云、SSGI）全部失准。
     */
    private static boolean isolatePipeline() {
        return RenderConfig.SCOPE_PIP_ISOLATE_PIPELINE == null
                || RenderConfig.SCOPE_PIP_ISOLATE_PIPELINE.get();
    }

    /** 一旦出过错就永久停用。 */
    private static boolean failed = false;

    /**
     * 镜内那一遍抛异常时，{@code FeatureRenderDispatcher} 的 PreparedFrame 可能还开着没关。
     *
     * <p>{@code LevelRenderer#render} 的形状是「prepareFrame → frameGraph.execute → close」，
     * 中间抛异常 {@code close()} 就不会执行；而 {@code FeatureRenderDispatcher} 全程
     * <b>只有一个</b> PreparedFrame 实例，于是紧接着主画面那一遍会撞上
     * {@code IllegalStateException: PreparedFrame already in use}。
     * 不把它关掉，这里的优雅降级就会变成一句空话 —— 由
     * {@code FeatureRenderDispatcherMixin} 在下一次 {@code prepareFrame} 的 HEAD 消费。
     */
    private static volatile boolean preparedFrameMayBeLeaked = false;

    /**
     * 取走并清掉「上一次镜内那一遍失败了」这个一次性标志。
     *
     * @return 是否刚发生过一次失败的镜内渲染（即：可能有 PreparedFrame 没关）
     */
    public static boolean consumePreparedFrameLeak() {
        if (!preparedFrameMayBeLeaked) {
            return false;
        }
        preparedFrameMayBeLeaked = false;
        return true;
    }

    private static boolean loggedFirstCapture = false;

    private ScopePipRenderer() {
    }

    // ------------------------------------------------------------------
    // 判定
    // ------------------------------------------------------------------

    /**
     * 是否该让 {@code CameraSetupEvent#applyScopeMagnification} 的整屏变焦让位给 PIP。
     *
     * <p>让位是有前提的：<b>只有真正会产出目镜掩码的通道才能走 PIP</b>。
     * 掩码没产出 = 合成阶段贴不进任何像素，此时若还把整屏变焦也关掉，
     * 那把枪就彻底没有放大了 —— 这正是红点/全息与组合镜低倍档的处境。
     *
     * <h3>关于一帧延迟</h3>
     * FOV 事件发生在 {@code extract} 阶段，掩码画在同一帧稍后的手持渲染里，
     * 所以这里读到的是<b>上一帧</b>的结论。这不构成问题：唯一会读到「旧值 false」的
     * 时刻是刚开始抬镜的第一帧，那时 {@code aimingProgress ≈ 0.02}，
     * 变焦公式算出来几乎就是原始 FOV。
     */
    public static boolean suppressesWorldFovZoom() {
        return isEnabledForHeldGun();
    }

    /**
     * 【诊断】本特性有七道闸门，任何一道不满足都表现为「PIP 没生效、还是整屏变焦」——
     * 而这七种情况在画面上<b>完全一样</b>。每次状态<b>变化</b>时打一行日志，
     * 把「为什么没生效」变成一个可读的事实。<b>每种理由只播报一次</b>，避免刷屏。
     */
    private static String lastReportedGate = "";
    private static int gateChangeCount = 0;
    private static final java.util.Set<String> REPORTED_GATES = new java.util.HashSet<>();
    private static final int GATE_LOG_LIMIT = 40;

    private static void reportGate(@Nullable String reason) {
        // 功能整个关着的时候不吭声，否则对不用这个特性的玩家就是刷屏。
        if (RenderConfig.SCOPE_PIP_ENABLE == null || !RenderConfig.SCOPE_PIP_ENABLE.get()) {
            return;
        }
        String now = reason == null ? "ACTIVE" : reason;
        if (now.equals(lastReportedGate)) {
            return;
        }
        lastReportedGate = now;
        if (!REPORTED_GATES.add(now)) {
            return;
        }
        gateChangeCount++;
        if (gateChangeCount <= GATE_LOG_LIMIT) {
            GunMod.LOGGER.info("[TACZ Scope] Scope PIP gate -> {}", now);
            if (gateChangeCount == GATE_LOG_LIMIT) {
                GunMod.LOGGER.info("[TACZ Scope] Scope PIP gate changed {} times; muting further gate logs. "
                        + "If those lines were alternating rapidly, the gate is flapping, not settling.",
                        GATE_LOG_LIMIT);
            }
        }
    }

    private static boolean isEnabledForHeldGun() {
        String reason = inactiveReason();
        reportGate(reason);
        return reason == null;
    }

    /**
     * @return {@code null} 表示七道闸门全过；否则返回卡在哪一道的可读说明
     */
    @Nullable
    private static String inactiveReason() {
        if (failed) {
            return "disabled after a runtime failure (see the earlier error above)";
        }
        if (RenderConfig.SCOPE_PIP_ENABLE == null || !RenderConfig.SCOPE_PIP_ENABLE.get()) {
            return "ScopePipEnable is off";
        }
        // 合成完全依赖目镜掩码提供「镜内是哪些像素」。掩码关掉时 PIP 无从落地，
        // 此时必须让整屏变焦继续生效，否则倍镜等于失效。
        if (RenderConfig.SCOPE_MASK_ENABLE == null || !RenderConfig.SCOPE_MASK_ENABLE.get()) {
            return "ScopeMaskEnable is off (PIP needs the ocular mask to know where the lens is)";
        }
        // 掩码自己判定不安全的环境（目前是 Sulkan），PIP 无条件跟着关。
        if (IrisCompat.shouldDisableScopeMaskUnderShaderPack()) {
            return "the ocular mask reports this renderer as unsafe (Sulkan)";
        }
        // 光影包：默认关，但可由玩家打开。
        //
        // 这里刻意【不】沿用 shouldDisableScopeMaskUnderShaderPack 的结论 ——
        // 那个方法对 Iris 返回 false（本仓专门做了 assignPipeline → Iris HAND program
        // 的兼容层，目镜掩码在光影下是支持的）。PIP 比掩码多两件未验证的事：
        //   1. 抓取时机在 LevelRenderer#render 之后，而延迟管线的 composite 可能还没跑完，
        //      拷到的也许是未着色的中间结果；
        //   2. 合成写的是裸颜色，而光影通常在 tonemap 之前工作于线性/HDR 空间。
        // 两者都只是【观感】风险，所以默认关是保守，不是「已知不兼容」。
        if (IrisCompat.isUsingRenderPack() && !allowShaderPacks()) {
            return "a shader pack is active and ScopePipAllowShaderPacks is off";
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return "no level/player";
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            return "not in first person";
        }
        if (scopeMagnification() <= 1.0f) {
            return "held gun has no scope attachment with zoom > 1 (iron sights and 1x optics keep the old FOV zoom)";
        }
        // 【倍率下限】低倍镜不值得付 PIP 的每帧全屏拷贝（二次渲染模式下是整遍世界重画），
        // 而且 2×/3× 下整屏变焦的观感本来就自然。组合镜按当前档位判定 ——
        // scopeMagnification() 取的就是 zoom[zoomNumber]，切档自动跟随。
        // 这道闸门放在「有倍镜」判据之后：两者都回整屏变焦，但理由要分开报，
        // 玩家调 ScopePipMinMagnification 时才知道是这个旋钮在起作用。
        if (scopeMagnification() < minMagnification()) {
            return "current zoom level " + scopeMagnification() + "x is below ScopePipMinMagnification ("
                    + minMagnification() + "x); using classic full-screen zoom";
        }
        // 最后一道，也是最容易被误判成「PIP 坏了」的一道：目镜掩码到底有没有产出。
        // 【两个快照都认】本方法被三个时机调用，各自该看哪一份并不相同：
        //   FOV 让位（extract 阶段）、镜内抓取（renderLevel 里）→ 本帧掩码还没画，看上一帧；
        //   合成（手部 pass，掩码刚画完）                        → 看本帧。
        if (!ScopeMaskRenderer.hadMaskLastFrame() && !ScopeMaskRenderer.hasMaskThisFrame()) {
            return "no ocular mask produced (sight/red-dot channel, or the mask pass is not running "
                    + "in this renderer setup)";
        }
        return null;
    }

    /**
     * 本帧的瞄具倍率记忆值（NaN = 本帧还没算过）。
     *
     * <p>它每帧要被问将近十次，而每次都要读两次 NBT 并走一次 {@code TimelessAPI}
     * 的 Optional 查表 —— 都是有分配的。算一次就够。由 {@link #beginFrame()} 在帧首清空。
     */
    private static float magnificationThisFrame = Float.NaN;

    /** 每帧清一次帧内记忆值。挂在 {@code GameRenderer#extract} 的 HEAD。 */
    public static void beginFrame() {
        magnificationThisFrame = Float.NaN;
        frameIndex++;
        // 遮光环最终覆盖层的队列也在这里归零 —— 排队发生在手部 pass、
        // 刷新发生在合成之后，万一某一帧没走到刷新点，残留快照不能留到下一帧。
        ScopeFinalRingOverlay.beginFrame();
    }

    /**
     * 帧序号，仅在 {@link #beginFrame()} 递增 —— 也就是 {@code GameRenderer.extract}
     * 的 HEAD，每帧恰好一次。给「隔帧渲染」当时间轴用；用它而不是自己数 render 调用，
     * 是因为 render 侧的调用次数在光影下不是一帧一次（Iris 的手部 pass 一帧两趟）。
     */
    private static long frameIndex = 0;

    /** 上一次真正跑完镜内那一遍的帧序号；从未跑过 = {@code Long.MIN_VALUE}。 */
    private static long lastScopePassFrame = Long.MIN_VALUE;

    /**
     * 那一遍画进的是第几代离屏 target（{@link ScopePipTarget#generation()}）。
     *
     * <p>隔帧复用的前提是「上一帧的成品还躺在同一张纹理里」——
     * 窗口缩放触发 target 重建后，新纹理内容未定义，代数对不上就必须重画。
     */
    private static int lastScopePassGeneration = -1;

    private static float scopeMagnification() {
        float cached = magnificationThisFrame;
        if (!Float.isNaN(cached)) {
            return cached;
        }
        float value = computeScopeMagnification();
        magnificationThisFrame = value;
        return value;
    }

    /**
     * 当前瞄具配件的倍率；没装倍镜（含机瞄）时返回 1。
     *
     * <h3>为什么按配件取而不是 {@code IGun#getAimingZoom}</h3>
     * 后者把机瞄的 {@code ironZoom}（默认枪包里普遍是 1.2~1.5）也算进来。
     * 机瞄<b>没有</b> {@code ocular} 骨骼，掩码永远是空的 —— PIP 贴不进任何像素，
     * 却顺带丢掉了整屏变焦。所以这里只认真正的瞄具配件。
     */
    private static float computeScopeMagnification() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 1.0f;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return 1.0f;
        }
        Identifier scopeId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            scopeId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        }
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            return 1.0f;
        }
        return TimelessAPI.getClientAttachmentIndex(scopeId).map(index -> {
            float[] zoom = index.getZoom();
            if (zoom == null || zoom.length == 0) {
                return 1.0f;
            }
            CompoundTag scopeTag = iGun.getAttachmentTag(stack, AttachmentType.SCOPE);
            int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
            return zoom[Math.floorMod(zoomNumber, zoom.length)];
        }).orElse(1.0f);
    }

    /**
     * 光影路径专用：本帧是否该在 Iris 的帧缓冲里合成镜内画面。
     *
     * <p>与无光影路径共用同一套闸门，只是<b>反过来</b>要求光影处于启用状态。
     */
    public static boolean wantsIrisComposite() {
        if (failed) {
            return false;
        }
        if (RenderConfig.SCOPE_PIP_ENABLE == null || !RenderConfig.SCOPE_PIP_ENABLE.get()) {
            return false;
        }
        if (RenderConfig.SCOPE_MASK_ENABLE == null || !RenderConfig.SCOPE_MASK_ENABLE.get()) {
            return false;
        }
        if (!allowShaderPacks() || !IrisCompat.isUsingRenderPack()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return false;
        }
        if (scopeMagnification() <= 1.0f) {
            return false;
        }
        // 与无光影路径同一道倍率下限（见 inactiveReason 里的说明）。
        if (scopeMagnification() < minMagnification()) {
            return false;
        }
        if (currentAimingProgress() <= minAimingProgress()) {
            return false;
        }
        return ScopeMaskRenderer.hadMaskLastFrame();
    }

    /** 供光影合成路径读取当前倍率。 */
    public static float currentMagnification() {
        return Math.max(1.0f, scopeMagnification());
    }

    // ------------------------------------------------------------------
    // 倍率拆分：世界放大 W × 镜内放大 P = 瞄具倍率 Z
    // ------------------------------------------------------------------

    /**
     * 镜外世界要跟着放大多少倍（满开镜时）。
     *
     * <h3>它解决的是镜内分辨率，而且是唯一能真正解决的办法</h3>
     * 镜内画面是主画面中心那一小块放大来的：放大 Z 倍，就只有 {@code 1/Z} 的屏幕像素可用 ——
     * 这是<b>信息量</b>上限，锐化、双三次重建都只能改善主观锐度。
     * 唯一的出路是<b>让镜内少放大一点</b>：世界先放大 W，镜内只需再放大 {@code Z/W}。
     *
     * <p>取值按 {@code 世界 = Z^share、镜内 = Z^(1-share)} 拆分：倍率是<b>相乘</b>的，
     * 于是 share 在 [0,1] 上每一点都有意义、且与瞄具倍率无关 ——
     * 早前的「绝对上限」写法会被 Z 夹住，任何 ≥ Z 的取值都让镜内倍率退化成 1。
     *
     * <h3>二次渲染模式恒返回 1</h3>
     * 那条路的镜内像素是用窄 FOV <b>真画出来的</b>，本来就没有分辨率上限。
     */
    private static float worldZoomTarget() {
        if (rerenderMode()) {
            return 1.0f;
        }
        if (RenderConfig.SCOPE_PIP_WORLD_ZOOM_SHARE == null) {
            return 1.0f;
        }
        float share = Mth.clamp(RenderConfig.SCOPE_PIP_WORLD_ZOOM_SHARE.get().floatValue(), 0.0f, 1.0f);
        if (share <= 0.0f) {
            return 1.0f;
        }
        float zoom = Math.max(1.0f, scopeMagnification());
        return (float) Math.pow(zoom, share);
    }

    /**
     * 当前这一帧世界实际放大了多少 —— 与 {@code CameraSetupEvent} 里那条
     * {@code 1 + (zoom-1)·progress} 完全同式，两边必须逐帧一致，否则镜内外会打架。
     */
    public static float worldZoomAtProgress(float aimingProgress) {
        return 1.0f + (worldZoomTarget() - 1.0f) * Mth.clamp(aimingProgress, 0.0f, 1.0f);
    }

    /** 供 {@code CameraSetupEvent} 使用：PIP 生效时世界该放大多少。 */
    public static float currentWorldZoom() {
        return worldZoomAtProgress(currentAimingProgress());
    }

    /**
     * 供 meshloader 的距离闸门用：当前画面把远处放大了多少倍（裸眼 = 1）。
     *
     * <h3>为什么距离闸门需要它（2026-09-02 实机回报）</h3>
     * poly 层的提交发生在 extract 阶段、每帧一次，{@code MeshMaxRenderDistance}
     * 与 {@code MeshWorldFullDetailDistance} 都按<b>主相机裸眼距离</b>判定；而
     * 镜内那一遍复用同一批提交节点（SimpleFeatureRenderPhaseMixin 的「节点留给
     * 主画面」机制反向同理），不会重新过闸门。于是 4x 镜下 48 格的 poly 上限
     * 观感只有 12 格、16 格全模豁免观感只有 4 格 —— 玩家举镜看到的掉落物/
     * 第三人称 mesh 枪几乎必然是「未烘焙」的立方体。
     *
     * <p>开镜时把闸门距离乘上这个系数即可：物体在镜内的<b>角尺寸</b>正是放大了
     * 这么多倍，「多远该有细节」本就该按角尺寸算。取值随开镜进度渐变
     * （与整屏变焦/PIP 的 {@code 1+(zoom-1)·progress} 同式），收镜自动回 1，
     * 经典整屏变焦与 PIP 两种模式同样适用（两者都放大了世界观感）。</p>
     */
    public static float currentDetailZoom() {
        float progress = currentAimingProgress();
        if (progress <= 0.0f) {
            return 1.0f;
        }
        float magnification = Math.max(1.0f, scopeMagnification());
        return 1.0f + (magnification - 1.0f) * progress;
    }

    private static float currentAimingProgress() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return 0.0f;
        }
        return Mth.clamp(IClientPlayerGunOperator.fromLocalPlayer(player)
                .getClientAimingProgress(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)), 0.0f, 1.0f);
    }

    // ------------------------------------------------------------------
    // 抓取本帧的世界画面
    // ------------------------------------------------------------------

    /**
     * 把刚画完的世界拷进离屏纹理，留给稍后的合成阶段重采样。
     *
     * <h3>为什么必须卡在这一刻</h3>
     * 由 {@code GameRendererMixin} 注入在 {@code renderLevel} 里
     * {@code LevelRenderer#render} 那次调用<b>之后</b>：
     * 再早世界还没画完；再晚（越过 {@code renderItemInHand}）拷贝里会混进枪和手，
     * 镜片里就会出现一把缩小的枪。
     *
     * <p>已知的小缺口：发光实体描边由 {@code levelRenderer.doEntityOutline()}
     * 在 {@code renderLevel} 返回<b>之后</b>才贴到主画面，所以镜内看不到那圈描边。
     */
    public static void captureScene(Minecraft mc) {
        if (failed || rerenderMode()) {
            // 二次渲染模式下镜内画面由 renderScopeView 产出，不走拷贝。
            return;
        }
        // 光影下这个注入点的含义不同：Iris 把手部渲染搬进了 LevelRenderer#render 内部，
        // 于是「LevelRenderer#render 之后」= 整条 Iris 管线跑完之后 —— 抓到的正是最终画面。
        // 合成随后由 compositeAfterLevelUnderShaders() 在同一处完成。
        if (!isEnabledForHeldGun() || currentAimingProgress() <= minAimingProgress()) {
            sceneCaptured = false;
            return;
        }
        try {
            RenderTarget main = mc.gameRenderer.mainRenderTarget();
            if (main == null) {
                sceneCaptured = false;
                return;
            }
            GpuTexture source = main.getColorTexture();
            if (source == null || source.isClosed()) {
                sceneCaptured = false;
                return;
            }
            int w = source.getWidth(0);
            int h = source.getHeight(0);
            // 格式取自源纹理本身：copyTextureToTexture 要求两端一致，
            // 而主 target 的格式并不保证永远是 RGBA8_UNORM。
            TextureTarget copy = ScopePipTarget.getOrCreate(w, h, source.getFormat(), false);
            if (copy == null) {
                failed = true;
                sceneCaptured = false;
                return;
            }
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(source, copy.getColorTexture(), 0, 0, 0, 0, 0, w, h);
            sceneCaptured = true;
            if (!loggedFirstCapture) {
                loggedFirstCapture = true;
                GunMod.LOGGER.info("[TACZ Scope] Scope PIP active: reprojecting a {}x{} scene copy at {}x magnification.",
                        w, h, scopeMagnification());
                // 【把「旋钮为什么没反应」变成一行可读的事实】重投影模式不存在
                // 第二遍渲染，ScopePipResolutionScale 在这条路上无物可缩 ——
                // 玩家调它没反应不是 bug，是作用域如此。只在旋钮偏离 1.0 时说，
                // 且每局一次（跟着 loggedFirstCapture 走）。
                if (resolutionScale() < 0.999d) {
                    GunMod.LOGGER.info("[TACZ Scope] Note: ScopePipResolutionScale ({}) has no effect in "
                                    + "reprojection mode -- the lens is a resample of the already-rendered main "
                                    + "frame, so there is no second render to downscale. It only applies with "
                                    + "ScopePipRerender=true and no shader pack.",
                            resolutionScale());
                }
            }
        } catch (Exception e) {
            failed = true;
            sceneCaptured = false;
            GunMod.LOGGER.error("[TACZ Scope] Scope PIP scene capture failed; PIP disabled, "
                    + "falling back to whole-screen FOV zoom.", e);
        }
    }

    // ------------------------------------------------------------------
    // 二次渲染（ScopePipRerender = true）
    // ------------------------------------------------------------------

    /**
     * 【二次渲染模式】用窄 FOV 把世界再画一遍到离屏 target，得到<b>原生分辨率</b>的镜内画面。
     *
     * <h2>两处以上必须同时改的投影</h2>
     * <ol>
     *   <li>{@code RenderSystem.setProjectionMatrix(...)} —— 原版路径（实体、粒子、天空）看这个；</li>
     *   <li>{@link SodiumCompat#overrideProjection} —— 接管地形的渲染器只看它自己的快照；</li>
     *   <li>{@code CameraRenderState.projectionMatrix} —— Voxy 的 LOD 地形取值处；</li>
     *   <li>{@link PhysicsModCompat#overrideProjection} —— Physics Mod 的可动方块。</li>
     * </ol>
     */
    public static void renderScopeView(Minecraft mc,
                                       GraphicsResourceAllocator allocator,
                                       FogRenderer fogRenderer,
                                       GameRenderState gameRenderState,
                                       DeltaTracker deltaTracker) {
        if (failed || !rerenderMode()) {
            return;
        }
        if (redirectTarget != null) {
            // 理论不可达（我们调的是 levelRenderer.render，不是 renderLevel），
            // 但重入一次就会把离屏 target 画花且极难定位。
            return;
        }
        if (!isEnabledForHeldGun() || currentAimingProgress() <= minAimingProgress()) {
            sceneCaptured = false;
            return;
        }
        CameraRenderState camera = gameRenderState.levelRenderState.cameraRenderState;
        if (camera == null || !camera.initialized || camera.isPanoramicMode) {
            sceneCaptured = false;
            return;
        }
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) {
            sceneCaptured = false;
            return;
        }
        GpuTexture mainColor = main.getColorTexture();
        if (mainColor == null || mainColor.isClosed()) {
            sceneCaptured = false;
            return;
        }
        // 光影下不重定向渲染目标（Iris 画进自己的 colortex，最后由
        // FinalPassRenderer 合成到主帧缓冲），所以离屏纹理只当「拷贝目的地」用，
        // 不需要深度附件；无光影下我们要把整遍世界画进它，没有深度就没有遮挡关系。
        boolean iris = IrisCompat.isUsingRenderPack();
        float scale = iris ? 1.0f : (float) resolutionScale();
        int targetWidth = Math.max(1, Math.round(main.width * scale));
        int targetHeight = Math.max(1, Math.round(main.height * scale));
        TextureTarget pip = ScopePipTarget.getOrCreate(targetWidth, targetHeight, mainColor.getFormat(), !iris);
        if (pip == null) {
            failed = true;
            sceneCaptured = false;
            return;
        }

        // 【隔帧渲染 · ScopePipRerenderInterval】距上次真跑不足 N 帧，就直接复用
        // 离屏纹理里躺着的上一帧成品 —— 合成阶段照常执行，只有「世界那一遍」被省掉。
        //
        // 这是光影下唯一砍得到大头的杠杆：帧率对半的根因是整条 Iris 管线每帧跑两遍，
        // N=2 时那份额外开销直接减半，而镜外主画面永远满帧率。
        //
        // 复用的前提有两个，缺一必须重画：
        //   1. 代数一致 —— getOrCreate 刚因窗口缩放/光影切换重建过 target 的话，
        //      新纹理内容是未定义的，端出去就是花屏；
        //   2. 帧差 < N —— frameIndex 只在 extract HEAD 递增，每帧恰一次，
        //      不受 Iris 一帧两趟手部 pass 的影响。
        //
        // 收镜再开镜不强制重画：闸门（aimingProgress ≤ min）期间本方法根本不会走到
        // 这里，lastScopePassFrame 停在旧值，帧差早已 ≥ N，自然落到重画分支。
        // lastScopePassFrame >= 0 挡首帧：初始哨兵是 Long.MIN_VALUE，
        // frameIndex - MIN_VALUE 会【上溢成负数】、误判成「帧差 < N」——
        // 代数守卫（-1 != 首次分配后的 >=1）虽恰好也能挡住，但不能靠巧合。
        int interval = rerenderInterval();
        if (interval > 1
                && lastScopePassFrame >= 0
                && lastScopePassGeneration == ScopePipTarget.generation()
                && frameIndex - lastScopePassFrame < interval) {
            sceneCaptured = true;
            ScopePipTrace.mark("SCOPE-PASS SKIPPED (ScopePipRerenderInterval reuse)");
            return;
        }

        // 存档。刻意不用 RenderSystem.backupProjectionMatrix()：那对 save/restore 共用
        // 一个静态槽位，若二次渲染内部也用了它，我们的还原就会拿到别人的值。
        GpuBufferSlice savedProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType savedProjectionType = RenderSystem.getProjectionType();
        Matrix4f savedCameraProjection = new Matrix4f(camera.projectionMatrix);
        boolean cameraProjectionPatched = false;
        boolean physicsPatched = false;
        boolean sodiumPatched = false;
        try {
            if (!buildNarrowProjection(camera, pip)) {
                sceneCaptured = false;
                return;
            }
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(PROJECTION), ProjectionType.PERSPECTIVE);
            sodiumPatched = SodiumCompat.overrideProjection(NARROW_MATRIX);
            // 就地 set 而不是换引用：这个 Matrix4f 对象被别处持有着。
            camera.projectionMatrix.set(NARROW_MATRIX);
            cameraProjectionPatched = true;
            physicsPatched = PhysicsModCompat.overrideProjection(NARROW_MATRIX);

            boolean renderSky = !mc.gui.hud.getBossOverlay().shouldCreateWorldFog();
            ScopePipTrace.mark(iris
                    ? "SCOPE-PASS BEGIN (iris: full pipeline, captured from the main target)"
                    : "SCOPE-PASS BEGIN (redirect active)");
            redirectTarget = iris ? null : pip;
            scopePassActive = iris;
            scopePassIsolated = iris && isolatePipeline();
            // 【这里只换，绝不建】建栈必须发生在预热那个窗口里（见 IrisScopePipelineCompat）：
            // 在这里建过一次，代价是重复建会抛 "Pipeline data already bound"，
            // 而 Voxy 捕获后会拆掉整个 Iris。
            voxySystemThisPass = scopePassIsolated ? VoxyCompat.renderSystem() : null;
            voxySwapped = voxySystemThisPass != null
                    && VoxyScopePipelineCompat.swapIn(voxySystemThisPass);
            insideScopeLevelRender = true;
            try {
                mc.levelRenderer.render(
                        allocator,
                        deltaTracker,
                        // 方块高亮线框：镜内不画，屏幕空间的描边在镜内没有意义。
                        false,
                        camera,
                        camera.viewRotationMatrix,
                        fogRenderer.getBuffer(FogRenderer.FogMode.WORLD),
                        camera.fogData.color,
                        renderSky);
            } finally {
                // 必须最先清：从这里往后（主画面那一遍）各 phase 要恢复「取完就清空」的原样。
                currentPreparingStorage = null;
                insideScopeLevelRender = false;
                // 先把 Voxy 换回主管线，再清标志 —— 顺序不能反。
                if (voxySwapped) {
                    VoxyScopePipelineCompat.swapOut(voxySystemThisPass);
                    voxySwapped = false;
                }
                voxySystemThisPass = null;
                scopePassActive = false;
                scopePassIsolated = false;
                redirectTarget = null;
                ScopePipTrace.mark("SCOPE-PASS END");
            }
            if (iris) {
                // 立刻拷走：紧接着 vanilla 那一遍会把主帧缓冲整个重画。
                GpuTexture shaded = main.getColorTexture();
                if (shaded == null || shaded.isClosed()) {
                    sceneCaptured = false;
                    return;
                }
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                        shaded, pip.getColorTexture(), 0, 0, 0, 0, 0,
                        Math.min(main.width, pip.width), Math.min(main.height, pip.height));
            }
            sceneCaptured = true;
            scopePassCount++;
            // 隔帧渲染的书签：本帧真跑了、成品在这一代 target 里。
            lastScopePassFrame = frameIndex;
            lastScopePassGeneration = ScopePipTarget.generation();
            if (!loggedFirstCapture) {
                loggedFirstCapture = true;
                GunMod.LOGGER.info("[TACZ Scope] Scope PIP second-render pass active: {}x{} at {}x "
                                + "(sodium terrain projection synced: {}).",
                        pip.width, pip.height, scopeMagnification(), sodiumPatched);
                // 【旋钮作用域播报 · 每局一次】上面那行的 WxH 就是缩放是否生效的铁证：
                // 无光影 + scale=0.75 时它应当是主帧缓冲的 3/4。光影下则被强制 1.0 ——
                // Iris 画进自己那套 colortex，这张离屏纹理只是成品的拷贝目的地，
                // 缩小它省不掉 Iris 那遍的任何真实开销。把这件事明说，免得玩家
                // 调了没反应以为是 bug。
                if (iris && resolutionScale() < 0.999d) {
                    GunMod.LOGGER.info("[TACZ Scope] Note: ScopePipResolutionScale ({}) is forced to 1.0 under "
                                    + "shader packs -- Iris renders into its own buffers at native size and this "
                                    + "offscreen target is only a copy destination. To cut the scope pass cost "
                                    + "under shaders, use ScopePipShadowScale and/or ScopePipRerenderInterval.",
                            resolutionScale());
                }
            }
        } catch (Exception e) {
            failed = true;
            sceneCaptured = false;
            // 【降级要真的算数】上面那句 levelRenderer.render 若是在 frame graph
            // 执行途中抛的，本帧的 PreparedFrame 就还开着 —— 不关掉的话，
            // 紧随其后的主画面那一遍会以 "PreparedFrame already in use" 当场崩游戏。
            preparedFrameMayBeLeaked = true;
            GunMod.LOGGER.error("[TACZ Scope] Scope PIP second-render pass failed; PIP disabled, "
                    + "falling back to whole-screen FOV zoom.", e);
        } finally {
            if (cameraProjectionPatched) {
                camera.projectionMatrix.set(savedCameraProjection);
            }
            if (physicsPatched) {
                PhysicsModCompat.restoreProjection();
            }
            if (sodiumPatched) {
                SodiumCompat.restoreProjection();
            }
            // 【关键】把 Sodium「本帧区块 uniform 已上传」的闸重新打开。
            // 不做这一步，紧随其后的 vanilla 那一遍调 update() 会被早退挡掉，
            // 主画面的地形就继续用我们刚上传的【窄投影】绘制。
            SodiumCompat.resetChunkUniformUpload();
            RenderSystem.setProjectionMatrix(savedProjection, savedProjectionType);
        }
    }

    /**
     * 按瞄具倍率算出镜内那一遍的透视投影，写进 {@link #PROJECTION} 与 {@link #NARROW_MATRIX}。
     *
     * <h3>基准 FOV 从投影矩阵反解，而不是读 {@code options.fov()}</h3>
     * 当帧的世界 FOV 还叠着疾跑/药水/{@code fovEffectScale} 与我们自己的平滑。
     * 而透视矩阵的 {@code m11 = 1 / tan(fovY / 2)} 是恒等式，反解出来的就是
     * vanilla 本帧真正用的那个 FOV。
     */
    private static boolean buildNarrowProjection(CameraRenderState camera, TextureTarget pip) {
        float m11 = camera.projectionMatrix.m11();
        if (!Float.isFinite(m11) || m11 <= 1.0e-4f) {
            return false;
        }
        double baseFov = Math.toDegrees(2.0 * Math.atan(1.0 / m11));
        double pipFov = MathUtil.magnificationToFov(scopeMagnification(), baseFov);
        if (!Double.isFinite(pipFov) || pipFov <= 0.0) {
            return false;
        }
        // 近/远平面与 vanilla 相机逐值一致。
        PROJECTION.setupPerspective(0.05f, camera.depthFar, (float) pipFov, pip.width, pip.height);
        PROJECTION.getMatrix(NARROW_MATRIX);
        if (projectionBuffer == null) {
            projectionBuffer = new ProjectionMatrixBuffer("tacz scope pip");
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 合成
    // ------------------------------------------------------------------

    /**
     * 在阶段边界把放大后的世界贴进目镜孔径。
     *
     * <p>调用点在 {@code FeatureRenderDispatcherMixin}，紧跟
     * {@link ScopeMaskRenderer#renderAtPhaseBoundary()} 之后。
     */
    public static void compositeAtPhaseBoundary() {
        if (failed || !sceneCaptured) {
            return;
        }
        if (irisOwnsLens()) {
            // 见 captureScene：光影下这条路整条让开，镜片只能有一个主人。
            return;
        }
        // 【诊断】只跑镜内那一遍、不合成。
        if (RenderConfig.SCOPE_PIP_DEBUG_NO_COMPOSITE != null
                && RenderConfig.SCOPE_PIP_DEBUG_NO_COMPOSITE.get()) {
            return;
        }
        if (!ScopeMaskRenderer.isInHandPass()) {
            return;
        }
        // 每帧只合成一次。Iris 的 HandRenderer 一帧跑两次手部 pass，
        // 第二次再合成会把 solid 阶段已经画进孔径的东西（蚀刻准星等）整片盖掉。
        if (!ScopeMaskRenderer.claimCompositeSlot()) {
            return;
        }
        if (!isEnabledForHeldGun()) {
            return;
        }
        // 二次渲染模式下离屏纹理【已经】是窄 FOV 画出来的，倍率传 1 = 直接逐像素取用。
        // 重投影模式要除掉世界已经放大的那一份。
        runComposite(rerenderMode()
                ? 1.0f
                : Math.max(1.0f, scopeMagnification()) / worldZoomAtProgress(currentAimingProgress()));
    }

    /**
     * 【光影路径 · 屏幕空间】Iris 整条管线跑完之后，直接在<b>最终画面</b>上做镜内放大。
     *
     * <h2>为什么这条路比「在 pack 的着色器里采样 colortex」可靠得多</h2>
     * 那条路要求我们猜中「已着色的场景此刻躺在哪张 colortex 里」，而这个答案
     * <b>逐 pack 不同</b>。这里读的是 Iris <b>已经完工</b>的那张图：
     * 光照、体积雾、色调映射全部就位，与镜外像素<b>逐字节同源</b>。
     *
     * <h2>镜内为什么不会出现一把缩小的枪</h2>
     * 镜身在孔径内是 {@code discard} 的（模式 1），所以最终画面里
     * <b>孔径那块就是 1× 的世界</b>，没有枪。而重投影采样点是
     * {@code center + (uv-center)/Z}：uv 取遍孔径时，采样点只覆盖以中心为原点、
     * 半径缩小到 {@code 1/Z} 的一小块 —— 那块<b>整个落在孔径内部</b>。
     */
    public static void compositeAfterLevelUnderShaders() {
        if (failed || !sceneCaptured || !irisOwnsLens()) {
            return;
        }
        if (RenderConfig.SCOPE_PIP_DEBUG_NO_COMPOSITE != null
                && RenderConfig.SCOPE_PIP_DEBUG_NO_COMPOSITE.get()) {
            return;
        }
        // 每帧一次。这里不看 isInHandPass —— 调用点在手部 pass 之外。
        if (!ScopeMaskRenderer.claimCompositeSlot()) {
            return;
        }
        if (!isEnabledForHeldGun()) {
            return;
        }
        // 让诊断 trace 知道「这一帧真的合成了」—— 光影下没有 SCOPE-PASS。
        ScopePipTrace.mark("PIP COMPOSITE (screen space, after the Iris pipeline finished)");
        // 【倍率跟着开镜进度走】抬镜过程中瞄具还没移到屏幕中心，而重投影的采样点
        // 恒定绕屏幕中心收缩，此时那块可能还压在枪身上。
        if (rerenderMode()) {
            runComposite(1.0f);
            return;
        }
        float progress = currentAimingProgress();
        float zoom = Math.max(1.0f, scopeMagnification());
        runComposite((1.0f + (zoom - 1.0f) * progress) / worldZoomAtProgress(progress));
    }

    /** 合成本体：读离屏拷贝、按倍率重投影、只写进目镜孔径。两个调用点共用。 */
    private static void runComposite(float magnification) {
        TextureTarget scene = ScopePipTarget.current();
        RenderTarget mask = ScopeMaskTarget.current();
        if (scene == null || mask == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (main == null) {
            return;
        }
        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            // 不挂深度附件：合成不测试也不写深度（见 compositePipeline 的注释）。
            // Optional.empty() = 不清空，保留主 target 已有的画面。
            //
            // 注意这里【读】的是 scene 那张拷贝、【写】的是主 target ——
            // 两者是不同的纹理。若省掉拷贝直接采样主 target，就成了同一个 pass 里
            // 又读又写同一张纹理，那是未定义行为。
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "tacz_scope_pip_composite",
                    main.getColorTextureView(),
                    Optional.empty())) {
                pass.setPipeline(compositePipeline());
                // Globals（ScreenSize）由它提供，收缩带的纵横比修正要用。
                RenderSystem.bindDefaultUniforms(pass);
                // 倍率与锐化强度经 ColorModulator 的 r/g 送进着色器。
                pass.setUniform("DynamicTransforms",
                        RenderSystem.getDynamicUniforms().writeTransform(
                                new Matrix4f(),
                                new Vector4f(magnification, sharpness(), paintLensFlag(), 1.0f)));
                // 场景拷贝：LINEAR。着色器里的 Catmull-Rom 重建用一组硬件双线性抽头拼出来。
                pass.bindTexture("InSampler", scene.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                // 掩码：NEAREST。二值数据，线性过滤会在边缘产生 0.5 附近的中间值。
                pass.bindTexture(MASK_SAMPLER, mask.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                // 无顶点缓冲的全屏三角形：core/screenquad.vsh 用 gl_VertexID 造顶点。
                pass.draw(3, 1, 0, 0);
            }
        } catch (Exception e) {
            failed = true;
            GunMod.LOGGER.error("[TACZ Scope] Scope PIP composite failed; PIP disabled, "
                    + "falling back to whole-screen FOV zoom.", e);
        }
    }

    // ------------------------------------------------------------------
    // 配置读取（配置可能尚未加载，一律带 null 兜底）
    // ------------------------------------------------------------------

    private static float minAimingProgress() {
        return RenderConfig.SCOPE_PIP_MIN_AIMING_PROGRESS == null
                ? 0.05f : RenderConfig.SCOPE_PIP_MIN_AIMING_PROGRESS.get().floatValue();
    }

    /**
     * PIP 的最低启用倍率（当前档位倍率低于它就不做 PIP）。
     *
     * <p>配置可能尚未加载，一律带 null 兜底；默认值与姊妹分支一致（4.0）。
     */
    private static float minMagnification() {
        return RenderConfig.SCOPE_PIP_MIN_MAGNIFICATION == null
                ? 4.0f : RenderConfig.SCOPE_PIP_MIN_MAGNIFICATION.get().floatValue();
    }

    private static float sharpness() {
        return RenderConfig.SCOPE_PIP_SHARPNESS == null
                ? 0.5f : RenderConfig.SCOPE_PIP_SHARPNESS.get().floatValue();
    }

    /**
     * 合成着色器的诊断标志：0 = 把覆盖区涂成品红，1 = 正常出图。
     * 走 {@code ColorModulator.b}，那个通道本来就是常量 1.0 的空闲载体。
     */
    private static float paintLensFlag() {
        boolean paint = RenderConfig.SCOPE_PIP_DEBUG_PAINT_LENS != null
                && RenderConfig.SCOPE_PIP_DEBUG_PAINT_LENS.get();
        return paint ? 0.0f : 1.0f;
    }

    /**
     * 光影开着时，镜片归 Iris 那条 shader 注入路（模式 3）管，本类的
     * 「拷贝主画面 + 全屏重投影」通道必须整条让开。
     */
    private static boolean irisOwnsLens() {
        return IrisCompat.isUsingRenderPack();
    }

    /** 镜内那一遍每 N 帧才真跑一次（1 = 每帧）。见 {@code RenderConfig.SCOPE_PIP_RERENDER_INTERVAL}。 */
    private static int rerenderInterval() {
        return RenderConfig.SCOPE_PIP_RERENDER_INTERVAL == null
                ? 1 : RenderConfig.SCOPE_PIP_RERENDER_INTERVAL.get();
    }

    public static double resolutionScale() {
        return RenderConfig.SCOPE_PIP_RESOLUTION_SCALE == null
                ? 0.75d : RenderConfig.SCOPE_PIP_RESOLUTION_SCALE.get();
    }

    private static boolean allowShaderPacks() {
        return RenderConfig.SCOPE_PIP_ALLOW_SHADER_PACKS != null
                && RenderConfig.SCOPE_PIP_ALLOW_SHADER_PACKS.get();
    }
}

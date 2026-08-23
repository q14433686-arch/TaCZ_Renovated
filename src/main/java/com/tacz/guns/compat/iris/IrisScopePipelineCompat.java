package com.tacz.guns.compat.iris;

import com.tacz.guns.GunMod;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * 提前建好「瞄具专用的 Iris 管线」，免得第一次开镜时在帧中途卡顿。
 *
 * <h2>它解决的问题</h2>
 * 隔离模式下，镜内那一遍用的是 {@code tacz:scope_pip} 维度的独立管线。
 * 原本由 Iris 懒加载 —— 也就是玩家<b>第一次开镜的那一帧</b>，Iris 发现这个维度
 * 没管线，当场编译整份 shaderpack。实测表现为开镜瞬间严重掉帧（甚至一秒级卡顿），
 * 并且中途重置全局帧计数。
 *
 * <h2>做法：在进世界后的安全位置预热</h2>
 * 每一帧的 {@code GameRenderer#extract}（帧首、在世界渲染之前、不在任何 render pass 里），
 * 只要检测到「还没建过」，就调 Iris 的
 * <pre>
 * PipelineManager#preparePipeline(NamespacedId.of("tacz", "scope_pip"))
 * </pre>
 * 把管线在后台编译好并存进 Iris 的 map。第一次开镜时直接命中缓存，毫无卡顿。
 *
 * <h2>【关键】必须把当前管线指回去</h2>
 * {@code preparePipeline} 的副作用是把「当前管线」设为瞄具那套。
 * 所以预热完<b>必须</b>用原先的<b>真实维度</b>调一次把它指回主管线 ——
 * 那次是命中缓存，不会重建。漏掉这一步，接下来整帧都会用瞄具管线渲染主画面。
 *
 * <p>全程反射，Iris 不在或结构变了就静默放弃（退回懒加载，只是第一次开镜会卡）。
 */
public final class IrisScopePipelineCompat {

    private static final String NAMESPACED_ID = "net.irisshaders.iris.shaderpack.materialmap.NamespacedId";

    private static Object scopeDimensionId;
    private static boolean idResolveFailed;

    private static Method getPipelineManager;
    private static Method getPipelineNullable;
    private static Method preparePipeline;
    private static Method getCurrentDimension;
    private static boolean handlesResolved;
    private static boolean handlesFailed;

    /** 已经为哪一套主管线预热过。主管线换了（重载光影包/切维度）就要重来。 */
    private static Object prewarmedAgainst;
    /** Voxy 那一套是否已经尘埃落定（建好了，或确定用不上）。稳态快速路径就看它。 */
    private static boolean voxyStackSettled;
    /**
     * 是否正处在「瞄具那套 Iris 管线的构造过程」之中。
     *
     * <p>只有这一小段窗口里，{@code PackShadowDirectives.getResolution()} 的返回值
     * 才会决定<b>瞄具管线</b>那张阴影贴图的尺寸。窗口之外必须恢复原值，
     * 否则会把主画面的阴影一起改小。
     */
    private static volatile boolean buildingScopePipeline;

    /** 供 {@code IrisShadowResolutionMixin} 查询。 */
    public static boolean isBuildingScopePipeline() {
        return buildingScopePipeline;
    }

    /**
     * {@code LevelExtractor.allChanged()} 真的执行了 —— 也就是 Voxy 刚把整个
     * {@code VoxyRenderSystem} 拆了重建（改区块视距、F3+A、切资源包都会走到这里）。
     *
     * <p>光靠 {@link #prewarmIfNeeded()} 是发现不了的：它的稳态快速路径盯的是
     * <b>Iris 主管线</b>有没有换人，而 {@code allChanged()} 根本不碰 Iris 管线，
     * 于是 {@code voxyStackSettled} 一直是 true，我们逐帧直接返回，
     * 永远不会去问一句「Voxy 还是原来那个吗」。所以必须由这条事件来打破快速路径。
     */
    public static void onLevelRendererReload() {
        // 顺序：先把已经失效的那一套还回去，再把状态机打回「需要重新检查」。
        com.tacz.guns.compat.voxy.VoxyScopePipelineCompat.onRendererRebuilt();
        voxyStackSettled = false;
    }
    private static boolean loggedPrewarm;

    private IrisScopePipelineCompat() {
    }

    /**
     * 瞄具那套管线用的维度 id。
     *
     * @return Iris 的 {@code NamespacedId} 实例；拿不到时返回 {@code null}
     *         （此时不做隔离，退回与主画面共用管线）
     */
    public static Object scopeDimensionId() {
        if (scopeDimensionId != null || idResolveFailed) {
            return scopeDimensionId;
        }
        try {
            Class<?> cls = Class.forName(NAMESPACED_ID);
            scopeDimensionId = cls.getConstructor(String.class, String.class)
                    .newInstance(GunMod.MOD_ID, "scope_pip");
        } catch (Throwable t) {
            idResolveFailed = true;
            GunMod.LOGGER.warn("[TACZ Scope] Could not build the scope pipeline's dimension id; the "
                    + "scope pass will share the main shader pipeline.", t);
        }
        return scopeDimensionId;
    }

    private static boolean resolveHandles() {
        if (handlesResolved) {
            return !handlesFailed;
        }
        handlesResolved = true;
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            getPipelineManager = iris.getMethod("getPipelineManager");
            getCurrentDimension = iris.getMethod("getCurrentDimension");
            Class<?> manager = getPipelineManager.getReturnType();
            getPipelineNullable = manager.getMethod("getPipelineNullable");
            preparePipeline = manager.getMethod("preparePipeline", Class.forName(NAMESPACED_ID));
        } catch (Throwable t) {
            handlesFailed = true;
            GunMod.LOGGER.warn("[TACZ Scope] Could not resolve Iris' pipeline manager; the scope "
                    + "pipeline will be built lazily on first aim (expect one stutter).", t);
        }
        return !handlesFailed;
    }

    /**
     * 若还没预热过，就在<b>当前这一帧的安全位置</b>把瞄具管线建好。
     *
     * <p>由 {@code GameRenderer#extract} 的 HEAD 调用 —— 那里在世界渲染<b>之前</b>，
     * 不在任何 render pass 内，也不在我们的镜内那一遍里。
     *
     * <p>调用方负责判断「现在确实需要隔离管线」，本方法只管建与不建。
     */
    public static void prewarmIfNeeded() {
        if (!ModList.get().isLoaded("iris") || !IrisCompat.isUsingRenderPack()) {
            return;
        }
        Object id = scopeDimensionId();
        if (id == null || !resolveHandles()) {
            return;
        }
        try {
            Object manager = getPipelineManager.invoke(null);
            if (manager == null) {
                return;
            }
            Object mainPipeline = getPipelineNullable.invoke(manager);
            if (mainPipeline == null) {
                // 主管线还没建起来（刚进世界）。等它先建好，下一帧再说 ——
                // 抢在它前面预热会让「当前管线」指向瞄具那套，把这一帧的主画面画错。
                return;
            }
            // 【稳态快速路径】本方法逐帧都会被调到，所以「已经全部就绪」这条必须最便宜。
            // 主管线没换人、且 Voxy 那套也建好了 —— 直接回，不去碰任何 Voxy 反射。
            //
            // 早前这里每帧都要 VoxyCompat.renderSystem() + isBuiltFor()，
            // 明明什么都不用做，却照样付两次反射调用。
            if (prewarmedAgainst != mainPipeline) {
                voxyStackSettled = false;
            }
            if (prewarmedAgainst == mainPipeline && voxyStackSettled) {
                return;
            }

            // 慢路径：只有还没就绪时才走。Voxy 的第二套栈可能要等它自己先建好，
            // 所以这里每帧问一次 —— 光看「管线预热过没有」会漏掉
            // 「预热那一刻 Voxy 还没就绪」的情况。
            Object voxy = com.tacz.guns.compat.voxy.VoxyCompat.renderSystem();
            boolean voxyUsable = voxy != null
                    && com.tacz.guns.compat.voxy.VoxyScopePipelineCompat.isAvailable();
            boolean needVoxyStack = voxyUsable
                    && !com.tacz.guns.compat.voxy.VoxyScopePipelineCompat.isBuiltFor(voxy);
            if (prewarmedAgainst == mainPipeline && !needVoxyStack) {
                // 没装 Voxy、或它那套用不上 —— 记下来，以后走快速路径。
                voxyStackSettled = true;
                return;
            }
            Object realDimension = getCurrentDimension.invoke(null);
            // 打开窗口：把瞄具那套设成「当前管线」。第一次会真的编译，之后是缓存命中。
            //
            // 这个窗口<b>同时</b>是「瞄具管线正在构造」的唯一时机 —— 阴影贴图的分辨率
            // 就是在构造里读 PackShadowDirectives.getResolution() 定下来的，
            // 且此后由采样器一路捕获使用。所以要给镜内那一遍配一张更小的阴影图，
            // 只有在这里做才来得及。见 IrisShadowResolutionMixin。
            buildingScopePipeline = true;
            try {
                preparePipeline.invoke(manager, id);
            } finally {
                buildingScopePipeline = false;
            }
            try {
                // 【只能在这个窗口里建】Voxy 的 RenderPipelineFactory 取的正是
                // 「当前管线」，错过这里就会绑到主管线上，等于白建。
                if (needVoxyStack) {
                    com.tacz.guns.compat.voxy.VoxyScopePipelineCompat.ensureBuilt(voxy);
                }
            } catch (Throwable ignored) {
                // Voxy 那侧失败只影响镜内有没有 LOD，不该拖累管线预热本身
            } finally {
                // 【必须】把当前管线指回主管线，否则这一帧的主画面会用瞄具那套渲染。
                // 放 finally：上面抛了也绝不能把「当前管线」留在瞄具那套上。
                if (realDimension != null) {
                    preparePipeline.invoke(manager, realDimension);
                }
            }
            prewarmedAgainst = mainPipeline;
            if (!loggedPrewarm) {
                loggedPrewarm = true;
                GunMod.LOGGER.info("[TACZ Scope] Pre-built the scope pass' Iris pipeline now, so the "
                        + "first time you aim does not stall while the shader pack compiles.");
            }
        } catch (Throwable t) {
            handlesFailed = true;
            GunMod.LOGGER.warn("[TACZ Scope] Failed to pre-build the scope pipeline; it will be built "
                    + "on first aim instead (expect one stutter).", t);
        }
    }
}

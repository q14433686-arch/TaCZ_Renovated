package com.tacz.guns.compat.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.voxy.VoxyCompat;
import com.tacz.guns.compat.voxy.VoxyScopePipelineCompat;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 瞄具专用 Iris 管线的「维度 id」与<b>预热</b>。
 *
 * <h2>为什么要预热，而不是等第一次开镜时懒加载</h2>
 * Iris 的管线是用到才建的。不预热的话，那套瞄具管线会在<b>第一次开镜的那一帧</b>
 * 才开始编译整个 shaderpack —— 玩家实测「第一次 ADS 卡得非常厉害」就是它。
 * 而且那一刻正好落在我们的镜内那一遍<b>里面</b>，等于在一帧的中途做重活：
 * <ul>
 *   <li>{@code preparePipeline} 建管线时会把<b>全局</b>帧计数与计时器清零，
 *       在一帧中途干这个，时域效果当场错乱；</li>
 *   <li>Voxy 挂在「每个 Iris 管线构造」上的那套钩子也在此刻触发。</li>
 * </ul>
 *
 * <h3>预热之后要把「当前管线」指回去</h3>
 * {@code preparePipeline} 除了建/取，还会把 {@code PipelineManager.pipeline}
 * 指向刚取到的那套。预热完必须再用<b>真实维度</b>调一次把它指回主管线 ——
 * 那次是命中缓存，不会重建。漏掉这一步，接下来整帧都会用瞄具管线渲染主画面。
 *
 * <p>全程反射，Iris 不在或结构变了就静默放弃（退回懒加载，只是第一次开镜会卡）。
 *
 * <p><b>移植说明</b>：随姊妹分支 {@code TaCZ_Refabricated_Unofficial} 的镜内 PIP 一族
 * 同步而来，两处改动：
 * <ol>
 *   <li>{@code FabricLoader#isModLoaded} → {@code ModList#isLoaded}；</li>
 *   <li><b>未</b>同步姊妹分支那套「空闲释放瞄具管线」的实验入口
 *       （{@code releaseScopePipelineIfPresent}）—— 它是「光影下开镜帧率衰减」
 *       那次未结案调查的探针，不是修复，本仓不引入。</li>
 * </ol>
 */
public final class IrisScopePipelineCompat {

    private static final String NAMESPACED_ID = "net.irisshaders.iris.shaderpack.materialmap.NamespacedId";

    private static Object scopeDimensionId;
    private static boolean idResolveFailed;

    private static Method getPipelineManager;
    private static Method getPipelineNullable;
    private static Method preparePipeline;
    private static Method getCurrentDimension;
    private static Field pipelinesMapField;
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
     * 才会决定<b>瞄具管线</b>那张阴影贴图的尺寸。窗口之外必须恢复原值。
     */
    private static volatile boolean buildingScopePipeline;
    private static boolean loggedPrewarm;

    private IrisScopePipelineCompat() {
    }

    /** 供 {@code IrisShadowResolutionMixin} 查询。 */
    public static boolean isBuildingScopePipeline() {
        return buildingScopePipeline;
    }

    /**
     * {@code LevelExtractor.allChanged()} 真的执行了 —— 也就是 Voxy 刚把整个
     * {@code VoxyRenderSystem} 拆了重建（改区块视距、F3+A、切资源包都会走到这里）。
     *
     * <p>光靠 {@link #prewarmIfNeeded()} 是发现不了的：它的稳态快速路径盯的是
     * <b>Iris 主管线</b>有没有换人，而 {@code allChanged()} 根本不碰 Iris 管线。
     */
    public static void onLevelRendererReload() {
        // 顺序：先把已经失效的那一套还回去，再把状态机打回「需要重新检查」。
        VoxyScopePipelineCompat.onRendererRebuilt();
        voxyStackSettled = false;
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
            // Iris 的 PipelineManager 里只有一个 Map 字段（pipelinesPerDimension），
            // 这里按类型找而不是按名找，名字变了也不至于抓空。
            for (Field field : manager.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    pipelinesMapField = field;
                    break;
                }
            }
            if (pipelinesMapField != null) {
                pipelinesMapField.setAccessible(true);
            }
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
            if (prewarmedAgainst != mainPipeline) {
                voxyStackSettled = false;
            }
            if (prewarmedAgainst == mainPipeline && voxyStackSettled) {
                return;
            }

            // 慢路径：Voxy 的第二套栈可能要等它自己先建好，所以这里每帧问一次。
            Object voxy = VoxyCompat.renderSystem();
            boolean voxyUsable = voxy != null && VoxyScopePipelineCompat.isAvailable();
            boolean needVoxyStack = voxyUsable && !VoxyScopePipelineCompat.isBuiltFor(voxy);
            if (prewarmedAgainst == mainPipeline && !needVoxyStack) {
                voxyStackSettled = true;
                return;
            }
            Object realDimension = getCurrentDimension.invoke(null);
            // 打开窗口：把瞄具那套设成「当前管线」。第一次会真的编译，之后是缓存命中。
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
                    VoxyScopePipelineCompat.ensureBuilt(voxy);
                }
            } catch (Throwable ignored) {
                // Voxy 那侧失败只影响镜内有没有 LOD，不该拖累管线预热本身
            } finally {
                // 【必须】把当前管线指回主管线，否则这一帧的主画面会用瞄具那套渲染。
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

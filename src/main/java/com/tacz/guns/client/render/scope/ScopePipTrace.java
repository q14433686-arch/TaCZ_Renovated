package com.tacz.guns.client.render.scope;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.tacz.guns.GunMod;
import com.tacz.guns.config.client.RenderConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【诊断】把「镜内二次渲染」那一帧的渲染目标解析过程原样打出来。
 *
 * <h2>为什么需要它</h2>
 * 「放大画面溢出到镜外」这个症状在姊妹分支上靠静态分析连错四次：
 * 先怪 Sodium 绕开 {@code mainRenderTarget()}（错，它走），
 * 再怪 ImmediatelyFast 缓存帧缓冲（错，它只是跳过绑 0），
 * 再怪 SkyRenderer 缓存 target（是真 bug，但会被 vanilla 的 clear 抹掉），
 * 再怪 Voxy 是第三个地形渲染器（错，它的 target 与投影都取自被我们接管的两处）。
 *
 * <p>共同的失败模式是：<b>症状对得上就当成因果</b>。
 * 而这个问题的关键事实其实只有两条，都可以直接测出来，不必猜：
 * <ol>
 *   <li>镜内那一遍进行期间，有没有<b>谁解析到了真正的主 target</b>（= 漏出去的那一笔）；</li>
 *   <li>vanilla 的 clear 之后，还有没有人拿着我们的窄投影在画（= 延迟提交）。</li>
 * </ol>
 *
 * <p>{@code GameRenderer#mainRenderTarget()} 的注入点本来就是<b>所有</b>目标解析的必经之路
 * （帧图导入、vanilla 的 clear lambda、第三方地形渲染器全都从这里过）。
 * 在那里记一行「谁问的 + 给了哪个」，再配上镜内那一遍的进出标记，
 * 一帧的完整顺序就出来了。
 *
 * <p>只在 {@code ScopePipDebugTrace} 打开时工作，且<b>只记录有限几帧</b>。
 *
 * <p><b>移植说明</b>：随 26.2 姊妹分支的镜内 PIP 一族同步而来。
 * 保留了 {@code ARMED_FRAME_LIMIT} 那条硬性兜底 —— 那是姊妹分支上
 * 一次真实崩溃的修复（收摊条件依赖被诊断路径自己发信号，采不到样本时
 * 会一直武装到把渲染线程拖垮）。
 */
public final class ScopePipTrace {

    /** 最多记录几帧。够看清一帧的完整顺序，又不会把日志刷爆。 */
    private static final int FRAME_BUDGET = 3;
    /** 单帧内最多记录多少行，防御性上限（正常一帧几十行）。 */
    private static final int LINES_PER_FRAME = 400;

    /**
     * 【硬性兜底】最多<b>武装</b>这么多帧，之后无论有没有采到样本都永久收摊。
     *
     * <h3>为什么必须有这一条 —— 它修的是一次真实的崩溃</h3>
     * 原来的收摊条件只有 {@link #FRAME_BUDGET}，而预算<b>只在
     * {@code sawScopePass} 为真的帧才扣</b>，那个标志又只由
     * {@code renderScopeView}（二次渲染那条路）发出的 {@code SCOPE-PASS BEGIN} 置位。
     * <b>光影下二次渲染是被硬性拦下的</b>，于是那面旗永远立不起来 ⇒ 预算一分不扣 ⇒
     * {@code enabled()} 永远为真 ⇒ 整局游戏每帧都在 {@code mainRenderTarget()} 上跑
     * {@code StackWalker}。
     *
     * <p>后果不是「日志有点多」，而是<b>渲染线程被拖垮</b>：区块构建在工作线程照常堆积，
     * 渲染线程追不上，上传被一次性堆积处理，显存直接吃紧。
     * 教训：诊断开关的收摊条件<b>绝不能依赖被诊断的那条路自己发信号</b>。
     */
    private static final int ARMED_FRAME_LIMIT = 600;

    private static int framesTraced = 0;
    private static int framesArmed = 0;
    private static int linesThisFrame = 0;
    private static boolean tracingThisFrame = false;
    private static boolean announced = false;
    private static boolean loggedGiveUp = false;

    /**
     * 本帧的行缓冲。先缓冲、<b>只有当这一帧确实跑了镜内那一遍</b>才落盘并计入预算 ——
     * 否则三帧预算会烧在标题画面上，等玩家真进世界举枪时已经没有预算了。
     */
    private static final List<String> BUFFER = new ArrayList<>();
    /** 本帧是否真的跑了镜内那一遍（缓冲要不要落盘就看它）。 */
    private static boolean sawScopePass = false;

    private ScopePipTrace() {
    }

    public static boolean enabled() {
        return RenderConfig.SCOPE_PIP_DEBUG_TRACE != null
                && RenderConfig.SCOPE_PIP_DEBUG_TRACE.get()
                && framesTraced < FRAME_BUDGET
                && framesArmed < ARMED_FRAME_LIMIT;
    }

    /** 每帧开头调用（接在瞄具帧状态归零那一处）：结算上一帧，再开一帧的缓冲。 */
    public static void beginFrame() {
        if (tracingThisFrame && sawScopePass && !BUFFER.isEmpty()) {
            framesTraced++;
            GunMod.LOGGER.info("[TACZ Scope][trace] ---- frame with scope pass ({}/{}) ----",
                    framesTraced, FRAME_BUDGET);
            for (String line : BUFFER) {
                GunMod.LOGGER.info("[TACZ Scope][trace] {}", line);
            }
            GunMod.LOGGER.info("[TACZ Scope][trace] ---- end frame ----");
        }
        BUFFER.clear();
        sawScopePass = false;
        linesThisFrame = 0;
        tracingThisFrame = enabled();
        if (tracingThisFrame) {
            // 无条件计数：这一帧确实处于武装状态、确实会跑 StackWalker，就得算数。
            framesArmed++;
        } else if (RenderConfig.SCOPE_PIP_DEBUG_TRACE != null
                && RenderConfig.SCOPE_PIP_DEBUG_TRACE.get()
                && framesArmed >= ARMED_FRAME_LIMIT && !loggedGiveUp) {
            loggedGiveUp = true;
            GunMod.LOGGER.info("[TACZ Scope][trace] Gave up after {} armed frames without capturing a "
                            + "scope pass; tracing is now off for this session. Leaving it armed would "
                            + "stall the render thread badly enough to starve terrain uploads. "
                            + "Set ScopePipDebugTrace=false to silence this.", framesArmed);
        }
        if (tracingThisFrame && !announced) {
            announced = true;
            GunMod.LOGGER.info("[TACZ Scope][trace] Armed: will capture {} frames that actually run the "
                    + "scope pass. Any 'MAIN' between SCOPE-PASS BEGIN and END is the leak; its stack "
                    + "trace names the culprit.", FRAME_BUDGET);
        }
    }

    /** 记一个阶段标记（镜内那一遍的进出、合成等）。 */
    public static void mark(String what) {
        if (!tracingThisFrame) {
            return;
        }
        if (what.startsWith("SCOPE-PASS BEGIN") || what.startsWith("PIP COMPOSITE")) {
            // 必须同时认「PIP COMPOSITE」：光影下走的是屏幕空间合成，
            // 根本不存在 SCOPE-PASS。只认前者的话，光影下永远采不到样本。
            sawScopePass = true;
        }
        if (linesThisFrame++ > LINES_PER_FRAME) {
            return;
        }
        BUFFER.add("== " + what);
    }

    /**
     * 记一次 {@code mainRenderTarget()} 解析。
     *
     * @param resolved   实际返回的 target
     * @param redirected 是否被我们顶替成了离屏 target
     */
    public static void targetResolved(@Nullable RenderTarget resolved, boolean redirected) {
        if (!tracingThisFrame || linesThisFrame++ > LINES_PER_FRAME) {
            return;
        }
        BUFFER.add((redirected ? "SCOPE <- " : "MAIN  <- ") + caller());
    }

    /** 谁问的。用 {@code StackWalker} 而不是异常栈：它每帧要跑几十次。 */
    private static String caller() {
        try {
            List<String> frames = StackWalker.getInstance().walk(s -> s
                    .map(f -> f.getClassName() + "#" + f.getMethodName())
                    .filter(n -> !n.startsWith("com.tacz.guns.client.render.scope.ScopePipTrace"))
                    .filter(n -> !n.contains("GameRendererMixin"))
                    .skip(1)
                    .limit(3)
                    .collect(Collectors.toList()));
            return String.join("  <-  ", frames);
        } catch (Throwable t) {
            return "<stack unavailable>";
        }
    }
}

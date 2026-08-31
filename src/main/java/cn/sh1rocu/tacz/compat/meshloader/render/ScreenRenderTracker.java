package cn.sh1rocu.tacz.compat.meshloader.render;

/**
 * 「此刻是否正在 Screen 的 extract 阶段内部」的追踪器。
 *
 * <h2>为什么不能用 {@code minecraft.screen != null} 或
 * {@code RenderDistance.isGuiRender()}</h2>
 *
 * <p>世界提取与 GUI 提取在同一帧内是<b>分时</b>进行的。「菜单开着」
 * （{@code screen != null}）或「最近一段时间内画过 GUI」
 * （{@code RenderDistance.isGuiRender()} 的时间戳窗口）这两种判定在
 * <b>世界提取阶段</b>也为 true —— 拿它们做世界 GPU 路径的闸门，等于玩家一开背包，
 * 地上/别人手里的全部 mesh 枪瞬间跌回 collector 重路径。这正是上游 TML
 * {@code ScreenRenderTracker} 注释里记载的实机事故（菜单一开全场景掉帧）。</p>
 *
 * <h2>本仓的表皮差异（NeoForge 版）</h2>
 *
 * <p>姊妹分支用 Fabric 的 {@code ScreenEvents.beforeExtract/afterExtract}
 * （26.2 的 Fabric API 已把 Screen 事件窗口从 render 改成 extract，与上游 TML
 * 1.21.1 的 render 窗口语义一一对应）。NeoForge 没有等价事件，本仓改用 mixin
 * 注入 vanilla 的 {@code Screen#extractRenderState}（{@code ScreenExtractMixin}），
 * 窗口语义与她那份逐点对应 —— GUI 内嵌 3D（背包人偶、枪匠桌预览）的 submit
 * 恰好发生在这个窗口里。</p>
 *
 * <p>挂点存在性证据取自本仓自身：{@code GunRefitScreen extends Screen}
 * （直接继承，中间无 AbstractContainerScreen）里覆写了
 * {@code extractRenderState(GuiGraphicsExtractor, int, int, float)} 并调用
 * {@code super.extractRenderState(...)} —— 该方法在 {@code Screen} 上必然存在且可注入。</p>
 *
 * <h2>为什么用深度计数而不是布尔</h2>
 *
 * <p>子类覆写 {@code extractRenderState} 且内部调 {@code super.extractRenderState(...)}
 * 时，super 那一次的 RETURN 会先触发 —— 布尔会被它清零，外层剩下的提取阶段就漏了。
 * 深度计数天然处理这种嵌套。</p>
 *
 * <p>RETURN 在异常路径不触发会让计数泄漏，后果是世界 GPU 长期不开闸（退回
 * collector，安全侧），Screen 提取抛异常本身已是崩溃级事件，不为它再加机制。</p>
 *
 * <p>它挡住的事故是关 PR #33 的复刻版：Screen 内嵌 3D 预览的 submit 若落进世界表，
 * 要么被世界投影画到错误位置，要么因世界 pass 已消费而整层消失。</p>
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)，事件窗口按
 * 26.2 + NeoForge/mixin 适配。</p>
 */
public final class ScreenRenderTracker {

    private static int extractDepth = 0;

    private ScreenRenderTracker() {
    }

    /** 此刻是否正在 Screen 的 extract 阶段内部（而不是「有菜单开着」）。 */
    public static boolean isExtractingScreen() {
        return extractDepth > 0;
    }

    /** 由 {@code ScreenExtractMixin} 在 {@code Screen#extractRenderState} 的 HEAD 调用。 */
    public static void beginExtract() {
        extractDepth++;
    }

    /** 由 {@code ScreenExtractMixin} 在 {@code Screen#extractRenderState} 的 RETURN 调用。 */
    public static void endExtract() {
        if (extractDepth > 0) {
            extractDepth--;
        }
    }
}

package com.tacz.guns.compat.meshloader.render;

/**
 * 「今まさに GUI 画面（Screen）的提取阶段を描画している瞬間」を検出するトラッカー。
 *
 * <h3>问题</h3>
 * {@code Minecraft.getInstance().screen != null}（＝菜单是否「开着」）
 * 用来判定「GUI 内嵌 3D 渲染（如背包玩家娃娃）需要走 GUI 预算/关闭 GPU 路径」时，
 * 菜单开着期间一直为 true，导致世界内无关渲染（地面掉落物 / 展示框物品 /
 * 他人手持的枪）也被 GUI 预算/GPU 关闭误伤。mesh 枪一多，菜单一开，
 * 全屏 mesh 枪会瞬间集体切到重路径，造成严重性能劣化。
 *
 * <h3>解决</h3>
 * 世界渲染与 GUI 渲染在同一帧内是<b>不同时机</b>。借 NeoForge 的
 * {@code ScreenEvent.Render.Pre}/{@code ScreenEvent.Render.Post}（26.1.x 源码核实：
 * 回调参数为 {@code GuiGraphicsExtractor}，即 frame-graph「提取」语义，与 Fabric 侧
 * {@code fabric-screen-api} 的 beforeExtract/afterExtract 同一窗口）精确检测
 * 「此刻是否正在 Screen 的提取阶段内部」——只有真正正在提取 GUI 画面的瞬间才为 true，
 * 世界内无关渲染不受影响。
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)。</p>
 */
public final class ScreenRenderTracker {

    private static volatile boolean renderingScreen = false;

    private ScreenRenderTracker() {
    }

    /**
     * 当前是否正在某个 Screen 的提取阶段（GUI 画面本身的提取，含背包玩家娃娃等内嵌
     * 3D 展示）执行中。
     *
     * <p>与 {@code Minecraft.getInstance().screen != null} 不同，这里只在真正提取
     * GUI 内容的「瞬间」为 true。</p>
     */
    public static boolean isRenderingScreen() {
        return renderingScreen;
    }

    /** 注册到 NeoForge 客户端事件系统（由 {@code ClientGameEvents} 的 ScreenEvent 回调驱动）。 */
    public static void register() {
    }

    /** {@code ScreenEvent.Render.Pre} 回调入口：进入 GUI 提取窗口。 */
    public static void onScreenRenderPre() {
        renderingScreen = true;
    }

    /** {@code ScreenEvent.Render.Post} 回调入口：退出 GUI 提取窗口。 */
    public static void onScreenRenderPost() {
        renderingScreen = false;
    }
}

package cn.sh1rocu.tacz.compat.meshloader.render;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 「今まさに GUI 画面（Screen#render()）を描画している瞬間」を検出するトラッカー。
 *
 * <h3>问题</h3>
 * {@code Minecraft.getInstance().screen != null}（＝菜单是否「开着」）
 * 用来判定「GUI 内嵌 3D 渲染（如背包玩家娃娃）需要走 GUI 预算/关闭 GPU 路径」时，
 * 菜单开着期间一直为 true，导致世界内无关渲染（地面掉落物 / 展示框物品 /
 * 他人手持的枪）也被 GUI 预算/GPU 关闭误伤。mesh 枪一多，菜单一开，
 * 全屏 mesh 枪会瞬间集体切到重路径，造成严重性能劣化。
 *
 * <h3>解决</h3>
 * 世界渲染（{@code LevelRenderer}）与 GUI 渲染（{@code Screen#render()}）在同一帧内
 * 是<b>不同时机</b>。Fabric 版用 ScreenEvents 的 beforeRender/afterRender 逐屏捕捉；
 * 1.21.11 NeoForge 线用全局 {@link ScreenEvent.Render.Pre}/{@link ScreenEvent.Render.Post}
 * 等价替换 —— 只在真正执行 GUI 渲染的「瞬间」为 true，世界内无关渲染不受影响。
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)。</p>
 */
public final class ScreenRenderTracker {

    private static volatile boolean renderingScreen = false;

    private ScreenRenderTracker() {
    }

    /**
     * 当前是否正在 Screen#render()（GUI 画面本身的渲染，含背包玩家娃娃等内嵌
     * 3D 展示）执行中。
     *
     * <p>与 {@code Minecraft.getInstance().screen != null} 不同，这里只在真正执行
     * GUI 渲染的「瞬间」为 true。</p>
     */
    public static boolean isRenderingScreen() {
        return renderingScreen;
    }

    /** 注册到 NeoForge 的 ScreenEvent（原 Fabric ScreenEvents 的等价物）。客户端 setup 中调用一次。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Pre.class, event -> renderingScreen = true);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, event -> renderingScreen = false);
    }
}

package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.config.client.RenderConfig;
import org.joml.Matrix4f;

public final class RenderDistance {
    private static long guiRenderTimestamp;

    private RenderDistance() {
    }

    public static void markGuiRenderTimestamp() {
        guiRenderTimestamp = System.currentTimeMillis();
    }

    /**
     * 最近是否有 GUI（枪匠桌等）标记过渲染时间戳。
     *
     * <p>公开给 meshloader 的「近距离全模豁免」用：FIXED/HEAD 语境既出现在
     * 世界（展示台雕像、物品展示框、背枪）也出现在 GUI 预览（枪匠桌界面），
     * 只有非 GUI 的那一侧才允许按相机距离豁免顶点预算 —— 否则高模会被
     * 全量画进 GUI 图标。</p>
     *
     * <p><b>与姊妹分支的口径差异</b>：她侧这个判据是 100ms，本仓沿用
     * {@link #inRenderHighPolyModelDistance} 已经在用的 500ms ——
     * 更保守（更容易判成 GUI），宁可少豁免也不把 36 万顶点画进图标。</p>
     */
    public static boolean isGuiRender() {
        return System.currentTimeMillis() - guiRenderTimestamp < 500L;
    }

    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack) {
        if (System.currentTimeMillis() - guiRenderTimestamp < 500L) {
            return true;
        }
        int dist = RenderConfig.GUN_LOD_RENDER_DISTANCE.get();
        if (dist <= 0) {
            return true;
        }
        Matrix4f m = poseStack.last().pose();
        float x = m.m30();
        float y = m.m31();
        float z = m.m32();
        return (x * x + y * y + z * z) < (float) dist * dist;
    }
}

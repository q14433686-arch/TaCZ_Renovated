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
     * 「最近 500ms 内渲染过 GUI」的时间戳标记（枪匠桌 / GUI 预览语境的粗粒度标记）。
     *
     * <p>26.1.2 起改为 public：poly_mesh 世界 GPU 路径（{@code TaczPolyMeshGunModel}
     * 的 {@code isWorldGpuContext}）需要按它把 {@code FIXED}/{@code HEAD} 这两个双面语境
     * 里的「GUI 内嵌预览」那半拒收在 WORLD_DRAWS 表外 —— 与 1211 分支的用法一致
     * （{@code ScreenRenderTracker} 拦的是 Screen 提取窗口，枪匠桌这种 500ms 标记语境
     * 只能靠这里）。</p>
     *
     * <p>注：Fabric 姊妹仓同一方法在 26.1.2 上用的是 100ms 窗口；本仓保持历史 500ms
     * （两边基线早已分叉，本次只移植 public 化这一语义改动，窗口宽度属于既有行为差异）。</p>
     */
    public static boolean isGuiRender() {
        return System.currentTimeMillis() - guiRenderTimestamp < 500L;
    }

    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack) {
        if (isGuiRender()) {
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

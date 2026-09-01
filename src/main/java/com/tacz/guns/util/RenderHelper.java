package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.render.scope.ScopeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public final class RenderHelper {
    private RenderHelper() {
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light) {
        if (player == null || collector == null) {
            return;
        }
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof AvatarRenderer<?> avatar)) {
            return;
        }
        boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
        var texture = player.getSkin().body().texturePath();
        // 【镜内裁手】与枪身/火光同一个深度孔径门禁。手臂的 RenderType 是
        // AvatarRenderer#renderHand 内部自己挑的 entityTranslucent(skin)，无法在调用点直接换，
        // 因此在提交穿过时由代理把该类型原地替换成 ScopeRenderTypes.armClipped(skin)。
        // 失败方向仍与枪身/火光一致：孔径未就绪时原样返回真 collector，最坏回到「镜内见手臂」。
        collector = wrapForScopeClip(collector, texture);
        if (arm == HumanoidArm.RIGHT) {
            avatar.renderRightHand(poseStack, collector, light, texture, slim, player);
        } else {
            avatar.renderLeftHand(poseStack, collector, light, texture, slim, player);
        }
    }

    /**
     * 【镜内裁手】给手臂提交套上「镜内 discard」的 collector 代理。
     *
     * <p>为什么用代理而不是复刻提交：{@code AvatarRenderer#renderHand} 内部除了那句
     * {@code submitModelPart}，还有 resetPose/袖层可见性/手臂显隐一串模型状态整备 ——
     * 复刻提交就得复刻这些 vanilla 内部逻辑，版本一动就烂。代理让 vanilla 逻辑原样跑完，
     * 只在提交穿过时把 RenderType 换成 {@code ScopeRenderTypes.armClipped}。</p>
     *
     * <p>为什么敢用 identity 比较认出手臂的 RenderType：{@code RenderTypes.entityTranslucent}
     * 按贴图 memoize，同一皮肤贴图永远拿到同一实例（26.2 实读），因此 {@code ==} 即可精准命中，
     * 不会误伤同一次提交里的其它 RenderType。</p>
     */
    private static SubmitNodeCollector wrapForScopeClip(SubmitNodeCollector real, Identifier skinTexture) {
        if (!ScopeRenderTypes.shouldClipViewmodel()) {
            return real;
        }
        final RenderType vanillaArm = RenderTypes.entityTranslucent(skinTexture);
        final RenderType clippedArm = ScopeRenderTypes.armClipped(skinTexture);
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, args) -> {
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] == vanillaArm) {
                                args[i] = clippedArm;
                            }
                        }
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        // 把真实异常还原抛出，别让调用方看到一层反射包装。
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                });
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack, int light) {
        // Legacy VertexConsumer path; Feature Rendering uses the collector overload.
    }
}

package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.render.scope.ScopeBodyRenderTypes;
import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.lang.reflect.Proxy;

public final class RenderHelper {
    private RenderHelper() {
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light) {
        if (player == null || collector == null) {
            return;
        }
        AvatarRenderer<?> avatar = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        var texture = player.getSkin().body().texturePath();
        // 【镜内裁手】高倍镜掩码就绪时，把手臂提交改走「镜内 discard」管线。
        // 手臂的 RenderType 是 AvatarRenderer#renderHand 内部自己挑的
        // （entityTranslucent(skin)，字节码实读），无法在调用点直接换 ——
        // 用 collector 代理在提交穿过时原地替换。判定放在这里（submit task
        // 执行期）而不是 extract 期：掩码清单登记发生在瞄具提交内部，
        // 只有此刻的 maskReadyForViewmodel 才反映本帧真实状态。
        collector = wrapForScopeClip(collector, texture);
        FirstPersonAnimationCompat.beginDirectArmRender();
        try {
            if (arm == HumanoidArm.RIGHT) {
                avatar.renderRightHand(poseStack, collector, light, texture,
                        player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
            } else {
                avatar.renderLeftHand(poseStack, collector, light, texture,
                        player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            }
        } finally {
            FirstPersonAnimationCompat.endDirectArmRender();
        }
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack, int light) {
        // Legacy VertexConsumer path; Feature Rendering uses the collector overload.
    }

    /**
     * 【镜内裁手】给手臂提交套上「镜内 discard」的 collector 代理。
     *
     * <h2>为什么是代理而不是复刻提交</h2>
     * {@code AvatarRenderer#renderHand} 内部除了那一句 submitModelPart，
     * 还有 resetPose/袖层可见性/手臂显隐一串模型状态整备（字节码实读）——
     * 复刻提交就得复刻这些 vanilla 内部逻辑，版本一动就烂。代理让 vanilla
     * 逻辑原样跑完，只在提交穿过时换掉 RenderType。
     *
     * <h2>为什么敢用 identity 比较认出手臂的 RenderType</h2>
     * {@code RenderTypes.entityTranslucent} 是按贴图 memoize 的
     * （ENTITY_TRANSLUCENT 是 {@code Util.memoize} 的 BiFunction，字节码实读），
     * 同一皮肤贴图永远拿到同一实例 —— 代理里 {@code ==} 即可精准命中，
     * 不会误伤同一次提交里的其他 RenderType。
     *
     * <h2>为什么用 {@link Proxy} 而不是手写实现类</h2>
     * {@code SubmitNodeCollector} 继承 vanilla {@code OrderedSubmitNodeCollector}
     * 外加各加载器的注入接口 —— 手写实现要跟着这些接口的每次增删陪跑。
     * 动态代理自动覆盖全部方法面，反射开销无关紧要：每帧只有两次手臂提交
     * 穿过它，各自个位数方法调用。
     *
     * <p>掩码未就绪（低倍镜/光影/配置关闭）时原样返回真 collector ——
     * 与枪身/火光同一失败哲学，最坏回到「镜内见手臂」的现状。</p>
     */
    private static SubmitNodeCollector wrapForScopeClip(SubmitNodeCollector real, Identifier skinTexture) {
        if (!ScopeBodyRenderTypes.maskReadyForViewmodel(true)) {
            return real;
        }
        final RenderType vanillaArm = RenderTypes.entityTranslucent(skinTexture);
        final RenderType clippedArm = ScopeBodyRenderTypes.armClipped(skinTexture);
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
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // 把真实异常还原抛出，别让调用方看到一层反射包装。
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                });
    }
}

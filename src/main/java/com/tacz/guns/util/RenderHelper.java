package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopeRenderTypes;
import net.minecraft.client.Minecraft;
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
    /** 【镜内裁手】日志只打一次：成功走了代理 / 代理不可用退回原 collector。 */
    private static volatile boolean LOGGED_ARM_CLIPPED = false;
    private static volatile boolean LOGGED_ARM_CLIP_FAILED = false;

    private RenderHelper() {
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light) {
        renderFirstPersonArm(player, arm, poseStack, collector, light, false);
    }

    /**
     * 带「镜内裁手」开关的第一人称手臂提交。
     *
     * <p>{@code clipToScopeExterior} 由 {@code LeftHandRender} / {@code RightHandRender} 在
     * <b>extract 期</b>算出（与 {@code MuzzleFlashRender} 同一判据：
     * {@link ScopeRenderTypes#viewmodelFxClipApplies()}）——瞄具的目镜序列在枪身遍历之前登记
     * （{@code BedrockGunModel#submit} 先提交瞄具再 {@code super.submit}），所以此刻的闸门
     * 就是本帧的真实状态。闸门还带倍率下限（{@code ScopePipMinMagnification}，默认 4×）：
     * 低倍镜/组合镜的低倍档不裁 —— 没有镜内画面可让位，挖洞只会像破图。</p>
     */
    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light,
                                            boolean clipToScopeExterior) {
        if (player == null) {
            return;
        }
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof AvatarRenderer<?> avatar)) {
            return;
        }
        var skinTexture = player.getSkin().body().texturePath();
        SubmitNodeCollector target = clipToScopeExterior
                ? wrapForScopeClip(collector, skinTexture)
                : collector;
        boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
        if (arm == HumanoidArm.RIGHT) {
            avatar.renderRightHand(poseStack, target, light, skinTexture, slim, player);
        } else {
            avatar.renderLeftHand(poseStack, target, light, skinTexture, slim, player);
        }
    }

    /**
     * 【镜内裁手】给手臂提交套上「镜内 discard」的 collector 代理。
     *
     * <h2>为什么是代理而不是复刻提交</h2>
     * {@code AvatarRenderer#renderLeftHand/renderRightHand} 内部除了那一句
     * {@code submitModelPart(...)}，还有 resetPose / 袖层可见性 / 手臂显隐一串模型状态整备
     * —— 复刻提交就得复刻这些 vanilla 内部逻辑，版本一动就烂。代理让 vanilla 逻辑原样跑完，
     * 只在提交穿过时换掉 RenderType。
     *
     * <h2>为什么敢用 identity 比较认出手臂的 RenderType</h2>
     * {@code RenderTypes.entityTranslucent} 是按贴图 memoize 的（姊妹线 26.2 字节码实读结论；
     * 本线为 {@code entityTranslucent(tex, true)}，{@code ScopeRenderTypes#createFlashTranslucentType}
     * 的注释亦按此对齐）。同一皮肤贴图永远拿到同一实例 —— 代理里 {@code ==} 即可精准命中，
     * 不会误伤同一次提交里的其他 RenderType。
     *
     * <h2>复用的就是火光那条管线</h2>
     * {@link ScopeRenderTypes#flashTranslucentClipped(Identifier)} 是
     * {@code entityTranslucent} 的逐状态克隆（含 vanilla 的
     * {@code affectsCrumbling() + sortOnUpload()}，后者不补会出现二层袖压一层臂的错序）
     * + 目镜孔径 discard（{@code ScopeDepthCopyState.Operation#MASK_OUTSIDE}）。
     * 手臂与火光在管线状态上无差别 —— 26.2 的 {@code ScopeBodyRenderTypes#armClipped}
     * 也是复用 {@code FLASH_TRANSLUCENT_CLIPPED_PIPELINE}，同一结论。
     *
     * <h2>失败哲学</h2>
     * 任一环节不满足（{@code SubmitNodeCollector} 代理构造失败、闸门为假）都
     * <b>原样返回真 collector</b>：最坏回到「镜内见手臂」的现状，绝不画错手臂。
     */
    private static SubmitNodeCollector wrapForScopeClip(SubmitNodeCollector real, Identifier skinTexture) {
        if (real == null) {
            return null;
        }
        try {
            final RenderType vanillaArm = RenderTypes.entityTranslucent(skinTexture);
            final RenderType clippedArm = ScopeRenderTypes.flashTranslucentClipped(skinTexture);
            SubmitNodeCollector proxy = (SubmitNodeCollector) Proxy.newProxyInstance(
                    SubmitNodeCollector.class.getClassLoader(),
                    new Class<?>[]{SubmitNodeCollector.class},
                    (p, method, args) -> {
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
            if (!LOGGED_ARM_CLIPPED) {
                LOGGED_ARM_CLIPPED = true;
                GunMod.LOGGER.info("[TACZ Scope] In-scope arm clipping engaged: first-person arms now discard "
                        + "inside the ocular (depth-aperture mode 2, reused flash-translucent pipeline).");
            }
            return proxy;
        } catch (Throwable t) {
            if (!LOGGED_ARM_CLIP_FAILED) {
                LOGGED_ARM_CLIP_FAILED = true;
                GunMod.LOGGER.warn("[TACZ Scope] In-scope arm clipping unavailable; arms keep vanilla "
                        + "rendering (no visual regression).", t);
            }
            return real;
        }
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack, int light) {
        // Legacy VertexConsumer path; Feature Rendering uses the collector overload.
    }
}

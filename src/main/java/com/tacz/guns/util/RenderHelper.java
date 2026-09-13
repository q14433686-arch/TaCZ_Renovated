package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.render.scope.ScopeBodyRenderTypes;
import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
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

    /**
     * 第一人称手臂提交（26.2 Feature Rendering 的 collector 路径）。
     *
     * <p><b>中和 vanilla 1.21.9+ 第一人称手臂的 {@code zRot=±0.1}</b>
     * （2026-09-13 同步自 Fabric 1.21.11 线 {@code 61ab4a0} / 26.1.2 线 {@code 44bb362a}，
     * 三线同一份 vanilla 代码）。</p>
     *
     * <p>症状（全枪械第一人称手部错位）：所有手枪整体偏左、手没握住枪；
     * 双管换弹时手部绑定/动画错位、弹药悬浮在手上方；错位量恒定、非常有规律。
     * 1.21.1 上游无此问题，26.2 / 26.1.2 / 1.21.11 全分支复现。</p>
     *
     * <p>根因是 vanilla 在 1.21.1 → 1.21.9 的渲染重构里给
     * {@code AvatarRenderer#renderHand} 加了两行（货源 commit 已对 1.21.11 反编译源码
     * 逐行确认，1.21.9 / 1.21.10 / 26.1.2 同样存在；1.21.1 的
     * {@code PlayerRenderer#renderArm} 没有这两行，手臂是笔直渲染的）：</p>
     * <pre>
     * model.leftArm.zRot = -0.1F;   // 约 -5.7°
     * model.rightArm.zRot = 0.1F;   // 约 +5.7°
     * </pre>
     * <p>而 TACZ 全部枪模的手部定位（{@code righthand_pos}/{@code lefthand_pos}）
     * 都是按 1.21.1 的 {@code zRot=0} 姿态 authored 的。手臂网格绕肩部 pivot
     * 凭空多转 ±5.7°，手相对枪恒定偏转 —— 正是「错位一点点、很规律」的来源。</p>
     *
     * <p>修复：每次 vanilla 手部调用<b>之后</b>把<b>两条</b>手臂的 {@code zRot} 清零。
     * 依据是 26.2 的提交语义：{@code submitModelPart} 只拷贝<b>矩阵</b>
     * （{@code Pose#copy}），{@code ModelPart} 是<b>活引用</b>，旋转要到
     * {@code submitHandsWithItems} 之后的 {@code renderAllFeatures} 才被读取 ——
     * 因此 submit 之后、flush 之前的写入决定最终姿态。</p>
     *
     * <p>为什么必须两条一起清：vanilla 每次调用<i>同时</i>污染左右两条（调右手也写左臂）。
     * 双持/换弹时会连续提交两次，后一次会把前一条重新污染。</p>
     *
     * <p>为什么不会破坏 vanilla 物品的手臂：本方法只在 TACZ 接管 viewmodel 时被调用
     * （{@code ItemInHandRendererMixin#tacz$submitArmWithAnimatedItem} 拦下 vanilla 的
     * {@code submitArmWithItem} 之后），TACZ 接管的 flush 里不存在 vanilla 手臂提交；
     * 纯 vanilla 的 flush 根本走不到这里。</p>
     *
     * <p>与「镜内裁手」的关系：{@link #wrapForScopeClip} 只决定 collector 是否套代理
     * （换 RenderType），与手臂骨骼姿态无关；清零这一步<b>无条件</b>执行。</p>
     *
     * <p>与 {@link com.tacz.guns.mixin.client.PlayerModelMixin} 第 0 帧那段手臂归零
     * 的关系：那段只在 {@code ageInTicks == 0} 的 setupAnim 尾部生效，而
     * {@code renderHand} 的 {@code ±0.1} 是 setupAnim <b>之后</b>写的，覆盖不到，
     * 两处不重复也不冲突。</p>
     */
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
            // 【手臂对齐修复】中和 vanilla 1.21.9+ 的 zRot=±0.1，见本方法注释。
            resetFirstPersonArmLean(avatar);
        } finally {
            FirstPersonAnimationCompat.endDirectArmRender();
        }
    }

    /**
     * 中和 vanilla 1.21.9+ {@code AvatarRenderer#renderHand} 写入的
     * {@code leftArm.zRot = -0.1F} / {@code rightArm.zRot = 0.1F}，
     * 把第一人称手臂精确还原成 1.21.1 的笔直姿态（TACZ 枪模的手部定位是按该姿态
     * authored 的）。必须在<b>每次</b> vanilla 手部调用之后、flush 之前执行，
     * 且两条手臂一起清（vanilla 每次调用会同时污染左右两条）。
     * 详见 {@link #renderFirstPersonArm(LocalPlayer, HumanoidArm, PoseStack, SubmitNodeCollector, int)}。
     */
    private static void resetFirstPersonArmLean(AvatarRenderer<?> avatar) {
        if (avatar.getModel() instanceof PlayerModel playerModel) {
            playerModel.leftArm.zRot = 0.0F;
            playerModel.rightArm.zRot = 0.0F;
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

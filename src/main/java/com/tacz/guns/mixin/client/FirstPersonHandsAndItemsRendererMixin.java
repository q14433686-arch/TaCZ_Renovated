package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import com.tacz.guns.compat.shader.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称枪械渲染的接管点（26.3 拆分后的<b>渲染侧</b>）。
 *
 * <p>26.3 把 {@code ItemInHandRenderer} 拆成了状态类
 * {@code net.minecraft.client.player.FirstPersonHandsAndItems} 与渲染类
 * {@code net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer}。
 * 收枪保持（{@code KeepingItemRenderer}）随 {@code mainHandItem} 字段留在
 * {@link FirstPersonHandsAndItemsMixin}；本类只负责 submit 系列。</p>
 *
 * <h2>26.3 签名变化（已对照 refab 26.3 §2.3 与 NeoForge 26.3 patch 核实）</h2>
 * <pre>
 * 26.2: submitHandsWithItems(float, PoseStack, SubmitNodeCollector, LocalPlayer, int)
 * 26.3: submitHandsWithItems(float, PoseStack, SubmitNodeCollector,
 *                            PlayerRenderState, FirstPersonHandsAndItemsRenderState)
 *
 * 26.2: submitArmWithItem(AbstractClientPlayer, float, float, InteractionHand, float,
 *                         ItemStack, float, PoseStack, SubmitNodeCollector, int)
 * 26.3: submitArmWithItem(PlayerRenderState, FirstPersonHandsAndItemsRenderState, float, float,
 *                         InteractionHand, float, ItemStack, float, PoseStack,
 *                         SubmitNodeCollector, int)
 * </pre>
 *
 * <p><b>玩家实体不再作为参数传入</b>：26.3 走渲染状态对象。TACZ 的渲染链路
 * 仍以 {@code LocalPlayer} 为输入，且第一人称本来就只可能是本地玩家，因此这里从
 * {@code Minecraft.getInstance().player} 取回；取不到就原样放行给 vanilla。</p>
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsAndItemsRendererMixin {

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, PoseStack pMatrixStack, SubmitNodeCollector pCollector,
                                 PlayerRenderState pPlayerState, FirstPersonHandsAndItemsRenderState pHandState,
                                 CallbackInfo ci) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new BeforeRenderHandEvent(pMatrixStack));
    }

    /**
     * 第一人称枪械渲染入口。<b>这是修复"枪相对摄像机位置/大小不对 + 移动时抖动"的关键。</b>
     *
     * <p>在 {@code submitHandsWithItems} 调用 {@code submitArmWithItem} 的位置包裹调用，
     * 遇到 TACZ 动画物品时不进入后者，直接执行第一人称渲染。这既保持 SBM 的取消语义和干净
     * PoseStack，也不会被其他 Mod 对 {@code submitArmWithItem} 的覆盖或手部变换抢走。</p>
     *
     * <p><b>26.3 待实测</b>：{@code submitArmWithItem} 在 26.3 是 {@code private}。
     * {@code @WrapOperation} 包裹的是 {@code submitHandsWithItems} 内部的 {@code INVOKE}
     * 指令，私有不影响 wrap（调用点仍在字节码里），但这一点只有运行期能确证。</p>
     */
    @WrapOperation(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
    )
    private void tacz$submitArmWithAnimatedItem(FirstPersonHandsAndItemsRenderer instance,
                                                PlayerRenderState playerState,
                                                FirstPersonHandsAndItemsRenderState handState,
                                                float frameInterp,
                                                float xRot,
                                                InteractionHand hand,
                                                float attack,
                                                ItemStack itemStack,
                                                float inverseArmHeight,
                                                PoseStack poseStack,
                                                SubmitNodeCollector collector,
                                                int lightCoords,
                                                Operation<Void> original) {
        // 26.3: 玩家实体不再随参数传入。第一人称渲染的对象只可能是本地玩家；
        // 取不到（例如渲染线程早于玩家就绪）就原样放行，绝不吞掉 vanilla 的调用。
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            original.call(instance, playerState, handState, frameInterp, xRot, hand, attack, itemStack,
                    inverseArmHeight, poseStack, collector, lightCoords);
            return;
        }

        ItemStack mainRenderStack = FirstPersonAnimationCompat.getMainRenderStack(localPlayer);
        boolean mainHandOwnedByTacz = FirstPersonAnimationCompat.isTaczViewmodel(mainRenderStack);

        if (hand == InteractionHand.OFF_HAND && mainHandOwnedByTacz) {
            return;
        }

        ItemStack renderStack = hand == InteractionHand.MAIN_HAND ? mainRenderStack : itemStack;
        var renderer = com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry.INSTANCE
                .get(renderStack.getItem());
        if (!(renderer instanceof AnimateGeoItemRenderer<?, ?> geoRenderer)
                || geoRenderer.getModel(renderStack) == null) {
            original.call(instance, playerState, handState, frameInterp, xRot, hand, attack, itemStack,
                    inverseArmHeight, poseStack, collector, lightCoords);
            return;
        }

        if (!ShaderCompat.shouldRenderInCurrentHandPhase(renderStack)) {
            return;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return;
        }

        ItemDisplayContext context = localPlayer.getMainArm() == HumanoidArm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (geoRenderer.needReInit(renderStack)) {
            geoRenderer.tryInit(renderStack, localPlayer, frameInterp);
        }
        poseStack.pushPose();
        geoRenderer.renderFirstPerson(localPlayer, renderStack, context, poseStack,
                collector, lightCoords, frameInterp);
        poseStack.popPose();
    }
}

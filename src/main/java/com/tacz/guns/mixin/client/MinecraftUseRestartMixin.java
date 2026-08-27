package com.tacz.guns.mixin.client;

import me.xjqsh.lrtactical.client.input.UsePressGate;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 右键没松手时，不让 LRTactical 物品自动开始<b>第二次</b>使用。
 *
 * <p>完整的病因、字节码证据与时序论证见 {@link UsePressGate} 的类注释；
 * 这里只负责在原版重新发起使用之前把它掐掉。</p>
 *
 * <h2>为什么在 HEAD 而不是更晚</h2>
 * {@code startUseItem} 内部会走到 {@code MultiPlayerGameMode#useItem}，
 * 而 {@code ServerboundUseItemPacket} 是在那儿的 {@code startPrediction} 回调里
 * 构造并送出的（先于 {@code ItemStack#use}）。只有在 HEAD 取消，
 * 才能保证「本地不进入使用状态」与「不给服务端发包」同时成立。
 *
 * <h2>为什么单独开一个 mixin</h2>
 * 本门禁要的是「压根不进入」这一种语义，且归属 LR 输入修复，
 * 单独成类便于单独审阅与单独摘除 —— 删掉 mixins json 里这一行即可整体回退。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftUseRestartMixin {

    @Inject(method = "startUseItem()V", at = @At("HEAD"), cancellable = true)
    private void lr$blockHeldUseRestart(CallbackInfo ci) {
        if (UsePressGate.shouldBlockRestart()) {
            ci.cancel();
        }
    }
}

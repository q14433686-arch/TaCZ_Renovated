package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.effect.HarmfulEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 状态效果注册（NeoForge 26.2）—— 闪光弹的致盲与耳鸣。
 *
 * <h2>WP-LR2 改写说明</h2>
 * refab 侧 Fabric 直注册后手动取 Holder；NeoForge 26.2 的 {@link DeferredHolder}
 * 仍实现 {@code Holder<MobEffect>}，因此消费方
 * （{@code MobEffectInstance} 构造、{@code getEffect}）**零改动**，
 * 直接把字段当 Holder 用即可——这正是 refab javadoc 里"要持有 Holder"
 * 诉求的 NeoForge 原生解。
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EquipmentMod.MOD_ID);

    /** 致盲：屏幕糊白/黑。实际表现见 {@code BlindnessOverlay}。 */
    public static final DeferredHolder<MobEffect, HarmfulEffect> BLIND =
            EFFECTS.register("blinded", () -> new HarmfulEffect(0xFFFFFF));

    /** 耳鸣/失聪：压低所有音效音量。实际表现见 {@code DeafenSoundHandler}。 */
    public static final DeferredHolder<MobEffect, HarmfulEffect> DEAFENED =
            EFFECTS.register("deafened", () -> new HarmfulEffect(0xFFFFFF));

    private ModEffects() {
    }
}

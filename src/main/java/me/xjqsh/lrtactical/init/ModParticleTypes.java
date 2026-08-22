package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 粒子类型注册（NeoForge 26.2）。
 *
 * <p>26.2 vanilla 将 {@link SimpleParticleType#SimpleParticleType(boolean)} 设为
 * {@code protected}。这里用匿名子类调用受保护构造器，不依赖 Fabric 工厂，也不依赖
 * NeoForge 自身对该构造器的访问转换。
 *
 * <p>{@code alwaysSpawn = true}：烟雾遮蔽是战术平衡项，不能因客户端将粒子数量设为
 * “最少”而失效。消费方通过 {@code DeferredHolder#get()} 取实例。
 */
public final class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EquipmentMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE_CLOUD =
            PARTICLE_TYPES.register("smoke_cloud", () -> new SimpleParticleType(true) {
            });

    private ModParticleTypes() {
    }
}

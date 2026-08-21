package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 粒子类型注册（NeoForge 26.1.2）。
 *
 * <h2>WP-LR2 改写说明</h2>
 * refab 侧因 Fabric 上 {@code SimpleParticleType} 构造器 protected 而走
 * {@code FabricParticleTypes.simple(true)}；**26.1.2 NeoForge 环境该构造器
 * 可直接 new**（WP07 坑 B-9，r28 编译实证；26.2 变 protected，前滚时需改
 * 工厂/匿名子类——已在 PORT_262_BRIEF 差异映射 I 节记录在案）。
 *
 * <p>{@code alwaysSpawn = true} 的玩法理由沿用 refab 注释：烟雾遮蔽是战术
 * 平衡项，不能因客户端把粒子调"最少"就失效。
 *
 * <p>消费方注意：取实例需 {@code .get()}（原裸字段 2 处调用点，LR2-2 跟改）。
 */
public final class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EquipmentMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE_CLOUD =
            PARTICLE_TYPES.register("smoke_cloud", () -> new SimpleParticleType(true));

    private ModParticleTypes() {
    }
}

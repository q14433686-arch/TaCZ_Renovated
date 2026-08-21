package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.entity.GrenadeEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 实体类型注册。
 *
 * <h2>与 NeoForge 版的差异</h2>
 * NeoForge 用 {@code DeferredRegister} + {@code DeferredHolder} 延迟注册；
 * Fabric 没有这套设施，直接调 {@code Registry.register} 即可。
 * 写法沿用本仓库 {@code com.tacz.guns.init.ModEntities} 的既有模式
 * （静态字段 + {@code init()} 触发类加载），保持全仓一致。
 *
 * <p>注意 {@code init()} 看似空方法，实为<b>必需</b>：
 * 调用它才会触发本类的静态初始化，进而执行下面的 {@code register}。
 * 这与 TACZ 侧同名方法的用意相同。
 */
public final class ModEntities {
    /** 26.1：注册只能在 RegisterEvent 窗口内执行（注册表冻结机制），见 ModItems 注释。 */
    public static EntityType<GrenadeEntity> GRENADE;
    public static EntityType<me.xjqsh.lrtactical.entity.StickyGrenadeEntity> STICKY_GRENADE;
    public static EntityType<me.xjqsh.lrtactical.entity.SmokeGrenadeEntity> SMOKE_GRENADE;
    public static EntityType<me.xjqsh.lrtactical.entity.EffectCloudGrenadeEntity> EFFECT_CLOUD_GRENADE;
    public static EntityType<me.xjqsh.lrtactical.entity.sp.SpEffectCloudEntity> SP_EFFECT_CLOUD;
    public static EntityType<me.xjqsh.lrtactical.entity.StunGrenadeEntity> STUN_GRENADE;

    private ModEntities() {
    }

    /** 效果云本体 —— 由 {@link #EFFECT_CLOUD_GRENADE} 落地后生成。 */
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, event -> {
            if (event.getRegistryKey() != Registries.ENTITY_TYPE) {
                return;
            }
            GRENADE = register("explode_grenade", GrenadeEntity.TYPE);
            STICKY_GRENADE = register("sticky_grenade", me.xjqsh.lrtactical.entity.StickyGrenadeEntity.TYPE);
            SMOKE_GRENADE = register("smoke_grenade", me.xjqsh.lrtactical.entity.SmokeGrenadeEntity.TYPE);
            EFFECT_CLOUD_GRENADE = register("effect_cloud_grenade", me.xjqsh.lrtactical.entity.EffectCloudGrenadeEntity.TYPE);
            SP_EFFECT_CLOUD = register("sp_effect_cloud", me.xjqsh.lrtactical.entity.sp.SpEffectCloudEntity.TYPE);
            STUN_GRENADE = register("stun_grenade", me.xjqsh.lrtactical.entity.StunGrenadeEntity.TYPE);
        });
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, name), type);
    }
}

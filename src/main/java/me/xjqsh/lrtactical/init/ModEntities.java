package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.entity.EffectCloudGrenadeEntity;
import me.xjqsh.lrtactical.entity.GrenadeEntity;
import me.xjqsh.lrtactical.entity.SmokeGrenadeEntity;
import me.xjqsh.lrtactical.entity.StickyGrenadeEntity;
import me.xjqsh.lrtactical.entity.StunGrenadeEntity;
import me.xjqsh.lrtactical.entity.sp.SpEffectCloudEntity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体类型注册（NeoForge 26.1.2）。
 *
 * <h2>WP-LR2 改写说明</h2>
 * DeferredRegister + supplier 包裹实体类静态 {@code TYPE} 字段——
 * 正是 WP07 坑 A-3 验证过的安全习语：{@code EntityType.Builder#build(ResourceKey)}
 * 不写注册表，实体类保留静态 TYPE 安全；supplier 把类加载推迟到
 * RegisterEvent 窗口。与主 mod {@code com.tacz.guns.init.ModEntities}
 * （{@code () -> EntityKineticBullet.TYPE}）完全同形。
 *
 * <p>消费方注意：取实例需 {@code .get()}（LR2-2/3 跟改；渲染器注册处
 * {@code ModEntitiesRender} 为主要消费点）。
 */
public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(EquipmentMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeEntity>> GRENADE =
            ENTITY_TYPES.register("explode_grenade", () -> GrenadeEntity.TYPE);

    public static final DeferredHolder<EntityType<?>, EntityType<StickyGrenadeEntity>> STICKY_GRENADE =
            ENTITY_TYPES.register("sticky_grenade", () -> StickyGrenadeEntity.TYPE);

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE =
            ENTITY_TYPES.register("smoke_grenade", () -> SmokeGrenadeEntity.TYPE);

    public static final DeferredHolder<EntityType<?>, EntityType<EffectCloudGrenadeEntity>> EFFECT_CLOUD_GRENADE =
            ENTITY_TYPES.register("effect_cloud_grenade", () -> EffectCloudGrenadeEntity.TYPE);

    /** 效果云本体 —— 由 {@link #EFFECT_CLOUD_GRENADE} 落地后生成。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SpEffectCloudEntity>> SP_EFFECT_CLOUD =
            ENTITY_TYPES.register("sp_effect_cloud", () -> SpEffectCloudEntity.TYPE);

    public static final DeferredHolder<EntityType<?>, EntityType<StunGrenadeEntity>> STUN_GRENADE =
            ENTITY_TYPES.register("stun_grenade", () -> StunGrenadeEntity.TYPE);

    private ModEntities() {
    }
}

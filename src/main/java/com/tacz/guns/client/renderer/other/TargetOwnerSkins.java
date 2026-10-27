package com.tacz.guns.client.renderer.other;

import com.tacz.guns.GunMod;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端共用的「标靶主人皮肤」查询，对标靶方块与标靶矿车生效。
 *
 * <h2>为什么是 26.2 的 {@link PlayerSkinRenderCache}</h2>
 *
 * <p>上游 1.20.1 在渲染线程上直接 {@code GameProfile} + {@code SkinManager#createLookup(...)}
 * 同步取皮肤。这条路径在 26.x 有两个问题：一是 {@code new GameProfile(null, 名字)} 在新
 * authlib 里直接抛 NPE；二是同步查询会卡住渲染线程。</p>
 *
 * <p>26.2 的做法与原版头颅（{@code SkullBlockRenderer}）一致：方块实体的档案先交给
 * {@link PlayerSkinRenderCache}，它<b>异步</b>解析「档案 → 皮肤」，命中缓存直接返回，
 * 未命中时立刻返回默认皮肤、解析完成后下一帧自动换成真皮肤。因此这里既不做阻塞查询，
 * 也不自己判断「有没有 UUID」。</p>
 *
 * <p><b>与 1.21.11 线的差异</b>：1.21.11 线从 {@code Minecraft#playerSkinRenderCache()}
 * 拿缓存；26.2 的渲染器构造参数已经携带该缓存（方块实体 {@code Context#playerSkinRenderCache()}、
 * 实体 {@code Context#getPlayerSkinRenderCache()}），所以本线改由渲染器持有并传进来 ——
 * 26.2 侧的可编译依据见 {@code docs/records/SISTER_SYNC_262_20261027.md}。</p>
 *
 * <p>兜底：任何意外都不允许崩渲染线程，退回缓存自带的默认皮肤渲染类型，并留一条 WARN
 * （只报一次，不是静默降级）。</p>
 */
public final class TargetOwnerSkins {
    private static final AtomicBoolean REPORTED_LOOKUP_FAILURE = new AtomicBoolean();

    private TargetOwnerSkins() {
    }

    /**
     * 取得（可能仍在异步解析中的）主人皮肤渲染类型。
     *
     * @param cache   渲染器构造时从 provider context 拿到的原版皮肤缓存，不可为 null
     * @param profile 方块实体保存的主人档案；调用方负责判空
     */
    public static RenderType renderType(PlayerSkinRenderCache cache, ResolvableProfile profile) {
        try {
            return cache.getOrDefault(profile).renderType();
        } catch (RuntimeException e) {
            if (REPORTED_LOOKUP_FAILURE.compareAndSet(false, true)) {
                GunMod.LOGGER.warn("[TaCZ] Target skin lookup failed; using the default skin", e);
            }
            return PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
        }
    }
}

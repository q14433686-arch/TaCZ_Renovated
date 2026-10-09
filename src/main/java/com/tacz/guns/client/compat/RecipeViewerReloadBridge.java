package com.tacz.guns.client.compat;

import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Rebuilds optional recipe-viewer registrations after TACZ's authoritative gun-pack cache arrives.
 *
 * <p>JEI and REI build TACZ categories and displays from the synchronized common indexes. A remote
 * gun-pack cache can arrive after the viewers' first registration pass, so registering categories
 * alone cannot make newly synced guns, workbenches, attachments, or ammo-query entries visible.
 * Requests are deliberately coalesced and deferred to {@link #tick(Minecraft)}: packet handling,
 * viewer internals, and client resources must all remain on the Minecraft client thread.</p>
 *
 * <p>The lightweight entry points are optional implementation APIs, so this class uses reflection
 * and only probes a viewer that is installed. The verified 26.3 shapes are JEI
 * {@code mezz.jei.common.Internal#restartJei()} (present in the JEI NeoForge jar — common module
 * ships unshaded; see {@link #refreshJei()} for why the Fabric lifecycle event must not be used)
 * and REI {@code me.shedaniel.rei.RoughlyEnoughItemsCoreClient#reloadPlugins} with two nullable
 * arguments. If either installed viewer has moved its entry point, one normal client resource
 * reload is used as a safe fallback for the connection; it is never retriggered by the fallback
 * itself.</p>
 *
 * <p>【NeoForge 26.3 移植说明】相对 Fabric 姊妹仓的差异：{@code FabricLoader#isModLoaded} 换成
 * {@link ModList#isLoaded}；tick 挂点从 {@code ClientTickEvents.END_CLIENT_TICK} 换成
 * {@code ClientGameEvents#onClientTickPost}（NeoForge {@code ClientTickEvent.Post}，同一轮询边界）；
 * JEI 的 {@code JeiLifecycleEvents.AFTER_RECIPES_UPDATED} 事件是 Fabric 侧 API，NeoForge 上
 * 永远探不到（ClassNotFound 直接落资源重载兜底），故保留探针但不指望它命中。</p>
 *
 * <p>26.3 兼容现状（guide §5）：REI 无 26.3 构建，IMPL 层已按门面策略停用；本类的 REI 分支
 * 全反射、无编译期依赖，保留待 REI 恢复发布后自动生效。</p>
 */
public final class RecipeViewerReloadBridge {
    private static boolean reloadRequested;
    private static boolean reloadInProgress;
    private static boolean resourceFallbackUsed;

    private RecipeViewerReloadBridge() {
    }

    /** Queues one coalesced reload after cache installation and index rebuilding have completed. */
    public static void requestReload() {
        if (hasJei() || hasRei()) {
            reloadRequested = true;
        }
    }

    /** Drops pending work when the client leaves before the synchronized cache can be used. */
    public static void clear() {
        reloadRequested = false;
        reloadInProgress = false;
        resourceFallbackUsed = false;
    }

    /** Runs from {@code ClientTickEvent.Post} after the client has a level and player. */
    public static void tick(Minecraft client) {
        if (!reloadRequested || reloadInProgress || client.level == null || client.player == null) {
            return;
        }

        reloadRequested = false;
        reloadInProgress = true;
        int tableCount = CommonAssetsManager.get().getAllBlocks().size();
        int recipeCount = CommonAssetsManager.get().getAllTableRecipes().size();
        GunMod.LOGGER.info("[TACZ Recipe Viewer] Refreshing after gun-pack sync ({} table(s), {} recipe(s)).",
                tableCount, recipeCount);

        // Do not short-circuit: when both viewers are installed each receives its own refresh attempt.
        boolean requiresResourceFallback = false;
        if (hasJei() && !refreshJei()) {
            requiresResourceFallback = true;
        }
        if (hasRei() && !refreshRei()) {
            requiresResourceFallback = true;
        }
        if (!requiresResourceFallback || resourceFallbackUsed) {
            reloadInProgress = false;
            if (requiresResourceFallback) {
                GunMod.LOGGER.warn("[TACZ Recipe Viewer] Lightweight refresh is unavailable; the one fallback for this connection was already used.");
            } else {
                GunMod.LOGGER.info("[TACZ Recipe Viewer] JEI/REI refresh completed.");
            }
            return;
        }

        // An unrecognised viewer implementation gets exactly one resource-reload fallback per
        // connection. Resource reload does not enqueue this bridge, preventing a reload loop.
        resourceFallbackUsed = true;
        GunMod.LOGGER.warn("[TACZ Recipe Viewer] Viewer reload hook unavailable; falling back once to a client resource reload.");
        try {
            client.reloadResourcePacks().whenComplete((unused, throwable) -> client.execute(() -> {
                reloadInProgress = false;
                if (throwable == null) {
                    GunMod.LOGGER.info("[TACZ Recipe Viewer] Fallback client resource refresh completed.");
                } else {
                    GunMod.LOGGER.warn("[TACZ Recipe Viewer] Client resource refresh failed; recipe viewer data may be stale.",
                            throwable);
                }
            }));
        } catch (RuntimeException exception) {
            reloadInProgress = false;
            GunMod.LOGGER.warn("[TACZ Recipe Viewer] Could not start the client resource refresh.", exception);
        }
    }

    /**
     * Restart JEI so it re-runs plugin registration with the freshly synced gun-pack cache.
     *
     * <h2>Why {@code Internal.restartJei()} and not a hand-fired lifecycle event</h2>
     * <p>The Fabric event was the 26.2 entry point, but JEI's listener for it is
     * (26.2 and 26.3 source, {@code ClientLifecycleHandler#registerEvents}):</p>
     * <pre>
     *   if (!receivedRecipeSync) Internal.clearClientRecipes();
     *   receivedRecipeSync = false;
     *   stopJei(); startJei();
     * </pre>
     * <p>The flag is only set by a real recipe-sync packet and is consumed by the first start.
     * Firing the event by hand afterwards therefore <b>throws away the server-synced recipe
     * map</b>; JEI then falls back to {@code VanillaClientRecipeLoader}, which loads recipes from
     * the vanilla pack only. Every {@code minecraft:crafting_*} recipe shipped by a mod datapack —
     * TACZ's own workbench/ammo-box/target recipes and any gun pack's — silently disappears from
     * JEI, while {@code tacz:gun_smith_table_crafting} entries survive only because our plugin
     * builds them from the gun-pack cache instead of the recipe map.</p>
     *
     * <p>JEI 26.3 exposes {@code mezz.jei.common.Internal#restartJei()}, which does the same
     * stop/start <b>without</b> clearing the synced recipes. It lives in JEI's common module and is
     * present in the NeoForge jar too. Prefer it; the Fabric event probe below is kept only for
     * shape parity with the sister repo and never resolves on this loader.</p>
     */
    private static boolean refreshJei() {
        try {
            Class<?> internal = Class.forName("mezz.jei.common.Internal");
            Method restart = internal.getMethod("restartJei");
            restart.invoke(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            GunMod.LOGGER.debug("[TACZ Recipe Viewer] JEI Internal.restartJei unavailable; trying the recipes-updated event.", exception);
        }
        try {
            Class<?> lifecycleEvents = Class.forName("mezz.jei.fabric.events.JeiLifecycleEvents");
            Object event = lifecycleEvents.getField("AFTER_RECIPES_UPDATED").get(null);
            Object invoker = event.getClass().getMethod("invoker").invoke(event);
            if (!(invoker instanceof Runnable runnable)) {
                throw new IllegalStateException("JEI recipe-update invoker is not Runnable");
            }
            GunMod.LOGGER.warn("[TACZ Recipe Viewer] Using JEI's AFTER_RECIPES_UPDATED event as a fallback; "
                    + "this JEI build may drop server-synced vanilla-type recipes from its view.");
            runnable.run();
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            GunMod.LOGGER.debug("[TACZ Recipe Viewer] JEI lightweight refresh unavailable.", exception);
            return false;
        }
    }

    /** REI 26.2.820 reloads all plugin stages through reloadPlugins(MutableLong, ReloadStage). */
    private static boolean refreshRei() {
        try {
            Class<?> coreClient = Class.forName("me.shedaniel.rei.RoughlyEnoughItemsCoreClient");
            Method reload = null;
            for (Method candidate : coreClient.getMethods()) {
                if (candidate.getName().equals("reloadPlugins")
                        && Modifier.isStatic(candidate.getModifiers())
                        && candidate.getParameterCount() == 2) {
                    reload = candidate;
                    break;
                }
            }
            if (reload == null) {
                throw new NoSuchMethodException("RoughlyEnoughItemsCoreClient.reloadPlugins(MutableLong, ReloadStage)");
            }
            // Null requests the full plugin-stage reload; REI's own 26.2 UI uses this same
            // two-argument entry point for a manual reload.
            reload.invoke(null, null, null);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            GunMod.LOGGER.debug("[TACZ Recipe Viewer] REI lightweight refresh unavailable.", exception);
            return false;
        }
    }

    private static boolean hasJei() {
        return ModList.get().isLoaded("jei");
    }

    private static boolean hasRei() {
        return ModList.get().isLoaded("roughlyenoughitems") || ModList.get().isLoaded("rei");
    }
}

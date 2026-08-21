package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.pal.PalAnimationManager;
import com.tacz.guns.compat.playeranimator.pal.PalAssetManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Compatibility facade migrated from the discontinued KosmX PlayerAnimator to
 * ZigyTheBird's Player Animation Library (PAL 1.2.5), following
 * TaCZ_Refabricated_Unofficial 26.1.2 (game semantics).
 *
 * <p>PAL is an OPTIONAL runtime dependency (modid {@code player_animation_library},
 * identical on both loaders of the merged 26.1 jar); compile classpath is
 * CurseForge file 8454167 ("1.2.5+26.1", merged Fabric+NeoForge jar) via
 * CurseMaven. The old stub comment claimed "PAL has no 26.1.2 API" - that was
 * wrong. (r17/r18 first tried {@code maven.modrinth:player-animation-library:1.2.5},
 * which never resolves: Modrinth version numbers for this project are
 * "1.2.5+26.1"-style — the compat was effectively dead on arrival.)</p>
 */
public final class PlayerAnimatorCompat {
    public static final Identifier LOWER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "lower_animation");
    public static final Identifier LOOP_UPPER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "loop_upper_animation");
    public static final Identifier ONCE_UPPER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "once_upper_animation");
    public static final Identifier ROTATION_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "rotation");

    private static final String PAL = "player_animation_library";
    private static boolean installed;

    private PlayerAnimatorCompat() {
    }

    public static void init() {
        installed = ModList.get().isLoaded(PAL);
        GunMod.LOGGER.info("[TACZ PAL] init: installed={} (modid={})", installed, PAL);
        if (installed) {
            PalAnimationManager.init();
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    /** 已报告过 miss 原因的 display id —— 每个只打一条，避免每帧刷屏。 */
    private static final Set<Identifier> REPORTED = ConcurrentHashMap.newKeySet();

    public static boolean hasPlayerAnimator3rd(LivingEntity livingEntity, GunDisplayInstance display) {
        if (!installed) {
            if (REPORTED.add(Identifier.fromNamespaceAndPath("tacz", "not_installed"))) {
                GunMod.LOGGER.warn("[TACZ PAL] compat inactive: modid '{}' is not loaded", PAL);
            }
            return false;
        }
        if (!(livingEntity instanceof AbstractClientPlayer)) {
            return false;
        }
        // 诊断：明确指出每个 display 走不进 PAL 分支的原因（每 id 一次）。
        var fileId = display.getPlayerAnimator3rd();
        if (fileId == null) {
            if (REPORTED.add(display.getDisplayId())) {
                GunMod.LOGGER.info("[TACZ PAL] display {} has no player_animator_3rd data (vanilla third-person animation used)", display.getDisplayId());
            }
            return false;
        }
        if (!PalAnimationManager.hasAnimations(display)) {
            if (REPORTED.add(fileId)) {
                GunMod.LOGGER.warn("[TACZ PAL] animation file {} is NOT loaded (expected as assets/{}/player_animator/{}.json in a gun pack)", fileId, fileId.getNamespace(), fileId.getPath());
            }
            return false;
        }
        return true;
    }

    public static void playAnimation(LivingEntity livingEntity, GunDisplayInstance display, float limbSwingAmount) {
        if (installed && livingEntity instanceof AbstractClientPlayer player) {
            PalAnimationManager.play(player, display, limbSwingAmount);
        }
    }

    public static void stopAllAnimation(LivingEntity livingEntity) {
        stopAllAnimation(livingEntity, 8);
    }

    public static void stopAllAnimation(LivingEntity livingEntity, int fadeTime) {
        if (installed && livingEntity instanceof AbstractClientPlayer player) {
            PalAnimationManager.stopAll(player, fadeTime);
        }
    }

    /**
     * Registers the PAL asset reload listener onto the client reload event.
     * Pass {@code event::addListener} from
     * {@code AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)}.
     */
    public static void registerReloadListener(BiConsumer<Identifier, PreparableReloadListener> register) {
        if (installed) {
            GunMod.LOGGER.info("[TACZ PAL] reload listener registered as {}", PalAssetManager.ID);
            register.accept(PalAssetManager.ID, PalAssetManager.INSTANCE);
        }
    }
}

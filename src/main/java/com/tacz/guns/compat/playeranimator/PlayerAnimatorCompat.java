package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.pal.PalAnimationManager;
import com.tacz.guns.compat.playeranimator.pal.PalAssetManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.function.BiConsumer;

/**
 * Compatibility facade migrated from the discontinued KosmX PlayerAnimator to
 * ZigyTheBird's Player Animation Library (PAL 1.2.5), following
 * TaCZ_Refabricated_Unofficial 26.1.2 (game semantics).
 *
 * <p>PAL is an OPTIONAL runtime dependency (modid {@code player_animation_library});
 * compile classpath is {@code maven.modrinth:player-animation-library:1.2.5}.
 * The old stub comment claimed "PAL has no 26.1.2 API" - that was wrong: PAL
 * publishes 26.1.2 builds and the Fabric port already integrates with it.</p>
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
        if (installed) {
            PalAnimationManager.init();
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity livingEntity, GunDisplayInstance display) {
        return installed && livingEntity instanceof AbstractClientPlayer
                && PalAnimationManager.hasAnimations(display);
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
            register.accept(PalAssetManager.ID, PalAssetManager.INSTANCE);
        }
    }
}

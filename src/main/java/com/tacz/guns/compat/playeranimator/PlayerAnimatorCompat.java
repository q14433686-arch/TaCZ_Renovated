package com.tacz.guns.compat.playeranimator;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiConsumer;
import java.util.zip.ZipFile;

/** Optional PAL/player-animator compat. Disabled until that mod publishes a 26.1.2 API. */
public final class PlayerAnimatorCompat {
    public static final Identifier LOWER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "lower_animation");
    public static final Identifier LOOP_UPPER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "loop_upper_animation");
    public static final Identifier ONCE_UPPER_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "once_upper_animation");
    public static final Identifier ROTATION_ANIMATION = Identifier.fromNamespaceAndPath("tacz", "rotation");

    private PlayerAnimatorCompat() {
    }

    public static void init() {
    }

    public static boolean isInstalled() {
        return false;
    }

    public static boolean loadAnimationFromZip(ZipFile zipFile, String zipPath) {
        return false;
    }

    public static void registerReloadListener(java.util.function.Consumer<PreparableReloadListener> register) {
    }

    public static void registerReloadListener(BiConsumer<Identifier, PreparableReloadListener> register) {
    }

    public static void playAnimation(LivingEntity entity, Identifier animation) {
    }

    public static void playAnimation(LivingEntity entity, com.tacz.guns.client.resource.GunDisplayInstance display, float limbSwingAmount) {
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity entity, com.tacz.guns.client.resource.GunDisplayInstance display) {
        return false;
    }

    public static void stopAllAnimation(LivingEntity entity) {
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity entity) {
        return false;
    }
}

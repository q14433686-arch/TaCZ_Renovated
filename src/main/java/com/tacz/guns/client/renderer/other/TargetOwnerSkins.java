package com.tacz.guns.client.renderer.other;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.concurrent.atomic.AtomicBoolean;

/** Client-only, nonblocking player-skin lookup shared by targets and target minecarts. */
public final class TargetOwnerSkins {
    private static final AtomicBoolean REPORTED_LOOKUP_FAILURE = new AtomicBoolean();

    private TargetOwnerSkins() {
    }

    public static Identifier texture(ResolvableProfile profile) {
        try {
            // Vanilla's cache resolves the profile asynchronously and supplies a default meanwhile.
            return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin().body().texturePath();
        } catch (RuntimeException e) {
            if (REPORTED_LOOKUP_FAILURE.compareAndSet(false, true)) {
                GunMod.LOGGER.warn("[TaCZ] Target skin lookup failed; using the default skin", e);
            }
            return DefaultPlayerSkin.get(profile.partialProfile()).body().texturePath();
        }
    }
}

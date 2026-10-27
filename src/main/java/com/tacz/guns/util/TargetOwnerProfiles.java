package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

/** Creates name-only profiles without passing a null UUID to authlib's GameProfile. */
public final class TargetOwnerProfiles {
    private TargetOwnerProfiles() {
    }

    @Nullable
    public static ResolvableProfile fromName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return ResolvableProfile.createUnresolved(name);
        } catch (RuntimeException e) {
            // An invalid name must not abort block placement, chunk loading or rendering.
            GunMod.LOGGER.warn("[TaCZ] Cannot create target owner profile for '{}': {}", name, e.toString());
            return null;
        }
    }
}

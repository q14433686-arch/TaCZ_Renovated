package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import net.minecraft.world.item.component.ResolvableProfile;

/** Runs the actual profile factory against stubs; it is not an in-game test. */
public final class TargetOwnerProfilesTest {
    public static void main(String[] args) {
        require(TargetOwnerProfiles.fromName(null) == null, "null name");
        require(TargetOwnerProfiles.fromName("") == null, "empty name");
        require(TargetOwnerProfiles.fromName(" \t") == null, "blank name");
        for (String name : new String[]{"Notch", "靶子 1", "a name far longer than sixteen characters"}) {
            ResolvableProfile profile = TargetOwnerProfiles.fromName(name);
            require(profile != null && name.equals(profile.testName()), "name preserved: " + name);
        }
        require(TargetOwnerProfiles.fromName("throws") == null, "creation failure must be contained");
        require(GunMod.LOGGER.warnings == 1, "creation failure should be reported");
        System.out.println("TargetOwnerProfilesTest: passed (stubbed profile API)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

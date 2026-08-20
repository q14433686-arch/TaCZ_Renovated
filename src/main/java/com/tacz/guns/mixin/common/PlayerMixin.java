package com.tacz.guns.mixin.common;

import com.tacz.guns.api.entity.ForcePose;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements ForcePose {
    @Unique
    private Pose tacz$forcedPose;

    @Override
    public Pose tacz$getForcedPose() {
        return tacz$forcedPose;
    }

    @Override
    public void tacz$setForcedPose(Pose pose) {
        this.tacz$forcedPose = pose;
    }
}

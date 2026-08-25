package com.tacz.guns.mixin.compat.punchy;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies Punchy first-person yield mixins only when Punchy is on the loading list.
 *
 * <p>Evidence: NeoForge loader {@code LoadingModList#getModFileById(String)} —
 * {@code ModList.get()} is not initialized yet when mixin plugins run. Punchy publishes
 * as mod id {@code punchy} (Epic Fight Compat {@code @IfModLoaded("punchy")},
 * Curse project 1374153).</p>
 */
public final class PunchyCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById("punchy") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}

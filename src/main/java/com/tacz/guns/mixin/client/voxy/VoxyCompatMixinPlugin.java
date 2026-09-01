package com.tacz.guns.mixin.client.voxy;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the Voxy scope-pass mixins only when Voxy is actually installed.
 *
 * <p>NeoForge 26.1.2 适配：与 {@code IrisCompatMixinPlugin} 同款证据 ——
 * mixin 插件运行时 {@code ModList.get()} 尚未初始化，必须走
 * {@code FMLLoader.getCurrent().getLoadingModList().getModFileById(...)}。
 */
public final class VoxyCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById("voxy") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

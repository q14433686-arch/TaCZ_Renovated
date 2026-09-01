package com.tacz.guns.mixin.client.voxy;

import net.neoforged.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 只在 Voxy 在场时应用 {@code tacz.voxy.mixins.json} 里的混入。
 *
 * <p>三个混入的目标类都属于 Voxy 自己（{@code me.cortex.voxy.*}），
 * Voxy 不在时它们根本不存在 —— 靠这个插件把整份配置跳过，
 * 而不是靠 {@code require = 0} 逐个静默失败（后者会在日志里留一堆噪音）。
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
        return ModList.get().isLoaded("voxy");
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

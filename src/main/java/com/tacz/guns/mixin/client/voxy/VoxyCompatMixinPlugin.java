package com.tacz.guns.mixin.client.voxy;

import net.neoforged.fml.loading.FMLLoader;
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
 *
 * <p>【26.3.0.51-beta / FML 12.0.8 实机崩溃修复】此前这里裸调
 * {@code ModList.get().isLoaded("voxy")}，而 mixin 准备期 {@code ModList.get()}
 * 尚未初始化（返回 null）→ NPE 被包成 InvalidMixinException 刷满日志
 * （RawOutput.log 2026-10-05，3 处 ERROR）。IrisCompatMixinPlugin 的注释早就
 * 记录了这个坑（loader-11.0.15 时代）—— 改用同一解法：查 FML 的
 * {@code LoadingModList}（mixin 期可用），不碰 {@code ModList}。
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

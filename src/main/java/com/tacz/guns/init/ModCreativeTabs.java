package com.tacz.guns.init;

import com.tacz.guns.GunMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GunMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OTHER_TAB = TABS.register("other",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.tab.tacz.other"))
                    .icon(() -> ModItems.GUN_SMITH_TABLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GUN_SMITH_TABLE.get());
                        output.accept(ModItems.WORKBENCH_111.get());
                        output.accept(ModItems.WORKBENCH_211.get());
                        output.accept(ModItems.WORKBENCH_121.get());
                        output.accept(ModItems.TARGET.get());
                        output.accept(ModItems.STATUE.get());
                        output.accept(ModItems.TARGET_MINECART.get());
                        output.accept(ModItems.AMMO_BOX.get());
                        output.accept(ModItems.MODERN_KINETIC_GUN.get());
                        output.accept(ModItems.AMMO.get());
                        output.accept(ModItems.ATTACHMENT.get());
                    }).build());
}

package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.item.GunSmithTableItem;

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
                        // These are data-driven items. Adding the bare registry item here creates
                        // an empty stack with no gun/ammo/attachment/table id, which explains the
                        // generic "item.tacz.*" names and blank dynamic models. Populate the tab
                        // with the same fully initialized stacks used by the workbench and tooltips.
                        GunSmithTableItem.fillItemCategory().forEach(output::accept);
                        for (GunTabType type : GunTabType.values()) {
                            AbstractGunItem.fillItemCategory(type).forEach(output::accept);
                        }
                        AmmoItem.fillItemCategory().forEach(output::accept);
                        for (AttachmentType type : AttachmentType.values()) {
                            if (type != AttachmentType.NONE) {
                                AttachmentItem.fillItemCategory(type).forEach(output::accept);
                            }
                        }

                        // Static and variant items do not carry gun-pack ids.
                        output.accept(ModItems.TARGET.get());
                        output.accept(ModItems.STATUE.get());
                        output.accept(ModItems.TARGET_MINECART.get());
                        AmmoBoxItem.fillItemCategory(output);
                    }).build());
}

package com.tacz.guns.api.item.ammo;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class AmmoSourceRegistry {
    private static final AmmoSource ENTITY_INVENTORY = new AmmoSource() {
        @Override
        public boolean hasAmmo(LivingEntity shooter, ItemStack gunItem) {
            return AmmoSourceRegistry.hasAmmo(handler(shooter), gunItem);
        }

        @Override
        public int consumeAmmo(LivingEntity shooter, ItemStack gunItem, int requestedAmount) {
            return AmmoSourceRegistry.consumeAmmo(handler(shooter), gunItem, requestedAmount);
        }
    };

    private AmmoSourceRegistry() {
    }

    private static ResourceHandler<ItemResource> handler(LivingEntity shooter) {
        if (shooter instanceof Player player) {
            return VanillaContainerWrapper.of(player.getInventory());
        }
        return EmptyResourceHandler.instance();
    }

    public static AmmoSource getAmmoSource(LivingEntity shooter, ItemStack gunItem) {
        return ENTITY_INVENTORY;
    }

    public static boolean hasAmmo(LivingEntity shooter, ItemStack gunItem) {
        return getAmmoSource(shooter, gunItem).hasAmmo(shooter, gunItem);
    }

    public static int consumeAmmo(LivingEntity shooter, ItemStack gunItem, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        int consumed = getAmmoSource(shooter, gunItem).consumeAmmo(shooter, gunItem, requestedAmount);
        return Math.max(0, Math.min(consumed, requestedAmount));
    }

    public static boolean hasAmmo(ResourceHandler<ItemResource> itemHandler, ItemStack gunItem) {
        for (int i = 0; i < itemHandler.size(); i++) {
            ItemStack ammoStack = ItemUtil.getStack(itemHandler, i);
            if (ammoStack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, ammoStack)) {
                return true;
            }
            if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunItem, ammoStack)) {
                return true;
            }
        }
        return false;
    }

    public static int consumeAmmo(ResourceHandler<ItemResource> itemHandler, ItemStack gunItem, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }

        int remaining = requestedAmount;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < itemHandler.size(); i++) {
                ItemResource resource = itemHandler.getResource(i);
                if (resource.isEmpty()) {
                    continue;
                }

                ItemStack ammoStack = ItemUtil.getStack(itemHandler, i);
                if (ammoStack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, ammoStack)) {
                    remaining -= itemHandler.extract(i, resource, remaining, transaction);
                    if (remaining <= 0) {
                        break;
                    }
                }
                if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunItem, ammoStack)) {
                    int boxAmmoCount = ammoBox.getAmmoCount(ammoStack);
                    int extractCount = Math.min(boxAmmoCount, remaining);
                    if (extractCount <= 0) {
                        continue;
                    }

                    int stackCount = itemHandler.getAmountAsInt(i);
                    int remainCount = boxAmmoCount - extractCount;
                    ammoBox.setAmmoCount(ammoStack, remainCount);
                    if (remainCount <= 0) {
                        ammoBox.setAmmoId(ammoStack, DefaultAssets.EMPTY_AMMO_ID);
                    }

                    int removed = itemHandler.extract(i, resource, stackCount, transaction);
                    int restored = removed == stackCount
                            ? itemHandler.insert(i, ItemResource.of(ammoStack), stackCount, transaction)
                            : 0;
                    if (restored != stackCount) {
                        // Closing an uncommitted transaction rolls back this replacement and
                        // every earlier extraction performed by this call.
                        return 0;
                    }

                    remaining -= extractCount;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            transaction.commit();
        }
        return requestedAmount - remaining;
    }
}

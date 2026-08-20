package com.tacz.guns.api.item.ammo;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

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

    private static IItemHandler handler(LivingEntity shooter) {
        if (shooter instanceof Player player) {
            return new InvWrapper(player.getInventory());
        }
        return EmptyItemHandler.INSTANCE;
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

    public static boolean hasAmmo(IItemHandler itemHandler, ItemStack gunItem) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack ammoStack = itemHandler.getStackInSlot(i);
            if (ammoStack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, ammoStack)) {
                return true;
            }
            if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunItem, ammoStack)) {
                return true;
            }
        }
        return false;
    }

    public static int consumeAmmo(IItemHandler itemHandler, ItemStack gunItem, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        int remaining = requestedAmount;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack ammoStack = itemHandler.getStackInSlot(i);
            if (ammoStack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, ammoStack)) {
                ItemStack extracted = itemHandler.extractItem(i, remaining, false);
                remaining -= extracted.getCount();
                if (remaining <= 0) {
                    break;
                }
            }
            if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunItem, ammoStack)) {
                int boxAmmoCount = ammoBox.getAmmoCount(ammoStack);
                int extractCount = Math.min(boxAmmoCount, remaining);
                int remainCount = boxAmmoCount - extractCount;
                ammoBox.setAmmoCount(ammoStack, remainCount);
                if (remainCount <= 0) {
                    ammoBox.setAmmoId(ammoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                remaining -= extractCount;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return requestedAmount - remaining;
    }
}

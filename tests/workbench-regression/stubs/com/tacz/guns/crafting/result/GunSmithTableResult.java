package com.tacz.guns.crafting.result;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public final class GunSmithTableResult {
    public void init() {}
    public Identifier getGroup() { return Identifier.fromNamespaceAndPath("tacz", "test"); }
    public ItemStack getResult() { return ItemStack.EMPTY; }
}

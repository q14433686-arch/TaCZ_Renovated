package com.tacz.guns.crafting.result;

import com.tacz.guns.GunMod;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class GunSmithTableResult {
    private static final Identifier EMPTY_GROUP = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "empty");
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";
    public static final String CUSTOM = "custom";

    private final ItemStack result;
    private final Identifier group;

    public GunSmithTableResult(ItemStack result, @Nullable Identifier group) {
        this.result = result;
        this.group = group == null ? EMPTY_GROUP : group;
    }

    public GunSmithTableResult(RawGunTableResult raw, @Nullable Identifier group) {
        this(ItemStack.EMPTY, group);
    }

    public GunSmithTableResult(com.google.gson.JsonElement json, @Nullable Identifier group) {
        this(ItemStack.EMPTY, group);
    }

    public void init() {
        // RawGunTableResult / CUSTOM item parse is work package ④ (needs gun-pack indexes).
    }

    public ItemStack getResult() {
        return result;
    }

    public Identifier getGroup() {
        return group;
    }
}

package com.tacz.guns.crafting.result;

import com.google.gson.JsonElement;
import com.tacz.guns.GunMod;
import com.tacz.guns.util.CraftingHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A resolved gun-smith recipe result.
 *
 * <p>Gun, ammo and attachment results are deliberately kept as
 * {@link RawGunTableResult} until the common indexes have finished loading. Custom results are
 * kept as JSON for the same reason: constructing an {@link ItemStack} during resource reload can
 * happen before all registries/components are ready. {@link #init()} is idempotent and is called
 * by the client screen and the server menu immediately before the result is used.</p>
 */
public class GunSmithTableResult {
    private static final Identifier EMPTY_GROUP = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "empty");
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";
    public static final String CUSTOM = "custom";

    private ItemStack result;
    private Identifier group;
    @Nullable
    private final RawGunTableResult rawResult;
    @Nullable
    private final JsonElement customResult;
    @Nullable
    private final Identifier groupOverride;
    private boolean initialized;

    public GunSmithTableResult(ItemStack result, @Nullable Identifier group) {
        this.result = result == null ? ItemStack.EMPTY : result;
        this.group = group == null ? EMPTY_GROUP : group;
        this.rawResult = null;
        this.customResult = null;
        this.groupOverride = null;
        this.initialized = true;
    }

    public GunSmithTableResult(RawGunTableResult raw, @Nullable Identifier group) {
        this.result = ItemStack.EMPTY;
        this.group = group == null ? EMPTY_GROUP : group;
        this.rawResult = raw;
        this.customResult = null;
        this.groupOverride = group;
        this.initialized = false;
    }

    public GunSmithTableResult(JsonElement json, @Nullable Identifier group) {
        this.result = ItemStack.EMPTY;
        this.group = group == null ? EMPTY_GROUP : group;
        this.rawResult = null;
        this.customResult = json == null ? null : json.deepCopy();
        this.groupOverride = group;
        this.initialized = false;
    }

    /**
     * Resolves a deferred result once the common/client indexes are available.
     * Bad third-party custom results degrade to an empty result instead of breaking the entire
     * resource reload.
     */
    public synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            if (rawResult != null) {
                GunSmithTableResult resolved = RawGunTableResult.init(rawResult);
                this.result = resolved.result;
                this.group = groupOverride == null ? resolved.group : groupOverride;
            } else if (customResult != null && customResult.isJsonObject()) {
                this.result = CraftingHelper.getItemStack(customResult.getAsJsonObject(), true);
            }
        } catch (RuntimeException exception) {
            GunMod.LOGGER.warn("Failed to resolve gun smith table result", exception);
            this.result = ItemStack.EMPTY;
        } finally {
            this.initialized = true;
        }
    }

    public ItemStack getResult() {
        return result;
    }

    public Identifier getGroup() {
        return group;
    }
}

package net.minecraft.core.registries;
import net.minecraft.resources.Identifier;
public final class BuiltInRegistries {
    public static final Registry RECIPE_TYPE = new Registry();
    public static final class Registry { public Identifier getKey(Object value) { return Identifier.fromNamespaceAndPath("tacz", "gun_smith_table_crafting"); } }
}

package net.minecraft.resources;
import java.util.Objects;
public final class Identifier {
    private final String value;
    private Identifier(String value) { this.value = value; }
    public static Identifier fromNamespaceAndPath(String namespace, String path) { return new Identifier(namespace + ":" + path); }
    public String getPath() { return value.substring(value.indexOf(':') + 1); }
    public Identifier withPath(String path) { return new Identifier(value.substring(0, value.indexOf(':') + 1) + path); }
    @Override public String toString() { return value; }
    @Override public boolean equals(Object other) { return other instanceof Identifier id && value.equals(id.value); }
    @Override public int hashCode() { return Objects.hash(value); }
}

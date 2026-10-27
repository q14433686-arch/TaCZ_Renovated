package net.minecraft.network.chat;
public record Component(String text) {
    public static Component literal(String text) { return new Component(text); }
    public static Component empty() { return literal(""); }
    public String getString() { return text; }
}

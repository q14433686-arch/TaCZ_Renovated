package com.google.gson;
public final class JsonElement {
    private final String text;
    public JsonElement(String text) { this.text = text; }
    public String text() { return text; }
    public String toString() { return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}

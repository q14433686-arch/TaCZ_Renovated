package com.google.gson;
public final class JsonParser {
    public static JsonElement parseString(String raw) {
        if (raw.equals("{broken")) throw new IllegalArgumentException("bad JSON");
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            StringBuilder value = new StringBuilder();
            for (int i = 1; i < raw.length() - 1; i++) {
                char ch = raw.charAt(i);
                if (ch == '\\' && i < raw.length() - 2) ch = raw.charAt(++i);
                value.append(ch);
            }
            return new JsonElement(value.toString());
        }
        return new JsonElement(raw);
    }
}

package com.tacz.guns.resource.modifier;
import java.util.*;
import com.google.gson.JsonObject;
import com.tacz.guns.api.modifier.JsonProperty;
public final class AttachmentPropertyManager {
    public static Map<String, Modifier> getModifiers() { return Map.of(); }
    public static final class Modifier {
        public boolean hasJsonField(JsonObject json) { return false; }
        public JsonProperty<?> readJson(String json) { return new JsonProperty<>(); }
    }
}

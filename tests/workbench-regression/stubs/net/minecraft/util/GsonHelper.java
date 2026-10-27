package net.minecraft.util;
import com.google.gson.JsonObject;
public final class GsonHelper { public static int getAsInt(JsonObject object, String name) { return object.get(name).getAsInt(); } }

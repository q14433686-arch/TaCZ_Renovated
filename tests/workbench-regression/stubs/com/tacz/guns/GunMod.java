package com.tacz.guns;
import java.util.*;
public final class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = new Logger();
    public static final class Logger {
        public final List<String> messages = new ArrayList<>();
        public void error(Object... args) { messages.add(Arrays.toString(args)); }
        public void warn(Object... args) { messages.add(Arrays.toString(args)); }
        public void info(Object... args) { messages.add(Arrays.toString(args)); }
        public void debug(Object... args) { messages.add(Arrays.toString(args)); }
    }
}

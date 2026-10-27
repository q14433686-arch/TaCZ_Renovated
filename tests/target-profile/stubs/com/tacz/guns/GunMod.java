package com.tacz.guns;

/** Logger stub for the standalone profile test, not the runtime mod class. */
public final class GunMod {
    public static final TestLogger LOGGER = new TestLogger();

    public static final class TestLogger {
        public int warnings;

        public void warn(String message, Object... args) {
            warnings++;
        }
    }
}

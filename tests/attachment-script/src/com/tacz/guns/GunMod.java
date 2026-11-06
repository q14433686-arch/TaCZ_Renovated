package com.tacz.guns;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试替身：真 GuMod 的 {@code LOGGER} 来自 {@code LogUtils.getLogger()}（需要 Minecraft），
 * 这里只要一个能接收 warn 的对象，顺便把日志留在内存里给断言用。
 */
public final class GunMod {
    /** 测试替身的日志缓冲区：断言「坏脚本有被记一条」用。 */
    public static final List<String> LOG = new ArrayList<>();

    public static final Logger LOGGER = new Logger();

    private GunMod() {
    }

    public static final class Logger {
        public void warn(String message, Throwable throwable) {
            LOG.add("WARN " + message + " :: " + throwable);
        }

        public void warn(String message) {
            LOG.add("WARN " + message);
        }

        public void error(String message, Throwable throwable) {
            LOG.add("ERROR " + message + " :: " + throwable);
        }

        public void info(String message) {
            LOG.add("INFO " + message);
        }
    }
}

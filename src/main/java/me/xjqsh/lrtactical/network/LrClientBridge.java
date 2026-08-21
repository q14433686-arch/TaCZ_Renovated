package me.xjqsh.lrtactical.network;

/**
 * Dedicated-safe 反射桥（WP-LR2）。与 {@code com.tacz.guns.network.ClientPacketBridge}
 * 同模式：Class.forName 让客户端类不进入本类常量池，dedicated 上静默跳过。
 * WP03/R1 教训：S2C 处理若与 codec 同类直引客户端类型，专服注册期即
 * NoClassDefFoundError。
 */
public final class LrClientBridge {
    private LrClientBridge() {
    }

    public static void invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> handlers = Class.forName("me.xjqsh.lrtactical.client.network.LrClientPacketHandlers");
            handlers.getMethod(method, types).invoke(null, args);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // dedicated server
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

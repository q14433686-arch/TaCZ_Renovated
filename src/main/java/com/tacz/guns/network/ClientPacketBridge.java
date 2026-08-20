package com.tacz.guns.network;

/**
 * Dedicated-safe reflection bridge. Class.forName keeps client types out of
 * this class's constant pool so Dist cleaner / dedicated classpath cannot
 * throw NoClassDefFoundError: LocalPlayer at payload registration.
 */
public final class ClientPacketBridge {
    private ClientPacketBridge() {
    }

    public static void invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> handlers = Class.forName("com.tacz.guns.client.network.ClientPacketHandlers");
            handlers.getMethod(method, types).invoke(null, args);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // dedicated server
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

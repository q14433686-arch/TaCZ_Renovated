package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputePlayerAimStateEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.tacz.guns.api.item.IGun;

/**
 * Shoulder Surfing 5.x plugin: gun items participate in adaptive aiming/camera behavior.
 * Discovered via {@code shouldersurfing_plugin.json} at the mod jar root — the same
 * mechanism on every loader (verified against PluginLoaderNeoForge on the 26.1.2 line:
 * it scans ModList mod files for that JSON and instantiates the entrypoints).
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    @Override
    public void register(IEventBus eventBus) {
        eventBus.register((ComputePlayerAimStateEventHandler) event -> {
            if (!event.getResult()) {
                event.setResult(event.getEntity().getMainHandItem().getItem() instanceof IGun
                        || event.getEntity().getOffhandItem().getItem() instanceof IGun);
            }
        });
    }
}

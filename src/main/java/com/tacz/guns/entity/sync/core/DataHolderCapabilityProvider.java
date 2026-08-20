package com.tacz.guns.entity.sync.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class DataHolderCapabilityProvider {
    private static final Map<Entity, DataHolderCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final DataHolder holder = new DataHolder();

    public static DataHolderCapabilityProvider get(Entity entity) {
        synchronized (INSTANCES) {
            return INSTANCES.computeIfAbsent(entity, e -> new DataHolderCapabilityProvider());
        }
    }

    public static Optional<DataHolderCapabilityProvider> maybeGet(Entity entity) {
        synchronized (INSTANCES) {
            return Optional.ofNullable(INSTANCES.get(entity));
        }
    }

    public static void remove(Entity entity) {
        synchronized (INSTANCES) {
            INSTANCES.remove(entity);
        }
    }

    public void invalidate() {
    }

    public Optional<DataHolder> getDataHolder() {
        return Optional.of(this.holder);
    }
}

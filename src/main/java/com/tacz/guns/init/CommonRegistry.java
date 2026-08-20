package com.tacz.guns.init;

import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.GunPackLoader;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Mod-bus listeners are registered from {@link com.tacz.guns.GunMod} via {@code addListener}.
 * Evidence: AddPackFindersEvent implements IModBusEvent ②; EntityAttributeModificationEvent implements IModBusEvent ②.
 */
public final class CommonRegistry {
    private static boolean LOAD_COMPLETE = false;

    private CommonRegistry() {
    }

    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(ModSyncedEntityData::init);
    }

    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }

    public static void registerAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> event.add(type, ModAttributes.BULLET_RESISTANCE));
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        GunPackLoader.INSTANCE.packType = FMLEnvironment.getDist().isClient()
                ? PackType.CLIENT_RESOURCES
                : PackType.SERVER_DATA;
        event.addRepositorySource(GunPackLoader.INSTANCE);
    }
}

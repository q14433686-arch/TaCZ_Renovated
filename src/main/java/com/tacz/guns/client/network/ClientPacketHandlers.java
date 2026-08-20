package com.tacz.guns.client.network;

import com.tacz.guns.api.LogicalSide;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunFireSelectEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.network.message.ServerMessageLevelUp;
import com.tacz.guns.network.message.ServerMessageRefreshRefitScreen;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.network.message.ServerMessageSwapItem;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import com.tacz.guns.network.message.event.ServerMessageGunDraw;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.network.message.event.ServerMessageGunFireSelect;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import com.tacz.guns.network.message.event.ServerMessageGunKill;
import com.tacz.guns.network.message.event.ServerMessageGunMelee;
import com.tacz.guns.network.message.event.ServerMessageGunReload;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.resource.network.CommonNetworkCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    private static LivingEntity living(int id) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static void onGunFire(ServerMessageGunFire message) {
        LivingEntity living = living(message.shooterId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunFireEvent(living, message.gunItemStack, LogicalSide.CLIENT));
        }
    }

    public static void onGunShoot(ServerMessageGunShoot message) {
        LivingEntity living = living(message.shooterId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunShootEvent(living, message.gunItemStack, LogicalSide.CLIENT));
        }
    }

    public static void onGunReload(ServerMessageGunReload message) {
        LivingEntity living = living(message.shooterId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunReloadEvent(living, message.gunItemStack, LogicalSide.CLIENT));
        }
    }

    public static void onGunDraw(ServerMessageGunDraw message) {
        LivingEntity living = living(message.entityId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunDrawEvent(living, message.previousGunItem, message.currentGunItem, LogicalSide.CLIENT));
        }
    }

    public static void onGunFireSelect(ServerMessageGunFireSelect message) {
        LivingEntity living = living(message.shooterId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunFireSelectEvent(living, message.gunItemStack, LogicalSide.CLIENT));
        }
    }

    public static void onGunMelee(ServerMessageGunMelee message) {
        LivingEntity living = living(message.shooterId);
        if (living != null) {
            NeoForge.EVENT_BUS.post(new GunMeleeEvent(living, message.gunItemStack, LogicalSide.CLIENT));
        }
    }

    public static void onGunHurt(ServerMessageGunHurt message) {
    }

    public static void onGunKill(ServerMessageGunKill message) {
    }

    public static void onSound(ServerMessageSound message) {
        SoundPlayManager.playMessageSound(message);
    }

    public static void onCraft(ServerMessageCraft message) {
        if (Minecraft.getInstance().screen instanceof GunSmithTableScreen screen) {
            screen.updateIngredientCount();
        }
    }

    public static void onRefreshRefit(ServerMessageRefreshRefitScreen message) {
        GunRefitScreen.refresh();
    }

    public static void onSwapItem(ServerMessageSwapItem message) {
        NeoForge.EVENT_BUS.post(new SwapItemWithOffHand());
    }

    public static void onLevelUp(ServerMessageLevelUp message) {
    }

    public static void onUpdateEntityData(ServerMessageUpdateEntityData message) {
    }

    public static void onSyncGunPack(ServerMessageSyncGunPack message) {
        CommonNetworkCache.INSTANCE.fromNetwork(message.getCache());
        ClientIndexManager.reload();
    }

    public static void onSyncBaseTimestamp(ServerMessageSyncBaseTimestamp message) {
        LocalPlayerDataHolder.clientBaseTimestamp = System.currentTimeMillis();
    }
}

package com.tacz.guns.client.network;

import com.tacz.guns.api.LogicalSide;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.item.IGun;
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
import com.tacz.guns.client.compat.RecipeViewerReloadBridge;
import com.tacz.guns.network.message.ClientMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.network.message.ServerMessageLevelUp;
import com.tacz.guns.network.message.ServerMessageRefreshRefitScreen;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.network.message.ServerMessageSwapItem;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

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
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity bullet = level.getEntity(message.bulletId);
        Entity hurtEntity = level.getEntity(message.hurtEntityId);
        LivingEntity attacker = level.getEntity(message.attackerId) instanceof LivingEntity living ? living : null;
        NeoForge.EVENT_BUS.post(new EntityHurtByGunEvent.Post(
                bullet, hurtEntity, attacker,
                message.getGunId(), message.getGunDisplayId(), message.getAmount(), null,
                message.isHeadShot(), message.getHeadshotMultiplier(), LogicalSide.CLIENT
        ));
    }

    public static void onGunKill(ServerMessageGunKill message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity bullet = level.getEntity(message.bulletId);
        LivingEntity killedEntity = level.getEntity(message.killEntityId) instanceof LivingEntity living ? living : null;
        LivingEntity attacker = level.getEntity(message.attackerId) instanceof LivingEntity living ? living : null;
        NeoForge.EVENT_BUS.post(new EntityKillByGunEvent(
                bullet, killedEntity, attacker,
                message.getGunId(), message.getGunDisplayId(), message.getBaseDamage(), null,
                message.isHeadShot(), message.getHeadshotMultiplier(), LogicalSide.CLIENT
        ));
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
        // The server has just synchronized the modified gun stack. Rebuild the local
        // AttachmentCacheProperty as well; ADS, recoil, RPM, weight and silence consumers
        // read this cache rather than recalculating directly from the inventory every tick.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && IGun.getIGunOrNull(minecraft.player.getMainHandItem()) != null) {
            AttachmentPropertyManager.postChangeEvent(minecraft.player, minecraft.player.getMainHandItem());
        }
        GunRefitScreen.refresh();
    }

    public static void onSwapItem(ServerMessageSwapItem message) {
        NeoForge.EVENT_BUS.post(new SwapItemWithOffHand());
    }

    public static void onLevelUp(ServerMessageLevelUp message) {
        // Intentional no-op: the current TaCZ 1.1.8 API has no level/experience manager,
        // no server sender, and AbstractGunItem reports max level zero. Do not fabricate
        // a client-side progression or toast for this reserved compatibility payload.
    }

    public static void onUpdateEntityData(ServerMessageUpdateEntityData message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(message.entityId);
        if (entity == null) {
            return;
        }
        var syncedData = com.tacz.guns.entity.sync.core.SyncedEntityData.instance();
        message.getEntries().forEach(entry -> syncedData.set(entity, entry.getKey(), entry.getValue()));
    }

    public static void onSyncGunPack(ServerMessageSyncGunPack message) {
        CommonNetworkCache.INSTANCE.fromNetwork(message.getCache());
        ClientIndexManager.reload();
        RecipeViewerReloadBridge.requestReload();

        // Creative tab contents are built before the integrated server sends the gun-pack cache.
        // Rebuild them now so the tab receives initialized gun/ammo/attachment/workbench stacks
        // instead of the bare registry items that have no data-pack id or dynamic model.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.getConnection() != null && minecraft.player != null) {
            // LocalPlayer does not expose the server permission level in 26.1.2.
            // Creative mode is the same gate used by the client tab screen for this local world;
            // the custom TaCZ tab itself does not depend on this flag.
            boolean hasPermissions = minecraft.player.isCreative();
            // tryRebuildTabContents intentionally skips identical feature/permission inputs.
            // Flip once to invalidate the pre-sync build, then rebuild with the real permission
            // state so vanilla operator-only tabs are not left in the temporary state.
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.getConnection().enabledFeatures(), !hasPermissions, minecraft.level.registryAccess());
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.getConnection().enabledFeatures(), hasPermissions, minecraft.level.registryAccess());

            // 防御性修复（时序隐患）：上面两次静态 tryRebuildTabContents 只重建了各标签页的展示列表，
            // 不会重建创造模式搜索栏查询的 SessionSearchTrees（1.21.x 起搜索栏查的是异步构建的
            // FullTextSearchTree，而非 getHoverName）；同时它把 vanilla 的静态 CACHED_PARAMETERS 钉成了
            // 与屏幕后续相同的参数，玩家再打开创造背包时 CreativeModeInventoryScreen#tryRebuildTabContents
            // 会因「参数未变」返回 false 而跳过搜索树重建，搜索树停在空索引上 → 输入任何关键词都无结果。
            // 26.2 / 1.21.11 线已实机复现（同源修复见 1.21.11 线 PR #41）；26.1.2 线因同步到达更早、
            // 本 if 块可能整体被跳过而未复现，但代码缺陷完全一致，这里镜像原版屏幕内同款逻辑显式补建。
            // List.copyOf 做不可变快照；updateCreativeTooltips 内部在 Util.backgroundExecutor 上异步构建，
            // 主线程调用安全；其 TooltipFlag 为 NORMAL.asCreative()，不触发本 mod 带 isAdvanced() 门禁的 TooltipEvent。
            net.minecraft.client.multiplayer.SessionSearchTrees searchTrees = minecraft.getConnection().searchTrees();
            List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            searchTrees.updateCreativeTooltips(minecraft.level.registryAccess(), searchItems);
            searchTrees.updateCreativeTags(searchItems);
        }
    }

    public static void onSyncBaseTimestamp(ServerMessageSyncBaseTimestamp message) {
        LocalPlayerDataHolder.clientBaseTimestamp = System.currentTimeMillis();
        // Notify the server to update its baseTimestamp too, preventing unbounded
        // timestamp drift between client and server that makes every shoot fail the
        // network timestamp check (alpha out of [-300, 300+2*tick]).
        ClientPacketDistributor.sendToServer(ClientMessageSyncBaseTimestamp.INSTANCE);
    }
}

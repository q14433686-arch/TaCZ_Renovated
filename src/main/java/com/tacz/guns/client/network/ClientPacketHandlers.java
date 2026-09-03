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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
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
        if (Minecraft.getInstance().gui.screen() instanceof GunSmithTableScreen screen) {
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
        ClientPacketListener connection = minecraft.getConnection();
        if (minecraft.level != null && connection != null && minecraft.player != null) {
            // 26.2: the creative tab screen derives this gate from
            // Player#canUseGameMasterBlocks() && Options#operatorItemsTab(). isCreative() is not the
            // same predicate any more (since 1.21.11 canUseGameMasterBlocks reads the permission
            // set, not Abilities#instabuild), so mirroring the screen keeps the contents built here
            // byte-identical to the ones the screen would build: the operator tab is neither served
            // to a creative-but-not-op player nor stripped from an op player.
            boolean hasPermissions =
                    minecraft.player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
            // tryRebuildTabContents intentionally skips identical feature/permission inputs.
            // Flip once to invalidate the pre-sync build, then rebuild with the real permission
            // state so vanilla operator-only tabs are not left in the temporary state. The second
            // pass is also what makes the search tab see the gun-pack stacks: it aggregates the
            // other tabs' search stacks, so it needs one full pass after those tabs were filled.
            CreativeModeTabs.tryRebuildTabContents(
                    connection.enabledFeatures(), !hasPermissions, minecraft.level.registryAccess());
            CreativeModeTabs.tryRebuildTabContents(
                    connection.enabledFeatures(), hasPermissions, minecraft.level.registryAccess());
            refreshCreativeSearchTrees(connection, minecraft.level);
        }
    }

    /**
     * Re-indexes the creative search trees from the tab contents that were just rebuilt.
     *
     * <p>Rebuilding the tab display lists is not enough to make the stacks searchable. The creative
     * search bar queries the asynchronous trees owned by {@link SessionSearchTrees}, and vanilla
     * only feeds them from {@code CreativeModeInventoryScreen#tryRebuildTabContents} -- a method that
     * returns before its indexing step whenever {@code CreativeModeTabs.tryRebuildTabContents}
     * reports "parameters unchanged". The two rebuild calls above leave the memoized parameters
     * equal to the ones the screen presents on its next open, so the screen short-circuits, the
     * trees keep their initial {@code SearchTree.empty()} value and every keyword silently matches
     * nothing (guns, ammo, attachments, workbench and the LRTactical tab alike).
     *
     * <p>This mirrors the indexing loop NeoForge 26.2 puts in that screen method: every tab with a
     * search bar is indexed under its own key, and the global search tab maps to
     * {@link SessionSearchTrees#CREATIVE_NAMES} / {@link SessionSearchTrees#CREATIVE_TAGS}. Running
     * it here is what the screen would have run, so it is also harmless in the cases where vanilla
     * does rebuild and re-index on its own.
     *
     * <p>Runs on the client main thread (the payload handler enqueues work), which is the thread
     * vanilla indexes from; the tree build itself is dispatched to the background executor by
     * {@code SessionSearchTrees}.
     */
    private static void refreshCreativeSearchTrees(ClientPacketListener connection, ClientLevel level) {
        SessionSearchTrees searchTrees = connection.searchTrees;
        HolderLookup.Provider holders = level.registryAccess();
        CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar).forEach(tab -> {
            List<ItemStack> stacks = List.copyOf(tab.getDisplayItems());
            searchTrees.updateCreativeTooltips(
                    holders, stacks, CreativeModeTabSearchRegistry.getNameSearchKey(tab));
            searchTrees.updateCreativeTags(stacks, CreativeModeTabSearchRegistry.getTagSearchKey(tab));
        });
    }

    public static void onSyncBaseTimestamp(ServerMessageSyncBaseTimestamp message) {
        LocalPlayerDataHolder.clientBaseTimestamp = System.currentTimeMillis();
        // Notify the server to update its baseTimestamp too, preventing unbounded
        // timestamp drift between client and server that makes every shoot fail the
        // network timestamp check (alpha out of [-300, 300+2*tick]).
        ClientPacketDistributor.sendToServer(ClientMessageSyncBaseTimestamp.INSTANCE);
    }
}

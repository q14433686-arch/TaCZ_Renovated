package com.tacz.guns.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.vmlib.LuaGunLogicConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.manager.AttachmentDataManager;
import com.tacz.guns.resource.manager.AttachmentsTagManager;
import com.tacz.guns.resource.manager.CommonDataManager;
import com.tacz.guns.resource.manager.INetworkCacheReloadListener;
import com.tacz.guns.resource.manager.LootInjectionManager;
import com.tacz.guns.resource.manager.RecipeFilterManager;
import com.tacz.guns.resource.manager.ScriptManager;
import com.tacz.guns.resource.manager.TableRecipeManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.resource.pojo.data.loot.LootTableInjection;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import com.tacz.guns.resource.serialize.CommonAmmoIndexSerializer;
import com.tacz.guns.resource.serialize.CommonAttachmentIndexSerializer;
import com.tacz.guns.resource.serialize.CommonBlockIndexSerializer;
import com.tacz.guns.resource.serialize.CommonGunIndexSerializer;
import com.tacz.guns.resource.serialize.DistanceDamagePairSerializer;
import com.tacz.guns.resource.serialize.GunSmithTableIngredientSerializer;
import com.tacz.guns.resource.serialize.GunSmithTableResultSerializer;
import com.tacz.guns.resource.serialize.IdentifierSerializer;
import com.tacz.guns.resource.serialize.IgniteSerializer;
import com.tacz.guns.resource.serialize.ModifierTypeAdapterFactory;
import com.tacz.guns.resource.serialize.PairSerializer;
import com.tacz.guns.resource.serialize.Vec3Serializer;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Evidence: AddServerReloadListenersEvent#addListener(Identifier, PreparableReloadListener) ②
 * PreparableReloadListener.reload(SharedState, Executor, PreparationBarrier, Executor) ①
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class CommonAssetsManager implements ICommonResourceProvider {
    private static CommonAssetsManager INSTANCE;
    public static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredientSerializer())
            .registerTypeAdapter(GunSmithTableResult.class, new GunSmithTableResultSerializer())
            .registerTypeAdapter(ExtraDamage.DistanceDamagePair.class, new DistanceDamagePairSerializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(Ignite.class, new IgniteSerializer())
            .registerTypeAdapter(RecipeFilter.class, new RecipeFilter.Deserializer())
            .registerTypeAdapter(CommonGunIndex.class, new CommonGunIndexSerializer())
            .registerTypeAdapter(CommonAmmoIndex.class, new CommonAmmoIndexSerializer())
            .registerTypeAdapter(CommonAttachmentIndex.class, new CommonAttachmentIndexSerializer())
            .registerTypeAdapter(CommonBlockIndex.class, new CommonBlockIndexSerializer())
            .registerTypeAdapter(TabConfig.class, new TabConfig.Deserializer())
            // 配件内联 function 在反序列化时预编译（数据包加载与网络同步两条路径共用）。
            .registerTypeAdapterFactory(new ModifierTypeAdapterFactory())
            .create();

    private final List<INetworkCacheReloadListener> listeners = new ArrayList<>();
    /**
     * 本次资源重载预压缩好的枪包同步分片。重载完成后只压缩一次，所有玩家复用同一组
     * 不可变分片；{@code List.of()} 表示「还没有可发的快照」。
     */
    private List<ServerMessageSyncGunPack> networkSyncMessages = List.of();
    private CommonDataManager<GunData> gunData;
    private CommonDataManager<AttachmentData> attachmentData;
    private CommonDataManager<BlockData> blockData;
    private CommonDataManager<CommonAmmoIndex> ammoIndex;
    private CommonDataManager<CommonGunIndex> gunIndex;
    private CommonDataManager<CommonAttachmentIndex> attachmentIndex;
    private CommonDataManager<CommonBlockIndex> blockIndex;
    private CommonDataManager<TableRecipe> tableRecipe;
    private RecipeFilterManager recipeFilterManager;
    private LootInjectionManager lootInjectionManager;
    private AttachmentsTagManager attachmentsTagManager;
    List<LuaLibrary> libList = List.of(new LuaGunLogicConstant());
    private final ScriptManager scriptManager = new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList);
    public RecipeManager recipeManager;

    public void reloadAndRegister(AddServerReloadListenersEvent event) {
        AtomicInteger anon = new AtomicInteger();
        java.util.function.Consumer<PreparableReloadListener> register = listener -> {
            Identifier key;
            if (listener instanceof com.tacz.guns.resource.manager.JsonDataManager<?> json) {
                key = json.ID;
            } else {
                String name = listener.getClass().getSimpleName();
                if (name.isEmpty() || name.contains("$")) {
                    name = "listener_" + anon.incrementAndGet();
                }
                key = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, name.toLowerCase(Locale.ROOT));
            }
            event.addListener(key, listener);
        };

        gunData = registerNetwork(new CommonDataManager<>(DataType.GUN_DATA, GunData.class, GSON, "data/guns", "GunDataLoader"), register);
        attachmentData = registerNetwork(new AttachmentDataManager(), register);
        // 2026-08-21 LAN 实测回归修复：这两个管理器实现了 INetworkCacheReloadListener，
        // 但此前只 register.accept、未入 listeners —— ATTACHMENT_TAGS 与 RECIPE_FILTER
        // 从不进 getNetworkCache() 同步包。客户端缺 RecipeFilter 时，BLOCK_INDEX 的
        // CommonBlockIndex.checkData 第二个 Precondition 必炸（"there is no corresponding
        // data file"，默认包 tacz:gun_smith_table 也不能幸免）；缺 ATTACHMENT_TAGS 则
        // 客户端配件允装判断静默失效。
        // 证据：③ refab 26.1.2 CommonAssetsManager#reloadAndRegister —— 其 register(...)
        // 助手把 attachmentsTagManager 与 recipeFilterManager 一并加入 listeners（全同步），
        // 仅 lootInjectionManager 与 scriptManager 不同步。本处对齐该接线。
        attachmentsTagManager = registerNetwork(new AttachmentsTagManager(), register);
        recipeFilterManager = registerNetwork(new RecipeFilterManager(), register);
        lootInjectionManager = new LootInjectionManager();
        register.accept(lootInjectionManager);
        blockData = registerNetwork(new CommonDataManager<>(DataType.BLOCK_DATA, BlockData.class, GSON, "data/blocks", "BlockDataLoader"), register);
        register.accept(scriptManager);

        ammoIndex = registerNetwork(new CommonDataManager<>(DataType.AMMO_INDEX, CommonAmmoIndex.class, GSON, "index/ammo", "AmmoIndexLoader"), register);
        gunIndex = registerNetwork(new CommonDataManager<>(DataType.GUN_INDEX, CommonGunIndex.class, GSON, "index/guns", "GunIndexLoader"), register);
        attachmentIndex = registerNetwork(new CommonDataManager<>(DataType.ATTACHMENT_INDEX, CommonAttachmentIndex.class, GSON, "index/attachments", "AttachmentIndexLoader"), register);
        blockIndex = registerNetwork(new CommonDataManager<>(DataType.BLOCK_INDEX, CommonBlockIndex.class, GSON, "index/blocks", "BlockIndexLoader"), register);
        tableRecipe = registerNetwork(new TableRecipeManager(), register);

        // 1.2.0：同步快照在重载尾端一次性压缩好。顺序很重要 —— 必须在所有
        // INetworkCacheReloadListener 产出的缓存都写回之后取 getNetworkCache()，
        // 且要先清空上一轮的快照，避免重载失败时把旧数据当成新数据发出去。
        register.accept((sharedState, backgroundExecutor, barrier, gameExecutor) ->
                barrier.wait(null).thenRunAsync(() -> {
                    AllowAttachmentTagMatcher.resetCache();
                    networkSyncMessages = List.of();
                    networkSyncMessages = ServerMessageSyncGunPack.createMessages(getNetworkCache());
                }, gameExecutor));
    }

    private <T extends INetworkCacheReloadListener> T registerNetwork(T listener, java.util.function.Consumer<PreparableReloadListener> register) {
        listeners.add(listener);
        register.accept(listener);
        return listener;
    }

    public Map<DataType, Map<Identifier, String>> getNetworkCache() {
        ImmutableMap.Builder<DataType, Map<Identifier, String>> builder = ImmutableMap.builder();
        for (INetworkCacheReloadListener listener : listeners) {
            Map<Identifier, String> cache = listener.getNetworkCache();
            if (cache != null) {
                builder.put(listener.getType(), cache);
            }
        }
        return builder.build();
    }

    @Nullable
    @Override
    public GunData getGunData(Identifier id) {
        return gunData == null ? null : gunData.getData(id);
    }

    @Nullable
    @Override
    public AttachmentData getAttachmentData(Identifier id) {
        return attachmentData == null ? null : attachmentData.getData(id);
    }

    @Nullable
    @Override
    public BlockData getBlockData(Identifier id) {
        return blockData == null ? null : blockData.getData(id);
    }

    @Override
    @Nullable
    public RecipeFilter getRecipeFilter(Identifier id) {
        return recipeFilterManager == null ? null : recipeFilterManager.getFilter(id);
    }

    public Set<Identifier> getLootInjectionTargets() {
        if (lootInjectionManager == null) {
            return Set.of();
        }
        return lootInjectionManager.getInjectionTargets();
    }

    public List<LootTableInjection> getLootTableInjections(Identifier lootTable) {
        if (lootInjectionManager == null) {
            return List.of();
        }
        return lootInjectionManager.getInjections(lootTable);
    }

    @Nullable
    @Override
    public CommonGunIndex getGunIndex(Identifier gunId) {
        return gunIndex == null ? null : gunIndex.getData(gunId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonGunIndex>> getAllGuns() {
        return gunIndex == null ? Set.of() : gunIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAmmoIndex getAmmoIndex(Identifier ammoId) {
        return ammoIndex == null ? null : ammoIndex.getData(ammoId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonAmmoIndex>> getAllAmmos() {
        return ammoIndex == null ? Set.of() : ammoIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAttachmentIndex getAttachmentIndex(Identifier attachmentId) {
        return attachmentIndex == null ? null : attachmentIndex.getData(attachmentId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonAttachmentIndex>> getAllAttachments() {
        return attachmentIndex == null ? Set.of() : attachmentIndex.getAllData().entrySet();
    }

    @Override
    public LuaTable getScript(Identifier scriptId) {
        return scriptManager.getScript(scriptId);
    }

    @Nullable
    @Override
    public CommonBlockIndex getBlockIndex(Identifier blockId) {
        return blockIndex == null ? null : blockIndex.getData(blockId);
    }

    @Override
    public TableRecipe getTableRecipe(Identifier recipeId) {
        return tableRecipe == null ? null : tableRecipe.getData(recipeId);
    }

    @Override
    public Set<Map.Entry<Identifier, TableRecipe>> getAllTableRecipes() {
        return tableRecipe == null ? java.util.Collections.emptySet() : tableRecipe.getAllData().entrySet();
    }

    @Override
    public Set<Map.Entry<Identifier, CommonBlockIndex>> getAllBlocks() {
        return blockIndex == null ? Set.of() : blockIndex.getAllData().entrySet();
    }

    @Override
    public Set<String> getAttachmentTags(Identifier registryName) {
        return attachmentsTagManager == null ? Set.of() : attachmentsTagManager.getAttachmentTags(registryName);
    }

    @Override
    public Set<String> getAllowAttachmentTags(Identifier registryName) {
        return attachmentsTagManager == null ? Set.of() : attachmentsTagManager.getAllowAttachmentTags(registryName);
    }

    @Nullable
    public static CommonAssetsManager getInstance() {
        return INSTANCE;
    }

    public static void clearInstance() {
        INSTANCE = null;
    }

    public static ICommonResourceProvider get() {
        return INSTANCE == null ? CommonNetworkCache.INSTANCE : INSTANCE;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    @SubscribeEvent
    public static void onReload(AddServerReloadListenersEvent event) {
        var commonAssetsManager = new CommonAssetsManager();
        commonAssetsManager.reloadAndRegister(event);
        INSTANCE = commonAssetsManager;
        INSTANCE.recipeManager = event.getServerResources().getRecipeManager();
        int guns = INSTANCE.gunIndex == null ? 0 : INSTANCE.gunIndex.getAllData().size();
        GunMod.LOGGER.info("WP④ CommonAssetsManager listeners registered (gun index fills after reload). placeholderGuns={}", guns);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event instanceof TagsUpdatedEvent.ServerDataLoad && getInstance() != null && getInstance().recipeManager != null) {
            // Recipe results are deliberately NOT initialised here.  At this point in
            // TagsUpdatedEvent the item Holders' data-components may not be bound yet,
            // so new ItemStack(holder) would NPE ("Components not bound yet").
            // RawGunTableResult / GunSmithTableResult were designed for lazy init:
            // the client screen (GunSmithTableScreen) and server menu (GunSmithTableMenu)
            // call recipe.init() just before the result is actually consumed, by which
            // time all registries and components are fully ready.
            if (getInstance().gunIndex != null) {
                GunMod.LOGGER.info("WP④ gun pack loaded: guns={} ammo={} attachments={} blocks={} recipes={}",
                        getInstance().gunIndex.getAllData().size(),
                        getInstance().ammoIndex == null ? 0 : getInstance().ammoIndex.getAllData().size(),
                        getInstance().attachmentIndex == null ? 0 : getInstance().attachmentIndex.getAllData().size(),
                        getInstance().blockIndex == null ? 0 : getInstance().blockIndex.getAllData().size(),
                        getInstance().tableRecipe == null ? 0 : getInstance().tableRecipe.getAllData().size());
            }
            // Legacy vanilla crafting recipes from gun-pack `recipes/` are remapped to
            // `recipe/` in DelegatingPackResources (with RecipeCompat format conversion)
            // so the minecraft:recipe datapack registry loads them. RecipeManager on
            // 26.1.2 only copies that registry into an immutable RecipeMap — there is
            // no fromJson(Map) / mutable recipes field to inject into after the fact.
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearInstance();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (getInstance() == null) {
            return;
        }

        // PlayerList posts OnDatapackSyncEvent before it sends either the vanilla recipe update
        // or NeoForge's RecipeContentPayload. Send our authoritative cache now, then explicitly
        // request the full content of our own recipe type. That non-empty payload produces the
        // public client-side RecipesReceivedEvent after this cache has been installed and marks
        // JEI's native recipe state as synchronized, so JEI can start/restart its plugins there.
        // A TACZ cache payload alone cannot provide that lifecycle barrier.
        //
        // REI already reacts to the vanilla recipe-update packet which follows this event. Do not
        // race that reload with REI's internal reloadPlugins hook from the gun-pack packet handler.
        // 1.2.0：分片已在资源重载尾端压缩好，这里只负责按玩家发出去（同一组不可变分片
        // 被所有玩家复用，不再为每个玩家重新编码整份缓存）。
        List<ServerMessageSyncGunPack> messages = getInstance().networkSyncMessages;
        if (messages.isEmpty()) {
            // 重载尾端的快照还没准备好（例如 reload 与 sync 竞争）：退回现场编码，
            // 至少不把「没有枪包数据」发给客户端。
            messages = ServerMessageSyncGunPack.createMessages(getInstance().getNetworkCache());
        }
        List<ServerMessageSyncGunPack> toSend = messages;
        event.getRelevantPlayers().forEach(player -> toSend.forEach(message -> NetworkHandler.sendToClientPlayer(message, player)));
        event.sendRecipes(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
    }

    public static void reloadAllPack() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PackRepository packrepository = server.getPackRepository();
        packrepository.reload();
        Collection<String> collection = packrepository.getSelectedIds();
        server.reloadResources(collection);
    }
}

package me.xjqsh.lrtactical.client.resource.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.manager.JsonDataManager;
import me.xjqsh.lrtactical.client.resource.display.ThrowableDisplayInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.Map;

/**
 * 加载 {@code assets/<ns>/display/throwable/*.json}。
 *
 * <p>设计与 {@link MeleeDisplayManager} 完全一致，说明见该类注释。
 */
public class ThrowableDisplayManager extends JsonDataManager<ThrowableDisplayInstance> {
    public ThrowableDisplayManager(Gson pGson) {
        super(null, pGson, "display/throwable", "LrThrowableDisplay");
    }

    /**
     * 声明「必须在 TACZ 的模型/动画/脚本加载完之后再跑」。理由见
     * {@link MeleeDisplayManager#getFabricDependencies()}。
     */
    // WP-LR2：getFabricDependencies 为 Fabric 专有（reload 依赖排序）。NeoForge 无等价物，
    // 顺序由 AddClientReloadListenersEvent 注册顺序承载（弱保证，WP07 C 表）——
    // LR 的 display 监听器在 LrClientEvents 中注册，晚于 tacz ClientSetupEvent 的资产监听器。

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        for (Map.Entry<Identifier, JsonElement> entry : pObject.entrySet()) {
            Identifier id = entry.getKey();
            try {
                var pojo = getGson().fromJson(entry.getValue(), ThrowableDisplayInstance.ThrowableDisplay.class);
                dataMap.put(id, ThrowableDisplayInstance.create(pojo, id));
            } catch (JsonParseException | IllegalArgumentException e) {
                GunMod.LOGGER.error(getMarker(), "Failed to load display file {}", id, e);
            }
        }
    }
}

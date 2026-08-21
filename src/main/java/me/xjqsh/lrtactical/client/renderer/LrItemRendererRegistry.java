package me.xjqsh.lrtactical.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Item → 动态渲染器注册表（WP-LR2）。
 * 源自 refab 垫片 {@code me.xjqsh.lrtactical.client.renderer.LrItemRendererRegistry}
 * （GPL-3.0），本身零加载器依赖，改名迁入 LR 包以避免在本仓引入 cn.sh1rocu 包名。
 * 仅客户端类加载路径使用。
 */
public class LrItemRendererRegistry {
    public static final LrItemRendererRegistry INSTANCE = new LrItemRendererRegistry();

    private final Map<Item, DynamicItemRenderer> renderers = new IdentityHashMap<>();

    private LrItemRendererRegistry() {
    }

    public void register(Item item, DynamicItemRenderer renderer) {
        renderers.put(item, renderer);
    }

    public DynamicItemRenderer get(Item item) {
        return renderers.get(item);
    }

    /** 26.x 自定义物品渲染接口（SubmitNodeCollector 路径）。 */
    public interface DynamicItemRenderer {
        void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay);
    }
}

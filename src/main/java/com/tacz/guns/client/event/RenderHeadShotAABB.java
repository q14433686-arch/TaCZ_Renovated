package com.tacz.guns.client.event;

import net.neoforged.neoforge.client.event.RenderLivingEvent;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class RenderHeadShotAABB {
    public static void onRenderEntity(RenderLivingEvent.Post<?, ?, ?> event) {
        // 【第 35 轮修复】补回 F3+B 门禁。
        if (!Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
            return;
        }
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get() || event.getSubmitNodeCollector() == null || event.getPoseStack() == null) {
            return;
        }
        var renderState = event.getRenderState();
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(renderState.entityType);
        AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
        if (aabb == null) {
            float width = renderState.boundingBoxWidth;
            float eyeHeight = renderState.eyeHeight;
            // 扩张 0.01，避免和原版显示重合
            aabb = new AABB(-width / 2, eyeHeight - 0.25, -width / 2, width / 2, eyeHeight + 0.25, width / 2).inflate(0.01);
        }
        event.getSubmitNodeCollector().submitShapeOutline(
                event.getPoseStack(),
                net.minecraft.world.phys.shapes.Shapes.create(aabb),
                RenderTypes.lines(),
                0xFFFFFF00,
                2.5F,
                false);
    }
}

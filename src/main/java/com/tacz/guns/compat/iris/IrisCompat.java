package com.tacz.guns.compat.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tacz.guns.GunMod;
import com.tacz.guns.init.CompatRegistry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Set;

/** Optional Iris integration for the Minecraft 26.1.2 OpenGL renderer. Reflection-only. */
public final class IrisCompat {
    private static final Set<RenderPipeline> ASSIGNED_SCOPE_PIPELINES = new HashSet<>();
    private static boolean loggedScopePipelineFailure;

    private IrisCompat() {
    }

    public static void initCompat() {
    }

    public static boolean isRenderShadow() {
        return false;
    }

    public static boolean isUsingRenderPack() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (Boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static synchronized boolean assignPipelineToIris(RenderPipeline pipeline,
                                                            String irisProgramName,
                                                            String debugName) {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        if (ASSIGNED_SCOPE_PIPELINES.contains(pipeline)) {
            return true;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Class<?> programClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object irisProgram = Enum.valueOf((Class<? extends Enum>) programClass.asSubclass(Enum.class), irisProgramName);
            apiClass.getMethod("assignPipeline", RenderPipeline.class, programClass).invoke(api, pipeline, irisProgram);
            ASSIGNED_SCOPE_PIPELINES.add(pipeline);
            GunMod.LOGGER.info("[TACZ Iris] Assigned {} to the Iris {} program.", debugName, irisProgramName);
            return true;
        } catch (Throwable t) {
            if (!loggedScopePipelineFailure) {
                loggedScopePipelineFailure = true;
                GunMod.LOGGER.warn("[TACZ Iris] Iris cannot classify render pipeline {}; vanilla pipeline used.",
                        debugName, t);
            }
            return false;
        }
    }

    public static synchronized void assignCommonEntityPipelinesToHandIfNeeded() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return;
        }
        assignPipelineToIris(RenderPipelines.ENTITY_CUTOUT, "HAND", "entity_cutout");
        assignPipelineToIris(RenderPipelines.ENTITY_TRANSLUCENT, "HAND_TRANSLUCENT", "entity_translucent");
        assignPipelineToIris(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, "HAND_TRANSLUCENT", "entity_translucent_emissive");
    }

    public static boolean isHandRendererActive() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS) || !isUsingRenderPack()) {
            return false;
        }
        try {
            Class<?> handRendererClass = Class.forName("net.irisshaders.iris.pathways.HandRenderer");
            Object instance = handRendererClass.getField("INSTANCE").get(null);
            return (Boolean) handRendererClass.getMethod("isActive").invoke(instance);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Mirrors Iris' {@code MixinItemInHandRenderer#iris$skipTranslucentHands} phase gate.
     * When Iris is not in a shader-pack hand pass this returns true. During an Iris hand pass,
     * solid items render only in the solid phase; translucent items only in the translucent phase.
     */
    public static boolean shouldRenderInCurrentHandPhase(ItemStack stack) {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS) || !isUsingRenderPack()) {
            return true;
        }
        try {
            Class<?> handRendererClass = Class.forName("net.irisshaders.iris.pathways.HandRenderer");
            Object instance = handRendererClass.getField("INSTANCE").get(null);
            boolean active = (Boolean) handRendererClass.getMethod("isActive").invoke(instance);
            if (!active) {
                return true;
            }
            boolean renderingSolid = (Boolean) handRendererClass.getMethod("isRenderingSolid").invoke(instance);
            boolean itemTranslucent = (Boolean) handRendererClass.getMethod("isHandTranslucent", ItemStack.class)
                    .invoke(instance, stack);
            return renderingSolid != itemTranslucent;
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Deprecated
    public static boolean endBatch(Object bufferSource) {
        return false;
    }

    public static boolean endBatch(SubmitNodeCollector collector) {
        return false;
    }
}

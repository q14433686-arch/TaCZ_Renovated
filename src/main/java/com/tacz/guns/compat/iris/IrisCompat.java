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
            // Iris 1.11.3+mc26.1.2 起会对常见 entity 管线做自动分类（日志
            // "Found fine program match ..."），此时重复 assign 会抛
            // IllegalStateException("Shader already assigned")。Iris 已分类 = 目的已达成，
            // 视为成功，不再告警（2026-08-21 LAN 实测日志，records/SERVER_TEST_20260821_LAN.md）。
            Throwable cause = t;
            while (cause != null) {
                if (cause instanceof IllegalStateException
                        && cause.getMessage() != null
                        && cause.getMessage().startsWith("Shader already assigned")) {
                    ASSIGNED_SCOPE_PIPELINES.add(pipeline);
                    GunMod.LOGGER.info("[TACZ Iris] {} already classified by Iris ({}); keeping Iris assignment.",
                            debugName, cause.getMessage());
                    return true;
                }
                cause = cause.getCause();
            }
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

    /**
     * The post-composite overlay hook ({@code IrisRenderingPipeline#finalizeLevelRendering} TAIL)
     * and the late translucent hand pass are bytecode-audited specifically against the Iris
     * <b>26.1 分支</b>（1.11.x，本分支审计基线 commit
     * f4c06978f3a1c64869e40cd5cc7c8ed383085cc0，对应 MC 26.1.2）。其他 Iris 构建保持原 solid-pass
     * 行为，而不是冒险在内部 final 时序变化时得到一颗隐形准星。
     * NeoForge 侧用 {@code ModList.getModContainerById} 读取版本字符串（与
     * {@code GunHudOverlay} 同款用法），语义与 Fabric 侧
     * {@code getFriendlyString().startsWith("1.11")} 一致。
     */
    public static boolean supportsFinalScopeOverlay() {
        return ModList.get().getModContainerById(CompatRegistry.IRIS)
                .map(container -> container.getModInfo().getVersion().toString().startsWith("1.11"))
                .orElse(false);
    }

    /**
     * @return whether the active Iris hand renderer is currently extracting its solid pass.
     *         A scope reticle is frozen only in this pass and emitted later by the Iris-only
     *         {@code HAND_TRANSLUCENT} bridge.
     */
    public static boolean isRenderingSolidHandPass() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS) || !isUsingRenderPack()) {
            return false;
        }
        try {
            Class<?> handRendererClass = Class.forName("net.irisshaders.iris.pathways.HandRenderer");
            Object instance = handRendererClass.getField("INSTANCE").get(null);
            return (Boolean) handRendererClass.getMethod("isActive").invoke(instance)
                    && (Boolean) handRendererClass.getMethod("isRenderingSolid").invoke(instance);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Iris renders hands from its own solid/translucent level phases and suppresses vanilla's hand call.
     * This flag is also used by TACZ's view-bob handling.
     */
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

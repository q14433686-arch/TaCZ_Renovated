package com.tacz.guns.compat.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tacz.guns.GunMod;
import com.tacz.guns.init.CompatRegistry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Optional Iris 1.11.x integration for the Minecraft 26.2 OpenGL renderer. Reflection-only. */
public final class IrisCompat {
    private static final Set<RenderPipeline> ASSIGNED_SCOPE_PIPELINES = new HashSet<>();
    private static boolean loggedScopePipelineFailure;

    private IrisCompat() {
    }

    public static void initCompat() {
    }

    public static boolean isRenderShadow() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (Boolean) apiClass.getMethod("isRenderingShadowPass").invoke(api);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Iris 反射句柄，解析一次后缓存。
     *
     * <h3>为什么值得缓存</h3>
     * {@link #isUsingRenderPack()} 与 {@link #isHandRendererActive()} 在全仓库有 30+ 个调用点，
     * 其中好几个是<b>逐帧、甚至一帧多次</b>（手部 pass 判定、bob 事件、掩码与合成的闸门）。
     * 缓存之后只剩一次 {@code invoke}，热路径上的分配直接归零。
     */
    private static boolean irisHandlesResolved;
    @Nullable
    private static Object irisApiInstance;
    @Nullable
    private static Method mIsShaderPackInUse;
    @Nullable
    private static Object handRendererInstance;
    @Nullable
    private static Method mHandRendererIsActive;

    private static void resolveIrisHandles() {
        if (irisHandlesResolved) {
            return;
        }
        irisHandlesResolved = true;
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);
            mIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
        } catch (Throwable ignored) {
            irisApiInstance = null;
            mIsShaderPackInUse = null;
        }
        try {
            Class<?> handRendererClass = Class.forName("net.irisshaders.iris.pathways.HandRenderer");
            handRendererInstance = handRendererClass.getField("INSTANCE").get(null);
            mHandRendererIsActive = handRendererClass.getMethod("isActive");
        } catch (Throwable ignored) {
            handRendererInstance = null;
            mHandRendererIsActive = null;
        }
    }

    /**
     * 本帧「是否在用光影包」的记忆值。
     *
     * <p>这个答案在一帧之内<b>不可能变</b>（切换光影是玩家操作，发生在帧与帧之间），
     * 而它每帧要被问很多次，所以记一次就够。由 {@link #beginFrame()} 在帧首清空。
     */
    private static byte usingRenderPackThisFrame = -1;

    /** 每帧清一次帧内记忆值。挂在 {@code GameRenderer#extract} 的 HEAD。 */
    public static void beginFrame() {
        usingRenderPackThisFrame = -1;
    }

    public static boolean isUsingRenderPack() {
        if (usingRenderPackThisFrame >= 0) {
            return usingRenderPackThisFrame != 0;
        }
        boolean result = computeUsingRenderPack();
        usingRenderPackThisFrame = (byte) (result ? 1 : 0);
        return result;
    }

    private static boolean computeUsingRenderPack() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        resolveIrisHandles();
        if (irisApiInstance == null || mIsShaderPackInUse == null) {
            return false;
        }
        try {
            return (Boolean) mIsShaderPackInUse.invoke(irisApiInstance);
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
            int minorRevision = (Integer) apiClass.getMethod("getMinorApiRevision").invoke(api);
            if (minorRevision < 3) {
                throw new IllegalStateException("Iris API revision " + minorRevision + " has no assignPipeline");
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object irisProgram = Enum.valueOf((Class<? extends Enum>) programClass.asSubclass(Enum.class), irisProgramName);
            apiClass.getMethod("assignPipeline", RenderPipeline.class, programClass).invoke(api, pipeline, irisProgram);
            ASSIGNED_SCOPE_PIPELINES.add(pipeline);
            GunMod.LOGGER.info("[TACZ Iris] Assigned {} to the Iris {} program.", debugName, irisProgramName);
            return true;
        } catch (Throwable t) {
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

    /** Assigns a custom mask-aware pipeline to Iris' first-person HAND program. */
    public static boolean assignScopePipelineToHand(RenderPipeline pipeline, String debugName) {
        return assignPipelineToIris(pipeline, "HAND", debugName);
    }

    /**
     * Iris on the OpenGL backend uses the optional uniform bridge. A Vulkan shader replacement
     * such as Sulkan has no verified equivalent API, so it takes the ordinary unmasked fallback.
     */
    public static boolean shouldDisableScopeMaskUnderShaderPack() {
        return ModList.get().isLoaded("sulkan");
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
        resolveIrisHandles();
        if (handRendererInstance == null || mHandRendererIsActive == null) {
            return false;
        }
        try {
            return (Boolean) mHandRendererIsActive.invoke(handRendererInstance);
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

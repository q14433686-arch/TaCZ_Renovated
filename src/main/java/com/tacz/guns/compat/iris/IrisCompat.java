package com.tacz.guns.compat.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tacz.guns.GunMod;
import com.tacz.guns.compat.iris.legacy.IrisCompatLegacy;
import com.tacz.guns.compat.iris.newly.IrisCompatNewly;
import com.tacz.guns.init.CompatRegistry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Optional Iris integration for the Minecraft 26.1.2 OpenGL renderer. Reflection-only.
 *
 * <p>2026-09-01 随 mesh GPU 移植补齐（姊妹 9ed6b93/2839843 语义）：
 * {@link #isRenderShadow()} 从恒 false 升级为版本感知（Iris 1.7.0 起走 newly 桥，
 * 之前走 legacy 桥 —— 两桥目前反射同一入口，分界保留以便未来分裂）；
 * {@link #assignCommonEntityPipelinesToHandIfNeeded()} 升级为 7 条管线一次性分配
 * 并带 {@code HAND_CUTOUT} 优先回退；新增 {@link #supportsHandFlushHook()} /
 * {@link #assignMeshPipelineToHand(RenderPipeline)} /
 * {@link #assignMeshPipelineToEntity(RenderPipeline)} 供常驻 VBO 层使用。</p>
 */
public final class IrisCompat {
    /** Iris 1.7.0 起 {@code ShadowRenderingState} 成为 shadow pass 的稳定查询入口（姊妹基线裁定）。 */
    private static final ArtifactVersion SHADOW_API_SPLIT_VERSION = new DefaultArtifactVersion("1.7.0");

    private static Supplier<Boolean> isRenderingShadow = () -> false;
    private static final Set<RenderPipeline> ASSIGNED_SCOPE_PIPELINES = new HashSet<>();
    private static boolean loggedScopePipelineFailure;
    private static boolean commonEntityPipelinesAssigned = false;
    private static boolean commonEntityPipelinesAssignAttempted = false;

    private IrisCompat() {
    }

    /**
     * NeoForge 等价物：Fabric 侧的 {@code FabricLoader.getModContainer(...).getMetadata().getVersion()}
     * 换成 {@code ModList.getModContainerById(...).getModInfo().getVersion()}（与
     * {@code GunPackLoader.modVersionMatch} / {@code GunHudOverlay} 同款用法）。
     *
     * <p>Maven {@code ArtifactVersion} 比较语义与 FabricLoader 的 semver 比较在
     * {@code 1.11.x >= 1.7.0} 这类目标上一致；build 元数据（{@code +mc26.1.2}）先剥掉再比，
     * 与 {@code GunPackLoader.stripBuildMetadata} 同套路。</p>
     */
    public static void initCompat() {
        ModList.get().getModContainerById(CompatRegistry.IRIS).ifPresent(mod -> {
            ArtifactVersion version = mod.getModInfo().getVersion();
            String raw = version.toString();
            int plus = raw.indexOf('+');
            if (plus >= 0) {
                version = new DefaultArtifactVersion(raw.substring(0, plus));
            }
            if (version.compareTo(SHADOW_API_SPLIT_VERSION) >= 0) {
                isRenderingShadow = IrisCompatNewly::isRenderShadow;
            } else {
                isRenderingShadow = IrisCompatLegacy::isRenderShadow;
            }
        });
    }

    /**
     * 查询 Iris 是否正在渲染阴影遍。mesh GPU 的世界表消费点与提交侧都靠它拒收阴影遍
     * （{@link com.tacz.guns.compat.meshloader.render.PolyMeshGpuRenderer} 的
     * {@code shouldSubmitGpuWorld} / {@code renderAtWorldFlush}，以及
     * {@code PolyRenderPolicy} 的 {@code POLY_IN_SHADOW} 开关）。
     */
    public static boolean isRenderShadow() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        try {
            return isRenderingShadow.get();
        } catch (Throwable ignored) {
            return false;
        }
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

    /** Classifies a TACZ custom pipeline through Iris' public API while keeping Iris optional. */
    public static synchronized boolean assignPipelineToIris(RenderPipeline pipeline,
                                                            String irisProgramName,
                                                            String debugName) {
        return assignPipelineToIrisAny(pipeline, new String[]{irisProgramName}, debugName);
    }

    /**
     * 多候选名分配版本（姊妹 2839843）：Iris 不同构建对同一条管线的「完美匹配」目标
     * 可能在 {@code HAND_CUTOUT} 与 {@code HAND} 之间漂移，按序尝试，全部失败才告警一次。
     */
    private static boolean assignPipelineToIrisAny(RenderPipeline pipeline, String[] irisProgramNames, String debugName) {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return false;
        }
        if (ASSIGNED_SCOPE_PIPELINES.contains(pipeline)) {
            return true;
        }

        Throwable lastFailure = null;
        for (String irisProgramName : irisProgramNames) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Class<?> programClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
                Object api = apiClass.getMethod("getInstance").invoke(null);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object irisProgram = Enum.valueOf(
                        (Class<? extends Enum>) programClass.asSubclass(Enum.class), irisProgramName);
                apiClass.getMethod("assignPipeline", RenderPipeline.class, programClass)
                        .invoke(api, pipeline, irisProgram);
                ASSIGNED_SCOPE_PIPELINES.add(pipeline);
                GunMod.LOGGER.info("[TACZ Iris] Assigned {} to the Iris {} program.",
                        debugName, irisProgramName);
                return true;
            } catch (Throwable t) {
                // Iris 1.11.3+mc26.1.2 起会对常见 entity 管线做自动分类（日志
                // "Found fine program match ..."），此时重复 assign 会抛
                // IllegalStateException("Shader already assigned")。Iris 已分类 = 目的已达成，
                // 视为成功，不再告警（2026-08-21 LAN 实测日志，records/SERVER_TEST_20260821_LAN.md）。
                if (isAlreadyAssigned(t)) {
                    ASSIGNED_SCOPE_PIPELINES.add(pipeline);
                    GunMod.LOGGER.debug("[TACZ Iris] {} is already classified by Iris; keeping existing assignment.",
                            debugName);
                    return true;
                }
                lastFailure = t;
            }
        }

        if (!loggedScopePipelineFailure) {
            loggedScopePipelineFailure = true;
            GunMod.LOGGER.warn("[TACZ Iris] Iris cannot classify render pipeline {} as {}; "
                            + "vanilla pipeline behavior will be used.",
                    debugName, String.join("/", irisProgramNames), lastFailure);
        }
        return false;
    }

    private static boolean isAlreadyAssigned(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains("Shader already assigned")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assign vanilla entity/item pipelines used inside the first-person hand pass to Iris' hand
     * programs. Some Iris versions otherwise rediscover the same "perfect program match" every
     * frame. Try this once per client session only, even if a subset fails.
     */
    public static synchronized void assignCommonEntityPipelinesToHandIfNeeded() {
        if (!ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return;
        }
        if (commonEntityPipelinesAssigned || commonEntityPipelinesAssignAttempted) {
            return;
        }
        commonEntityPipelinesAssignAttempted = true;

        boolean ok = true;
        ok &= assignPipelineToIrisAny(RenderPipelines.ENTITY_CUTOUT,
                new String[]{"HAND_CUTOUT", "HAND"}, "entity_cutout");
        ok &= assignPipelineToIrisAny(RenderPipelines.ENTITY_CUTOUT_CULL,
                new String[]{"HAND_CUTOUT", "HAND"}, "entity_cutout_cull");
        ok &= assignPipelineToIrisAny(RenderPipelines.ENTITY_TRANSLUCENT,
                new String[]{"HAND_TRANSLUCENT"}, "entity_translucent");
        ok &= assignPipelineToIrisAny(RenderPipelines.ENTITY_TRANSLUCENT_CULL,
                new String[]{"HAND_TRANSLUCENT"}, "entity_translucent_cull");
        ok &= assignPipelineToIrisAny(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
                new String[]{"HAND_TRANSLUCENT"}, "entity_translucent_emissive");
        ok &= assignPipelineToIrisAny(RenderPipelines.ITEM_CUTOUT,
                new String[]{"HAND_CUTOUT", "HAND"}, "item_cutout");
        ok &= assignPipelineToIrisAny(RenderPipelines.ITEM_TRANSLUCENT,
                new String[]{"HAND_TRANSLUCENT"}, "item_translucent");

        commonEntityPipelinesAssigned = ok;
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
     * Mirrors Iris' own {@code MixinItemInHandRenderer#iris$skipTranslucentHands} phase gate.
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
            // Fail open: a broken optional Iris reflection bridge must not make the held item vanish.
            return true;
        }
    }

    /**
     * poly_mesh GPU 路径（常驻 VBO）在手部 flush 的绘制钩子是否可用。
     *
     * <p>该钩子依赖 Iris 26.1 线的 {@code MixinItemInHandRenderer} 拦截架构（源码核实，
     * Iris 26.1 分支 commit f4c0697）：{@code @WrapWithCondition} 掏掉
     * {@code renderHandsWithItems} 里的 {@code renderAllFeatures()}，
     * {@code @WrapOperation} 把 {@code endBatch()} 换成 {@code HandRenderer#endRender()}
     * （内部仍是 renderAllFeatures + endBatch），并且 Iris 自己也是从
     * {@code iris$renderHandsWithCustomRenderer} → <b>同一个</b> {@code renderHandsWithItems}
     * 进来的 —— 所以 TACZ 的 {@code @Inject(renderHandsWithItems, RETURN)} 钩子天然落在
     * Iris 的手部阶段内。这一架构在本仓审计基线（Iris 1.11.x + MC 26.1.2，
     * {@code supportsFinalScopeOverlay} 同款版本门）上成立。</p>
     *
     * <p>版本不匹配时返回 false：{@code MeshGpuUnderShaders} 的路径整体拒收并保持
     * collector（宁可不加速，不能画错）。</p>
     */
    public static boolean supportsHandFlushHook() {
        return ModList.get().getModContainerById(CompatRegistry.IRIS)
                .map(container -> container.getModInfo().getVersion().toString().startsWith("1.11"))
                .orElse(false);
    }

    /**
     * Classify the mesh renderer's own pipeline as Iris' hand program so the resident-VBO pass,
     * which never goes through a vanilla {@code RenderType}, still receives shader-pack lighting.
     *
     * <p>{@code IrisApi.assignPipeline} maps a {@link RenderPipeline} to an Iris program; Iris'
     * {@code ShaderKey.findBestMatch} picks {@code HAND_CUTOUT} for our pipeline because it declares
     * {@code ALPHA_CUTOUT} and the (possibly Iris-extended) entity vertex format. Failures are
     * swallowed the same way as the scope pipelines: without the assignment the gun still draws,
     * just with vanilla lighting.</p>
     */
    public static boolean assignMeshPipelineToHand(RenderPipeline pipeline) {
        return assignPipelineToIris(pipeline, "HAND", "mesh_entity_hand");
    }

    /**
     * Same classification for the <b>world</b> mesh pass: the resident-VBO pipeline should be lit
     * by the pack's entity program instead of falling back to the vanilla one.
     *
     * <p>常量已按 Q4 要求核实（Iris 26.1 分支源码 {@code api/v0/IrisProgram.java}）：
     * 全量枚举为 {@code BASIC, TEXTURED, TERRAIN, TERRAIN_SOLID, TERRAIN_CUTOUT, TRANSLUCENT,
     * SKY_BASIC, SKY_TEXTURED, ARMOR_GLINT, ENTITIES, ENTITIES_TRANSLUCENT, CLOUDS, BLOCK,
     * BLOCK_TRANSLUCENT, HAND, HAND_TRANSLUCENT, PARTICLES, PARTICLES_TRANSLUCENT,
     * EMISSIVE_ENTITIES, BEACON_BEAM, LINES} —— 没有 {@code ENTITY}/{@code MAIN}，
     * 世界路径用 {@code ENTITIES}。{@code EMISSIVE_ENTITIES} 刻意<b>不</b>用于本渲染器的
     * 无光照兜底管线：那条只跳过 lightmap 采样，不等于「恒全亮」。</p>
     *
     * <p>{@code MeshGpuWorldUnderShaders} 保持默认 false：组合已按源码核实，但 26.1.2 上
     * 没有实机验证（见 MESH_LOADER.md 复测矩阵）。</p>
     */
    public static boolean assignMeshPipelineToEntity(RenderPipeline pipeline) {
        return assignPipelineToIrisAny(pipeline, new String[]{"ENTITIES"}, "mesh_entity_world");
    }

    /** @deprecated Feature rendering owns batch flushes in 26.1.2. */
    @Deprecated
    public static boolean endBatch(Object bufferSource) {
        return false;
    }

    /** Feature rendering owns batch flushes in 26.1.2. */
    public static boolean endBatch(SubmitNodeCollector collector) {
        return false;
    }
}

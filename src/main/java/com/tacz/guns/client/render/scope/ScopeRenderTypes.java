package com.tacz.guns.client.render.scope;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.GunMod;
import com.tacz.guns.compat.shader.ShaderCompat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenGL-only depth-aperture scope render types for Minecraft 26.2.
 *
 * <p>26.2 removed {@code RenderType#draw}. Operation boundaries are therefore associated with
 * the prepared pipeline and intercepted in {@code PreparedRenderTypeScopeDepthCopyMixin}.
 * Vulkan deliberately bypasses this path and uses the transparent, unmasked fallback.</p>
 */
public final class ScopeRenderTypes {
    private static final RenderSetup FAKE_SETUP = RenderSetup.builder(RenderPipelines.GUI_TEXTURED)
            .createRenderSetup();

    private static final Map<RenderPipeline, ScopeDepthCopyState.Operation> PIPELINE_OPERATIONS =
            new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> APERTURE_COPY_BODIES = new IdentityHashMap<>();
    private static final Map<Identifier, RenderType> DEPTH_APERTURES = new HashMap<>();
    private static final Map<Identifier, RenderType> DEPTH_CLEANUPS = new HashMap<>();
    private static final Map<Identifier, RenderType> ETCHED_RETICLES = new HashMap<>();
    private static final Map<Identifier, RenderType> VISIBLE_RETICLES = new HashMap<>();
    private static final Map<Identifier, RenderType> VIEWMODEL_CUTOUT_TYPES = new HashMap<>();
    private static final Map<Identifier, RenderType> FLASH_TRANSLUCENT_TYPES = new HashMap<>();
    private static final Map<Identifier, RenderType> FLASH_SWIRL_TYPES = new HashMap<>();

    private static final BindGroupLayout DEPTH_CLEANUP_SAMPLERS = BindGroupLayout.builder()
            .withSampler(ScopeDepthCopyState.SAMPLER_UNIFORM)
            .withSampler(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM)
            .withSampler(ScopeDepthCopyState.POST_BODY_SAMPLER_UNIFORM)
            .build();
    private static final BindGroupLayout DEPTH_MASK_SAMPLERS = BindGroupLayout.builder()
            .withSampler(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM)
            .withSampler(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM)
            .build();

    /** Set during extraction when this first-person gun submission actually queued an ocular aperture. */
    private static boolean apertureScheduledForViewmodel;

    /**
     * Writes ocular geometry to the existing hand depth attachment without touching color.
     * Scope-body fragments behind that geometry fail their ordinary depth test, leaving world color visible.
     */
    private static RenderPipeline APERTURE_COPY_PIPELINE;
    private static RenderPipeline DEPTH_APERTURE_PIPELINE;

    /** Restores the aperture region from the exact world-depth backup before later translucent world passes. */
    private static RenderPipeline DEPTH_CLEANUP_PIPELINE;

    /**
     * Etched reticles sample the world-depth backup and the ocular aperture depth per pixel
     * and only survive where ocularDepth &gt; worldDepth + epsilon.
     */
    private static RenderPipeline ETCHED_RETICLE_PIPELINE;

    /**
     * Small illuminated reticles use the same screen-space ocular mask and still write near hand
     * depth to protect their surviving pixels from later world translucency.
     */
    private static RenderPipeline VISIBLE_RETICLE_PIPELINE;

    /** Entity cutout plus an outside-aperture mask for the gun body and non-scope attachments. */
    private static RenderPipeline VIEWMODEL_CUTOUT_PIPELINE;

    /** Ordinary entity translucency plus an outside-aperture fragment mask for the flash quad. */
    private static RenderPipeline FLASH_TRANSLUCENT_PIPELINE;

    /** Vanilla energy-swirl states plus the same outside-aperture mask for the glow layer. */
    private static RenderPipeline FLASH_SWIRL_PIPELINE;
    private static volatile boolean pipelinesRegistered;

    private ScopeRenderTypes() {
    }

    /**
     * Builds GL-only pipelines during NeoForge's registration event. Vulkan never constructs
     * them, so its shader compiler cannot encounter the raw-GL depth uniforms.
     */
    public static synchronized void init() {
        if (DEPTH_APERTURE_PIPELINE != null || !ScopeDepthCopyState.isOpenGlBackend()) {
            return;
        }
        APERTURE_COPY_PIPELINE = createApertureCopyPipeline();
        DEPTH_APERTURE_PIPELINE = createDepthAperturePipeline();
        DEPTH_CLEANUP_PIPELINE = createDepthCleanupPipeline();
        ETCHED_RETICLE_PIPELINE = createEtchedReticlePipeline();
        VISIBLE_RETICLE_PIPELINE = createVisibleReticlePipeline();
        VIEWMODEL_CUTOUT_PIPELINE = createViewmodelCutoutPipeline();
        FLASH_TRANSLUCENT_PIPELINE = createFlashTranslucentPipeline();
        FLASH_SWIRL_PIPELINE = createFlashSwirlPipeline();
    }

    /** Registers all GL-only pipelines through NeoForge's 26.2 custom-pipeline event. */
    public static synchronized void registerPipelines(RegisterRenderPipelinesEvent event) {
        if (pipelinesRegistered) {
            return;
        }
        init();
        if (DEPTH_APERTURE_PIPELINE == null) {
            return;
        }
        event.registerPipeline(APERTURE_COPY_PIPELINE);
        event.registerPipeline(DEPTH_APERTURE_PIPELINE);
        event.registerPipeline(DEPTH_CLEANUP_PIPELINE);
        event.registerPipeline(ETCHED_RETICLE_PIPELINE);
        event.registerPipeline(VISIBLE_RETICLE_PIPELINE);
        event.registerPipeline(VIEWMODEL_CUTOUT_PIPELINE);
        event.registerPipeline(FLASH_TRANSLUCENT_PIPELINE);
        event.registerPipeline(FLASH_SWIRL_PIPELINE);
        pipelinesRegistered = true;
    }

    public static boolean supportsDepthAperture() {
        return pipelinesRegistered && ScopeDepthCopyState.isOpenGlBackend();
    }

    public static ScopeDepthCopyState.Operation operationFor(PreparedRenderType prepared) {
        return PIPELINE_OPERATIONS.getOrDefault(prepared.pipeline(), ScopeDepthCopyState.Operation.NONE);
    }

    private static RenderPipeline registerOperation(RenderPipeline pipeline,
                                                     ScopeDepthCopyState.Operation operation) {
        PIPELINE_OPERATIONS.put(pipeline, operation);
        return pipeline;
    }

    /** Starts extraction of one first-person gun; prevents a previous frame's aperture from clipping fire. */
    public static void beginViewmodelSubmission() {
        apertureScheduledForViewmodel = false;
    }

    /** @return whether this gun submission queued a valid depth-aperture sequence before its FX. */
    public static boolean hasScheduledViewmodelAperture() {
        return apertureScheduledForViewmodel;
    }

    /**
     * Wraps the plain scope-body type so its draw boundary first copies the aperture depth
     * (world depth plus only the ocular differences) into the mask texture, then draws the body.
     */
    public static RenderType apertureCopy(RenderType base) {
        return APERTURE_COPY_BODIES.computeIfAbsent(base, ScopeRenderTypes::createApertureCopyType);
    }

    public static RenderType depthAperture(Identifier texture) {
        if (!supportsDepthAperture() || DEPTH_APERTURE_PIPELINE == null) {
            throw new IllegalStateException("Depth-aperture pipeline requested before GL pipeline registration");
        }
        // This method is called while extracting an active first-person ocular, before the gun's
        // functional muzzle-flash node is visited. The flag only selects a masked RenderType;
        // draw-time validation still fails open when a depth copy is unavailable.
        apertureScheduledForViewmodel = true;
        return DEPTH_APERTURES.computeIfAbsent(texture, ScopeRenderTypes::createDepthApertureType);
    }

    public static RenderType depthCleanup(Identifier texture) {
        return DEPTH_CLEANUPS.computeIfAbsent(texture, ScopeRenderTypes::createDepthCleanupType);
    }

    public static RenderType etchedReticle(Identifier texture) {
        return ETCHED_RETICLES.computeIfAbsent(texture, ScopeRenderTypes::createEtchedReticleType);
    }

    public static RenderType visibleReticle(Identifier texture) {
        return VISIBLE_RETICLES.computeIfAbsent(texture, ScopeRenderTypes::createVisibleReticleType);
    }

    /**
     * Replaces an ordinary first-person gun/attachment cutout type only after an ocular was queued.
     * All other contexts and failed aperture cycles retain the caller's original behavior.
     */
    public static RenderType clipForViewmodel(RenderType original, Identifier texture, boolean applies) {
        if (!applies || !apertureScheduledForViewmodel) {
            return original;
        }
        // Gun displays may opt into entityTranslucent; retain that blend/sort recipe rather than
        // silently forcing every body through cutout. AttachmentRender supplies cutout here.
        if (original.hasBlending()) {
            return FLASH_TRANSLUCENT_TYPES.computeIfAbsent(texture, ScopeRenderTypes::createFlashTranslucentType);
        }
        return VIEWMODEL_CUTOUT_TYPES.computeIfAbsent(texture, ScopeRenderTypes::createViewmodelCutoutType);
    }

    /** Muzzle-flash background quad: retain vanilla appearance outside the ocular, discard inside. */
    public static RenderType flashTranslucentClipped(Identifier texture) {
        return FLASH_TRANSLUCENT_TYPES.computeIfAbsent(texture, ScopeRenderTypes::createFlashTranslucentType);
    }

    /** Muzzle-flash additive glow: retain vanilla energy-swirl appearance outside the ocular. */
    public static RenderType flashSwirlClipped(Identifier texture) {
        return FLASH_SWIRL_TYPES.computeIfAbsent(texture, ScopeRenderTypes::createFlashSwirlType);
    }

    private static RenderType createApertureCopyType(RenderType base) {
        if (APERTURE_COPY_PIPELINE == null || base.pipeline() != RenderPipelines.ENTITY_CUTOUT) {
            return base;
        }
        return new PipelineOverrideRenderType(
                "tacz_scope_body_aperture_copy", base, APERTURE_COPY_PIPELINE);
    }

    private static RenderType createDepthApertureType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(DEPTH_APERTURE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .createRenderSetup();
        return RenderType.create("tacz_scope_depth_aperture", setup);
    }

    private static RenderType createDepthCleanupType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(DEPTH_CLEANUP_PIPELINE)
                .withTexture("Sampler0", texture)
                // Satisfy RenderPass validation; ScopeDepthCopyState replaces these placeholders
                // with world/aperture/post-body depth copies at the actual draw boundary.
                .withTexture(ScopeDepthCopyState.SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.POST_BODY_SAMPLER_UNIFORM, texture)
                .useLightmap()
                .useOverlay()
                .createRenderSetup();
        return RenderType.create("tacz_scope_depth_cleanup", setup);
    }

    private static RenderType createEtchedReticleType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(ETCHED_RETICLE_PIPELINE)
                .withTexture("Sampler0", texture)
                // Placeholder bindings satisfy RenderPass validation; ScopeDepthCopyState rebinds
                // both samplers to the live world/aperture depth copies when the mask draw runs.
                .withTexture(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .useLightmap()
                .useOverlay()
                .createRenderSetup();
        return RenderType.create("tacz_scope_etched_reticle", setup);
    }

    private static RenderType createVisibleReticleType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(VISIBLE_RETICLE_PIPELINE)
                .withTexture("Sampler0", texture)
                // See createEtchedReticleType: placeholders replaced with live depth at draw time.
                .withTexture(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .useOverlay()
                .affectsCrumbling()
                .sortOnUpload()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("tacz_scope_visible_reticle", setup);
    }

    private static RenderType createViewmodelCutoutType(Identifier texture) {
        // Bytecode-equivalent to RenderTypes.entityCutout(texture, true), plus the two depth
        // samplers consumed by the outside-aperture branch.
        RenderSetup setup = RenderSetup.builder(VIEWMODEL_CUTOUT_PIPELINE)
                .withTexture("Sampler0", texture)
                .withTexture(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("tacz_scope_viewmodel_cutout", setup);
    }

    private static RenderType createFlashTranslucentType(Identifier texture) {
        // Bytecode-equivalent to RenderTypes.entityTranslucent(texture, true), with two placeholder
        // depth samplers added. ScopeDepthCopyState replaces those bindings at the real draw boundary.
        RenderSetup setup = RenderSetup.builder(FLASH_TRANSLUCENT_PIPELINE)
                .withTexture("Sampler0", texture)
                .withTexture(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .sortOnUpload()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("tacz_scope_flash_translucent", setup);
    }

    private static RenderType createFlashSwirlType(Identifier texture) {
        // Exact RenderTypes.energySwirl setup: animated UV transform, lightmap/overlay bindings and
        // upload sorting are preserved; only the depth-mask samplers are additional.
        RenderSetup setup = RenderSetup.builder(FLASH_SWIRL_PIPELINE)
                .withTexture("Sampler0", texture)
                .withTexture(ScopeDepthCopyState.MASK_WORLD_SAMPLER_UNIFORM, texture)
                .withTexture(ScopeDepthCopyState.APERTURE_SAMPLER_UNIFORM, texture)
                .setTextureTransform(new TextureTransform.OffsetTextureTransform(1.0F, 1.0F))
                .useLightmap()
                .useOverlay()
                .sortOnUpload()
                .createRenderSetup();
        return RenderType.create("tacz_scope_flash_swirl", setup);
    }

    private static RenderPipeline createApertureCopyPipeline() {
        RenderPipeline pipeline = registerOperation(
                clonePipeline(
                        RenderPipelines.ENTITY_CUTOUT,
                        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_aperture_copy"))
                        .build(),
                ScopeDepthCopyState.Operation.APERTURE_COPY);
        ShaderCompat.assignPipeline(pipeline, "HAND", "scope_aperture_copy");
        return pipeline;
    }

    private static RenderPipeline createDepthAperturePipeline() {
        RenderPipeline source = RenderPipelines.ENTITY_CUTOUT;
        RenderPipeline.Builder builder = clonePipeline(source,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_depth_aperture"));

        ColorTargetState sourceColor = source.getColorTargetState();
        builder.withColorTargetState(new ColorTargetState(
                sourceColor.blendFunction(),
                sourceColor.format(),
                ColorTargetState.WRITE_NONE
        ));
        DepthStencilState sourceDepth = source.getDepthStencilState();
        CompareOp depthTest = sourceDepth == null ? CompareOp.GREATER_THAN_OR_EQUAL : sourceDepth.depthTest();
        // Pull the invisible ocular very slightly toward the camera to avoid coplanar scope-body leakage.
        builder.withDepthStencilState(new DepthStencilState(depthTest, true, 1.0F, 1.0F));

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.BACKUP);
        ShaderCompat.assignPipeline(pipeline, "HAND", "scope_depth_aperture");
        return pipeline;
    }

    private static RenderPipeline createDepthCleanupPipeline() {
        RenderPipeline source = RenderPipelines.ENTITY_CUTOUT;
        RenderPipeline.Builder builder = clonePipeline(source,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_depth_cleanup"));
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_depth_cleanup"));
        builder.withBindGroupLayout(DEPTH_CLEANUP_SAMPLERS);

        ColorTargetState sourceColor = source.getColorTargetState();
        builder.withColorTargetState(new ColorTargetState(
                sourceColor.blendFunction(),
                sourceColor.format(),
                ColorTargetState.WRITE_NONE
        ));
        // Cleanup geometry rasterizes only the ocular footprint and writes exact sampled world depth.
        builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true));

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.RESTORE);
        ShaderCompat.assignPipeline(pipeline, "HAND", "scope_depth_cleanup");
        return pipeline;
    }

    private static RenderPipeline createEtchedReticlePipeline() {
        RenderPipeline source = RenderPipelines.ENTITY_CUTOUT;
        RenderPipeline.Builder builder = clonePipeline(source,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_etched_reticle"));
        // entity.fsh clone plus the ocular screen-space mask branch at the top of main().
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_reticle_mask"));
        builder.withBindGroupLayout(DEPTH_MASK_SAMPLERS);
        // Large blackout panels are still removed on the CPU; the retained thin marks render after
        // the exact depth restore and the mask clips them to the ocular footprint.
        builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true));

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.MASK);
        ShaderCompat.assignPipeline(pipeline, "HAND", "scope_etched_reticle");
        return pipeline;
    }

    private static RenderPipeline createVisibleReticlePipeline() {
        RenderPipeline source = RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
        RenderPipeline.Builder builder = clonePipeline(source,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_visible_reticle"));
        // Same entity.fsh clone plus ocular mask; under Iris the equivalent branch is injected.
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_reticle_mask"));
        builder.withBindGroupLayout(DEPTH_MASK_SAMPLERS);
        // The ocular depth writer must not hide the small dot/cross geometry placed behind the lens.
        builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true));

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.MASK);
        ShaderCompat.assignPipeline(pipeline, "HAND_TRANSLUCENT", "scope_visible_reticle");
        return pipeline;
    }

    private static RenderPipeline createViewmodelCutoutPipeline() {
        RenderPipeline.Builder builder = clonePipeline(
                RenderPipelines.ENTITY_CUTOUT,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_viewmodel_cutout"));
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_flash_clip"));
        builder.withBindGroupLayout(DEPTH_MASK_SAMPLERS);

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.MASK_OUTSIDE);
        ShaderCompat.assignPipeline(pipeline, "HAND", "scope_viewmodel_cutout");
        return pipeline;
    }

    private static RenderPipeline createFlashTranslucentPipeline() {
        RenderPipeline.Builder builder = clonePipeline(
                RenderPipelines.ENTITY_TRANSLUCENT,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_flash_translucent"));
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_flash_clip"));
        builder.withBindGroupLayout(DEPTH_MASK_SAMPLERS);

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.MASK_OUTSIDE);
        ShaderCompat.assignPipeline(pipeline, "HAND_TRANSLUCENT", "scope_flash_translucent");
        return pipeline;
    }

    private static RenderPipeline createFlashSwirlPipeline() {
        RenderPipeline.Builder builder = clonePipeline(
                RenderPipelines.ENERGY_SWIRL,
                Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_flash_swirl"));
        builder.withFragmentShader(Identifier.fromNamespaceAndPath(
                GunMod.MOD_ID, "core/scope_flash_clip"));
        builder.withBindGroupLayout(DEPTH_MASK_SAMPLERS);

        RenderPipeline pipeline = registerOperation(
                builder.build(), ScopeDepthCopyState.Operation.MASK_OUTSIDE);
        ShaderCompat.assignPipeline(pipeline, "HAND_TRANSLUCENT", "scope_flash_swirl");
        return pipeline;
    }

    private static RenderPipeline.Builder clonePipeline(RenderPipeline source, Identifier location) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(location)
                .withVertexShader(source.getVertexShader())
                .withFragmentShader(source.getFragmentShader())
                .withPolygonMode(source.getPolygonMode())
                .withCull(source.isCull())
                .withPrimitiveTopology(source.getPrimitiveTopology());

        source.getShaderDefines().flags().forEach(builder::withShaderDefine);
        source.getShaderDefines().values().forEach((name, value) -> copyDefine(builder, name, value));
        source.getBindGroupLayouts().forEach(builder::withBindGroupLayout);

        VertexFormat[] vertexFormats = source.getVertexFormatBindings();
        for (int i = 0; i < vertexFormats.length; i++) {
            if (vertexFormats[i] != null) {
                builder.withVertexBinding(i, vertexFormats[i]);
            }
        }
        ColorTargetState[] colorTargets = source.getColorTargetStates();
        for (int i = 0; i < colorTargets.length; i++) {
            if (colorTargets[i] != null) {
                builder.withColorTargetState(i, colorTargets[i]);
            }
        }

        DepthStencilState sourceDepth = source.getDepthStencilState();
        if (sourceDepth == null) {
            builder.withDepthStencilState(Optional.empty());
        } else {
            builder.withDepthStencilState(sourceDepth);
        }
        return builder;
    }

    private static void copyDefine(RenderPipeline.Builder builder, String name, String value) {
        try {
            if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0) {
                builder.withShaderDefine(name, Float.parseFloat(value));
            } else {
                builder.withShaderDefine(name, Integer.parseInt(value));
            }
        } catch (NumberFormatException ignored) {
            builder.withShaderDefine(name);
        }
    }

    /**
     * Delegates texture/setup preparation to an existing render type, but substitutes a cloned,
     * operation-tagged pipeline. This replaces the removed 26.1 {@code RenderType#draw} wrapper.
     */
    private static final class PipelineOverrideRenderType extends RenderType {
        private final RenderType wrapped;
        private final RenderPipeline operationPipeline;

        private PipelineOverrideRenderType(String name, RenderType wrapped, RenderPipeline operationPipeline) {
            super(name, FAKE_SETUP);
            this.wrapped = wrapped;
            this.operationPipeline = operationPipeline;
        }

        @Override
        public boolean hasBlending() {
            return this.wrapped.hasBlending();
        }

        @Override
        public OutputTarget outputTarget() {
            return this.wrapped.outputTarget();
        }

        @Override
        public PreparedRenderType prepare() {
            PreparedRenderType prepared = this.wrapped.prepare();
            return new PreparedRenderType(
                    this.operationPipeline,
                    prepared.outputTarget(),
                    prepared.dynamicTransforms(),
                    prepared.scissorState(),
                    prepared.textures());
        }

        @Override
        public VertexFormat format() {
            return this.wrapped.format();
        }

        @Override
        public PrimitiveTopology primitiveTopology() {
            return this.wrapped.primitiveTopology();
        }

        @Override
        public Optional<RenderType> outline() {
            return this.wrapped.outline();
        }

        @Override
        public boolean isOutline() {
            return this.wrapped.isOutline();
        }

        @Override
        public RenderPipeline pipeline() {
            return this.operationPipeline;
        }

        @Override
        public boolean affectsCrumbling() {
            return this.wrapped.affectsCrumbling();
        }

        @Override
        public boolean canConsolidateConsecutiveGeometry() {
            return this.wrapped.canConsolidateConsecutiveGeometry();
        }

        @Override
        public boolean sortOnUpload() {
            return this.wrapped.sortOnUpload();
        }
    }
}

package com.tacz.guns.config.client;

import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import net.neoforged.neoforge.common.ModConfigSpec;

public class RenderConfig {
    public static ModConfigSpec.BooleanValue ENABLE_LASER_FADE_OUT;
    public static ModConfigSpec.IntValue GUN_LOD_RENDER_DISTANCE;
    public static ModConfigSpec.IntValue BULLET_HOLE_PARTICLE_LIFE;
    public static ModConfigSpec.DoubleValue BULLET_HOLE_PARTICLE_FADE_THRESHOLD;
    public static ModConfigSpec.EnumValue<CrosshairType> CROSSHAIR_TYPE;
    public static ModConfigSpec.DoubleValue HIT_MARKET_START_POSITION;
    public static ModConfigSpec.BooleanValue HEAD_SHOT_DEBUG_HITBOX;
    /** 瞄准镜镜内裁剪（目镜掩码）总开关。默认<b>开启</b>。 */
    public static ModConfigSpec.BooleanValue SCOPE_MASK_ENABLE;
    /** 调试：将当帧离屏目镜掩码显示在屏幕左上角。 */
    public static ModConfigSpec.BooleanValue SCOPE_MASK_DEBUG;
    /** 用目镜投影凸包填充稀疏板条模型的孔径。 */
    public static ModConfigSpec.BooleanValue SCOPE_MASK_HULL_FILL;
    /** 将物理 ocular_ring 从裁剪批次摘出并使用普通 RenderType 重画。 */
    public static ModConfigSpec.BooleanValue SCOPE_OCULAR_RING_FIX;
    /** 红点/低倍 sight 通道不裁镜身，对齐上游 renderSight。 */
    public static ModConfigSpec.BooleanValue SCOPE_SIGHT_CLIP_FIX;
    public static ModConfigSpec.BooleanValue GUN_HUD_ENABLE;
    public static ModConfigSpec.BooleanValue KILL_AMOUNT_ENABLE;
    public static ModConfigSpec.DoubleValue KILL_AMOUNT_DURATION_SECOND;
    public static ModConfigSpec.IntValue TARGET_RENDER_DISTANCE;
    public static ModConfigSpec.BooleanValue FIRST_PERSON_BULLET_TRACER_ENABLE;
    public static ModConfigSpec.BooleanValue DISABLE_INTERACT_HUD_TEXT;
    public static ModConfigSpec.BooleanValue AUTO_SELECT_GUN_SMITH_TABLE_FILTER;
    public static ModConfigSpec.IntValue DAMAGE_COUNTER_RESET_TIME;
    public static ModConfigSpec.BooleanValue DISABLE_MOVEMENT_ATTRIBUTE_FOV;
    public static ModConfigSpec.BooleanValue ENABLE_TACZ_ID_IN_TOOLTIP;
    public static ModConfigSpec.BooleanValue BLOCK_ENTITY_TRANSLUCENT;

    /**
     * 瞄准镜「镜内画中画（PIP）」总开关。默认<b>关闭</b>。
     *
     * <p>开启后世界 FOV <b>不再</b>整屏变焦（{@code CameraSetupEvent#applyScopeMagnification}
     * 对装了倍镜的枪整体让位），改为把已画好的世界拷一份、按倍率在屏幕空间重投影，
     * 再由目镜掩码把这张图贴进镜片孔径 —— 于是镜外保持 1×、只有镜片里是放大的。
     *
     * <p>代价是镜内画面来自主画面中心裁切区的放大，高倍镜（6× 以上）会明显变软。
     * 因为这属于观感取舍而非性能取舍，默认关闭、由玩家自己选。
     *
     * <p>随姊妹分支 {@code TaCZ_Refabricated_Unofficial} 的 {@code 26.2(main)} 同步而来。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ENABLE;
    /** 开镜进度低于该值时不做 PIP（此时孔径几乎闭合，拷贝纯属浪费）。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_MIN_AIMING_PROGRESS;
    /**
     * 镜内锐化强度（0 = 关）。镜内画面是按倍率放大来的，锐化不能凭空造出细节，
     * 但能挽回主观锐度；实际强度按倍率线性加权，低倍镜不会被过度处理。
     */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_SHARPNESS;
    /**
     * 瞄具倍率里有多大一份由<b>世界</b>承担（0 = 全归镜内，纯 PIP；1 = 全归世界，等于关掉 PIP）。
     *
     * <p>镜内清晰度的硬上限是「屏幕分辨率 ÷ 镜内放大倍数」。倍率是相乘的，
     * 按 {@code 世界 = Z^share、镜内 = Z^(1-share)} 拆分后总倍率恒为 Z，
     * 而镜内拿到的真实像素<b>多 Z^share 倍</b>。刻意用<b>比例</b>而不是绝对倍率上限。
     */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_WORLD_ZOOM_SHARE;
    /**
     * 允许在光影包启用时也跑 PIP。默认<b>关闭</b>。
     *
     * <p>关闭是保守默认，不是「已知不兼容」—— 延迟管线下拷到的也许是未着色的中间结果，
     * 且合成写的是裸颜色，镜内可能偏灰或过曝。两者都只是观感风险。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ALLOW_SHADER_PACKS;
    /**
     * 镜内画面改用「窄 FOV 把世界再画一遍」，而不是重投影主画面。默认<b>关闭</b>。
     *
     * <p>重投影的镜内分辨率上限是「屏幕分辨率 ÷ 倍率」。本模式的镜内像素是真画出来的，
     * 没有那个上限；代价是每帧多跑一遍完整世界渲染。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_RERENDER;
    /** 二次渲染（非光影模式）下镜内画面的渲染分辨率比例（1.0 = 屏幕原生分辨率）。开销按面积走。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_RESOLUTION_SCALE;
    /** 镜内那一遍的阴影贴图分辨率比例（1.0 = 与主画面相同）。仅二次渲染 + 隔离管线 + 光影时生效。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_SHADOW_SCALE;
    /**
     * 二次渲染 + 光影时，是否给镜内那一遍配一套独立的 Iris 管线。
     *
     * <p>不隔离的话，Iris 那一整族「上一帧」uniform 会被一帧推进两次，
     * 主画面的时域效果（TAA、体积云、SSGI）全部失准。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ISOLATE_PIPELINE;
    /** 【诊断】跑完镜内那一遍，但<b>不做合成</b> —— 用来切开「放大画面溢出到镜外」的两种成因。 */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_DEBUG_NO_COMPOSITE;
    /** 【诊断】把镜内那一遍期间的渲染目标解析顺序打进日志（只记前几帧）。 */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_DEBUG_TRACE;
    /** 【诊断】把合成实际覆盖到的区域涂成纯品红：整屏变品红 = 合成没被掩码约束住。 */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_DEBUG_PAINT_LENS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("render");

        builder.comment("Whether or not apply fadeout effect on the laser beam. Close this may improve laser performance under some shaders.");
        ENABLE_LASER_FADE_OUT = builder.define("EnableLaserFadeOut", true);

        builder.comment("How far to display the lod model, 0 means always display");
        GUN_LOD_RENDER_DISTANCE = builder.defineInRange("GunLodRenderDistance", 0, 0, Integer.MAX_VALUE);

        builder.comment("The existence time of bullet hole particles, in tick");
        BULLET_HOLE_PARTICLE_LIFE = builder.defineInRange("BulletHoleParticleLife", 400, 0, Integer.MAX_VALUE);

        builder.comment("The threshold for fading out when rendering bullet hole particles");
        BULLET_HOLE_PARTICLE_FADE_THRESHOLD = builder.defineInRange("BulletHoleParticleFadeThreshold", 0.98, 0, 1);

        builder.comment("The crosshair when holding a gun");
        CROSSHAIR_TYPE = builder.defineEnum("CrosshairType", CrosshairType.DOT_1);

        builder.comment("The starting position of the hit marker");
        HIT_MARKET_START_POSITION = builder.defineInRange("HitMarketStartPosition", 4d, -1024d, 1024d);

        builder.comment("Whether or not to display the head shot's hitbox");
        HEAD_SHOT_DEBUG_HITBOX = builder.define("HeadShotDebugHitbox", false);
        SCOPE_MASK_ENABLE = builder
                .comment("Whether to clip scope bodies, reticles and viewmodel effects using the 26.2 off-screen ocular mask.")
                .define("ScopeMaskEnable", true);
        SCOPE_MASK_DEBUG = builder
                .comment("Debug: draw the off-screen ocular mask at the top-left corner.")
                .define("ScopeMaskDebug", false);
        SCOPE_MASK_HULL_FILL = builder
                .comment("Fill the convex hull of the ocular projection; fixes sparse/sliver ocular geometry. "
                        + "Set false to use the raw ocular geometry as an instant fallback.")
                .define("ScopeMaskHullFill", true);
        SCOPE_OCULAR_RING_FIX = builder
                .comment("Draw the physical ocular_ring unclipped while aiming, matching upstream stencil-ALWAYS semantics.")
                .define("ScopeOcularRingFix", true);
        SCOPE_SIGHT_CLIP_FIX = builder
                .comment("Do not clip the scope body for red-dot/low-power sight channels, matching upstream renderSight.")
                .define("ScopeSightClipFix", true);

        builder.comment("Whether or not to display the gun's HUD");
        GUN_HUD_ENABLE = builder.define("GunHUDEnable", true);

        builder.comment("Whether or not to display the kill amount");
        KILL_AMOUNT_ENABLE = builder.define("KillAmountEnable", true);

        builder.comment("The duration of the kill amount, in second");
        KILL_AMOUNT_DURATION_SECOND = builder.defineInRange("KillAmountDurationSecond", 3, 0, Double.MAX_VALUE);

        builder.comment("The farthest render distance of the target, including minecarts type");
        TARGET_RENDER_DISTANCE = builder.defineInRange("TargetRenderDistance", 128, 0, Integer.MAX_VALUE);

        builder.comment("Whether or not to render first person bullet trail");
        FIRST_PERSON_BULLET_TRACER_ENABLE = builder.define("FirstPersonBulletTracerEnable", true);

        builder.comment("Disable the interact hud text in center of the screen");
        DISABLE_INTERACT_HUD_TEXT = builder.define("DisableInteractHudText", false);

        builder.comment("Whether or not to automatically select the gun smith table's held item filter when opening it with a gun, attachment or ammo in main hand");
        AUTO_SELECT_GUN_SMITH_TABLE_FILTER = builder.define("AutoSelectGunSmithTableFilter", true);

        builder.comment("Max time the damage counter will reset");
        DAMAGE_COUNTER_RESET_TIME = builder.defineInRange("DamageCounterResetTime", 2000, 10, Integer.MAX_VALUE);

        builder.comment("Disable the fov effect from the movement speed attribute while holding a gun");
        DISABLE_MOVEMENT_ATTRIBUTE_FOV = builder.define("DisableMovementAttributeFov", true);

        builder.comment("Enable the display of the TACZ ID in the tooltip when Advanced Tooltip is enabled");
        ENABLE_TACZ_ID_IN_TOOLTIP = builder.define("EnableTaczIdInTooltip", true);

        builder.comment("Enable translucent while render block entity or not. Enable this option will result in ADDITIONAL PERFORMANCE OVERHEAD.");
        BLOCK_ENTITY_TRANSLUCENT = builder.define("EnableBlockEntityTranslucent", false);

        // ---- Scope picture-in-picture (synced from the Fabric sister branch's 26.2 line) ----
        SCOPE_PIP_ENABLE = builder
                .comment("Magnify only INSIDE the scope lens instead of zooming the whole screen",
                        "(picture-in-picture). The view around the scope tube stays at 1x.",
                        "Implemented by reprojecting a copy of the already-rendered frame, so it costs",
                        "one fullscreen copy and stays compatible with terrain renderer replacements.",
                        "Tradeoff: the lens magnifies a centre crop of the frame, so high-power scopes",
                        "(6x and up) look noticeably softer than the surrounding view.",
                        "Requires ScopeMaskEnable and is skipped while a shader pack is active. Default off.")
                .define("ScopePipEnable", false);
        SCOPE_PIP_MIN_AIMING_PROGRESS = builder
                .comment("Skip the picture-in-picture work while the aiming progress is below this value",
                        "(the ocular aperture is still nearly closed down there).")
                .defineInRange("ScopePipMinAimingProgress", 0.05d, 0.0d, 1.0d);
        SCOPE_PIP_SHARPNESS = builder
                .comment("Sharpening applied to the scope image (0 = off). The lens magnifies a centre crop",
                        "of the frame by exactly the scope's zoom factor, so high-power optics are soft;",
                        "sharpening cannot invent detail but recovers a lot of apparent crispness.",
                        "Strength is scaled by magnification, so low-power optics stay untouched.")
                .defineInRange("ScopePipSharpness", 0.5d, 0.0d, 1.0d);
        SCOPE_PIP_WORLD_ZOOM_SHARE = builder
                .comment("How much of the scope's magnification the WORLD takes, instead of the lens.",
                        "Trades the purity of PIP for real sharpness inside the lens.",
                        "",
                        "The lens magnifies a centre crop of the frame, so at Zx it only has 1/Z of the",
                        "screen's pixels to work with -- that is the whole reason high-power optics look",
                        "soft. Zoom factors multiply, so the split is:",
                        "    world = Z^share      lens = Z^(1-share)      world * lens = Z always",
                        "  0.0 = the lens does all the work; outside stays 1x (purest PIP, softest)",
                        "  0.5 = split evenly; the lens image is built from sqrt(Z)x more real pixels",
                        "  1.0 = the world does all the work (identical to turning PIP off)",
                        "Ignored when ScopePipRerender is on -- that path already renders at native",
                        "resolution, so zooming the world would cost image quality for nothing.")
                .defineInRange("ScopePipWorldZoomShare", 0.0d, 0.0d, 1.0d);
        SCOPE_PIP_ALLOW_SHADER_PACKS = builder
                .comment("Allow the scope picture-in-picture to run while a shader pack is active.",
                        "Off by default as a precaution, NOT because it is known to be broken: under a",
                        "deferred shader pipeline the captured frame may not be fully shaded yet, and the",
                        "composite writes raw colour before tonemapping, so the lens could look flat or",
                        "blown out. Nothing can be corrupted by trying it - turn it on and look.")
                .define("ScopePipAllowShaderPacks", false);
        SCOPE_PIP_RERENDER = builder
                .comment("Draw the scope image by rendering the world a SECOND time with a narrow FOV,",
                        "instead of reprojecting the already-rendered frame. The lens then has native",
                        "resolution instead of being capped at screen resolution / zoom, which matters a",
                        "lot for 6x-8x optics. Costs a full extra world render every frame.",
                        "",
                        "EXPERIMENTAL: it drives LevelRenderer#render twice per frame, so every third-party",
                        "renderer's per-frame state (projection snapshot, chunk uniform upload gate, LOD",
                        "viewport) has to be synced by hand. Default off.")
                .define("ScopePipRerender", false);
        SCOPE_PIP_RESOLUTION_SCALE = builder
                .comment("Render resolution scale for the scope pass in rerender mode (1.0 = native).",
                        "Default 0.75 (~56% pixels of full frame), greatly reducing the GPU cost of the",
                        "scope view. Non-shader path only; under a shader pack the scope pass runs at 1.0")
                .defineInRange("ScopePipResolutionScale", 0.75d, 0.25d, 1.0d);
        SCOPE_PIP_SHADOW_SCALE = builder
                .comment("Shadow map resolution for the scope pass, as a fraction of the pack's own.",
                        "Only used with ScopePipRerender + ScopePipIsolatePipeline + a shader pack.",
                        "Iris renders shadows once per world render, so rendering the world twice draws",
                        "the whole shadow map twice per frame. Cost scales with AREA, so 0.5 cuts that",
                        "pass' shadow work to about a quarter. Rounded down to a power of two, minimum 256.",
                        "Only the lens is affected; the main view keeps the pack's full shadow map.",
                        "Takes effect when the scope pipeline is built, so restart or change dimension.")
                .defineInRange("ScopePipShadowScale", 0.5d, 0.25d, 1.0d);
        SCOPE_PIP_ISOLATE_PIPELINE = builder
                .comment("Give the scope pass its own shader pipeline, so its temporal state cannot",
                        "corrupt the main view. Only has any effect with ScopePipRerender on and a",
                        "shader pack active.",
                        "Costs an extra set of shader buffers and a one-time shader compile the first",
                        "time you aim (pre-built in advance, so it does not stall the first ADS).")
                .define("ScopePipIsolatePipeline", true);
        SCOPE_PIP_DEBUG_NO_COMPOSITE = builder
                .comment("[Diagnostics] Run the scope pass but skip the composite step. Splits the two",
                        "possible causes of 'the magnified image leaks outside the lens'.")
                .define("ScopePipDebugNoComposite", false);
        SCOPE_PIP_DEBUG_TRACE = builder
                .comment("[Diagnostics] Log how render targets resolve during the scope pass (first few",
                        "frames only). Every attempt at diagnosing this statically guessed wrong.")
                .define("ScopePipDebugTrace", false);
        SCOPE_PIP_DEBUG_PAINT_LENS = builder
                .comment("[Diagnostics] Paint everything the composite covers in solid magenta.",
                        "Whole screen turning magenta = the composite is not constrained by the mask.")
                .define("ScopePipDebugPaintLens", false);

        builder.pop();
    }
}

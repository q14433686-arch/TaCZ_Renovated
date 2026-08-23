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
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ENABLE;
    /** 开镜进度低于该值时不做 PIP（此时孔径几乎闭合，拷贝纯属浪费）。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_MIN_AIMING_PROGRESS;
    /**
     * 镜内锐化强度（0 = 关）。
     *
     * <p>镜内画面是主画面中心区按倍率放大来的，放大倍数<b>就是</b>瞄具倍率 ——
     * 6 倍镜就是 6× 放大，必然变软。锐化不能凭空造出细节，但能显著挽回主观锐度。
     * 实际强度按倍率线性加权（1× 不锐化，6× 及以上取满），所以低倍镜不会被过度处理。
     */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_SHARPNESS;
    /**
     * 瞄具倍率里有多大一份由<b>世界</b>承担（0 = 全归镜内，纯 PIP；1 = 全归世界，等于关掉 PIP）。
     *
     * <p>镜内清晰度的硬上限是「屏幕分辨率 ÷ 镜内放大倍数」。倍率是相乘的，
     * 按 {@code 世界 = Z^share、镜内 = Z^(1-share)} 拆分后总倍率恒为 Z，
     * 而镜内拿到的真实像素<b>多 Z^share 倍</b>。
     * 这是唯一能真正增加镜内分辨率（而非仅提升主观锐度）的旋钮。
     *
     * <p>刻意用<b>比例</b>而不是绝对倍率上限：后者会被 Z 夹住，
     * 于是任何 ≥ Z 的取值都让镜内倍率退化成 1（PIP 名存实亡），
     * 且那个临界点随瞄具倍率漂移 —— 实测中玩家正是撞上了这条。
     */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_WORLD_ZOOM_SHARE;
    /**
     * 开镜时持枪晃动的强度倍数（{@code 1.0} = 与改动前一致）。
     *
     * <p>晃动本身是「枪跟不上视角转动」的滞后量，腰射与开镜原本一视同仁。
     * 但开镜后视野被瞄具收窄、镜内还被放大 Z 倍，同样的角度抖动在镜内会被放大同样的倍数
     * —— 现实里高倍镜正是「越放大越难稳住」。本项让开镜那一档单独可调。
     *
     * <p>按开镜进度插值：腰射恒为 1，满开镜取到本值。{@code 0} = 满开镜时完全不晃。
     */
    public static ModConfigSpec.DoubleValue AIMING_SWAY_INTENSITY;
    /**
     * 允许在光影包启用时也跑 PIP。默认<b>关闭</b>。
     *
     * <p>关闭是保守默认，不是「已知不兼容」—— 见 {@code ScopePipRenderer} 里的说明。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ALLOW_SHADER_PACKS;
    /**
     * 镜内画面改用「窄 FOV 把世界再画一遍」，而不是重投影主画面。默认<b>关闭</b>。
     *
     * <p>重投影的镜内分辨率上限是「屏幕分辨率 ÷ 倍率」—— 8 倍镜下惨不忍睹。
     * 本模式的镜内像素是真画出来的，没有那个上限；代价是每帧多跑一遍完整世界渲染。
     *
     * <p>已知：与 Sodium 的地形投影快照需要同步（{@code SodiumCompat} 负责）；
     * 早前的实现还出现过「镜外实体消失」，尚未定位，所以默认关闭、按需自测。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_RERENDER;
    /** 镜内二次渲染的分辨率缩放比例（0.25 到 1.0，默认 0.5）。面积开销按平方走，0.5 可省约 75% 像素光照填充。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_RESOLUTION_SCALE;
    /**
     * 二次渲染 + 光影时，是否给镜内那一遍配一套独立的 Iris 管线。
     *
     * <p>不隔离的话，Iris 那一整族「上一帧」uniform 会被一帧推进两次，
     * 主画面的时域效果（TAA、体积云、SSGI）全部失准 —— 表现为拖影、云噪点，
     * 以及<b>开镜时镜外整屏发糙</b>。隔离的代价是多一套 colortex（显存）。
     */
    /** 镜内那一遍的阴影贴图分辨率比例（1.0 = 与主画面相同）。开销按面积走。 */
    public static ModConfigSpec.DoubleValue SCOPE_PIP_SHADOW_SCALE;
    public static ModConfigSpec.BooleanValue SCOPE_PIP_ISOLATE_PIPELINE;
    /**
     * 【诊断】跑完镜内那一遍，但<b>不做合成</b>。
     *
     * <p>用来一刀切开「放大画面溢出到镜外」这个症状的两种可能：
     * <ul>
     *   <li>画面干净（只有正常世界、镜片里什么都没有）→ 二次渲染是<b>关在离屏 target 里</b>的，
     *       溢出来自合成/掩码；</li>
     *   <li>放大画面照样溢出 → 二次渲染<b>漏到主画面</b>了，与合成无关。</li>
     * </ul>
     * 两种情况的修法完全不同，靠肉眼看成品是分不出来的。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_DEBUG_NO_COMPOSITE;
    /**
     * 【诊断】把「镜内二次渲染」那一帧的渲染目标解析过程打出来（限前 3 帧）。
     *
     * <p>任何在 SCOPE-PASS BEGIN 与 END 之间解析到的 MAIN 就是漏到主画面的那一笔，
     * 日志里附带调用栈，一眼定凶。
     */
    public static ModConfigSpec.BooleanValue SCOPE_PIP_DEBUG_TRACE;
    /**
     * 【诊断】把合成覆盖到的区域涂成纯品红。
     *
     * <p>用来一眼看清合成到底盖了多大范围：
     * <ul>
     *   <li>整屏变品红 → 合成<b>没有被掩码约束住</b>，放大的世界会被整屏糊上去；</li>
     *   <li>只有镜片是品红 → 合成范围是对的，溢出来自二次渲染本身漏到了主画面。</li>
     * </ul>
     */
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

        SCOPE_PIP_ENABLE = builder
                .comment("Scope Picture-in-Picture (PIP): renders a magnified world inside the scope",
                        "while keeping the surrounding world at normal FOV (1x).",
                        "When enabled, aiming with magnified optics will no longer zoom the entire screen.",
                        "Default: false (the original whole-screen FOV zoom behaviour).")
                .define("ScopePipEnable", false);
        SCOPE_PIP_MIN_AIMING_PROGRESS = builder
                .comment("Do not run PIP while aiming progress is below this threshold (0.0 to 1.0).",
                        "Near 0 the ocular aperture is essentially closed, so copying/reprojecting",
                        "the scene is wasted work.")
                .defineInRange("ScopePipMinAimingProgress", 0.05d, 0.0d, 1.0d);
        SCOPE_PIP_SHARPNESS = builder
                .comment("Maximum unsharp-mask sharpness applied to the PIP image (0 = off, 1 = max).",
                        "The scope image is magnified from the center of the rendered scene, so high-power",
                        "optics soften noticeably. Sharpening cannot recover missing pixels, but helps",
                        "perceived clarity. Strength scales with magnification (1x = no sharpening,",
                        "6x+ = full strength), so low-power optics are never over-sharpened.")
                .defineInRange("ScopePipSharpness", 0.5d, 0.0d, 1.0d);
        SCOPE_PIP_WORLD_ZOOM_SHARE = builder
                .comment("How much of the scope's magnification is shared with the outside world,",
                        "between 0.0 (pure PIP, world stays 1x) and 1.0 (whole-screen zoom, PIP off).",
                        "",
                        "The hard limit on PIP sharpness is (screen pixels / magnification) -- an 8x scope",
                        "only has 1/8th the pixels to reconstruct from. Sharing zoom with the world is the",
                        "ONLY way to give the lens more real pixels without rerendering the scene:",
                        "  0.0 = pure PIP: outside world stays 1x, lens is softest (default)",
                        "  0.5 = split evenly; the lens image is built from sqrt(Z)x more real pixels",
                        "  1.0 = the world does all the work (identical to turning PIP off)",
                        "",
                        "Total magnification is always exactly the scope's zoom, at every setting --",
                        "this only moves where it comes from. Scale-independent, so the same value",
                        "behaves the same on a 2x and an 8x optic.",
                        "Ignored when ScopePipRerender is on -- that path already renders at native",
                        "resolution, so zooming the world would cost image quality for nothing.")
                .defineInRange("ScopePipWorldZoomShare", 0.0d, 0.0d, 1.0d);
        AIMING_SWAY_INTENSITY = builder
                .comment("How much the gun sways while aiming down sights, as a multiplier.",
                        "",
                        "Sway is the gun lagging behind your view when you turn -- it is what makes the",
                        "sight picture drift and settle. Hip fire is never affected by this setting; the",
                        "value is blended in by aiming progress, so it reaches full strength only when",
                        "fully scoped.",
                        "  0.0 = rock steady once fully aimed, no sway at all",
                        "  1.0 = the original amount, identical to before this option existed",
                        "  1.5 = default, noticeably more alive without being hard to aim",
                        "  3.0+ = heavy, deliberately difficult",
                        "Worth raising with high-power optics: a narrow field of view (and the PIP lens,",
                        "which magnifies by the scope's zoom on top) multiplies the same angular wobble,",
                        "the way real magnified optics get harder to hold steady.",
                        "The fast-turn safety clamp still applies, so the gun cannot swing off screen.")
                .defineInRange("AimingSwayIntensity", 1.5d, 0.0d, 5.0d);
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
                        "This now works with shader packs too. The scope pass runs the shader pipeline",
                        "to completion first, its finished image is copied aside, and the normal frame",
                        "then renders over it -- two sequential frames as far as Iris is concerned, so",
                        "they reuse the same buffers and cost no extra VRAM.",
                        "Expect roughly HALF the frame rate with shaders on, since the whole pipeline",
                        "(shadow maps and composite chain included) runs twice. Temporal effects such",
                        "as TAA advance twice per frame as well, which can show up as ghosting or",
                        "shimmer; if that bothers you, use ScopePipWorldZoomShare instead.",
                        "",
                        "EXPERIMENTAL: an earlier attempt made entities vanish from the main view.",
                        "Default off.")
                .define("ScopePipRerender", false);
        SCOPE_PIP_RESOLUTION_SCALE = builder
                .comment("Resolution scale for the secondary world render pass (0.25 to 1.0).",
                        "Only used when ScopePipRerender is true.",
                        "Rendering at 0.5 (half resolution) cuts the pixel fillrate and lighting cost",
                        "by ~75% while maintaining sharp lens clarity on high-DPI displays.",
                        "Default: 0.5. Set to 1.0 for native 1:1 screen resolution.")
                .defineInRange("ScopePipResolutionScale", 0.5d, 0.25d, 1.0d);
        SCOPE_PIP_SHADOW_SCALE = builder
                .comment("Shadow map resolution for the scope pass, as a fraction of the pack's own.",
                        "Only used with ScopePipRerender + ScopePipIsolatePipeline + a shader pack.",
                        "",
                        "Iris renders shadows once per world render, so rendering the world twice draws",
                        "the whole shadow map twice per frame -- often one of the most expensive things",
                        "in a shader frame. Cost scales with AREA, so 0.5 cuts that pass' shadow work to",
                        "about a quarter. Rounded down to a power of two, minimum 256.",
                        "Only the lens is affected; the main view keeps the pack's full shadow map.",
                        "  1.0 = same as the main view (no saving)",
                        "  0.5 = default, ~1/4 the shadow cost for the scope pass",
                        "  0.25 = ~1/16, visibly blockier shadows in the lens",
                        "Takes effect when the scope pipeline is built, so restart or change dimension.")
                .defineInRange("ScopePipShadowScale", 0.5d, 0.25d, 1.0d);
        SCOPE_PIP_ISOLATE_PIPELINE = builder
                .comment("Give the scope pass its own shader pipeline, so its temporal state cannot",
                        "corrupt the main view. Only has any effect with ScopePipRerender on and a",
                        "shader pack active.",
                        "",
                        "Iris advances every 'previous frame' value when it is READ, not once per frame.",
                        "Rendering twice therefore leaves the main view reprojecting against the scope",
                        "pass's matrices, which breaks TAA, volumetric clouds and SSGI at once: ghosting,",
                        "shimmering clouds, and a grainy screen outside the scope while aiming (that",
                        "graininess is temporal accumulation failing, not sharpening).",
                        "Isolating the pass gives it separate buffers and separate uniforms, so both",
                        "views stay correct.",
                        "",
                        "Costs an extra set of shader buffers (a few hundred MB of VRAM at high",
                        "resolutions) and a one-time shader compile the first time you aim. Turn this",
                        "off if VRAM is tight, and the artifacts above come back.",
                        "Note: in dimensions other than the Overworld the lens may use the pack's",
                        "fallback shaders, since the pass uses its own dimension id.",
                        "",
                        "Voxy is handled alongside this: the scope pass is given its own Voxy viewport",
                        "too, the same way Voxy already separates the Iris shadow pass. Without that,",
                        "Voxy's per-view LOD state gets driven by two different projections in one frame",
                        "and its distant terrain corrupts permanently after the first time you aim.")
                .define("ScopePipIsolatePipeline", true);
        SCOPE_PIP_DEBUG_NO_COMPOSITE = builder
                .comment("[DEBUG] Run the scope pass but skip pasting it into the lens. Use this to tell",
                        "whether magnified imagery leaking outside the scope comes from the off-screen",
                        "pass escaping onto the screen, or from the composite/mask not confining it:",
                        "  clean screen, empty lens -> the off-screen pass is contained; blame the composite",
                        "  magnified imagery still leaks -> the pass itself is escaping to the main target")
                .define("ScopePipDebugNoComposite", false);
        SCOPE_PIP_DEBUG_TRACE = builder
                .comment("[DEBUG] Log which code resolves which render target during the scope pass,",
                        "for the first few frames only. Any line marked MAIN between SCOPE-PASS BEGIN",
                        "and SCOPE-PASS END is imagery escaping onto the screen; anything logged after",
                        "the vanilla clear means a renderer submits its draws late and cannot be",
                        "redirected at all.",
                        "",
                        "EXPENSIVE. While armed this walks the call stack on every render-target",
                        "resolve, and Sodium, Voxy and the frame graph all hit that path many times a",
                        "frame. Leave it off for normal play: it stalls the render thread enough that",
                        "terrain uploads pile up, and the resulting oversized GPU buffer request can",
                        "fail outright while exploring. It now disarms itself after a few hundred",
                        "frames regardless, but there is no reason to pay for it unless you are",
                        "chasing a scope render bug.")
                .define("ScopePipDebugTrace", false);
        SCOPE_PIP_DEBUG_PAINT_LENS = builder
                .comment("[DEBUG] Paint the area the scope composite actually covers in solid magenta.",
                        "  whole screen magenta -> the composite is NOT confined by the ocular mask",
                        "  only the lens magenta -> the composite is fine and the leak is elsewhere")
                .define("ScopePipDebugPaintLens", false);

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

        builder.pop();
    }
}

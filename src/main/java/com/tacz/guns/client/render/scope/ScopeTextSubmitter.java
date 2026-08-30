package com.tacz.guns.client.render.scope;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把一段文字用【镜内裁剪】管线提交 —— 绕开 vanilla 字体管线的徒手路径
 * （镜内文字一案的核心部件）。
 *
 * <h2>为什么必须绕开 vanilla 管线</h2>
 * {@code submitText} 的下游 {@code TextFeatureRenderer.buildGroup}（字节码实读）：
 * <pre>
 * PreparedText p = font.prepareText(string, x, y, color, shadow, false, bg);
 * p.visit(glyphRenderer);   // 每个 renderable：
 * //   consumer = getVertexBuilder(renderable.renderType(displayMode));  ← 写死
 * //   renderable.render(pose, consumer, lightCoords, false);
 * </pre>
 * RenderType 由 {@code GlyphRenderTypes} 三件套写死，无注入点。
 * 但 {@code prepareText → visit} 这两步是公开 API，且
 * {@code TextRenderable#render(Matrix4fc, VertexConsumer, int, boolean)}
 * 接受任意 consumer —— 本类做的就是把 vanilla 那两步搬过来，
 * 唯独把 RenderType 换成 {@link ScopeTextRenderTypes#clippedText}。
 *
 * <h2>多页字体图集的处理</h2>
 * 每个字形的 UV 挂在它自己那页图集上（{@code TextRenderable#textureView}）。
 * vanilla 靠 renderType 里已绑好的页纹理分批；我们按 view 分组，
 * 每组对应一个「壳 Identifier」（{@link PageHandle}，思路与
 * {@link ScopeMaskTextureHandle} 相同：AbstractTexture 空壳 + 每帧刷新指向），
 * 组内字形共用一次 submitCustomGeometry。默认位图字体常驻页数 1-2，
 * 分组开销可忽略。
 *
 * <h2>光照与淡入</h2>
 * 管线底是 WORLD_TEXT_SNIPPET（带 lightmap），{@code packedLight} 原样传递 ——
 * 瞄具文字的 {@code textLight} 语义（display json 可配全亮）不变。
 * 淡入不走 alpha 乘法（字形顶点色 alpha 由 render 内部写死为文字颜色的 alpha），
 * 而是沿用调用方现有的「开镜进度门禁」：进度不足时调用方压根不走本路径。
 */
public final class ScopeTextSubmitter {

    /**
     * 字体图集页的壳纹理（复用 {@link ScopeMaskTextureHandle} 的思路，
     * 生命周期声明也相同：本类不拥有页纹理，close 为 no-op）。
     *
     * <p>sampler 抄 vanilla {@code FontTexture.<init>}（字节码实读）：
     * {@code getRepeat(FilterMode.NEAREST)} —— 字体图集就是这么绑的，
     * 换 clamp 会在字形边缘出现与 vanilla 不一致的采样。</p>
     */
    private static final class PageHandle extends AbstractTexture {
        void point(GpuTextureView view) {
            this.texture = view.texture();
            this.textureView = view;
            this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        }

        @Override
        public void close() {
            // 页纹理归 FontTexture 所有，勿动。
        }
    }

    /** view → 壳。字体图集页会随资源重载更换 view，用 identity 语义即可。 */
    private static final Map<GpuTextureView, Identifier> PAGE_IDS = new HashMap<>();
    private static final Map<Identifier, PageHandle> PAGE_HANDLES = new HashMap<>();
    private static int nextPageOrdinal = 0;

    private static Identifier pageId(GpuTextureView view) {
        Identifier id = PAGE_IDS.get(view);
        if (id == null) {
            // 注册名只求稳定唯一；序号一旦发出永不复用，
            // 资源重载后旧 view 的表项残留也只是闲置壳，无害。
            id = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "scope_text_page_" + nextPageOrdinal++);
            PAGE_IDS.put(view, id);
        }
        PageHandle handle = PAGE_HANDLES.get(id);
        if (handle == null) {
            handle = new PageHandle();
            PAGE_HANDLES.put(id, handle);
            Minecraft.getInstance().getTextureManager().register(id, handle);
        }
        // 每帧刷新指向（幂等）：与 ScopeMaskTextureHandle 同一防悬空策略。
        handle.point(view);
        return id;
    }

    /**
     * 用镜内裁剪管线提交一段文字。调用方语义与
     * {@code OrderedSubmitNodeCollector#submitText} 对齐（NORMAL 模式子集）。
     *
     * <p>vanilla buildGroup 对 NORMAL 走 {@code prepareText(seq, x, y, color,
     * shadow, false, backgroundColor)} —— 这里传 background=0（瞄具文字
     * 从不配底色，{@code TextShowRender} 现状也是传 0）。</p>
     *
     * @param finalOverlay true = 用「不交给 Iris」的管线副本（{@code core/scope_text_final}），
     *                     供光影下 LevelRenderer#render 之后的最终覆盖那一遍使用；
     *                     false = 走手持 pass 里的常规裁剪管线
     * @return true = 已提交（可能是空文本的静默成功）；
     *         false = 本帧不可用（掩码没就绪），调用方应回退 vanilla submitText
     */
    public static boolean submit(SubmitNodeCollector collector,
                                 PoseStack poseStack,
                                 float x, float y,
                                 FormattedCharSequence text,
                                 boolean shadow,
                                 int packedLight,
                                 int color,
                                 boolean finalOverlay) {
        Map<GpuTextureView, List<TextRenderable>> byPage = prepare(x, y, text, shadow, color);
        if (byPage == null) {
            return false;
        }
        if (byPage.isEmpty()) {
            return true;
        }
        org.joml.Matrix4f pose = new org.joml.Matrix4f(poseStack.last().pose());
        for (Map.Entry<GpuTextureView, List<TextRenderable>> entry : byPage.entrySet()) {
            Identifier page = pageId(entry.getKey());
            List<TextRenderable> renderables = entry.getValue();
            // finalOverlay = true 时换用「不交给 Iris」的管线副本，见
            // ScopeTextRenderTypes#finalText —— 字形与裁剪完全一致，只是由我们的
            // 着色器执行（光影下最后覆盖那一遍用）。
            collector.submitCustomGeometry(new PoseStack(),
                    finalOverlay ? ScopeTextRenderTypes.finalText(page) : ScopeTextRenderTypes.clippedText(page),
                    (entryPose, consumer) ->
                            renderables.forEach(r -> r.render(pose, consumer, packedLight, false)));
        }
        return true;
    }

    /**
     * 最终覆盖那一遍的入口 —— 与 {@link #submit} 唯一的不同是收集器类型。
     *
     * <p>26.2 里 {@code OrderedSubmitNodeCollector} 与 {@code SubmitNodeCollector}
     * 是<b>平级</b>的两个类型（前者不是后者的子类），而
     * {@code SubmitNodeStorage#order} 只能给出前者，所以两条入口只能并列存在。
     * 除了提交动作，准备流程与 {@link #submit} 共用 {@link #prepare}。</p>
     */
    public static boolean submitOrdered(OrderedSubmitNodeCollector collector,
                                        PoseStack poseStack,
                                        float x, float y,
                                        FormattedCharSequence text,
                                        boolean shadow,
                                        int packedLight,
                                        int color) {
        Map<GpuTextureView, List<TextRenderable>> byPage = prepare(x, y, text, shadow, color);
        if (byPage == null) {
            return false;
        }
        if (byPage.isEmpty()) {
            return true;
        }
        org.joml.Matrix4f pose = new org.joml.Matrix4f(poseStack.last().pose());
        for (Map.Entry<GpuTextureView, List<TextRenderable>> entry : byPage.entrySet()) {
            Identifier page = pageId(entry.getKey());
            List<TextRenderable> renderables = entry.getValue();
            // 最终覆盖一律用「不交给 Iris」的管线副本，无例外。
            collector.submitCustomGeometry(new PoseStack(), ScopeTextRenderTypes.finalText(page),
                    (entryPose, consumer) ->
                            renderables.forEach(r -> r.render(pose, consumer, packedLight, false)));
        }
        return true;
    }

    /**
     * 门禁检查 + 按字体图集页分组。{@link #submit} 与 {@link #submitOrdered} 共用。
     *
     * @return {@code null} = 本帧不可用（调用方应回退/跳过）；空 map = 没有可画的字形（静默成功）
     */
    private static Map<GpuTextureView, List<TextRenderable>> prepare(float x, float y,
                                                                    FormattedCharSequence text,
                                                                    boolean shadow, int color) {
        // 门禁与 resolveReticleRenderType 同一份：总开关、光影安全、掩码就绪。
        // （第一人称与开镜进度由调用方 BedrockAttachmentModel 的文字工厂把守。）
        if (!com.tacz.guns.config.client.RenderConfig.SCOPE_MASK_ENABLE.get()) {
            return null;
        }
        if (com.tacz.guns.compat.iris.IrisCompat.shouldDisableScopeMaskUnderShaderPack()) {
            // 只有 Vulkan 系着色器替代（Sulkan）会整体停用掩码 —— 它没有
            // 与 Iris 桥等价的注入通道，target 里可能是陈旧内容，采样会裁错位置。
            // Iris 不在此列：光影下掩码是活的（由注入进光影着色器的
            // tacz_ScopeMaskMode 分支执行），这里必须继续走裁剪管线。
            //
            // 早前这里写的是「光影下掩码整体停用」，那是 Iris 桥落地前的旧政策；
            // 正是这句过时注释让 scope_text_clipped 漏登了 IrisScopeMaskState
            // 的 mode 表，表现为「光影下镜内文字完全不裁」。注释必须跟着机制走。
            return null;
        }
        if (!ScopeMaskTextureHandle.syncToMaskTarget()) {
            return null;
        }
        Font font = Minecraft.getInstance().font;
        Font.PreparedText prepared = font.prepareText(text, x, y, color, shadow, false, 0);

        // 按图集页分组 —— 一组一个 RenderType，一次 custom geometry。
        Map<GpuTextureView, List<TextRenderable>> byPage = new HashMap<>();
        prepared.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptRenderable(TextRenderable renderable) {
                GpuTextureView view = renderable.textureView();
                if (view == null) {
                    // EmptyGlyph 等无纹理项：没有像素要画，静默跳过。
                    return;
                }
                byPage.computeIfAbsent(view, v -> new ArrayList<>()).add(renderable);
            }
        });
        // 空 = 没有可画的字形（空白文本等），调用方静默成功。
        // 真正的提交留给两个入口 —— 它们的收集器类型不同，无法共用一个方法签名。
        return byPage;
    }

    private ScopeTextSubmitter() {
    }
}

#version 330

// 【镜内文字 · 最终覆盖】片元着色器。
//
// 母本：core/scope_text.fsh（= vanilla core/text.fsh + SCOPE_MASK），去掉一样东西：
//   apply_fog —— 这一遍画在【光影包全部 composite/final pass 之后】，
//   雾早就在光影管线里算过了，再叠一次就是二次加雾。
//
// 与 core/scope_ring_final.fsh 同源同理由（那条去掉的是 SCOPE_MASK 与 apply_fog；
// 本条要保留 SCOPE_MASK —— 文字必须被约束在目镜孔内，只是不再过雾）。
//
// 【为什么需要这一条管线】
// 光影下，assign 给 Iris HAND program 的自定义管线会被光影包的手部着色器整条
// 替换 —— 我们的 fsh 一行都不跑，文字的裁剪只能靠注入分支 tacz_ScopeMaskMode。
// 但文字用的是 TEXT 顶点格式（POSITION_TEX_LIGHTMAP_COLOR，【没有 Normal】），
// 交给光影包的 HAND 程序后光照与 lightmap 语义对不上，字形就画成一片黑块
// （注入分支只做 discard，不接管着色，修不了这个）。
//
// 所以光影下的对策是：文字【根本不进 Iris 管线】，延后到 LevelRenderer#render
// 返回之后、用本管线（不 assign 给 Iris，因此由我们自己的着色器执行）重画。
// 这样字形、alpha 裁剪、目镜裁剪全部回到我们手里。

#moj_import <minecraft:dynamictransforms.glsl>
#ifdef SCOPE_MASK
// globals.glsl 提供 ScreenSize（scope_body.fsh 同款用法）。
#moj_import <minecraft:globals.glsl>
#endif

uniform sampler2D Sampler0;

#ifdef SCOPE_MASK
// 目镜掩码：白 = 该像素属于镜内（目镜投影覆盖），黑 = 镜外。
// 由 ScopeMaskRenderer 在阶段边界渲染到离屏 target。
uniform sampler2D ScopeMaskSampler;
#endif

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
#ifdef SCOPE_MASK
    // 与 scope_body.fsh / scope_text.fsh 完全一致的采样约定：
    // gl_FragCoord 左下原点，掩码 target 纹理原点也在左下，不翻 Y。
    vec2 maskUv = gl_FragCoord.xy / ScreenSize;
    if (texture(ScopeMaskSampler, maskUv).r <= 0.5) {
        // 目镜投影之外 —— 文字被镜筒挡住，不可见。
        discard;
    }
#endif

#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif

#ifdef IS_SEE_THROUGH
    vec4 color = texColor * vertexColor;
#else
    vec4 color = texColor * vertexColor * ColorModulator;
#endif
    if (color.a < 0.1) {
        // 字形轮廓之外的透明像素 —— 没有这一句，每个字形就是一块实心方块。
        discard;
    }

    fragColor = color;
}

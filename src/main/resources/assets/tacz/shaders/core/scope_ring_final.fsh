#version 330

// 物理目镜框（遮光环）的「最终覆盖」片元着色器。
//
// 它是 core/scope_body.fsh 去掉两样东西之后的形态：
//   1. 整段 SCOPE_MASK —— 目镜框是实体件，永远不该被掩码裁掉
//      （案例⑨ / 邻链 commit 0b7c4cd 的取证：上游 1.21.1 以 stencilFunc(ALWAYS) 画它）；
//   2. apply_fog —— 这一遍画在【光影包所有 composite/final pass 之后】，
//      雾早就在光影管线里算过了，再叠一次就是二次加雾。
//
// 除此之外与 scope_body.fsh（= vanilla entity.fsh）逐行一致：
// 顶点数据、lightmap、overlay 全都照常，只是最终颜色不再过雾。
//
// 机制随 1.21.11 邻链的 scope_ring_final.fsh 移植（她的 commit 2710c7c
// 「render reticle after Iris final composite」）；那条分支用同一招解决
// 「遮光环被光影包的后置 pass 盖掉」，本仓的同源形态是「遮光环被镜内画中画的
// 合成盖掉」—— 光影下合成跑在 LevelRenderer#render 之后，也就是画在手部之后。
//
// 顶点着色器直接复用 core/scope_body（与 vanilla entity.vsh 逐字节相同）。

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif

#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }
    faceVertexColor.a = 1.0;
#endif

    color *= faceVertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

    // 没有 apply_fog：见文件头。
    fragColor = color;
}

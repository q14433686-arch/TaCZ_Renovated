package com.tacz.guns.compat.iris;

/**
 * Injects a dormant TACZ scope-mask discard branch into Iris (or Iris-processed)
 * fragment sources. Mode 0 is a no-op; {@link IrisScopeMaskState} raises it to
 * 1 (body discard-inside) or 2 (reticle discard-outside) per draw.
 *
 * <p>Kept independent of any single Iris class: {@code ShaderCreator#link}'s
 * argument index has already moved between 1.11 builds, so every mixin that
 * can see a GLSL string should run this helper.
 */
public final class IrisScopeMaskPatch {
    private IrisScopeMaskPatch() {
    }

    public static String patchFragmentSource(String source) {
        if (source == null || source.length() < 32) {
            return source;
        }
        if (source.contains("tacz_ScopeMaskMode")) {
            return source;
        }
        if (!looksLikeFragmentShader(source) || !isModernGlsl(source)) {
            return source;
        }
        int main = source.indexOf("void main");
        if (main < 0) {
            return source;
        }
        int brace = source.indexOf('{', main);
        if (brace < 0) {
            return source;
        }

        String declarations = "\n// TACZ Iris scope mask bridge: 0=off, 1=body discard-inside, 2=reticle discard-outside\n"
                + "uniform int tacz_ScopeMaskMode;\n"
                + "uniform sampler2D tacz_ScopeMaskSampler;\n\n";

        String branch = "\n    if (tacz_ScopeMaskMode != 0) {\n"
                + "        vec2 tacz_scopeMaskUv = gl_FragCoord.xy / max(vec2(textureSize(tacz_ScopeMaskSampler, 0)), vec2(1.0));\n"
                + "        vec2 tacz_maskSample = texture(tacz_ScopeMaskSampler, tacz_scopeMaskUv).rg;\n"
                + "        bool tacz_insideScope = tacz_maskSample.r > 0.5;\n"
                + "        if (tacz_insideScope) {\n"
                + "            float tacz_progress = tacz_maskSample.g;\n"
                + "            if (tacz_progress < 0.999) {\n"
                + "                const int RINGS = 3;\n"
                + "                const int STEPS = 8;\n"
                + "                float inside = 0.0;\n"
                + "                float total = 0.0;\n"
                + "                float unit = 0.055;\n"
                + "                vec2 tacz_texSize = vec2(textureSize(tacz_ScopeMaskSampler, 0));\n"
                + "                for (int r = 1; r <= RINGS; r++) {\n"
                + "                    float radius = unit * float(r) / float(RINGS);\n"
                + "                    for (int i = 0; i < STEPS; i++) {\n"
                + "                        float a = 6.2831853 * float(i) / float(STEPS);\n"
                + "                        vec2 off = vec2(cos(a), sin(a)) * radius;\n"
                + "                        off.x *= tacz_texSize.y / max(tacz_texSize.x, 1.0);\n"
                + "                        total += 1.0;\n"
                + "                        inside += texture(tacz_ScopeMaskSampler, tacz_scopeMaskUv + off).r > 0.5 ? 1.0 : 0.0;\n"
                + "                    }\n"
                + "                }\n"
                + "                float depth = total > 0.0 ? inside / total : 1.0;\n"
                + "                if (depth < 1.0 - tacz_progress) {\n"
                + "                    tacz_insideScope = false;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "        if ((tacz_ScopeMaskMode == 1 && tacz_insideScope) || (tacz_ScopeMaskMode == 2 && !tacz_insideScope)) {\n"
                + "            discard;\n"
                + "        }\n"
                + "    }\n";

        String beforeMain = source.substring(0, main);
        String afterMain = source.substring(main);
        int newBrace = afterMain.indexOf('{');
        return beforeMain + declarations + afterMain.substring(0, newBrace + 1) + branch + afterMain.substring(newBrace + 1);
    }

    private static boolean looksLikeFragmentShader(String source) {
        if (!source.contains("void main")) {
            return false;
        }
        return source.contains("gl_FragColor")
                || source.contains("fragColor")
                || source.contains("out vec4")
                || source.contains("out_FragColor")
                || source.contains("layout(location = 0) out")
                || source.contains("layout(location=0) out");
    }

    private static boolean isModernGlsl(String source) {
        if (source.contains("texture(") || source.contains("textureSize")) {
            return true;
        }
        int version = readGlslVersion(source);
        return version >= 130;
    }

    private static int readGlslVersion(String source) {
        int idx = source.indexOf("#version");
        if (idx < 0) {
            return 0;
        }
        int i = idx + 8;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        int value = 0;
        while (i < source.length() && Character.isDigit(source.charAt(i))) {
            value = value * 10 + (source.charAt(i) - '0');
            i++;
        }
        return value;
    }
}

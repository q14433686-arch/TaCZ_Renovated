package com.tacz.guns.compat.meshloader.render;

import com.tacz.guns.compat.meshloader.config.PolyRenderPolicy;
import com.tacz.guns.compat.meshloader.core.PolyMeshModel;
import com.tacz.guns.compat.iris.IrisCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Iris 光影包开关切换检测：光影状态翻转时失效全部 {@link PolyMeshModel} 的
 * VBO 缓存（常驻 VBO 的实体顶点格式/stride 依赖当时的光影状态，切换后按
 * 新管线解读旧 buffer 会属性错位，表现为模型拉伸）。
 *
 * <p>用渲染帧开始（NeoForge {@code RenderFrameEvent.Pre}，由
 * {@code ClientGameEvents#onRenderFramePre} 驱动）逐帧比对，
 * 弱引用注册模型，模型被 GC 后自动退出。</p>
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)；
 * NeoForge 26.1.2 适配：Fabric {@code RenderTickEvent}(START) →
 * NeoForge {@code RenderFrameEvent.Pre}（同为渲染帧首、每反射一次），
 * {@code FabricLoader.isModLoaded} → {@code ModList.get().isLoaded}。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ShaderStateTracker {

    private static final boolean IRIS_LOADED =
            ModList.get().isLoaded("iris");

    private static Boolean lastShaderState = null;

    private static final Set<PolyMeshModel> registeredModels =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ShaderStateTracker() {
    }

    /** 由 {@code TaczMeshyIntegration} 调用；NeoForge 侧无独立注册点，事件在 ClientGameEvents。 */
    public static void register() {
        lastShaderState = null;
    }

    /** {@code ClientGameEvents#onRenderFramePre} 的渲染帧首回调（等价 Fabric RenderTickEvent START）。 */
    public static void onRenderFrame() {
        if (!IRIS_LOADED) {
            return;
        }
        if (registeredModels.isEmpty()) {
            return;
        }

        boolean currentState = IrisCompat.isUsingRenderPack();
        // 顺带把「现在有没有光影」交给配置层缓存：只在光影下改行为的开关（见
        // PolyRenderPolicy#illuminatedLight）每帧/每骨都要读它，不能让它们自己去反射查 Iris。
        PolyRenderPolicy.setShadersActive(currentState);

        if (lastShaderState == null) {
            lastShaderState = currentState;
            return;
        }

        if (lastShaderState != currentState) {
            lastShaderState = currentState;
            for (PolyMeshModel model : registeredModels) {
                model.invalidateVboCache();
            }
        }
    }

    public static void registerModel(PolyMeshModel model) {
        if (model != null) {
            registeredModels.add(model);
        }
    }

    public static void unregisterModel(PolyMeshModel model) {
        registeredModels.remove(model);
    }
}

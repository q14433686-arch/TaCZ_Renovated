package cn.sh1rocu.tacz.compat.meshloader.render;

import cn.sh1rocu.tacz.compat.meshloader.config.PolyRenderPolicy;
import cn.sh1rocu.tacz.compat.meshloader.core.PolyMeshModel;
import com.tacz.guns.compat.iris.IrisCompat;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Iris 光影包开关切换检测：光影状态翻转时失效全部 {@link PolyMeshModel} 的
 * VBO 缓存（常驻 VBO 的实体顶点格式/stride 依赖当时的光影状态，切换后按
 * 新管线解读旧 buffer 会属性错位，表现为模型拉伸）。
 *
 * <p>Fabric 版用 RenderTickEvent（START 相位）逐帧比对；1.21.11 NeoForge 线
 * 由本线 {@code ClientGameEvents#onRenderFramePre}（NeoForge {@code RenderFrameEvent.Pre}，
 * 语义同为帧首）调用 {@link #onClientFrameStart()}，其余逻辑逐字保留。
 * 弱引用注册模型，模型被 GC 后自动退出。</p>
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)。</p>
 */
public final class ShaderStateTracker {

    private static final boolean IRIS_LOADED =
            ModList.get().isLoaded("iris");

    private static Boolean lastShaderState = null;

    private static final Set<PolyMeshModel> registeredModels =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ShaderStateTracker() {
    }

    /** 由 {@code TaczMeshyIntegration} 调用。当前实现无事件注册（帧首回调挂在 ClientGameEvents）。 */
    public static void register() {
        // 帧首逐帧比对由 ClientGameEvents#onRenderFramePre → onClientFrameStart() 驱动。
    }

    /** 帧首回调（等价原 Fabric RenderTickEvent START 监听器本体）。 */
    public static void onClientFrameStart() {
        if (!IRIS_LOADED || registeredModels.isEmpty()) {
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

package cn.sh1rocu.tacz.compat.meshloader;

import cn.sh1rocu.tacz.compat.meshloader.core.PolyMeshSupport;
import cn.sh1rocu.tacz.compat.meshloader.model.TaczPolyMeshGunModel;
import cn.sh1rocu.tacz.compat.meshloader.render.ScreenRenderTracker;
import cn.sh1rocu.tacz.compat.meshloader.render.ShaderStateTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * TacZ Mesh Loader 整合入口。
 *
 * <p>在客户端 setup 时注册：{@code model_type: "mesh"} 枪模构造器、
 * geo 解析缓存失效监听器，以及两个状态追踪基建
 * （{@link ScreenRenderTracker} 精确 GUI 渲染瞬间、
 * {@link ShaderStateTracker} Iris 开关态切换）。</p>
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)；
 * 1.21.11 NeoForge 线把 Fabric 的 {@code ResourceManagerHelper} /
 * {@code IdentifiableResourceReloadListener} 换成原生
 * {@link PreparableReloadListener}（由本线 {@code ClientSetupEvent} 的
 * {@code AddClientReloadListenersEvent} 登记），语义不变。</p>
 */
public final class TaczMeshyIntegration {

    /** 与 Fabric 版 {@code getFabricId()} 相同的注册名，便于日志对齐。 */
    private static final Identifier RELOAD_LISTENER_ID =
            Identifier.fromNamespaceAndPath("tacz", "poly_mesh_parse_cache");

    private TaczMeshyIntegration() {
    }

    public static void onClientSetup() {
        TaczPolyMeshGunModel.register();
        // 状态追踪基建：第 0 步铺好，第 1 步 GPU 静态烘焙直接消费。
        ScreenRenderTracker.register();
        ShaderStateTracker.register();
    }

    /**
     * geo 解析缓存失效监听器（原 Fabric 版 {@code registerReloadListener} 的内容）。
     * 由本线 {@code ClientSetupEvent#onClientResourceReload} 经
     * {@code AddClientReloadListenersEvent#registerReloadListener} 登记。
     */
    public static PreparableReloadListener reloadListener() {
        return new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                                  ResourceManager manager,
                                                  Executor backgroundExecutor,
                                                  Executor gameExecutor) {
                return barrier.wait(null).thenRunAsync(PolyMeshSupport::invalidateParseCache, gameExecutor);
            }
        };
    }

    public static Identifier reloadListenerId() {
        return RELOAD_LISTENER_ID;
    }
}

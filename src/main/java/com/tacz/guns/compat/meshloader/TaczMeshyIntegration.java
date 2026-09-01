package com.tacz.guns.compat.meshloader;

import com.tacz.guns.compat.meshloader.core.PolyMeshSupport;
import com.tacz.guns.compat.meshloader.model.TaczPolyMeshGunModel;
import com.tacz.guns.compat.meshloader.render.ScreenRenderTracker;
import com.tacz.guns.compat.meshloader.render.ShaderStateTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * TacZ Mesh Loader 整合入口。
 *
 * <p>在客户端 setup 时注册：{@code model_type: "mesh"} 枪模构造器、
 * geo 解析缓存失效监听器，以及两个状态追踪基建
 * （{@link ScreenRenderTracker} 精确 GUI 渲染瞬间、
 * {@link ShaderStateTracker} Iris 开关态切换）。</p>
 *
 * <p>NeoForge 26.1.2 适配（相对 Fabric 侧）：资源重载监听器改走
 * {@code AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)}
 * （本仓 {@code ClientSetupEvent#onClientResourceReload} 的既有模式，与
 * {@code PlayerAnimatorCompat} 同款），因此这里只暴露
 * {@link #registerReloadListener(BiConsumer)}，由客户端事件接手注册；
 * 帧/GUI 状态追踪同理改为 {@code ClientGameEvents} 里的 NeoForge 事件驱动
 * （{@code RenderFrameEvent.Pre} / {@code ScreenEvent.Render.Pre/Post}）。</p>
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)。</p>
 */
public final class TaczMeshyIntegration {

    /** Client reload listener 的全局握点（本仓既有 PAL/资产监听器同款常量）。 */
    public static final Identifier MESH_PARSE_CACHE_ID =
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
     * Registers the poly-mesh parse-cache invalidation listener.
     * Pass {@code event::addListener} from
     * {@code AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)}.
     */
    public static void registerReloadListener(BiConsumer<Identifier, PreparableReloadListener> register) {
        register.accept(MESH_PARSE_CACHE_ID, new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(SharedState sharedState,
                                                  Executor backgroundExecutor,
                                                  PreparationBarrier barrier,
                                                  Executor gameExecutor) {
                return barrier.wait(null).thenRunAsync(PolyMeshSupport::invalidateParseCache, gameExecutor);
            }
        });
    }
}

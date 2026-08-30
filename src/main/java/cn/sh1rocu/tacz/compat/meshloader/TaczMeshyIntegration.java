package cn.sh1rocu.tacz.compat.meshloader;

import cn.sh1rocu.tacz.compat.meshloader.core.PolyMeshSupport;
import cn.sh1rocu.tacz.compat.meshloader.model.TaczPolyMeshGunModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * TacZ Mesh Loader 整合入口。
 *
 * <p>移植自 VellEagle/TacZMeshLoader 1.21.1_fabric (GPL-3.0)。</p>
 *
 * <p><b>移植说明</b>（相对姊妹分支 {@code arena/01a04e96} 的 {@code 8c6ad27}）：
 * 她侧用 Fabric 的 {@code ResourceManagerHelper} + {@code IdentifiableResourceReloadListener}
 * 注册解析缓存失效监听器；本仓改走 NeoForge 的
 * {@code AddClientReloadListenersEvent#addListener(Identifier, PreparableReloadListener)}
 * （与 {@code PlayerAnimatorCompat#registerReloadListener} 同一范式）。
 * 监听器的 {@code reload} 签名两边一致（本仓
 * {@code ClientAssetsManager.ClientIndexReloadListener} 可证），故方法体照搬。</p>
 */
public final class TaczMeshyIntegration {

    private TaczMeshyIntegration() {
    }

    public static void onClientSetup() {
        TaczPolyMeshGunModel.register();
    }

    /**
     * 注册「资源重载即失效 geo 解析缓存」的监听器。
     *
     * @param register {@code AddClientReloadListenersEvent::addListener}
     */
    public static void registerReloadListener(BiConsumer<Identifier, PreparableReloadListener> register) {
        register.accept(ParseCacheReloader.ID, new ParseCacheReloader());
    }

    private static final class ParseCacheReloader implements PreparableReloadListener {
        static final Identifier ID = Identifier.fromNamespaceAndPath("tacz", "poly_mesh_parse_cache");

        @Override
        public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor,
                                              PreparationBarrier barrier, Executor gameExecutor) {
            return barrier.wait(null).thenRunAsync(PolyMeshSupport::invalidateParseCache, gameExecutor);
        }
    }
}

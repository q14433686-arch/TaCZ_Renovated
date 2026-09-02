package com.tacz.guns.event;

/**
 * 【遗留空壳，勿再接线】上游 Forge 用它在专用服务端启动时加载枪包
 * （{@code DedicatedServerReloadManager.loadGunPack()}，该管理器未随本移植保留）。
 *
 * <p>本仓库 26.1.2 线的服务端枪包加载已由
 * {@code com.tacz.guns.resource.CommonAssetsManager} 通过
 * {@code AddServerReloadListenersEvent} 接管（客户端与专服共用同一套 reload 管线，
 * 专服冒烟记录见 docs/records/SERVER_TEST_20260821_DEDICATED*.md），
 * 因此本类保持无注册、无调用是<b>有意为之</b>，仅作谱系对照保留。
 */
public class CommonLoadPack {
    public static void loadGunPack() {
//        DedicatedServerReloadManager.loadGunPack();
    }
}

package com.tacz.guns.config;

import com.tacz.guns.compat.meshloader.config.MeshyConfig;
import com.tacz.guns.config.client.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec spec = init();

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        KeyConfig.init(builder);
        RenderConfig.init(builder);
        ResourceConfig.init(builder);
        SoundConfig.init(builder);
        ZoomConfig.init(builder);
        // 内置 TacZ Mesh Loader（poly_mesh / GPU 静态烘焙）的 18 项客户端配置，
        // 挂在本 spec 的 mesh_loader 段下；与 RenderClothConfig / 语言键三方齐平
        // （Fabric 侧有 docs/check_mesh_config_parity.py 自查，NeoForge 侧见移植记录）。
        MeshyConfig.init(builder);
        return builder.build();
    }
}

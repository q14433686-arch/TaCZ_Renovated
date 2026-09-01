package com.tacz.guns.config;

import com.tacz.guns.config.client.*;
import cn.sh1rocu.tacz.compat.meshloader.config.MeshyConfig;
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
        // 内置 TacZ Mesh Loader（poly_mesh）的客户端选项。
        MeshyConfig.init(builder);
        return builder.build();
    }
}

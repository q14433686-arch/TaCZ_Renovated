package com.tacz.guns.client.gui.compat;

import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * In-game config landing page matching NeoForge's native {@link ConfigurationScreen}
 * (the same widget style Carry On uses on 26.1.2).
 *
 * <p>Only Client and Common are listed. Those two are always editable in-game.
 * Server configs are world-locked / remote-disabled, and Fabric/upstream Cloth
 * never exposed them on the T-key screen either. Startup ({@code tacz-pre.toml})
 * is a restart-time pack flag, not an in-game gameplay option.</p>
 */
public class TaczConfigHomeScreen extends Screen {
    private final Screen lastScreen;

    public TaczConfigHomeScreen(Screen lastScreen) {
        super(Component.translatable("tacz.configuration.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 6 + 12;
        y = addConfigButton(y,
                "tacz.configuration.section.tacz.client.toml",
                ModConfig.Type.CLIENT,
                ClientConfig.spec);
        y = addConfigButton(y,
                "tacz.configuration.section.tacz.common.toml",
                ModConfig.Type.COMMON,
                CommonConfig.spec);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private int addConfigButton(int y, String translationKey, ModConfig.Type type, ModConfigSpec spec) {
        Component name = Component.translatable(translationKey);
        this.addRenderableWidget(Button.builder(name, button ->
                        this.minecraft.setScreen(new ConfigurationScreen.ConfigurationSectionScreen(
                                this, type, spec, name)))
                .bounds(this.width / 2 - 100, y, 200, 20)
                .build());
        return y + 24;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(gui, mouseX, mouseY, partialTick);
        gui.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}

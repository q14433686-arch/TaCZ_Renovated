package com.tacz.guns.client.input;

import net.neoforged.neoforge.client.event.InputEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.client.gui.compat.ClothConfigScreen;
import com.tacz.guns.compat.cloth.MenuIntegration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.net.URI;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

public class ConfigKey {
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key.tacz.open_config.desc",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            TaCZKeyCategory.TACZ);

    public static void onOpenConfig(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS
                && OPEN_CONFIG_KEY.matches(new net.minecraft.client.input.KeyEvent(event.getKey(), event.getScanCode(), event.getModifiers()))) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            var configScreen = MenuIntegration.getConfigScreen(minecraft.screen);
            if (configScreen != null) {
                // NeoForge 26.1.2 has a native ConfigurationScreen for the ModConfigSpec
                // instances registered by GunMod. Open it directly instead of routing the
                // key through the optional Fabric/Cloth Config integration.
                minecraft.setScreen(configScreen);
            } else {
                // This is only a defensive fallback for a client that somehow reaches the
                // key handler before the mod container has been initialized.
                ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(ClothConfigScreen.CLOTH_CONFIG_URL));
                HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("gui.tacz.cloth_config_warning.download"));
                MutableComponent component = Component.translatable("gui.tacz.cloth_config_warning.tips").withStyle(style ->
                        style.withColor(0x5555FF).withUnderlined(true).withClickEvent(clickEvent).withHoverEvent(hoverEvent));
                player.sendSystemMessage(component);
            }
        }
    }
}

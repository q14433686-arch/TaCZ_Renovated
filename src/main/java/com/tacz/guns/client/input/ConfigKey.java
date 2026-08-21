package com.tacz.guns.client.input;

import net.neoforged.neoforge.client.event.InputEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.GunMod;
import com.tacz.guns.compat.cloth.MenuIntegration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

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
            ModList.get().getModContainerById(GunMod.MOD_ID)
                    .ifPresent(container -> minecraft.setScreen(MenuIntegration.getConfigScreen(container, minecraft.screen)));
        }
    }
}

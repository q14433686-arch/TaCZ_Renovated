package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.config.PreLoadConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OverwriteCommand {
    private static final String OVERWRITE_NAME = "overwrite";
    private static final String ENABLE = "enable";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(OVERWRITE_NAME);
        RequiredArgumentBuilder<CommandSourceStack, Boolean> enable = Commands.argument(ENABLE, BoolArgumentType.bool());
        reload.then(enable.executes(OverwriteCommand::setOverwrite));
        return reload;
    }

    private static int setOverwrite(CommandContext<CommandSourceStack> context) {
        boolean enable = BoolArgumentType.getBool(context, ENABLE);
        PreLoadConfig.override.set(!enable);
        // 这条命令绕过了 Cloth 面板的 savingRunnable，而 DefaultPackDebug 是 tacz-pre.toml
        // 里的键：不显式落盘的话，命令行改完重启就回默认（姊妹 1.21.11 线 2026-09-01 的
        // cd14a2a 修了同一个病根的另一条入口；本线原生 NeoForge ModConfigSpec#save 即
        // loadedConfig.save()，无需那边自建的 ConfigPersist 那套）。
        PreLoadConfig.spec.save();
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            if (PreLoadConfig.override.get()) {
                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_off"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_on"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}

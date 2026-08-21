package me.xjqsh.lrtactical.client.network;

import me.xjqsh.lrtactical.init.ModCapabilities;
import me.xjqsh.lrtactical.network.ServerMessageCustomCooldown;
import me.xjqsh.lrtactical.network.ServerMessageSyncLrPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * LR 的 S2C 客户端落地处理（仅经 {@code LrClientBridge} 反射调用）。
 * 本类是 LR 包内唯一允许引用 {@code net.minecraft.client} 的网络处理类。
 */
public final class LrClientPacketHandlers {
    private LrClientPacketHandlers() {
    }

    /** 索引同步：单机（memory connection）跳过——本地已有服务端数据（refab 同语义）。 */
    public static void onSyncLrPack(ServerMessageSyncLrPack message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean remoteConnection = player.connection.getConnection() != null
                && !player.connection.getConnection().isMemoryConnection();
        if (!remoteConnection) {
            return;
        }
        message.applyToLocalIndexes();
    }

    /** 分类冷却：只落到本地玩家的客户端冷却表（非权威，仅驱动遮罩渲染）。 */
    public static void onCustomCooldown(ServerMessageCustomCooldown message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var cooldowns = ModCapabilities.coolDowns(player);
        if (message.duration() <= 0) {
            cooldowns.removeCooldown(message.id());
        } else {
            cooldowns.addCooldown(message.id(), message.duration());
        }
    }
}

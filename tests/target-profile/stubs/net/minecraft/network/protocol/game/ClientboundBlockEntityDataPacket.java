package net.minecraft.network.protocol.game;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class ClientboundBlockEntityDataPacket implements Packet<ClientGamePacketListener> {
    public static ClientboundBlockEntityDataPacket create(BlockEntity entity) { return new ClientboundBlockEntityDataPacket(); }
}

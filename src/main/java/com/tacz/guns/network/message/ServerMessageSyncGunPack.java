package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.network.GunPackSync;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枪包数据同步（服务端 → 客户端），**1.2.0 的压缩分片协议**。
 *
 * <p>1.1.8 的写法是把整份缓存塞进一条 payload 的 {@code StreamCodec.composite}；
 * 枪包一多，单包超过网络层上限 ⇒ 进服失败。现在拆成
 * 「一条 payload = 一个 {@link GunPackSync.Part}」：
 * <ul>
 *   <li>服务端在资源重载完成后只压缩一次（{@link #createMessages}），
 *       所有玩家复用同一组不可变分片；</li>
 *   <li>客户端按 {@link GunPackSync.Receiver} 收齐后一次性建索引，
 *       避免在半份数据上重建。</li>
 * </ul>
 *
 * <p>26.2 接线要点（与上游 1.20.1 版的差异，逐条有依据）：
 * <ul>
 *   <li>沿用本仓既有的 dedicated-safe 反射桥 {@code ClientPacketBridge}，
 *       {@code Minecraft}/{@code ClientIndexManager} 不进入本类常量池
 *       （{@code AGENTS.md} §3.4 双端安全）；上游直接把 {@code doSync} 写成
 *       {@code @OnlyIn(Dist.CLIENT)} 静态方法，在 26.x 不能当类加载保护用；</li>
 *   <li>连接身份校验放在客户端侧：{@code IPayloadContext#connection()}
 *       （② NeoForge {@code 26.2.0} {@code IPayloadContext:50}）与
 *       {@code Minecraft#getConnection()} 持有的 {@code ClientPacketListener#getConnection()}
 *       必须是同一个实例，且仍处于连接状态 —— 否则是上一局的迟到分片，直接丢弃。</li>
 * </ul>
 */
public class ServerMessageSyncGunPack implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSyncGunPack> TYPE = new CustomPacketPayload.Type<>(
        Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "server_sync_gun_pack")
    );
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncGunPack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ServerMessageSyncGunPack decode(FriendlyByteBuf buffer) {
            return new ServerMessageSyncGunPack(new GunPackSync.Part(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(GunPackSync.CHUNK_SIZE)));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, ServerMessageSyncGunPack message) {
            GunPackSync.Part part = message.part;
            buffer.writeUUID(part.transferId());
            buffer.writeVarInt(part.index());
            buffer.writeVarInt(part.compressedSize());
            buffer.writeVarInt(part.uncompressedSize());
            buffer.writeByteArray(part.data());
        }
    };
    private static final GunPackSync.Receiver RECEIVER = new GunPackSync.Receiver();

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final GunPackSync.Part part;

    /**
     * 客户端解码路径用。
     */
    public ServerMessageSyncGunPack(GunPackSync.Part part) {
        this.part = part;
    }

    /**
     * 资源重载后仅调用一次，所有玩家复用同一组压缩分片。
     *
     * <p>编码格式必须与 1.1.8 的单包格式逐字节一致（先写种类数，再逐种类写
     * {@code writeEnum} + 条目数 + 逐条目 {@code writeIdentifier} + {@code writeUtf}），
     * 这样客户端 {@link #readCache} 只需要换成带校验的读法。
     */
    public static List<ServerMessageSyncGunPack> createMessages(Map<DataType, Map<Identifier, String>> cache) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(256, GunPackSync.MAX_SYNC_SIZE));
        try {
            buffer.writeMap(cache, FriendlyByteBuf::writeEnum, (dataBuffer, map) ->
                    dataBuffer.writeMap(map, FriendlyByteBuf::writeIdentifier,
                            (jsonBuffer, json) -> jsonBuffer.writeUtf(json, GunPackSync.MAX_SYNC_SIZE)));
            byte[] uncompressed = new byte[buffer.readableBytes()];
            buffer.readBytes(uncompressed);
            List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(uncompressed);
            GunMod.LOGGER.info("Prepared gun pack sync: {} bytes -> {} compressed bytes, {} part(s)",
                    uncompressed.length, parts.get(0).compressedSize(), parts.size());
            return parts.stream().map(ServerMessageSyncGunPack::new).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare gun pack sync", e);
        } finally {
            buffer.release();
        }
    }

    public static void handle(ServerMessageSyncGunPack message, IPayloadContext context) {
        net.minecraft.network.Connection connection = context.connection();
        context.enqueueWork(() -> com.tacz.guns.network.ClientPacketBridge.invoke(
                "onSyncGunPackPart",
                new Class[]{com.tacz.guns.network.message.ServerMessageSyncGunPack.class,
                        net.minecraft.network.Connection.class},
                message, connection));
    }

    /**
     * 客户端收下一片，收齐则返回完整缓存；未收齐返回 {@code null}。
     * 分片自洽性错误以 {@link IllegalArgumentException} 抛出，由调用方断开连接。
     */
    @OnlyIn(Dist.CLIENT)
    public static Map<DataType, Map<Identifier, String>> acceptPart(GunPackSync.Part part) throws IOException {
        byte[] uncompressed = RECEIVER.accept(part);
        if (uncompressed == null) {
            return null;
        }
        return readCache(uncompressed);
    }

    /** 连接切换 / 登出时丢弃未完成的事务。 */
    @OnlyIn(Dist.CLIENT)
    public static void clearPendingSync() {
        RECEIVER.clear();
    }

    public GunPackSync.Part getPart() {
        return part;
    }

    /**
     * 解压后缓存的读取端。逐项校验种类数、条目数、重复 ID、尾部残字节 ——
     * 任何一条不符都视为损坏快照，抛 {@link DecoderException}。
     */
    public static Map<DataType, Map<Identifier, String>> readCache(byte[] uncompressed) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(uncompressed));
        try {
            int types = buffer.readVarInt();
            if (types < 0 || types > DataType.values().length) {
                throw new DecoderException("Invalid gun pack sync data type count: " + types);
            }
            Map<DataType, Map<Identifier, String>> cache = new EnumMap<>(DataType.class);
            for (int i = 0; i < types; i++) {
                DataType type = buffer.readEnum(DataType.class);
                int entries = buffer.readVarInt();
                if (entries < 0 || entries > buffer.readableBytes() / 2) {
                    throw new DecoderException("Invalid gun pack sync entry count: " + entries);
                }
                Map<Identifier, String> data = new HashMap<>();
                for (int j = 0; j < entries; j++) {
                    Identifier id = buffer.readIdentifier();
                    if (data.put(id, buffer.readUtf(GunPackSync.MAX_SYNC_SIZE)) != null) {
                        throw new DecoderException("Duplicate gun pack sync resource: " + id);
                    }
                }
                if (cache.put(type, data) != null) {
                    throw new DecoderException("Duplicate gun pack sync data type: " + type);
                }
            }
            if (buffer.isReadable()) {
                throw new DecoderException("Trailing gun pack sync data");
            }
            return cache;
        } finally {
            buffer.release();
        }
    }
}

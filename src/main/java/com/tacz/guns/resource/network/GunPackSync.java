package com.tacz.guns.resource.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 枪包同步快照的压缩 / 分片协议（服务端与客户端共用，**不含任何 Minecraft 依赖**）。
 *
 * <p>背景：1.1.8 线把整份 {@code Map<DataType, Map<Id, String>>} 塞进单条 payload。
 * 枪包一多，单包超过网络层的上限，进服直接失败。1.2.0 改为
 * 「GZIP 压缩 → 256 KiB 分片 → UUID 事务」，服务端每次资源重载只压缩一次，
 * 所有玩家复用同一组分片。
 *
 * <p>移植依据：上游 {@code 1.2.0-forge-mc1.20.1} / {@code 1.2.0-neoforge-1.21.1} 的
 * {@code com.tacz.guns.resource.network.GunPackSync}（两平台字节相同，纯 JDK），
 * 本类按 26.2 原样落地 —— 它不碰渲染、不碰注册表，也不碰任何加载器 API。
 *
 * <p>协议不变量（客户端 {@link Receiver} 逐条校验，异常即断开连接）：
 * <ul>
 *   <li>同一时刻只接收一个事务；{@code index == 0} 开启新事务并丢弃未完成的旧事务；</li>
 *   <li>分片必须同 {@code transferId}、{@code index} 严格递增（TCP 保证有序）；</li>
 *   <li>声明长度必须自洽：{@code compressedSize} / {@code uncompressedSize} 都在
 *       {@code (0, }{@link #MAX_SYNC_SIZE}{@code ]}，且分片数与 index 范围匹配；</li>
 *   <li>解压时多读一个字节，用来发现「声明长度小于真实长度」的放大攻击。</li>
 * </ul>
 */
public final class GunPackSync {
    /** 单片最大字节数。 */
    public static final int CHUNK_SIZE = 256 * 1024;
    /** 压缩前后各自的上限。 */
    public static final int MAX_SYNC_SIZE = 64 * 1024 * 1024;

    private GunPackSync() {
    }

    public static List<Part> compressAndSplit(byte[] uncompressed) throws IOException {
        checkSize(uncompressed.length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(uncompressed);
        }
        byte[] compressed = output.toByteArray();
        checkSize(compressed.length);

        UUID transferId = UUID.randomUUID();
        List<Part> parts = new ArrayList<>();
        for (int offset = 0; offset < compressed.length; offset += CHUNK_SIZE) {
            parts.add(new Part(transferId, offset / CHUNK_SIZE, compressed.length, uncompressed.length,
                    Arrays.copyOfRange(compressed, offset, Math.min(offset + CHUNK_SIZE, compressed.length))));
        }
        return List.copyOf(parts);
    }

    private static byte[] decompress(byte[] compressed, int uncompressedSize) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            // 多读取一个字节以检查长度声明，避免无上限地解压数据。
            byte[] uncompressed = gzip.readNBytes(uncompressedSize + 1);
            if (uncompressed.length != uncompressedSize) {
                throw new IOException("Gun pack sync decompressed size does not match " + uncompressedSize);
            }
            return uncompressed;
        }
    }

    private static void checkSize(int size) {
        if (size <= 0 || size > MAX_SYNC_SIZE) {
            throw new IllegalArgumentException(
                    "Gun pack sync size must be between 1 and " + MAX_SYNC_SIZE + " bytes: " + size);
        }
    }

    public record Part(UUID transferId, int index, int compressedSize, int uncompressedSize, byte[] data) {
        public Part {
            Objects.requireNonNull(transferId);
            Objects.requireNonNull(data);
            checkSize(compressedSize);
            checkSize(uncompressedSize);
            int partCount = (compressedSize + CHUNK_SIZE - 1) / CHUNK_SIZE;
            if (index < 0 || index >= partCount) {
                throw new IllegalArgumentException("Invalid gun pack sync part index: " + index);
            }
            int expectedSize = Math.min(CHUNK_SIZE, compressedSize - index * CHUNK_SIZE);
            if (data.length != expectedSize) {
                throw new IllegalArgumentException("Invalid gun pack sync part size: " + data.length);
            }
        }
    }

    /** 同时只接收一个同步快照；连接切换时需要清理。 */
    public static final class Receiver {
        private UUID transferId;
        private byte[] compressed;
        private int uncompressedSize;
        private int nextIndex;

        /**
         * TCP 保证分片有序，只有收到最后一片才返回解压后的完整数据。
         *
         * @return 收齐时返回未压缩数据，否则 {@code null}
         */
        public byte[] accept(Part part) throws IOException {
            if (part.index() == 0) {
                clear();
                transferId = part.transferId();
                compressed = new byte[part.compressedSize()];
                uncompressedSize = part.uncompressedSize();
            }
            if (!part.transferId().equals(transferId) || part.index() != nextIndex
                    || compressed.length != part.compressedSize()
                    || uncompressedSize != part.uncompressedSize()) {
                clear();
                throw new IllegalArgumentException("Inconsistent or out-of-order gun pack sync part");
            }
            int offset = part.index() * CHUNK_SIZE;
            System.arraycopy(part.data(), 0, compressed, offset, part.data().length);
            nextIndex++;
            if (offset + part.data().length < compressed.length) {
                return null;
            }

            byte[] complete = compressed;
            int completeSize = uncompressedSize;
            clear();
            return decompress(complete, completeSize);
        }

        public void clear() {
            transferId = null;
            compressed = null;
            uncompressedSize = 0;
            nextIndex = 0;
        }

        /** 是否有未收完的事务（诊断用）。 */
        public boolean isPending() {
            return transferId != null;
        }
    }
}

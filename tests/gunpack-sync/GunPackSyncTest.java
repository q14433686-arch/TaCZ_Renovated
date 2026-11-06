package com.tacz.guns.resource.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Exercises the real {@link GunPackSync} protocol against a stand-in for the
 * {@code Map<DataType, Map<Identifier, String>>} cache. No Minecraft, Netty or
 * Gson types are involved — this is the transport layer only, not the codec.
 */
public final class GunPackSyncTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        smallSnapshotRoundTrips();
        largeSnapshotSplitsIntoOrderedParts();
        emptySnapshotIsRejected();
        receiverRejectsOutOfOrderParts();
        receiverRejectsForeignTransfer();
        receiverRestartsOnIndexZero();
        partRejectsInconsistentDeclarations();
        receiverRejectsInflatedLengthClaim();
        foreignGzipPartsAreAccepted();
        System.out.println("GunPackSyncTest: passed (" + checks + " checks)");
    }

    /** 一整个快照要能被切分、按序重装、并逐字节还原。 */
    private static void smallSnapshotRoundTrips() throws Exception {
        byte[] payload = buildPayload(3, 2_000);
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        byte[] result = feedAll(receiver, GunPackSync.compressAndSplit(payload));
        require(result != null, "small snapshot is delivered");
        require(Arrays.equals(payload, result), "small snapshot round-trips byte for byte");
        require(!receiver.isPending(), "receiver is idle after a completed transfer");
    }

    /** 超过 CHUNK_SIZE 的快照必须切成多片，且 index 从 0 连续递增。 */
    private static void largeSnapshotSplitsIntoOrderedParts() throws Exception {
        // 随机数据几乎不可压缩，能稳定地跨越多个分片。
        byte[] payload = randomNoise(3 * GunPackSync.CHUNK_SIZE + 1234);
        List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(payload);
        require(parts.size() >= 4, "large snapshot splits into several parts, got " + parts.size());
        UUID transferId = parts.get(0).transferId();
        for (int i = 0; i < parts.size(); i++) {
            GunPackSync.Part part = parts.get(i);
            require(part.index() == i, "part index is contiguous at " + i);
            require(part.transferId().equals(transferId), "all parts share one transfer id");
            require(part.uncompressedSize() == payload.length, "all parts declare the same uncompressed size");
            require(part.compressedSize() == parts.get(0).compressedSize(), "all parts declare the same compressed size");
            int expectedLength = Math.min(GunPackSync.CHUNK_SIZE,
                    part.compressedSize() - i * GunPackSync.CHUNK_SIZE);
            require(part.data().length == expectedLength, "part " + i + " has the expected slice length");
        }
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        byte[] result = feedAll(receiver, parts);
        require(result != null, "large snapshot is delivered");
        require(Arrays.equals(payload, result), "large snapshot round-trips byte for byte");
    }

    private static void emptySnapshotIsRejected() {
        expectThrows(() -> GunPackSync.compressAndSplit(new byte[0]), "empty snapshot is not sendable");
        expectThrows(() -> GunPackSync.compressAndSplit(new byte[GunPackSync.MAX_SYNC_SIZE + 1]),
                "oversized snapshot is not sendable");
    }

    /** 乱序 / 缺片：第 2 片先到时事务必须被作废，而不是把错位数据拼起来。 */
    private static void receiverRejectsOutOfOrderParts() throws Exception {
        List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(randomNoise(GunPackSync.CHUNK_SIZE * 2));
        require(parts.size() >= 3, "test fixture spans at least three parts");
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        require(receiver.accept(parts.get(0)) == null, "first part never completes a transfer");
        expectThrows(() -> receiver.accept(parts.get(2)), "skipping a part is rejected");
        require(!receiver.isPending(), "receiver cleared itself after the rejection");
    }

    private static void receiverRejectsForeignTransfer() throws Exception {
        List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(randomNoise(GunPackSync.CHUNK_SIZE * 2));
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        require(receiver.accept(parts.get(0)) == null, "first part accepted");
        GunPackSync.Part foreign = new GunPackSync.Part(UUID.randomUUID(), parts.get(1).index(),
                parts.get(1).compressedSize(), parts.get(1).uncompressedSize(), parts.get(1).data());
        expectThrows(() -> receiver.accept(foreign), "a part from another transfer is rejected");
        require(!receiver.isPending(), "receiver cleared itself after the rejection");
    }

    /** 新的 index 0 表示新事务：未完成的旧事务直接丢弃。 */
    private static void receiverRestartsOnIndexZero() throws Exception {
        byte[] first = buildPayload(2, 512);
        byte[] second = buildPayload(4, 32_000);
        List<GunPackSync.Part> firstParts = GunPackSync.compressAndSplit(first);
        List<GunPackSync.Part> secondParts = GunPackSync.compressAndSplit(second);
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        require(receiver.accept(firstParts.get(0)) == null, "abandoned transfer is accepted");
        byte[] result = feedAll(receiver, secondParts);
        require(result != null && Arrays.equals(second, result), "the restarted transfer wins");
    }

    /** Part 自身要在构造期拒绝自相矛盾的声明（分片数、index 范围、切片长度）。 */
    private static void partRejectsInconsistentDeclarations() throws Exception {
        UUID id = UUID.randomUUID();
        expectThrows(() -> new GunPackSync.Part(id, 5, 100, 100, new byte[100]),
                "index beyond the declared part count is rejected");
        expectThrows(() -> new GunPackSync.Part(id, 0, 100, 100, new byte[99]),
                "part whose data length disagrees with the slice is rejected");
        expectThrows(() -> new GunPackSync.Part(id, -1, 100, 100, new byte[100]),
                "negative index is rejected");
        expectThrows(() -> new GunPackSync.Part(id, 0, 0, 100, new byte[0]),
                "zero compressed size is rejected");
        expectThrows(() -> new GunPackSync.Part(id, 0, 100, GunPackSync.MAX_SYNC_SIZE + 1, new byte[100]),
                "uncompressed size above the cap is rejected");
    }

    /** 声明长度小于真实解压长度时必须失败（防解压放大），不能静默截断。 */
    private static void receiverRejectsInflatedLengthClaim() throws Exception {
        byte[] payload = buildPayload(2, 4096);
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(deflated)) {
            gzip.write(payload);
        }
        byte[] compressed = deflated.toByteArray();
        GunPackSync.Part lying = new GunPackSync.Part(UUID.randomUUID(), 0, compressed.length,
                payload.length - 10, compressed);
        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        expectThrows(() -> receiver.accept(lying), "a snapshot that decompresses longer than declared is rejected");
        require(!receiver.isPending(), "receiver cleared itself after the rejection");
    }

    /**
     * 互操作：分片由**外部实现**（Python {@code gzip.compress}）生成，本类的
     * {@link GunPackSync.Receiver} 必须原样还原，且长度与摘要逐字节对上。
     *
     * <p>这条用例覆盖的是「协议本身」而不是「我们的压缩器」：即使本机没有 javac/JDK，
     * 夹具也能由 {@code scripts/gen_gunpack_sync_fixture.py} 重新生成，因此 CI 上的
     * 通过意味着 26.2 的接收端能读别处产出的快照。
     */
    private static void foreignGzipPartsAreAccepted() throws Exception {
        java.nio.file.Path fixture = java.nio.file.Path.of("tests/gunpack-sync/fixtures/python-gzip.parts");
        if (!java.nio.file.Files.isRegularFile(fixture)) {
            throw new AssertionError("missing fixture: " + fixture.toAbsolutePath());
        }
        String expectedDigest = null;
        long expectedSize = -1;
        List<GunPackSync.Part> parts = new ArrayList<>();
        for (String line : java.nio.file.Files.readAllLines(fixture)) {
            if (line.startsWith("# payload-sha256:")) {
                expectedDigest = line.substring(line.indexOf(':') + 1).trim();
                continue;
            }
            if (line.startsWith("# payload-size:")) {
                expectedSize = Long.parseLong(line.substring(line.indexOf(':') + 1).trim());
                continue;
            }
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            require(f.length == 5, "fixture line has five fields");
            parts.add(new GunPackSync.Part(
                    UUID.fromString(f[0]),
                    Integer.parseInt(f[1]),
                    Integer.parseInt(f[2]),
                    Integer.parseInt(f[3]),
                    java.util.Base64.getDecoder().decode(f[4])));
        }
        require(expectedDigest != null && expectedSize > 0, "fixture carries its payload digest and size");
        require(parts.size() >= 2, "fixture spans more than one part so reassembly is exercised");

        GunPackSync.Receiver receiver = new GunPackSync.Receiver();
        byte[] payload = feedAll(receiver, parts);
        require(payload != null, "foreign parts are delivered");
        require(payload.length == expectedSize, "foreign payload has the declared size");
        require(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(payload)).equals(expectedDigest), "foreign payload matches its SHA-256");
    }

    // ---------------------------------------------------------------- helpers

    private static byte[] feedAll(GunPackSync.Receiver receiver, List<GunPackSync.Part> parts) throws IOException {
        byte[] result = null;
        for (GunPackSync.Part part : parts) {
            byte[] accepted = receiver.accept(part);
            if (accepted != null) {
                require(result == null, "only the final part completes the transfer");
                result = accepted;
            }
        }
        return result;
    }

    /** 有重复苗头的压缩友好载荷：靠近 JSON 的形状，便于检出顺序与边界问题。 */
    private static byte[] buildPayload(int types, int entriesPerType) {
        StringBuilder builder = new StringBuilder();
        for (int type = 0; type < types; type++) {
            for (int entry = 0; entry < entriesPerType; entry++) {
                builder.append("{\"t\":").append(type).append(",\"e\":").append(entry)
                        .append(",\"name\":\"tacz:gun_").append(entry).append("\"}\n");
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] randomNoise(int size) {
        byte[] bytes = new byte[size];
        new Random(42L).nextBytes(bytes);
        return bytes;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectThrows(ThrowingRunnable action, String message) {
        checks++;
        try {
            action.run();
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("expected a failure: " + message);
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private GunPackSyncTest() {
    }
}

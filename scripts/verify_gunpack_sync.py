#!/usr/bin/env python3
"""静态守卫：枪包同步协议（1.2.0 压缩分片）的接线是否完整。

为什么需要它：这条链路跨 5 个文件 + 一处反射桥，任何一处改名都不会让编译失败，
但会让「进服时枪包数据没同步」这种故障在运行期静默发生：

    CommonAssetsManager（生产分片）
        → ServerMessageSyncGunPack（编解码 + 反射桥调用）
            → ClientPacketHandlers#onSyncGunPackPart（客户端收片、建索引）
        → CommonNetworkCacheEvent（登出丢弃未完成事务）

本脚本只做源码文本层面的接线核对，不替代 `gunPackSyncTest`（协议行为）与
`build`（编译）；不启动游戏、不连网。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/tacz/guns"

FAILURES: list[str] = []
CHECKS = 0


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check(condition: bool, message: str) -> None:
    global CHECKS
    CHECKS += 1
    if not condition:
        FAILURES.append(message)


def main() -> int:
    protocol = SRC / "resource/network/GunPackSync.java"
    message = SRC / "network/message/ServerMessageSyncGunPack.java"
    handlers = SRC / "client/network/ClientPacketHandlers.java"
    assets = SRC / "resource/CommonAssetsManager.java"
    login_out = SRC / "client/event/CommonNetworkCacheEvent.java"
    network = SRC / "network/NetworkHandler.java"

    for path in (protocol, message, handlers, assets, login_out, network):
        check(path.exists(), f"缺少文件：{path.relative_to(ROOT)}")
    if FAILURES:
        return report()

    protocol_src = read(protocol)
    message_src = read(message)
    handlers_src = read(handlers)
    assets_src = read(assets)
    login_out_src = read(login_out)
    network_src = read(network)

    # --- 1. 协议层必须保持「纯 JDK」：这是它能被独立单元测试、且不引入
    #        双端类加载问题的前提（AGENTS.md §3.4）。
    for banned in ("net.minecraft", "net.neoforged", "io.netty", "com.google.gson"):
        check(
            banned not in protocol_src,
            f"GunPackSync.java 不应依赖 {banned}（协议层必须是纯 JDK）",
        )
    for symbol in ("CHUNK_SIZE", "MAX_SYNC_SIZE", "compressAndSplit", "class Receiver"):
        check(symbol in protocol_src, f"GunPackSync.java 缺少协议要件 {symbol}")
    check(
        "readNBytes(uncompressedSize + 1)" in protocol_src,
        "GunPackSync.Receiver#decompress 必须多读一个字节以拒绝「声明长度偏小」的快照",
    )

    # --- 2. 消息体：只携带分片，不再整包编码整份缓存。
    check(
        "ByteBufCodecs.map" not in message_src,
        "ServerMessageSyncGunPack 不应再整包编码整份缓存（1.1.8 的写法会让大枪包进不了服）",
    )
    for symbol in ("GunPackSync.Part", "createMessages", "readCache", "acceptPart", "clearPendingSync"):
        check(symbol in message_src, f"ServerMessageSyncGunPack 缺少 {symbol}")
    check(
        "context.connection()" in message_src,
        "ServerMessageSyncGunPack#handle 必须把 IPayloadContext#connection() 传给客户端处理器",
    )

    # --- 3. 反射桥：调用点的方法名与参数类型必须和 ClientPacketHandlers 的签名逐字一致。
    #     这是最容易静默失效的一处（编译期完全查不出来）。
    bridge = re.search(
        r'ClientPacketBridge\.invoke\(\s*"(\w+)",\s*new Class\[\]\{([^}]*)\}',
        message_src,
        re.S,
    )
    check(bridge is not None, "ServerMessageSyncGunPack 未通过 ClientPacketBridge 调用客户端处理器")
    if bridge:
        method = bridge.group(1)
        raw_types = [t.strip() for t in bridge.group(2).split(",") if t.strip()]
        # "com.tacz.guns.network.message.ServerMessageSyncGunPack.class" -> "ServerMessageSyncGunPack"
        types = [t.removesuffix(".class").rsplit(".", 1)[-1] for t in raw_types]
        expected = [
            "ServerMessageSyncGunPack",
            "Connection",
        ]
        check(
            types == expected,
            f"反射桥参数类型应为 {expected}，实际是 {types}",
        )
        # 客户端签名：参数类型顺序一致，且方法必须是 public static
        signature = re.search(
            r"public static void " + re.escape(method) + r"\(([^)]*)\)",
            handlers_src,
        )
        check(signature is not None, f"ClientPacketHandlers 缺少 public static void {method}(...)")
        if signature:
            params = signature.group(1)
            check(
                re.search(r"ServerMessageSyncGunPack\s+\w+", params) is not None,
                f"{method} 的第一个参数必须是 ServerMessageSyncGunPack",
            )
            check(
                re.search(r"Connection\s+\w+", params) is not None,
                f"{method} 的第二个参数必须是 Connection（与反射桥类型数组对应）",
            )
        # ClientPacketBridge 是 dedicated-safe 的：处理器类本身不能被本类直接引用。
        check(
            "com.tacz.guns.client.network.ClientPacketHandlers" not in message_src,
            "ServerMessageSyncGunPack 不能直接引用客户端处理器类（会破坏 dedicated 类加载安全）",
        )

    # --- 4. 客户端收片顺序：先校验连接身份，再落地缓存。
    check(
        "listener.getConnection() != connection" in handlers_src,
        "客户端收片必须先校验分片所属连接与当前连接是同一实例（丢弃上一局的迟到分片）",
    )
    check(
        "acceptPart" in handlers_src,
        "客户端必须通过 ServerMessageSyncGunPack#acceptPart 收片",
    )
    check(
        "clearInstance()" in handlers_src and "isMemoryConnection()" in handlers_src,
        "非内存连接收到完整快照后必须清掉本地 CommonAssetsManager 实例",
    )

    # --- 5. 服务端：重载尾端预压缩一次，DatapackSync 只负责发送。
    check(
        "networkSyncMessages = ServerMessageSyncGunPack.createMessages(getNetworkCache())" in assets_src,
        "CommonAssetsManager 必须在资源重载尾端一次性生成分片",
    )
    check(
        re.search(r"onDatapackSync\(OnDatapackSyncEvent event\)", assets_src) is not None,
        "CommonAssetsManager 必须保留 OnDatapackSyncEvent 发送入口",
    )
    check(
        assets_src.count("networkSyncMessages") >= 4,
        "onDatapackSync 应当复用预压缩的 networkSyncMessages，而不是每个玩家重新编码",
    )
    check(
        "event.sendRecipes(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get())" in assets_src,
        "配方内容请求不能随协议改造一起丢掉（JEI/REI 的原生同步屏障依赖它）",
    )

    # --- 6. 登出丢弃未完成事务。
    check(
        "ServerMessageSyncGunPack.clearPendingSync()" in login_out_src,
        "CommonNetworkCacheEvent 必须在登出时丢弃未完成的枪包同步事务",
    )

    # --- 7. 协议版本必须已抬升：旧客户端的单包格式与新分片格式不兼容。
    version = re.search(r'VERSION\s*=\s*"([^"]+)"', network_src)
    check(version is not None, "NetworkHandler 缺少 VERSION 常量")
    if version:
        check(
            version.group(1) != "1.0.5",
            "改了枪包同步协议就必须抬 NetworkHandler.VERSION，否则旧客户端不会被握手术拒绝",
        )

    return report()


def report() -> int:
    if FAILURES:
        for failure in FAILURES:
            print(f"::error::{failure}")
        print(f"verify_gunpack_sync: FAILED ({len(FAILURES)}/{CHECKS} checks)")
        return 1
    print(f"verify_gunpack_sync: OK ({CHECKS} checks)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""生成枪包同步协议的「外部实现」夹具。

为什么用 Python 生成：`gunPackSyncTest` 要证明的是**协议**可互操作 —— 26.2 的接收端
能读别处产出的快照。如果夹具由我们自己的 `GunPackSync.compressAndSplit` 生成，
那么「压缩器与解压器同时写错」这种 bug 会被两家一起掩盖。改用 Python 的
`gzip.compress`（独立实现，同 GZIP 规范）产分片，接收端就必须真的按规范解。

输出：`tests/gunpack-sync/fixtures/python-gzip.parts`（文本，一行一片，可 review）。
内容为「可读 JSON 前缀 + 不可压缩噪声」，保证压缩后仍跨过 256 KiB 的分片边界。

复现：`python3 scripts/gen_gunpack_sync_fixture.py`
"""

from __future__ import annotations

import base64
import gzip
import hashlib
import random
import sys
import uuid
from pathlib import Path

CHUNK_SIZE = 256 * 1024
OUT = Path(__file__).resolve().parent.parent / "tests/gunpack-sync/fixtures/python-gzip.parts"


def build_payload() -> bytes:
    prefix = "".join(
        '{"type":%d,"idx":%d,"ns":"tacz"}\n' % (i % 11, i) for i in range(4000)
    ).encode("utf-8")
    # 固定种子：夹具必须可重现，否则每次生成都会改动仓库。
    # 注意 Random 实例只能建一次 —— 每轮新建会产出同一个字节，噪声就不再是不可压缩的。
    rng = random.Random(1234)
    noise = bytes(rng.getrandbits(8) for _ in range(300 * 1024))
    return prefix + noise


def main() -> int:
    payload = build_payload()
    compressed = gzip.compress(payload, mtime=0)
    parts = [compressed[i:i + CHUNK_SIZE] for i in range(0, len(compressed), CHUNK_SIZE)]
    transfer_id = uuid.uuid5(uuid.NAMESPACE_URL, "tacz:gunpack-sync-fixture")
    lines = [
        "# python-gzip parts fixture: one part per line, tab separated",
        "# fields: transferId  index  compressedSize  uncompressedSize  base64(data)",
        "# payload-sha256: %s" % hashlib.sha256(payload).hexdigest(),
        "# payload-size: %d" % len(payload),
        "# compressed-size: %d" % len(compressed),
    ]
    for index, data in enumerate(parts):
        lines.append("\t".join([
            str(transfer_id), str(index), str(len(compressed)), str(len(payload)),
            base64.b64encode(data).decode("ascii"),
        ]))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("wrote %s (%d parts, %d compressed bytes, %d payload bytes)"
          % (OUT.relative_to(OUT.parent.parent.parent.parent), len(parts), len(compressed), len(payload)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

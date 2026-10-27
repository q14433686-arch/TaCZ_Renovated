#!/usr/bin/env python3
"""Static guards for 1.21.11 workbench loot and Carry On wiring.

Adapted from Refabricated 1.21.11 PR #120. This NeoForge baseline had both a
manual companion-half drop and an early, silent companion removal. Neither can
be removed in isolation: loot must own the drop while neighbor updates remove
the other half. Carry On redirects to the root and removes it with updates.
These checks do not execute Minecraft or prove the runtime drop counts.

Use --strict to fail CI on a finding; without it, the script only reports.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BLOCK_DIR = ROOT / "src/main/java/com/tacz/guns/block"
LOOT_DIR = ROOT / "src/main/resources/data/tacz/loot_table/blocks"

# 双方块：loot 条件属性名 -> 必须是根半边的值
DOUBLE_BLOCK_LOOT = {
    "gun_smith_table.json": ("part", "foot"),
    "workbench_b.json": ("part", "foot"),
    "workbench_c.json": ("half", "lower"),
    "statue.json": ("half", "lower"),
    "target.json": ("half", "lower"),
}
# 需要从方块实体拷 BlockId 的物品（枪包工作台）
BLOCK_ID_LOOT = {"workbench_a.json", "workbench_b.json", "workbench_c.json"}

errors: list[str] = []
notes: list[str] = []
# 每一项各自的失败数，用来在汇总里分别打印 OK / FAIL
failed: dict[str, int] = {"A": 0, "B": 0, "C": 0, "D": 0, "E": 0}


def fail(item: str, message: str) -> None:
    failed[item] += 1
    errors.append(f"[{item}] {message}")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def method_body(text: str, header: str) -> str | None:
    """粗略取出以 header 开头的方法体（大括号配平）。"""
    idx = text.find(header)
    if idx < 0:
        return None
    start = text.find("{", idx)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
    return None


def check_no_manual_drops() -> None:
    """A. 方块类里不得手工 popResource。"""
    offenders = []
    for path in sorted(BLOCK_DIR.rglob("*.java")):
        if "entity" in path.relative_to(BLOCK_DIR).parts:
            continue
        text = strip_comments(path.read_text(encoding="utf-8"))
        for lineno, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if re.search(r"\bpopResource\s*\(", stripped):
                offenders.append(f"{path.relative_to(ROOT)}:{lineno}: {stripped}")
    if offenders:
        fail(
            "A",
            "方块类里出现了手工掉落调用（掉落应只由战利品表负责）：\n    " + "\n    ".join(offenders),
        )


def iter_conditions(node, out: list[dict]) -> None:
    """收集 JSON 里所有 dict（旧 schema 的 condition 对象也在其中）。"""
    if isinstance(node, dict):
        out.append(node)
        for value in node.values():
            iter_conditions(value, out)
    elif isinstance(node, list):
        for value in node:
            iter_conditions(value, out)


def check_loot_tables() -> None:
    """B. 六份战利品表的 schema 与根半边条件。"""
    expected = set(DOUBLE_BLOCK_LOOT) | {"workbench_a.json"}
    present = {p.name for p in LOOT_DIR.glob("*.json")}
    missing = expected - present
    if missing:
        fail("B", f"缺少战利品表：{sorted(missing)}")

    for name in sorted(present & expected):
        path = LOOT_DIR / name
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            fail("B", f"{name}: JSON 解析失败：{exc}")
            continue

        raw = path.read_text(encoding="utf-8")
        # 26.3 的新 schema 键（condition/modifier/match_block）在本线会被静默忽略
        # （RecordCodecBuilder 忽略未知键 ⇒ 产出条件与 copy_custom_data 全丢）。
        if re.search(r'"(modifier|random_sequence|match_block)"', raw):
            fail(
                "B",
                f"{name}: 出现了 26.3 的战利品表 schema 键（modifier/random_sequence/match_block）。"
                " 本线仍用 conditions/functions/block_state_property，混用的后果是条件与函数被静默丢弃。",
            )

        dicts: list[dict] = []
        iter_conditions(data, dicts)

        if name in DOUBLE_BLOCK_LOOT:
            prop, root_value = DOUBLE_BLOCK_LOOT[name]
            block_id = "tacz:" + name.removesuffix(".json")
            entries = [(pool, entry) for pool in data.get("pools", [])
                       for entry in pool.get("entries", []) if entry.get("name") == block_id]
            if not entries:
                fail("B", f"{name}: 找不到 {block_id} 的产出条目")
            for pool, entry in entries:
                conditions = pool.get("conditions", []) + entry.get("conditions", [])
                if not any(condition.get("condition") == "minecraft:block_state_property"
                           and condition.get("block") == block_id
                           and condition.get("properties", {}).get(prop) == root_value
                           for condition in conditions):
                    fail("B", f"{name}: {block_id} 产出条目缺少 {prop}={root_value} 根半边条件")

        if name in BLOCK_ID_LOOT:
            has_copy = any(
                node.get("function") == "minecraft:copy_custom_data"
                and node.get("source") == "block_entity"
                and any(op.get("source") == "BlockId" and op.get("target") == "BlockId"
                        for op in node.get("ops", []))
                for node in dicts
            )
            if not has_copy:
                fail("B", f"{name}: 缺少 block_entity 的 BlockId → BlockId 拷贝")


def check_is_root_matches_loot() -> None:
    """C. Java 的 isRoot 必须指向 loot 条件里的根半边。"""
    expectations = [
        (BLOCK_DIR / "GunSmithTableBlockB.java", r"Part\.FOOT", "GunSmithTableBlockB"),
        (BLOCK_DIR / "GunSmithTableBlockC.java", r"TableHalf\.LOWER", "GunSmithTableBlockC"),
    ]
    for path, needle, label in expectations:
        text = path.read_text(encoding="utf-8")
        match = re.search(r"public boolean isRoot\(BlockState blockState\)\s*\{(.*?)\n    \}", text, re.S)
        if not match:
            fail("C", f"{label}: 找不到 isRoot(...) 方法体，无法核对根半边")
            continue
        if not re.search(needle, match.group(1)):
            fail("C", f"{label}: isRoot() 不再是 {needle}，与战利品表的根半边条件不一致")


def check_creative_suppression_direction() -> None:
    """D. 创造模式的抑制判据：守卫挂在非根半边，被静默拿掉的是根半边。"""
    cases = [
        # (文件, 守卫 `if (var == Enum.X)`, 邻居核对 `getValue(PROP) == Enum.Y)`, loot 里的根半边值, 标签)
        (
            BLOCK_DIR / "GunSmithTableBlockB.java",
            r"if\s*\(\s*bedPart\s*==\s*BedPart\.(\w+)\s*\)",
            r"getValue\(\s*PART\s*\)\s*==\s*BedPart\.(\w+)\s*\)",
            DOUBLE_BLOCK_LOOT["workbench_b.json"][1],
            "GunSmithTableBlockB",
        ),
        (
            BLOCK_DIR / "GunSmithTableBlockC.java",
            r"if\s*\(\s*half\s*==\s*TableHalf\.(\w+)\s*\)",
            r"getValue\(\s*HALF\s*\)\s*==\s*TableHalf\.(\w+)\s*\)",
            DOUBLE_BLOCK_LOOT["workbench_c.json"][1],
            "GunSmithTableBlockC",
        ),
    ]
    for path, guard_re, neighbour_re, root_value, label in cases:
        text = strip_comments(path.read_text(encoding="utf-8"))
        body = method_body(text, "BlockState playerWillDestroy(")
        if body is None:
            fail("D", f"{label}: 找不到 playerWillDestroy(...)，无法核对创造模式抑制判据")
            continue
        guard = re.search(guard_re, body)
        neighbour = re.search(neighbour_re, body)
        if not guard or not neighbour:
            fail("D", f"{label}: playerWillDestroy 里认不出「守卫 / 邻居核对」写法，请同步更新本脚本的正则")
            continue
        for required in ("!level.isClientSide()", "player.isCreative()", "level.setBlock(",
                         "Blocks.AIR.defaultBlockState()", "Block.UPDATE_SUPPRESS_DROPS"):
            if required not in body:
                fail("D", f"{label}: 创造模式抑制路径缺少 {required}")
        guard_part = guard.group(1).lower()
        neighbour_part = neighbour.group(1).lower()
        if guard_part == root_value:
            fail(
                "D",
                f"{label}: 创造模式抑制的守卫挂在根半边（{guard_part}）上 —— 判据写反了。"
                f"会产出的是根半边（loot 条件 {root_value}），守卫应挂在非根半边上，"
                "否则依靠邻接更新的生命周期不能正确抑制根半边的掉落",
            )
        if neighbour_part != root_value:
            fail(
                "D",
                f"{label}: 创造模式下被静默拿掉的邻居是 {neighbour_part}，应是根半边 {root_value}"
                "（要抑制的是会走战利品表产出的那一半）",
            )


def check_companion_lifecycle() -> None:
    """E. Loot needs neighbor updates; Carry On must pick up and restore the root."""
    base = strip_comments((BLOCK_DIR / "AbstractGunSmithTableBlock.java").read_text(encoding="utf-8"))
    if re.search(r"\b(?:void|BlockState)\s+affectNeighborsAfterRemoval\s*\(", base):
        fail("E", "基类仍提前删除另一半，可能跳过根半边的战利品表")
    placement = method_body(base, "void onPlace(") or ""
    for required in ("isRoot(state)", "getCompanionPos(", "getCompanionState(", "level.setBlock("):
        if required not in placement:
            fail("E", f"工作台根方块放置时缺少 {required}")
    for label in ("GunSmithTableBlockB", "GunSmithTableBlockC"):
        source = strip_comments((BLOCK_DIR / f"{label}.java").read_text(encoding="utf-8"))
        shape = method_body(source, "BlockState updateShape(") or ""
        if "facingState.is(this)" not in shape or "Blocks.AIR.defaultBlockState()" not in shape:
            fail("E", f"{label}: 无法确认伴随方块移除时的邻接更新")
        if label == "GunSmithTableBlockC" and (
            not re.search(r'EnumProperty\.create\(\s*"half"\s*,\s*TableHalf\.class\s*\)', source)
            or not re.search(r"enum\s+TableHalf\s+implements\s+StringRepresentable", source)
        ):
            fail("E", "C 型需要自有 TableHalf 值类型，否则 Carry On 会按原版双格方块拒绝")
    pickup_path = ROOT / "src/main/java/com/tacz/guns/mixin/carryon/CarryOnPickupHandlerMixin.java"
    place_path = ROOT / "src/main/java/com/tacz/guns/mixin/carryon/CarryOnPlacementHandlerMixin.java"
    if not pickup_path.is_file() or not place_path.is_file():
        fail("E", "缺少 Carry On 拾取或放置 mixin")
        return
    pickup = strip_comments(pickup_path.read_text(encoding="utf-8"))
    place = strip_comments(place_path.read_text(encoding="utf-8"))
    for required in ("@ModifyVariable", '"tryPickUpBlock"', "!table.isRoot(state)", "table.getRootPos(pos, state)"):
        if required not in pickup:
            fail("E", f"Carry On 拾取缺少根方块重定向: {required}")
    for required in ("getCarriedBlock(player)", "table.getCompanionPos(", "table.getCompanionState(", "denyPlacement("):
        if required not in place:
            fail("E", f"Carry On 放置缺少完整工作台预检查: {required}")


def main() -> int:
    parser = argparse.ArgumentParser(description="校验方块掉落接线（离线静态检查）")
    parser.add_argument("--strict", action="store_true", help="有问题时退出 1")
    args = parser.parse_args()

    if not BLOCK_DIR.is_dir():
        print(f"[ERR] 找不到 {BLOCK_DIR}", file=sys.stderr)
        return 1 if args.strict else 0

    check_no_manual_drops()
    check_loot_tables()
    check_is_root_matches_loot()
    check_creative_suppression_direction()
    check_companion_lifecycle()

    def status(item: str) -> str:
        return "FAIL" if failed[item] else "OK"

    print("掉落接线检查：")
    print(f"  A. 方块类无手工 popResource —— {status('A')}")
    print(f"  B. 六份战利品表 schema + 根半边条件 —— {status('B')}")
    print(f"  C. isRoot() 与 loot 根半边一致 —— {status('C')}")
    print(f"  D. 创造模式抑制判据挂在非根半边、拿掉根半边 —— {status('D')}")
    print(f"  E. 邻接更新与 Carry On 根方块接线 —— {status('E')}")
    for note in notes:
        print(f"  · {note}")
    for error in errors:
        print(f"[FAIL] {error}")

    if not errors:
        print("全部通过。注意：这只是静态检查，不替代编译与实机掉落测试。")
        return 0
    return 1 if args.strict else 0


if __name__ == "__main__":
    raise SystemExit(main())

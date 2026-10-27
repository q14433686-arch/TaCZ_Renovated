#!/usr/bin/env python3
"""Static guards for 26.3 multi-block loot and Carry On lifecycle wiring.

The checks understand the 26.3 loot-table schema. They verify source wiring, not Minecraft's
runtime drop count; use the in-game matrix in docs/records/SISTER_SYNC_263_20261027.md as the
runtime acceptance checklist.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BLOCK_DIR = Path("src/main/java/com/tacz/guns/block")
LOOT_DIR = Path("src/main/resources/data/tacz/loot_table/blocks")
PICKUP_MIXIN = Path("src/main/java/com/tacz/guns/mixin/carryon/CarryOnPickupHandlerMixin.java")
PLACEMENT_MIXIN = Path("src/main/java/com/tacz/guns/mixin/carryon/CarryOnPlacementHandlerMixin.java")

ROOT_STATES = {
    "gun_smith_table": ("part", "foot"),
    "workbench_b": ("part", "foot"),
    "workbench_c": ("half", "lower"),
    "statue": ("half", "lower"),
    "target": ("half", "lower"),
}
WORKBENCHES = {"workbench_a", "workbench_b", "workbench_c"}


def java_code(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)


def method_body(text: str, name: str) -> str:
    for match in re.finditer(r"\b" + re.escape(name) + r"\s*\(", text):
        open_paren = text.find("(", match.start())
        depth = 0
        close_paren = -1
        for pos in range(open_paren, len(text)):
            if text[pos] == "(":
                depth += 1
            elif text[pos] == ")":
                depth -= 1
                if depth == 0:
                    close_paren = pos
                    break
        if close_paren < 0:
            continue
        next_pos = close_paren + 1
        while next_pos < len(text) and text[next_pos].isspace():
            next_pos += 1
        if next_pos >= len(text) or text[next_pos] != "{":
            continue
        start = next_pos + 1
        depth = 1
        for pos in range(start, len(text)):
            if text[pos] == "{":
                depth += 1
            elif text[pos] == "}":
                depth -= 1
                if depth == 0:
                    return text[start:pos]
    return ""


def walk(node):
    if isinstance(node, dict):
        yield node
        for child in node.values():
            yield from walk(child)
    elif isinstance(node, list):
        for child in node:
            yield from walk(child)


def check(root: Path) -> list[str]:
    errors: list[str] = []
    block_dir = root / BLOCK_DIR
    loot_dir = root / LOOT_DIR
    if not block_dir.is_dir() or not loot_dir.is_dir():
        return ["missing block source or loot-table directory"]

    # Drops must come from loot tables, never an extra manual clone in a Block class.
    for path in sorted(block_dir.glob("*.java")):
        if re.search(r"\bpopResource\s*\(", java_code(path)):
            errors.append(f"{path.name}: block class manually spawns an item drop")

    expected_tables = set(ROOT_STATES) | WORKBENCHES
    for name in sorted(expected_tables):
        path = loot_dir / f"{name}.json"
        try:
            table = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{path.name}: cannot read loot table: {exc}")
            continue

        nodes = list(walk(table))
        if any({"conditions", "functions", "function"} & node.keys() for node in nodes):
            errors.append(f"{path.name}: contains 26.2 loot schema keys")
        if any(node.get("condition") == "minecraft:block_state_property" for node in nodes):
            errors.append(f"{path.name}: contains the 26.2 block_state_property condition")

        entries = [
            entry
            for pool in table.get("pools", [])
            for entry in pool.get("entries", [])
            if entry.get("type") == "minecraft:item" and entry.get("name") == f"tacz:{name}"
        ]
        if len(entries) != 1:
            errors.append(f"{path.name}: expected exactly one {name} item entry, found {len(entries)}")
            continue

        entry = entries[0]
        if name in ROOT_STATES:
            prop, value = ROOT_STATES[name]
            condition = entry.get("condition", {})
            if not isinstance(condition, dict) or (
                condition.get("type") != "minecraft:match_block"
                or condition.get("blocks") != f"tacz:{name}"
                or condition.get("state", {}).get(prop) != value
            ):
                errors.append(f"{path.name}: item entry must be limited to {prop}={value} using match_block")

        if name in WORKBENCHES:
            modifier = entry.get("modifier", {})
            ops = modifier.get("ops", []) if isinstance(modifier, dict) else []
            if not isinstance(modifier, dict) or (
                modifier.get("type") != "minecraft:copy_custom_data"
                or modifier.get("source") != "block_entity"
                or not any(op.get("source") == "BlockId" and op.get("target") == "BlockId" for op in ops)
            ):
                errors.append(f"{path.name}: missing BlockId copy_custom_data on its item entry")

    for filename, root_part, broken_part, companion_part in (
        ("GunSmithTableBlockB.java", "BedPart.FOOT", "BedPart.HEAD", "BedPart.FOOT"),
        ("GunSmithTableBlockC.java", "TableHalf.LOWER", "TableHalf.UPPER", "TableHalf.LOWER"),
    ):
        path = block_dir / filename
        try:
            code = java_code(path)
        except OSError as exc:
            errors.append(f"{filename}: {exc}")
            continue

        if root_part not in method_body(code, "isRoot"):
            errors.append(f"{filename}: isRoot no longer matches its root loot state ({root_part})")

        destroy = method_body(code, "playerWillDestroy")
        if not all(part in destroy for part in (
            "player.isCreative()", broken_part, companion_part, "Block.UPDATE_SUPPRESS_DROPS"
        )):
            errors.append(f"{filename}: missing creative-mode companion drop suppression")
        if not re.search(r"\b(?:bedPart|half)\s*==\s*" + re.escape(broken_part), destroy):
            errors.append(f"{filename}: creative suppression must trigger when breaking {broken_part}")
        if not re.search(r"blockstate\.getValue\([^)]*\)\s*==\s*" + re.escape(companion_part), destroy):
            errors.append(f"{filename}: creative suppression must remove the root half {companion_part}")

        shape = method_body(code, "updateShape")
        if "facingState.is(this)" not in shape or "Blocks.AIR.defaultBlockState()" not in shape:
            errors.append(f"{filename}: neighbor updates no longer remove a broken companion half")

    abstract = java_code(block_dir / "AbstractGunSmithTableBlock.java")
    if re.search(r"\b(?:void|BlockState)\s+affectNeighborsAfterRemoval\s*\(", abstract):
        errors.append("AbstractGunSmithTableBlock still clears the companion before neighbor loot runs")
    placement = method_body(abstract, "onPlace")
    for required in ("isRoot(state)", "getCompanionPos(", "getCompanionState(", "level.setBlock("):
        if required not in placement:
            errors.append(f"AbstractGunSmithTableBlock#onPlace is missing {required}")

    table_c = java_code(block_dir / "GunSmithTableBlockC.java")
    if not re.search(r'EnumProperty\.create\(\s*"half"\s*,\s*TableHalf\.class\s*\)', table_c) or not re.search(
        r"enum\s+TableHalf\s+implements\s+StringRepresentable", table_c
    ):
        errors.append("C-type table must retain its custom TableHalf type for Carry On compatibility")

    try:
        pickup = java_code(root / PICKUP_MIXIN)
        placement_mixin = java_code(root / PLACEMENT_MIXIN)
    except OSError as exc:
        errors.append(f"Carry On mixin source missing: {exc}")
    else:
        if not all(part in pickup for part in ("!table.isRoot(state)", "table.getRootPos(pos, state)")):
            errors.append("CarryOnPickupHandlerMixin must redirect the companion half to the root")
        if not all(part in placement_mixin for part in (
            "getCompanionPos(", "getCompanionState(", "level.isUnobstructed(", "denyPlacement(player, cir)"
        )):
            errors.append("CarryOnPlacementHandlerMixin must preflight the full table before placement")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict", action="store_true", help="return nonzero on a regression")
    parser.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    args = parser.parse_args()
    errors = check(args.root)
    for error in errors:
        print(f"[FAIL] {error}")
    if not errors:
        print("26.3 multi-block loot wiring: OK (static checks only; runtime testing still required)")
    return int(bool(errors) and args.strict)


if __name__ == "__main__":
    raise SystemExit(main())

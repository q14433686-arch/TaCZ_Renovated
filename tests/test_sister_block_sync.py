#!/usr/bin/env python3
"""Offline model of the two-half removal order (shared by the 1.21.11 and 26.2 lines);
not a Minecraft test.

The accompanying static guard checks that the actual Java/loot files use the
assumptions exercised here. The model follows the vanilla Level#setBlock /
Block#updateOrDestroy and NeoForge player-destruction order documented in the
sync audit. It deliberately does not model loot randomness, Carry On permission
checks, successful mixin application, or every mod hook. In particular, a
modeled pickup is not proof that a given mod configuration allows that pickup.
"""

import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
LOOT = ROOT / "src/main/resources/data/tacz/loot_table/blocks"


class TwoHalfTable:
    def __init__(self, loot_name, property_name, root, companion, creative_guard=None):
        self.loot = json.loads((LOOT / f"{loot_name}.json").read_text(encoding="utf-8"))
        self.block_id = f"tacz:{loot_name}"
        self.property_name = property_name
        self.root = root
        self.companion = companion
        self.creative_guard = creative_guard or companion
        self.parts = {root, companion}
        self.identity = "custom_pack:workbench"
        self.drops = []

    def loot_part(self, part, entity_id):
        for pool in self.loot["pools"]:
            for entry in pool["entries"]:
                if entry.get("name") != self.block_id:
                    continue
                conditions = pool.get("conditions", []) + entry.get("conditions", [])
                if any(c.get("condition") == "minecraft:block_state_property"
                       and c.get("properties", {}).get(self.property_name) != part
                       for c in conditions):
                    continue
                copies_id = any(f.get("function") == "minecraft:copy_custom_data"
                                and any(op.get("source") == "BlockId" and op.get("target") == "BlockId"
                                        for op in f.get("ops", []))
                                for f in pool.get("functions", []) + entry.get("functions", []))
                self.drops.append((self.block_id, entity_id if copies_id else None))

    def remove(self, part, drop=False, block_drops=True):
        if part not in self.parts:
            return False
        if drop and block_drops:
            self.loot_part(part, self.identity if part == self.root else None)
        self.parts.remove(part)
        other = self.companion if part == self.root else self.root
        if other in self.parts:
            # Neighbor shape update destroys the remaining half with drops enabled;
            # Level#setBlock clears UPDATE_SUPPRESS_DROPS before propagation.
            self.remove(other, drop=True, block_drops=block_drops)
        return True

    def break_as_player(self, part, creative, block_drops=True):
        # Capture the root block entity before the player removal callback.
        captured_identity = self.identity if part == self.root else None
        if creative and part == self.creative_guard:
            other = self.root if part == self.companion else self.companion
            self.remove(other, drop=False, block_drops=block_drops)
        removed = self.remove(part, drop=False, block_drops=block_drops)
        if removed and not creative and block_drops:
            self.loot_part(part, captured_identity)

    def carry_pickup(self, clicked):
        # Assume pickup is permitted and the mixin redirects to the root. Under
        # those assumptions the mod removes the saved BE before removeBlock.
        # This deliberately does not simulate Carry On's eligibility checks.
        assert clicked in self.parts
        saved_identity = self.identity
        self.identity = None
        self.remove(self.root)
        return saved_identity


class SisterBlockLifecycleTests(unittest.TestCase):
    CASES = (
        ("gun_smith_table", "part", "foot", "head", False),
        ("workbench_b", "part", "foot", "head", True),
        ("workbench_c", "half", "lower", "upper", True),
    )

    def test_survival_both_halves_one_drop_with_identity(self):
        for name, prop, root, companion, copies_id in self.CASES:
            for clicked in (root, companion):
                with self.subTest(table=name, clicked=clicked):
                    table = TwoHalfTable(name, prop, root, companion)
                    table.break_as_player(clicked, creative=False)
                    self.assertEqual(set(), table.parts)
                    self.assertEqual([(f"tacz:{name}", "custom_pack:workbench" if copies_id else None)], table.drops)

    def test_creative_both_halves_no_drop(self):
        for name, prop, root, companion, _ in self.CASES:
            for clicked in (root, companion):
                with self.subTest(table=name, clicked=clicked):
                    table = TwoHalfTable(name, prop, root, companion)
                    table.break_as_player(clicked, creative=True)
                    self.assertEqual(set(), table.parts)
                    self.assertEqual([], table.drops)

    def test_reversed_creative_guard_would_leak(self):
        for name, prop, root, companion, _ in self.CASES:
            for clicked in (root, companion):
                with self.subTest(table=name, clicked=clicked):
                    table = TwoHalfTable(name, prop, root, companion, creative_guard=root)
                    table.break_as_player(clicked, creative=True)
                    self.assertEqual(1, len(table.drops))

    def test_abstract_carry_both_halves_keeps_identity_without_drops(self):
        for name, prop, root, companion, _ in self.CASES:
            for clicked in (root, companion):
                with self.subTest(table=name, clicked=clicked):
                    table = TwoHalfTable(name, prop, root, companion)
                    carried = table.carry_pickup(clicked)
                    self.assertEqual("custom_pack:workbench", carried)
                    self.assertEqual(set(), table.parts)
                    self.assertEqual([], table.drops)

    def test_do_block_drops_false(self):
        for name, prop, root, companion, _ in self.CASES:
            for clicked in (root, companion):
                with self.subTest(table=name, clicked=clicked):
                    table = TwoHalfTable(name, prop, root, companion)
                    table.break_as_player(clicked, creative=False, block_drops=False)
                    self.assertEqual(set(), table.parts)
                    self.assertEqual([], table.drops)


if __name__ == "__main__":
    unittest.main(verbosity=2)

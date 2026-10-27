#!/usr/bin/env python3
"""Regression-test the offline guards against deliberately broken copies of the source."""

from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class GuardMutationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        guns = "src/main/java/com/tacz/guns"
        for directory in (f"{guns}/block", f"{guns}/mixin/carryon",
                          "src/main/resources/data/tacz/loot_table/blocks"):
            shutil.copytree(ROOT / directory, self.root / directory)
        for name in (
            f"{guns}/util/TargetOwnerProfiles.java",
            f"{guns}/entity/TargetMinecart.java",
            f"{guns}/client/renderer/other/TargetOwnerSkins.java",
            f"{guns}/client/renderer/block/TargetRenderer.java",
            f"{guns}/client/renderer/entity/TargetMinecartRenderer.java",
            "scripts/verify_block_drops.py",
            "scripts/verify_block_entity_persistence.py",
        ):
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, dest)
        self.addCleanup(self.temp.cleanup)

    def run_guard(self, script):
        return subprocess.run([sys.executable, str(self.root / "scripts" / script), "--strict"],
                              text=True, capture_output=True, check=False)

    def mutate(self, relative, old, new):
        path = self.root / relative
        text = path.read_text(encoding="utf-8")
        self.assertIn(old, text)
        path.write_text(text.replace(old, new, 1), encoding="utf-8")

    def test_clean_fixture_passes(self):
        for script in ("verify_block_drops.py", "verify_block_entity_persistence.py"):
            result = self.run_guard(script)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_inverted_creative_guard_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/GunSmithTableBlockB.java",
                    "if (bedPart == BedPart.HEAD)", "if (bedPart == BedPart.FOOT)")
        result = self.run_guard("verify_block_drops.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[D]", result.stdout)

    def test_early_companion_removal_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/AbstractGunSmithTableBlock.java",
                    "public abstract boolean isRoot(",
                    "protected void affectNeighborsAfterRemoval() {}\n    public abstract boolean isRoot(")
        result = self.run_guard("verify_block_drops.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[E]", result.stdout)

    def test_carry_on_rejecting_c_half_type_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/GunSmithTableBlockC.java",
                    'EnumProperty.create("half", TableHalf.class)',
                    'EnumProperty.create("half", DoubleBlockHalf.class)')
        result = self.run_guard("verify_block_drops.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[E]", result.stdout)

    def test_block_id_copy_regression_is_detected(self):
        self.mutate("src/main/resources/data/tacz/loot_table/blocks/workbench_b.json",
                    '"target": "BlockId"', '"target": "wrong"')
        result = self.run_guard("verify_block_drops.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("BlockId", result.stdout)

    def test_missing_statue_removal_hook_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/entity/StatueBlockEntity.java",
                    "public void preRemoveSideEffects(", "public void noLongerCalled(")
        result = self.run_guard("verify_block_entity_persistence.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[C]", result.stdout)

    def test_null_profile_and_missing_neoforge_parent_are_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/TargetBlock.java",
                    "e.setOwner(TargetOwnerProfiles.fromName(stack.getHoverName().getString()));",
                    "new GameProfile(null, stack.getHoverName().getString());")
        self.mutate("src/main/java/com/tacz/guns/block/entity/TargetBlockEntity.java",
                    "super.saveAdditional(output);", "")
        result = self.run_guard("verify_block_entity_persistence.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[E]", result.stdout)
        self.assertIn("[F]", result.stdout)

    def test_target_placement_without_refresh_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/block/entity/TargetBlockEntity.java",
                    "        this.refresh();", "")
        result = self.run_guard("verify_block_entity_persistence.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[G]", result.stdout)

    def test_skin_lookup_regression_is_detected(self):
        self.mutate("src/main/java/com/tacz/guns/client/renderer/other/TargetOwnerSkins.java",
                    "playerSkinRenderCache().getOrDefault(profile)", "getSkinManager()")
        result = self.run_guard("verify_block_entity_persistence.py")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[G]", result.stdout)


if __name__ == "__main__":
    unittest.main(verbosity=2)

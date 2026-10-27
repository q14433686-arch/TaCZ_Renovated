#!/usr/bin/env python3
"""Mutation regressions proving the static block, persistence, recipe, and pickup guards."""

from __future__ import annotations

import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
ROOT = SCRIPTS_DIR.parent
sys.path.insert(0, str(SCRIPTS_DIR))

from verify_block_drops import check as check_block_drops  # noqa: E402
from verify_block_entity_persistence import check as check_block_entities  # noqa: E402
from verify_workbench_recipes import check as check_workbench_recipes  # noqa: E402

COPY_PATHS = (
    Path("src/main/java/com/tacz/guns/block"),
    Path("src/main/java/com/tacz/guns/entity"),
    Path("src/main/java/com/tacz/guns/client/renderer/block"),
    Path("src/main/java/com/tacz/guns/client/renderer/entity"),
    Path("src/main/java/com/tacz/guns/mixin/carryon"),
    Path("src/main/java/com/tacz/guns/resource/pojo/data/recipe"),
    Path("src/main/java/com/tacz/guns/resource/manager"),
    Path("src/main/java/com/tacz/guns/resource/network"),
    Path("src/main/java/com/tacz/guns/crafting"),
    Path("src/main/java/com/tacz/guns/api"),
    Path("src/main/java/com/tacz/guns/client/gui"),
    Path("src/main/java/com/tacz/guns/inventory"),
    Path("src/main/java/com/tacz/guns/compat/jei"),
    Path("src/main/java/com/tacz/guns/compat/rei"),
    Path("src/main/resources/data/tacz/loot_table/blocks"),
)


class BlockGuardMutationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory(prefix="tacz-block-guards-")
        self.root = Path(self.tmp.name)
        for relative in COPY_PATHS:
            source = ROOT / relative
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(source, target, dirs_exist_ok=True)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def assert_guard_rejects(self, check, expected: str) -> None:
        errors = check(self.root)
        self.assertTrue(any(expected in error for error in errors), "\n".join(errors))

    def test_current_sources_pass_all_static_suites(self) -> None:
        self.assertEqual([], check_block_drops(self.root))
        self.assertEqual([], check_block_entities(self.root))
        self.assertEqual([], check_workbench_recipes(self.root))

    def test_old_loot_schema_key_is_rejected(self) -> None:
        path = self.root / "src/main/resources/data/tacz/loot_table/blocks/workbench_a.json"
        table = json.loads(path.read_text(encoding="utf-8"))
        table["pools"][0]["entries"][0]["conditions"] = []
        path.write_text(json.dumps(table), encoding="utf-8")
        self.assert_guard_rejects(check_block_drops, "contains 26.2 loot schema keys")

    def test_wrong_creative_half_guard_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/block/GunSmithTableBlockB.java"
        text = path.read_text(encoding="utf-8")
        needle = "if (bedPart == BedPart.HEAD)"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "if (bedPart == BedPart.FOOT)", 1), encoding="utf-8")
        self.assert_guard_rejects(check_block_drops, "creative suppression must trigger")

    def test_missing_neoforge_parent_persistence_call_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/block/entity/StatueBlockEntity.java"
        text = path.read_text(encoding="utf-8")
        needle = "super.saveAdditional(output);"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "", 1), encoding="utf-8")
        self.assert_guard_rejects(check_block_entities, "StatueBlockEntity: saveAdditional must call the NeoForge parent")

    def test_legacy_owner_uuid_array_migration_is_required(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/block/entity/TargetBlockEntity.java"
        text = path.read_text(encoding="utf-8")
        needle = 'int[] id = legacy.getIntArray("Id").orElse(null);'
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "int[] id = null;", 1), encoding="utf-8")
        self.assert_guard_rejects(check_block_entities, "legacy flat keys and Owner compound migration are incomplete")

    def test_stale_target_profile_callback_guard_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/block/entity/TargetBlockEntity.java"
        text = path.read_text(encoding="utf-8")
        needle = "&& this.profileLookupGeneration == generation"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "", 1), encoding="utf-8")
        self.assert_guard_rejects(check_block_entities, "stale profile lookup guard")

    def test_synchronous_minecart_skin_lookup_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/client/renderer/entity/TargetMinecartRenderer.java"
        text = path.read_text(encoding="utf-8")
        needle = "this.skinCache = ctx.getPlayerSkinRenderCache();"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "", 1), encoding="utf-8")
        self.assert_guard_rejects(check_block_entities, "TargetMinecartRenderer: resolve skin from the cache")

    def test_missing_recipe_material_validation_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/resource/pojo/data/recipe/TableRecipe.java"
        text = path.read_text(encoding="utf-8")
        needle = "if (recipe.materials == null) {"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "if (recipe.materials.isEmpty()) {", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "TableRecipe#validationError must reject")

    def test_explicit_empty_material_list_remains_valid(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/resource/pojo/data/recipe/TableRecipe.java"
        text = path.read_text(encoding="utf-8")
        needle = "        return null;"
        self.assertIn(needle, text)
        injected = '        if (recipe.materials.isEmpty()) { return "materials is empty"; }\n' + needle
        path.write_text(text.replace(needle, injected, 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "must preserve explicitly empty materials lists")

    def test_unvalidated_recipe_json_is_not_synced_to_old_clients(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/resource/manager/TableRecipeManager.java"
        text = path.read_text(encoding="utf-8")
        needle = "if (dataMap.containsKey(id)) {"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "if (true) {", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "sync only recipes accepted")

    def test_invalid_recipe_from_older_server_is_rejected(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/resource/network/CommonNetworkCache.java"
        text = path.read_text(encoding="utf-8")
        needle = "String error = TableRecipe.validationError(recipe);"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "String error = null;", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "CommonNetworkCache must independently reject")

    def test_recipe_materials_are_snapshotted_before_use(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/crafting/GunSmithTableRecipe.java"
        text = path.read_text(encoding="utf-8")
        needle = "List<GunSmithTableIngredient> snapshot = inputs == null ? null : new ArrayList<>(inputs);"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "List<GunSmithTableIngredient> snapshot = inputs;", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "must snapshot and reject null material entries")

    def test_recipe_api_and_crafting_paths_must_use_validation(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/inventory/GunSmithTableMenu.java"
        text = path.read_text(encoding="utf-8")
        needle = "if (TableRecipe.validationError(pojo) != null) {"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "if (pojo == null || pojo.getResult() == null) {", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "inventory/GunSmithTableMenu.java must guard each recipe construction")

    def test_carryon_redirection_must_not_precede_range_check(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/mixin/carryon/CarryOnPickupHandlerMixin.java"
        text = path.read_text(encoding="utf-8")
        needle = '@ModifyVariable(method = "tryPickUpBlock", at = @At(value = "INVOKE"'
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, '@ModifyVariable(method = "tryPickUpBlock", at = @At("HEAD"', 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "root redirection must happen at the getCarryData call")

    def test_carryon_redirect_requires_matching_companion_state(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/mixin/carryon/CarryOnPickupHandlerMixin.java"
        text = path.read_text(encoding="utf-8")
        needle = "state.equals(table.getCompanionState(rootState))"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "true", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "exactly matching root/companion pair")

    def test_orphan_companion_carryon_pickup_must_be_cancelled(self) -> None:
        path = self.root / "src/main/java/com/tacz/guns/mixin/carryon/CarryOnPickupHandlerMixin.java"
        text = path.read_text(encoding="utf-8")
        needle = "cir.setReturnValue(false);"
        self.assertIn(needle, text)
        path.write_text(text.replace(needle, "// missing orphan rejection", 1), encoding="utf-8")
        self.assert_guard_rejects(check_workbench_recipes, "reject orphaned companion halves")


if __name__ == "__main__":
    unittest.main(verbosity=2)

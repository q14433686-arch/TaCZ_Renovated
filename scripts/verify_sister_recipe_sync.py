#!/usr/bin/env python3
"""Static wiring checks only; not a Java codec round-trip or game test."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/tacz/guns'


class RecipeSyncChecks(unittest.TestCase):
    def test_registry_decoder_defers_to_existing_legacy_path(self):
        serializer = (JAVA / 'crafting/GunSmithTableSerializer.java').read_text()
        ingredient = (JAVA / 'crafting/GunSmithTableIngredient.java').read_text()
        self.assertIn('ExtraCodecs.JSON.fieldOf("item")', serializer)
        self.assertNotIn('Ingredient.CODEC.fieldOf("item")', serializer)
        self.assertIn('GunSmithTableIngredient::new', serializer)
        self.assertIn('RecipeCompat.normalizeLegacyIngredient(raw)', ingredient)
        self.assertIn('rawIngredient.deepCopy()', ingredient)

    def test_encoding_keeps_native_network_path_and_raw_fallback(self):
        source = (JAVA / 'crafting/GunSmithTableSerializer.java').read_text()
        self.assertIn('Ingredient.CODEC.encodeStart(ops, resolved)', source)
        self.assertLess(source.index('ingredient.getRawIngredient()'), source.index('Ingredient resolved = ingredient.getIngredient()'))
        self.assertIn('return raw != null ? raw : com.google.gson.JsonNull.INSTANCE', source)
        self.assertIn('recipe.init();', source)
        self.assertIn('Ingredient.CONTENTS_STREAM_CODEC.encode', source)

    def test_workbench_consumers_check_whole_recipe_before_constructing(self):
        # Static wiring guard: execution and server/network behavior are covered by
        # the separate Java harness, not by this string check.
        for relative, expected in (
            ('client/gui/GunSmithTableScreen.java', 2),
            ('inventory/GunSmithTableMenu.java', 1),
            ('api/TimelessAPI.java', 2),
            ('compat/jei/GunModPlugin.java', 1),
            ('compat/rei/REIClientPlugin.java', 1),
        ):
            source = (JAVA / relative).read_text()
            with self.subTest(path=relative):
                self.assertEqual(expected, source.count('new GunSmithTableRecipe('))
                self.assertEqual(expected, source.count('TableRecipe.validationError('))
                self.assertLess(source.index('TableRecipe.validationError('),
                                source.index('new GunSmithTableRecipe('))

    def test_rejected_server_recipes_are_not_synced_and_old_server_data_is_checked(self):
        server = (JAVA / 'resource/manager/TableRecipeManager.java').read_text()
        client = (JAVA / 'resource/network/CommonNetworkCache.java').read_text()
        self.assertIn('String error = TableRecipe.validationError(recipe)', server)
        self.assertLess(server.index('super.apply(ours,'), server.index('networkCache.forEach('))
        self.assertIn('if (dataMap.containsKey(id))', server)
        self.assertIn('networkCache = Map.copyOf(validNetworkCache)', server)
        self.assertIn('String error = TableRecipe.validationError(recipe)', client)
        self.assertLess(client.index('TableRecipe.validationError(recipe)'),
                        client.index('tableRecipe.put(entry.getKey(), recipe)'))


if __name__ == '__main__':
    unittest.main(verbosity=2)

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


if __name__ == '__main__':
    unittest.main(verbosity=2)

#!/usr/bin/env python3
"""Static wiring checks for 26.1.2 workbench recipe validation and Carry On pickup."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/tacz/guns'


class WorkbenchRecipeAndPickupChecks(unittest.TestCase):
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
            source = (JAVA / relative).read_text(encoding='utf-8')
            with self.subTest(path=relative):
                self.assertEqual(expected, source.count('new GunSmithTableRecipe('))
                self.assertEqual(expected, source.count('TableRecipe.validationError('))
                self.assertLess(source.index('TableRecipe.validationError('),
                                source.index('new GunSmithTableRecipe('))

    def test_rejected_server_recipes_are_not_synced_and_old_server_data_is_checked(self):
        server = (JAVA / 'resource/manager/TableRecipeManager.java').read_text(encoding='utf-8')
        client = (JAVA / 'resource/network/CommonNetworkCache.java').read_text(encoding='utf-8')
        self.assertIn('String error = TableRecipe.validationError(recipe)', server)
        self.assertLess(server.index('super.apply(ours,'), server.index('networkCache.forEach('))
        self.assertIn('if (dataMap.containsKey(id))', server)
        self.assertIn('networkCache = Map.copyOf(validNetworkCache)', server)
        self.assertIn('String error = TableRecipe.validationError(recipe)', client)
        self.assertLess(client.index('TableRecipe.validationError(recipe)'),
                        client.index('tableRecipe.put(entry.getKey(), recipe)'))

    def test_carryon_pickup_mixin_post_reach_and_orphan_guard(self):
        mixin = (JAVA / 'mixin/carryon/CarryOnPickupHandlerMixin.java').read_text(encoding='utf-8')
        target = (
            'Ltschipp/carryon/common/carry/CarryOnDataManager;'
            'getCarryData(Lnet/minecraft/world/entity/player/Player;)'
            'Ltschipp/carryon/common/carry/CarryOnData;'
        )
        self.assertNotIn('@At("HEAD")', mixin)
        self.assertEqual(2, mixin.count(target))
        self.assertIn('tacz$hasMatchingRoot(level, pos, state, table)', mixin)
        self.assertIn('!tacz$hasMatchingRoot(level, pos, state, table)', mixin)


if __name__ == '__main__':
    unittest.main(verbosity=2)

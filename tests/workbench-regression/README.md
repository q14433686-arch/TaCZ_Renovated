# Workbench recipe and pickup regression tests

`workbenchRecipeValidationTest` runs the production recipe POJO, ingredient Gson
adapter, server reload managers, network cache and recipe object against small
Minecraft/NeoForge stubs. It uses Minecraft's Gson dependency, not a game. It
checks null/missing materials, explicit empty lists, lenient trailing commas,
serializer failures, server sync filtering, old-server data, and a defensive
recipe snapshot. The stub result deserializer does not exercise actual item or
registry resolution, and the server crafting menu is not executed here.

`carryOnPickupTest` invokes the actual mixin handlers against stub states and
checks the injection selector, root matching, broken/orphaned halves and the
unmodified path for other blocks. This alone does not apply a mixin.

`carryOnTransformTest` additionally uses the project's Mixin implementation to
transform a **mock** Carry On-shaped `PickupHandler`, execute the transformed
method and verify both handler injections. Its small service and Minecraft stubs
are in `tests/carryon-transform/`. The test does not use real Carry On bytecode,
NeoForge mixin-plugin selection, real reach/permission events or a running game.

With a full JDK 25 and the project dependencies:

```sh
./gradlew workbenchRecipeValidationTest carryOnPickupTest carryOnTransformTest
```

All three tasks are included in `check` and `build`. The static consumer-wiring
checks also run via `python3 scripts/verify_sister_recipe_sync.py`. None of these
is a substitute for testing both halves in Minecraft with Carry On installed.

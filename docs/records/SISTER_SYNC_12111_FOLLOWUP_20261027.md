# 1.21.11 follow-up sync for 26.3 — 2026-10-27

## Scope and version decision

- Active line remains Minecraft 26.3 + NeoForge. This is a follow-up to `SISTER_SYNC_263_20261027.md`, not a loader/version switch.
- Newly reviewed TaCZ Renovated 1.21.11 tip: `682adce2ddda535326afd7a31938c450c44edd51`; relevant source commit: `581daaacc8e21ef55259480c804442e17b455fbe` (`Guard malformed workbench recipes and companion pickup`).
- 1.21.11's current version remains `1.1.8+neoforge.1.21.11.R3-hotfix4`; its temporary hotfix5 proposal was withdrawn. The Refabricated 26.3 branch remains at `cc990c3f784aa31c8fd7340f8cc158609c9bd16d`, version `1.1.8+fabric.26.3.R1-hotfix3`.
- Keep this unpublished candidate at `1.1.8+neoforge.26.3.R1-hotfix3`. No new 26.3 tag was requested or published; fold the additional fixes into the existing unreleased candidate rather than inventing `R1-hotfix4`.

## New fixes adapted

1. **Reject malformed workbench recipes atomically**
   - `TableRecipe#validationError` rejects a null recipe/result/material list and any null material entry. It does not remove a bad ingredient, which could lower the recipe cost; explicit `materials: []` remains valid.
   - The server `TableRecipeManager#parseJson` rejects the whole malformed resource. `JsonDataManager` logs the failed resource ID and reason.
   - After server parsing, filter `CommonDataManager#networkCache` against the accepted `dataMap`, so rejected raw JSON is not sent to older clients.
   - `CommonNetworkCache` validates again when receiving recipe JSON from an older server. The screen, menu, TimelessAPI, JEI, and REI skip invalid POJOs before constructing `GunSmithTableRecipe`.
   - `GunSmithTableRecipe` takes a defensive list snapshot and rejects null lists/items, while preserving empty lists.

2. **Carry On multi-block pickup ordering and orphan defense**
   - The earlier 26.3 mixin rewrote the clicked position at method HEAD, before Carry On's reach and general-eligibility checks. Move the redirect to the `CarryOnDataManager#getCarryData(Player)` call after those checks, so reach is tested at the clicked half and state/BE/callback/removal use the root.
   - Redirect only when root and companion positions/states match exactly. Cancel pickup of a detached companion, including when Carry On's `pickupAllBlocks` bypasses its block-entity requirement.
   - Keep the existing complete-placement space preflight unchanged.

## Source evidence

- 1.21.11 commit `581daaacc8e21ef55259480c804442e17b455fbe` records the malformed-recipe and pickup issues, validation rules, filters, and Mixin injection point.
- Carry On 26.3 / 2.12.0 source commit `168f3fb28fd7ff48d342483142469c3023c54ec0`: `tryPickUpBlock` checks `isBlockInDistance` and calls `canCarryGeneral` before its first *direct* `CarryOnDataManager#getCarryData` invocation. (`canCarryGeneral` itself reads the player's carry data, but does not inspect the clicked block position.) It later fetches the block entity/state, runs permissions/callback checks, saves carry data, then removes the block entity and calls `removeBlock(pos, false)`. The new injection selector targets that first direct invocation. This confirms source shape only, not runtime mixin compatibility.
- Refabricated 26.3 tip `cc990c3f...` has not moved since the previous sync and does not yet contain this recipe-validation or pickup-order follow-up.

## Verification and limits

- `python3 scripts/verify_workbench_recipes.py --strict` — **PASS** (static wiring checks only).
- `python3 scripts/test_block_guards.py` — **PASS**, 16 test cases (baseline plus 15 mutation regressions) including null-material rejection, preservation of empty-list semantics, server-to-old-client filtering, client receive rejection, consumer guards, defensive snapshots, pickup order/root matching, and orphan rejection.
- Existing loot/drop, block-entity persistence, Python syntax, version consistency, relative Markdown-link, and `git diff --check` checks are recorded in the main sync record and were rerun for this follow-up.
- The upstream also added Java/Gson stub tests and a Mixin-transformer harness. They were reviewed but not copied into this line: this 26.3 branch uses a Java 25 toolchain, and this sandbox has no Java executable with which to validate an additional Gradle/JVM harness. The added Python static/mutation gates are **not behaviorally equivalent** to those Java tests.
- Local Gradle compile/build and GitHub Actions: **not run** in this turn. This sandbox has no Java executable or Gradle cache.
- No runtime claim: real table GUI/menu behavior, malformed/legacy gun-pack recipes, Gson trailing-comma decoding, JEI/REI displays, permission/reach boundaries, orphan blocks, and Carry On 2.12.0 mixin injection remain to be tested in Minecraft. `require=0` still allows a descriptor mismatch to silently skip the optional injection.

#!/usr/bin/env python3
"""Static guards for malformed gun-smith recipes and Carry On pickup ordering on 26.3."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = Path("src/main/java/com/tacz/guns")
RECIPE_CONSUMERS = {
    "api/TimelessAPI.java": 2,
    "client/gui/GunSmithTableScreen.java": 2,
    "inventory/GunSmithTableMenu.java": 1,
    "compat/jei/GunModPlugin.java": 1,
    "compat/rei/REIClientPlugin.java": 1,
}


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def java_code(path: Path) -> str:
    return strip_comments(path.read_text(encoding="utf-8"))


def method_body(text: str, method: str) -> str:
    for match in re.finditer(r"\b" + re.escape(method) + r"\s*\(", text):
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


def check(root: Path) -> list[str]:
    errors: list[str] = []
    source = root / SRC
    try:
        table_recipe = java_code(source / "resource/pojo/data/recipe/TableRecipe.java")
        manager = java_code(source / "resource/manager/TableRecipeManager.java")
        network_cache = java_code(source / "resource/network/CommonNetworkCache.java")
        recipe = java_code(source / "crafting/GunSmithTableRecipe.java")
        pickup = java_code(source / "mixin/carryon/CarryOnPickupHandlerMixin.java")
        consumers = {name: java_code(source / name) for name in RECIPE_CONSUMERS}
    except OSError as exc:
        return [f"missing workbench recipe source: {exc}"]

    validation = method_body(table_recipe, "validationError")
    if not all(part in validation for part in (
        "recipe == null", "recipe.result == null", "recipe.materials == null",
        "recipe.materials.get(index) == null", "return null"
    )):
        errors.append("TableRecipe#validationError must reject null recipes/results/material lists/items")
    if "materials.isEmpty()" in validation or "materials.size() == 0" in validation:
        errors.append("TableRecipe#validationError must preserve explicitly empty materials lists")

    parse_json = method_body(manager, "parseJson")
    if not all(part in parse_json for part in (
        "TableRecipe.validationError(recipe)", "throw new JsonParseException(error)"
    )):
        errors.append("TableRecipeManager#parseJson must reject the complete malformed recipe")

    apply = method_body(manager, "apply")
    if not all(part in apply for part in (
        "super.apply(ours", "networkCache.forEach", "dataMap.containsKey(id)",
        "validNetworkCache.put(id, json)", "networkCache = Map.copyOf(validNetworkCache)"
    )):
        errors.append("TableRecipeManager must sync only recipes accepted into the server data map")
    if apply.find("super.apply(ours") > apply.find("networkCache.forEach"):
        errors.append("TableRecipeManager must filter network cache after server-side parsing")

    if not all(part in network_cache for part in (
        "case RECIPES -> {", "TableRecipe.validationError(recipe)",
        "throw new JsonParseException(error)", "tableRecipe.put(entry.getKey(), recipe)"
    )):
        errors.append("CommonNetworkCache must independently reject malformed recipes from older servers")

    ctor = method_body(recipe, "GunSmithTableRecipe")
    if not all(part in ctor for part in (
        "new ArrayList<>(inputs)", "snapshot.contains(null)",
        "throw new IllegalArgumentException", "this.inputs = snapshot"
    )):
        errors.append("GunSmithTableRecipe must snapshot and reject null material entries")

    for relative, expected in RECIPE_CONSUMERS.items():
        code = consumers[relative]
        validation_positions = [m.start() for m in re.finditer(r"TableRecipe\.validationError\(", code)]
        constructor_positions = [m.start() for m in re.finditer(r"new GunSmithTableRecipe\(", code)]
        if len(validation_positions) != expected or len(constructor_positions) != expected:
            errors.append(
                f"{relative} must guard each recipe construction exactly once "
                f"(found {len(validation_positions)} validation(s), {len(constructor_positions)} constructor(s))"
            )
            continue
        for index, (validation_pos, constructor_pos) in enumerate(zip(validation_positions, constructor_positions), 1):
            if validation_pos > constructor_pos:
                errors.append(f"{relative} recipe construction #{index} is not preceded by validation")

    at_target = "Ltschipp/carryon/common/carry/CarryOnDataManager;getCarryData(Lnet/minecraft/world/entity/player/Player;)Ltschipp/carryon/common/carry/CarryOnData;"
    if "@ModifyVariable(method = \"tryPickUpBlock\", at = @At(value = \"INVOKE\"" not in pickup:
        errors.append("CarryOn pickup root redirection must happen at the getCarryData call, after reach checks")
    if at_target not in pickup or "@At(\"HEAD\")" in pickup:
        errors.append("CarryOn pickup mixins must target getCarryData rather than rewrite the clicked position at HEAD")
    matching_root = method_body(pickup, "tacz$hasMatchingRoot")
    if not all(part in matching_root for part in (
        "rootState.is(table)", "table.isRoot(rootState)",
        "table.getCompanionPos(rootPos, rootState)", "table.getCompanionState(rootState)"
    )):
        errors.append("CarryOn pickup must redirect only an exactly matching root/companion pair")
    orphan_guard = method_body(pickup, "tacz$rejectOrphanTablePart")
    if "!table.isRoot(state)" not in orphan_guard or "!tacz$hasMatchingRoot" not in orphan_guard or "cir.setReturnValue(false)" not in orphan_guard:
        errors.append("CarryOn pickup must reject orphaned companion halves")

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
        print("26.3 workbench recipe validation and Carry On pickup guards: OK (static checks only)")
    return int(bool(errors) and args.strict)


if __name__ == "__main__":
    raise SystemExit(main())

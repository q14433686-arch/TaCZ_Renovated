#!/usr/bin/env python3
"""Static guards for 26.3 block-entity persistence, removal and target-skin wiring.

These guards catch known source regressions; they do not execute Minecraft's codecs, profile
service, renderer, or NeoForge attachment lifecycle. Compile and game acceptance remain separate.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = Path("src/main/java/com/tacz/guns")


def strip_comments(text: str) -> str:
    def blank(match: re.Match[str]) -> str:
        return re.sub(r"[^\n]", " ", match.group(0))

    text = re.sub(r"/\*.*?\*/", blank, text, flags=re.S)
    return re.sub(r"//[^\n]*", blank, text)


def java_code(path: Path) -> str:
    return strip_comments(path.read_text(encoding="utf-8"))


def method_body(text: str, header: str) -> str:
    search_from = 0
    while True:
        idx = text.find(header, search_from)
        if idx < 0:
            return ""
        open_paren = text.find("(", idx, idx + len(header))
        if open_paren < 0:
            return ""
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
            return ""
        next_pos = close_paren + 1
        while next_pos < len(text) and text[next_pos].isspace():
            next_pos += 1
        if next_pos < len(text) and text[next_pos] == "{":
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
        search_from = idx + len(header)


def check(root: Path) -> list[str]:
    errors: list[str] = []
    source = root / SRC
    if not source.is_dir():
        return ["missing Java source directory"]

    # Block entities which own an ItemStack must have an explicit removal policy.
    for path in sorted((source / "block/entity").glob("*.java")):
        raw = path.read_text(encoding="utf-8")
        code = strip_comments(raw)
        if re.search(r"\bItemStack\s+\w+\s*=", code) and not re.search(
            r"\bvoid\s+preRemoveSideEffects\s*\(", code
        ) and "tacz:no-drop-on-remove" not in raw:
            errors.append(f"{path.name}: ItemStack field has no preRemoveSideEffects policy")
        if re.search(r"\b(?:public|protected)\s+void\s+onRemove\s*\(", code):
            errors.append(f"{path.name}: obsolete BlockEntity onRemove hook")

    target_path = source / "block/entity/TargetBlockEntity.java"
    statue_path = source / "block/entity/StatueBlockEntity.java"
    target_block_path = source / "block/TargetBlock.java"
    minecart_path = source / "entity/TargetMinecart.java"
    block_renderer_path = source / "client/renderer/block/TargetRenderer.java"
    cart_renderer_path = source / "client/renderer/entity/TargetMinecartRenderer.java"
    try:
        target = java_code(target_path)
        statue = java_code(statue_path)
        target_block = java_code(target_block_path)
        minecart = java_code(minecart_path)
        block_renderer = java_code(block_renderer_path)
        cart_renderer = java_code(cart_renderer_path)
    except OSError as exc:
        return errors + [f"missing target/statue source: {exc}"]

    # NeoForge BlockEntity persistence hooks own NeoForgeData and attachment storage.
    for name, code in (("TargetBlockEntity", target), ("StatueBlockEntity", statue)):
        load = method_body(code, "loadAdditional(")
        save = method_body(code, "saveAdditional(")
        if not load or "super.loadAdditional(" not in load:
            errors.append(f"{name}: loadAdditional must call the NeoForge parent")
        if not save or "super.saveAdditional(" not in save:
            errors.append(f"{name}: saveAdditional must call the NeoForge parent")

    statue_remove = method_body(statue, "preRemoveSideEffects(")
    if not all(part in statue_remove for part in (
        "super.preRemoveSideEffects(pos, state)", "!level.isClientSide()", "this.dropItem(state, false)"
    )):
        errors.append("StatueBlockEntity: removal hook must drop server-side using the saved state")
    statue_drop = method_body(statue, "dropItem(BlockState state, boolean notify)")
    if not all(part in statue_drop for part in (
        "state.hasProperty(FACING)", "Block.popResource(", "this.gunItem = ItemStack.EMPTY"
    )):
        errors.append("StatueBlockEntity: state-aware drop must be safe and idempotent")

    target_load = method_body(target, "loadAdditional(")
    target_save = method_body(target, "saveAdditional(")
    legacy = method_body(target, "readLegacyOwner(")
    if not all(part in target_load for part in (
        "super.loadAdditional(input)", "OWNER_PROFILE_TAG", "ResolvableProfile.CODEC", "readLegacyOwner(input)"
    )):
        errors.append("TargetBlockEntity: modern owner profile must load with flat/compound legacy fallback")
    if not all(part in target_save for part in (
        "super.saveAdditional(output)", "OWNER_PROFILE_TAG", "ResolvableProfile.CODEC",
        "OWNER_UUID_TAG", "OWNER_NAME_TAG"
    )):
        errors.append("TargetBlockEntity: modern profile and legacy owner keys are not saved")
    if not all(part in legacy for part in (
        "OWNER_UUID_TAG", "OWNER_NAME_TAG", "LEGACY_OWNER_TAG", "getIntArray", "UUIDUtil.uuidFromIntArray"
    )):
        errors.append("TargetBlockEntity: legacy flat keys and Owner compound migration are incomplete")
    if "this.owner = " not in target_load or "this.name = null" not in target_load:
        errors.append("TargetBlockEntity: loading absent data must clear stale owner/name values")

    resolve = method_body(target, "resolveOwnerProfile(")
    current_lookup = method_body(target, "isCurrentLookup(")
    if not all(part in resolve for part in (
        "Util.nonCriticalIoPool().execute", "fetchByName", "fetchById", "server.execute",
        "isCurrentLookup", "ResolvableProfile.createUnresolved(complete.id())"
    )):
        errors.append("TargetBlockEntity: owner lookup must be asynchronous, stale-safe and dynamic for client skins")
    if "this.profileLookupGeneration == generation" not in current_lookup or "this.owner == requested" not in current_lookup:
        errors.append("TargetBlockEntity: stale profile lookup guard must include owner identity and generation")
    if "ResolvableProfile.createResolved(complete)" in resolve:
        errors.append("TargetBlockEntity: do not freeze the server lookup result into a static skin profile")
    if "setOwnerName(stack.getHoverName().getString())" not in method_body(target_block, "setPlacedBy("):
        errors.append("TargetBlock: named placement must use a dynamic profile, not a null UUID GameProfile")

    minecart_owner = method_body(minecart, "getOwnerProfile(")
    minecart_legacy = method_body(minecart, "getGameProfile(")
    if not all(part in minecart_owner for part in (
        "this.getCustomName()", "!name.equals(this.ownerProfileName)",
        "ResolvableProfile.createUnresolved(name)", "this.ownerProfile = null"
    )):
        errors.append("TargetMinecart: name changes/clears must rebuild the cached dynamic profile")
    if "profile.partialProfile()" not in minecart_legacy:
        errors.append("TargetMinecart: legacy profile accessor must return the non-null-UUID partial profile")

    block_extract = method_body(block_renderer, "extractRenderState(")
    if "context.playerSkinRenderCache()" not in block_renderer or not all(part in block_extract for part in (
        "getOwnerProfile()", "skinCache.getOrDefault(owner)",
        "DefaultPlayerSkin.get(owner.partialProfile())", "state.skinTexture = null"
    )):
        errors.append("TargetRenderer: use 26.3 PlayerSkinRenderCache and a safe default fallback")

    cart_extract = method_body(cart_renderer, "extractRenderState(")
    cart_submit = method_body(cart_renderer, "submitMinecartContents(")
    if "ctx.getPlayerSkinRenderCache()" not in cart_renderer or not all(part in cart_extract for part in (
        "entity.getOwnerProfile()", "skinCache.getOrDefault(owner)",
        "DefaultPlayerSkin.get(owner.partialProfile())", "targetState.skinTexture = null"
    )):
        errors.append("TargetMinecartRenderer: resolve skin from the cache while extracting render state")
    if "t.skinTexture" not in cart_submit or "createLookup(" in cart_renderer or "getSkinManager()" in cart_renderer:
        errors.append("TargetMinecartRenderer: submit must use the extracted texture, not synchronous SkinManager lookup")

    # Authlib 7 rejects null profile fields at runtime; this is not a compile-time error.
    for path in sorted(source.rglob("*.java")):
        code = java_code(path)
        if re.search(r"new\s+GameProfile\s*\(\s*null\b", code):
            errors.append(f"{path.relative_to(root)}: GameProfile constructor receives a literal null")

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
        print("26.3 block-entity persistence/removal and target skins: OK (static checks only)")
    return int(bool(errors) and args.strict)


if __name__ == "__main__":
    raise SystemExit(main())

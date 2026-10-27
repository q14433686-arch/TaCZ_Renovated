#!/usr/bin/env python3
"""Static guards for 26.1.2 block-entity persistence and removal.

Adapted from NeoForge 1.21.11 PR #64 / Refabricated 1.21.11 PR #120. Also check the NeoForge superclass
persistence hooks and target renderers. These checks are not codec round-trips,
a Java compilation or in-game tests. Use --strict to fail CI on findings.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java"

WRITE_CALL = re.compile(
    r"\.(?:putString|putInt|putBoolean|putFloat|putLong|putDouble|putUUID|store)\(\s*([^,]+),"
)
READ_CALL = re.compile(
    r"\.(?:getStringOr|getIntOr|getBooleanOr|getFloatOr|getLongOr|getDoubleOr|read)\(\s*([^,]+),"
)
GATE = re.compile(r'(?:getStringOr|getIntOr)\(\s*([^,]+),\s*(?:""|0)\s*\)\s*(?:\.isEmpty\(\)|> 0|== 0)')
CONST = re.compile(r'static\s+final\s+String\s+([A-Za-z0-9_]+)\s*=\s*"([^"]*)"')
NEW_GAME_PROFILE = re.compile(r"\bnew\s+(?:[A-Za-z0-9_]+\.)*GameProfile\s*\(")

errors: list[str] = []
checked: list[str] = []
scanned_for_profiles = 0


def resolve(expr: str, consts: dict[str, str]) -> str | None:
    """把 `"Literal"` / `CONSTANT` 解析成字符串；解析不了返回 None（跳过，不误报）。"""
    expr = expr.strip()
    literal = re.fullmatch(r'"([^"]*)"', expr)
    if literal:
        return literal.group(1)
    if expr in consts:
        return consts[expr]
    return None


def method_body(text: str, header: str) -> str | None:
    """粗略取出以 header 开头的方法体（大括号配平）。"""
    idx = text.find(header)
    if idx < 0:
        return None
    start = text.find("{", idx)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
    return None


def keys_in(body: str, pattern: re.Pattern[str], consts: dict[str, str]) -> set[str]:
    out: set[str] = set()
    for expr in pattern.findall(body):
        key = resolve(expr, consts)
        if key:
            out.add(key)
    return out


def check_block_entity(path: Path) -> None:
    raw = path.read_text(encoding="utf-8")
    text = strip_comments(raw)
    if not re.search(r"class\s+\w+\s+extends\s+(?:[\w.]*\.)?BlockEntity\b", text):
        return

    name = path.stem
    checked.append(name)
    consts = dict(CONST.findall(text))

    save = method_body(text, "void saveAdditional(")
    load = method_body(text, "void loadAdditional(")

    if save is not None and load is not None:
        # NeoForge persists attachments and NeoForgeData in these superclass hooks.
        if "super.saveAdditional(" not in save or "super.loadAdditional(" not in load:
            errors.append(f"[F] {name}: 覆写持久化方法时必须调用 NeoForge 父类")
        written = keys_in(save, WRITE_CALL, consts)
        read = keys_in(load, READ_CALL, consts)

        for key in sorted(keys_in(load, GATE, consts) - written):
            errors.append(
                f"[A] {name}: loadAdditional 用 `{key}` 作前置判据，但 saveAdditional 从不写这个键"
                " —— 判据恒为假，整段数据读不回来（标靶主人的同款坑）"
            )
        for key in sorted(written - read):
            errors.append(f"[B] {name}: saveAdditional 写了 `{key}` 却从不读回（死数据，或读取处漏了）")
        for key in sorted(read - written):
            errors.append(f"[B] {name}: loadAdditional 读 `{key}` 却从不写入（永远读不到默认值之外的内容）")

    # C. 持有物品的方块实体必须在移除时处理内容物
    if re.search(r"\bItemStack\s+\w+\s*=", text) and "// tacz:no-drop-on-remove" not in raw:
        if not re.search(r"\bvoid\s+preRemoveSideEffects\s*\(", text):
            errors.append(
                f"[C] {name}: 持有 ItemStack 字段但没有 preRemoveSideEffects —— "
                "26.1.2 已无 Block#onRemove，也没有任何默认清理（BlockEntity#preRemoveSideEffects 的默认实现"
                "只倒空 Container），方块被移除时内容物会静默消失（雕像枪的同款坑）。"
                "确实不需要掉的请加 `// tacz:no-drop-on-remove` 注释"
            )

    # D. onRemove 在 1.21.11+ 已删除
    if re.search(r"\b(?:public|protected)\s+void\s+onRemove\s*\(", text):
        errors.append(
            f"[D] {name}: 出现了 `onRemove` 方法 —— 26.1.2 的 Block#onRemove 已删除，"
            "写了也不会被调用，应改用 preRemoveSideEffects"
        )


def strip_comments(text: str) -> str:
    """去掉注释但保留换行（行号不变）；字符串字面量里的 `//` 不处理，够用即可。"""

    def blank(match: re.Match[str]) -> str:
        return re.sub(r"[^\n]", " ", match.group(0))

    text = re.sub(r"/\*.*?\*/", blank, text, flags=re.S)
    return re.sub(r"//[^\n]*", blank, text)


def split_args(arglist: str) -> list[str]:
    """按顶层逗号切实参；尊重括号嵌套与字符串字面量。"""
    args: list[str] = []
    depth = 0
    current: list[str] = []
    in_str = False
    prev = ""
    for ch in arglist:
        if in_str:
            current.append(ch)
            if ch == '"' and prev != "\\":
                in_str = False
        elif ch == '"':
            in_str = True
            current.append(ch)
        elif ch in "([{":
            depth += 1
            current.append(ch)
        elif ch in ")]}":
            depth -= 1
            current.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
        prev = ch
    tail = "".join(current).strip()
    if tail:
        args.append(tail)
    return args


def check_null_game_profile(path: Path) -> None:
    """E. new GameProfile(..., null, ...) 运行期必抛 NPE（authlib 7 的 record 构造器拒绝 null）。"""
    global scanned_for_profiles
    raw = path.read_text(encoding="utf-8")
    if "GameProfile" not in raw:
        return
    scanned_for_profiles += 1
    text = strip_comments(raw)
    for match in NEW_GAME_PROFILE.finditer(text):
        start = match.end()
        depth = 1
        in_str = False
        prev = ""
        end = None
        for i in range(start, len(text)):
            ch = text[i]
            if in_str:
                if ch == '"' and prev != "\\":
                    in_str = False
            elif ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    end = i
                    break
            prev = ch
        if end is None:
            continue
        args = split_args(text[start:end])
        if any(arg == "null" for arg in args):
            lineno = text.count("\n", 0, match.start()) + 1
            errors.append(
                f"[E] {path.relative_to(ROOT)}:{lineno}: `new GameProfile(...)` 的实参里有字面量 null —— "
                "authlib 7 的 GameProfile 是 record，构造器会抛 NullPointerException"
                "（Profile ID / name must not be null），编译期看不出。"
                "需要「只有名字」的档案请用 TargetOwnerProfiles.fromName(...)"
            )


def check_target_skin_wiring() -> None:
    """The block and minecart must share a name-only profile and nonblocking lookup."""
    guns = SRC / "com/tacz/guns"
    paths = {
        "profile": guns / "util/TargetOwnerProfiles.java",
        "skin": guns / "client/renderer/other/TargetOwnerSkins.java",
        "block": guns / "block/TargetBlock.java",
        "entity": guns / "block/entity/TargetBlockEntity.java",
        "cart": guns / "entity/TargetMinecart.java",
        "block_renderer": guns / "client/renderer/block/TargetRenderer.java",
        "cart_renderer": guns / "client/renderer/entity/TargetMinecartRenderer.java",
    }
    if any(not path.is_file() for path in paths.values()):
        errors.append("[G] 标靶档案/渲染代码缺失")
        return
    sources = {name: strip_comments(path.read_text(encoding="utf-8")) for name, path in paths.items()}
    required = {
        "profile": ("ResolvableProfile.createUnresolved(name)", "name.isBlank()"),
        "skin": ("playerSkinRenderCache().getOrDefault(profile)", "DefaultPlayerSkin.get(profile.partialProfile())"),
        "block": ("TargetOwnerProfiles.fromName(",),
        "entity": ("TargetOwnerProfiles.fromName(input.getStringOr(OWNER_NAME_TAG", "output.putString(OWNER_NAME_TAG"),
        "cart": ("TargetOwnerProfiles.fromName(name)", "Objects.equals(name, this.cachedOwnerName)"),
        "block_renderer": ("TargetOwnerSkins.texture(owner)",),
        "cart_renderer": ("TargetOwnerSkins.texture(owner)", "entity.getOwnerProfile()"),
    }
    for name, needles in required.items():
        for needle in needles:
            if needle not in sources[name]:
                errors.append(f"[G] {paths[name].relative_to(ROOT)}: 缺少 {needle}")
    for name in ("block_renderer", "cart_renderer", "skin"):
        if "getSkinManager()" in sources[name]:
            errors.append(f"[G] {paths[name].relative_to(ROOT)}: 不应直接查询 SkinManager")
    placement = method_body(sources["block"], "void setPlacedBy(") or ""
    setter = method_body(sources["entity"], "void setOwner(") or ""
    name_at, owner_at, refresh_at = (placement.find("e.setCustomName("),
                                      placement.find("e.setOwner("), placement.rfind("e.refresh("))
    if name_at < 0 or owner_at < 0 or not (
        (name_at < owner_at and "this.refresh()" in setter) or refresh_at > max(name_at, owner_at)
    ):
        errors.append("[G] 标靶放置后必须在名称和档案都设置完毕后同步")
    removal = method_body(sources["entity"], "void loadAdditional(") or ""
    if "this.owner = " not in removal or "this.name = null" not in removal:
        errors.append("[G] TargetBlockEntity 重读数据时应清除缺失的主人和名称")
    statue = strip_comments((guns / "block/entity/StatueBlockEntity.java").read_text(encoding="utf-8"))
    removal = method_body(statue, "void preRemoveSideEffects(") or ""
    for needle in ("super.preRemoveSideEffects(", "!level.isClientSide()", "dropItem(state)"):
        if needle not in removal:
            errors.append(f"[G] StatueBlockEntity 移除回调缺少 {needle}")
    drop = method_body(statue, "void dropItem(BlockState state)") or ""
    for needle in ("state.hasProperty(FACING)", "Block.popResource(", "this.gunItem = ItemStack.EMPTY"):
        if needle not in drop:
            errors.append(f"[G] StatueBlockEntity 掉枪缺少 {needle}")


def main() -> int:
    parser = argparse.ArgumentParser(description="审计方块实体的持久化与移除收尾")
    parser.add_argument("--strict", action="store_true", help="有问题时退出 1")
    args = parser.parse_args()

    if not SRC.is_dir():
        print(f"[ERR] 找不到 {SRC}", file=sys.stderr)
        return 1 if args.strict else 0

    for path in sorted(SRC.rglob("*.java")):
        check_block_entity(path)
        check_null_game_profile(path)
    check_target_skin_wiring()

    print(f"方块实体持久化审计：检查 {len(checked)} 个类（{', '.join(checked) or '无'}）；"
          f"GameProfile 构造扫描 {scanned_for_profiles} 个涉及文件")
    if errors:
        print(f"[FAIL] 发现 {len(errors)} 项：")
        for error in errors:
            print(f"  - {error}")
        return 1 if args.strict else 0
    print("全部通过。注意：这只是静态检查，不替代编译与实机「破坏后重进存档」测试。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

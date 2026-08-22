# Modrinth 发布文案（TaCZ: Renovated R1）

> 复制粘贴用。`[[ ]]` 内容发布前替换，搜 `[[` 全部处理掉。
> 规则依据：Modrinth [Content Rules](https://modrinth.com/legal/rules)；
> 条文解读沿用姊妹项目 Modrinth.md 的已验证结论。

## ⚠️ 硬性规则速查

| 规则 | 对本项目的执行 |
|---|---|
| §5.2 标题只能是项目名，不带版本等 filler | 标题不含 `26.1.2` / `NeoForge` |
| §5.3 Summary 纯文本、不重复标题词 | Summary 不出现 "TaCZ" / "Renovated" |
| §1.9 不得让人误以为官方出品/背书 | `[UNOFFICIAL]` 前缀 + 描述首段免责 |
| §4 以 license 合规 fork 立足 | 描述写明 GPL-3.0 与全谱系来源 |
| §5.6 依赖必须填 Dependencies 字段 | 本项目**无必装前置**；可选集成按 optional 填 |
| §5.1 metadata 各站一致 | License/环境/标签与 GitHub、CurseForge 对齐 |
| NC 运营注意 | **Settings → Monetization 关闭** |

## ① Project Title

```
[UNOFFICIAL]TaCZ Renovated
```

> 与姊妹项目同构：无版本号；`[UNOFFICIAL]` 服务于 §1.9 硬规则，
> 且有站内先例（`[UNOFFICIAL] TaCZ 1.21.1 NeoForge Port`，49 万下载在线）。

## ② Summary（≤256 字符，纯文本）

```
A community port of the Timeless and Classics Guns mod to the NeoForge loader, with the LRTactical tactical-equipment framework built in. Multiplayer-tested on dedicated servers.
```

> 179 字符；无格式、不含标题词。

## ③ Description

```markdown
# Unofficial NeoForge port

> **This is an unofficial community port. It is not an official TACZ release and
> is not affiliated with, reviewed by, or endorsed by the TACZ Dev Team.**

## What this project does

**Timeless and Classics Guns: Zero (TaCZ)** is a modern firearms mod: deeply
customizable guns with attachments, optics and ammo types, a gun-smithing
workbench, and support for third-party "gun packs" that add entire weapon sets.

This project ports that mod to **NeoForge on Minecraft 26.1.2**, and additionally
ships a built-in port of the **LRTactical** tactical-equipment framework
(throwables, melee weapons, detonators, consumables) so content packs that
depend on `lrtactical` work out of the box.

It adds no new guns of its own — the goal is to keep the existing mod and its
gun-pack ecosystem working on this loader and game version.

## Why you might want it

- You want TaCZ's gun system on **NeoForge 26.1.2** — no other open-source build
  targets it.
- **No required dependencies.** Drop one jar into `mods/` and play.
- **Multiplayer is actually tested**: single-player, LAN and dedicated-server
  scenarios (join/sync, shooting, crafting, gun-pack hot reload, LRTactical
  items) were verified with logs archived in the repository. Untested so far:
  hosting panels, proxy networks (Velocity), hybrid servers (Bukkit+mods) —
  issues from those environments must be reproduced on a plain NeoForge
  dedicated server first.

## Credits and licensing

| | |
|---|---|
| Original mod | **Timeless and Classics Guns: Zero**, by the **TACZ Dev Team** |
| Game-semantics source | [TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) (Fabric 26.x sister project) |
| Its upstream | [`Sh1roCu/TACZ-Refabricated`](https://github.com/Sh1roCu/TACZ-Refabricated), baseline **1.1.8** |
| LRTactical original | **LesRaisins Tactical Equipements** — Programmer **xjqsh**, Artist **LeComte** |
| Source of this port | https://github.com/q14433686-arch/TaCZ-Renovated |

All code in this lineage is **GPL-3.0**, which permits this port; this project is
released under GPL-3.0 as well.

**Note:** the bundled default gun pack's *assets* are licensed
**CC BY-NC-ND 4.0** — separate from the code license. They are redistributed
here unmodified; don't sell them, and don't modify-then-redistribute them. To
make your own content, create a separate pack instead of editing the default.
**No LRTactical art is bundled at all** (it is All Rights Reserved): LR items
show vanilla placeholder models until a content pack provides visuals.

## What was changed from the original

This target version and loader required substantial rework, not a recompile:

- **Loader layer rewritten for NeoForge**: registration (DeferredRegister),
  event wiring, and networking (payload registrar / stream codecs).
- **Scope rendering** uses a depth-aperture path built for 26.1's renderer
  (the stencil buffer upstream relied on no longer exists). Some visual effects
  differ from the original.
- **Dedicated-server hardening**: several crashes that only occur on real
  servers (client-only class access in shared code paths, empty-ItemStack
  network encoding, missing data-sync channels) were found in live testing and
  fixed.
- **LRTactical framework** re-implemented on NeoForge idioms, including its
  network sync, tooltips, HUD and cooldown overlays. `flash_shield` is not
  ported (separate subsystem with All-Rights-Reserved art).

## Requirements

**Minecraft 26.1.2 · NeoForge 26.1.2.x · no required dependencies.**
Optional integrations (config UI, shaders, third-person animations, controller,
recipe viewers) are listed in the repository's compatibility matrix.
There is no Fabric build here — for Fabric (26.2 / 26.1.2 / 1.21.11) see the
sister project linked above.

## Installing gun packs

Gun packs go in **`.minecraft/tacz/`** (created on first launch). A zip or
folder is only recognised if **`gunpack.meta.json` exists at its root**;
otherwise it is silently skipped.

**Multiplayer note:** install the same packs on **both server and client** —
the server provides gameplay data (synced over the network), the client
provides models/textures/sounds. After adding a pack, run `/tacz reload` on the
server; on the client press **F3+T** (no restart needed).

**Version predicate:** this port reports `1.1.8` plus build metadata after `+`.
SemVer ignores build metadata, so packs requiring `"tacz": ">=1.1.8"` load
normally.

## Known limitation: packs requiring TacZ:Arcana

Some packs ship their assets encrypted (`recursion/taczpack.dat`) and require
the closed-source, Forge-only mod **TacZ:Arcana** to decrypt. No open TACZ
build can load them — symptom: entries and names appear, models/textures are
missing. This is not a defect of this port and cannot be fixed here.

## FAQ: REI/JEI cheat-give on a dedicated server yields purple items

If the recipe viewer is **not installed on the server**, its cheat-give sends a
bare item without data components, so TACZ cannot resolve the content id.
Install the same viewer version on the server, or take items from TaCZ's own
creative tabs, or `/give` with components. Same behaviour as the original mod.

## Feedback

Use the issue templates on the repository (link in the sidebar). Always attach
the full `latest.log` — for multiplayer issues, from both server and client.
Please do not report issues of this port to the original authors.

Provided "as is", without warranty of any kind.
```

## ④ 字段与设置

| 字段 | 值 |
|---|---|
| License | GPL-3.0-only |
| Environment | Client & Server 均必装 |
| Loaders / Game versions | NeoForge / 26.1.2 |
| Links | Source / Issues = 本仓库；Wiki/Discord 留空 |
| Dependencies | 无 required；可选集成如需展示填 optional |
| Monetization | **关闭** |
| 版本文件 | `Version number` = `1.1.8+neoforge.26.1.2.R1`，Changelog 粘 CHANGELOG R1 条目，Channel = Release |

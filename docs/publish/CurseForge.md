# CurseForge 发布文案（TaCZ: Renovated R1）

> 复制粘贴用。`[[ ]]` 内容发布前替换。规则依据：CurseForge
> [Moderation Policies](https://support.curseforge.com/support/solutions/articles/9000197279-moderation-policies)；
> 条文解读沿用姊妹项目 CurseForge.md 的已验证结论。

## ⚠️ 硬性规则速查

| 规则 | 对本项目的执行 |
|---|---|
| 项目名不得含游戏名/版本号，必须英文 | 名称不含 `26.1.2`/`Minecraft`/`NeoForge` |
| fork 必须自述改动、不得照抄原项目描述 | 描述为本仓库自撰，含 "What changed" 专节 |
| 必须署名并链接原作者 | Credits 表全谱系（TACZ Dev Team / Sh1roCu / 姊妹项目 / LR 原作） |
| **描述内禁止外部下载链接** | 描述不放 GitHub Releases 链接；Source/Issues 走 Links 字段 |
| 捐赠/个人链接只能在页面最底部 | 不放此类内容 |
| Avatar 400×400、不可纯色、不可用他人版权图 | **[[ 自制头像，勿用 TACZ 官方图 ]]** |
| NC 运营注意 | **项目设置关闭 Rewards Program** |

## ① Project Name

```
[UNOFFICIAL]TaCZ Renovated
```

> 无版本号、无游戏名、纯英文；`[UNOFFICIAL]` 属状态标注（站内先例：
> `[UNOFFICIAL] TaCZ NeoForge Port`，48 万下载已过审）。
> ❌ 不要用 `TaCZ Renovated NeoForge 26.1.2 Port` 一类写法。

## ② Summary

```
An unofficial community port of the TaCZ gun mod with the LRTactical tactical-equipment framework built in, verified in dedicated-server play.
```

## ③ Description

> 与 Modrinth 正文同源但独立成文（CF 禁抄他人描述不禁自家跨站复用；
> 差异点：无外部下载链接、英文在前中文在后）。

```markdown
# Unofficial NeoForge Port

> **Unofficial community port. Not an official TACZ release, and not affiliated
> with, reviewed by, or endorsed by the TACZ Dev Team.**

## What this project is

**Timeless and Classics Guns: Zero (TaCZ)** is a modern firearms mod — deeply
customizable guns, attachments, optics, ammo types, a gun-smithing workbench,
and third-party "gun pack" support.

This project is a community port of that mod to **NeoForge**, with a built-in
port of the **LRTactical** tactical-equipment framework (throwables, melee,
detonators, consumables) so content packs depending on `lrtactical` work out of
the box. No new guns are added; the goal is to keep the existing ecosystem
running. Exact Minecraft and loader versions are listed on each file.

## Credits — original work

| | |
|---|---|
| Original mod | **Timeless and Classics Guns: Zero**, by the **TACZ Dev Team** |
| Game-semantics source | **TaCZ Refabricated Unofficial** (Fabric sister project) |
| Its upstream | **Sh1roCu / TACZ-Refabricated**, baseline **1.1.8** |
| LRTactical original | **LesRaisins Tactical Equipements** — Programmer **xjqsh**, Artist **LeComte** |

All code in this lineage is **GPL-3.0**, which permits this port; this project
is likewise released under GPL-3.0. Full source and audit records are linked in
the **Source** field of this page.

**Assets:** the bundled default gun pack's assets are **CC BY-NC-ND 4.0**,
redistributed unmodified. No LRTactical art is bundled (All Rights Reserved) —
LR items show vanilla placeholder models until a content pack provides visuals.

## What changed from the original

- **Rewritten for NeoForge**: registration, event wiring and networking were
  re-implemented on NeoForge idioms (the original targets Forge; the Fabric
  sister project provided game semantics).
- **Scope rendering** re-implemented as a depth-aperture path for this game
  version's renderer (no stencil buffer anymore); some visuals differ from the
  original.
- **Dedicated-server hardening**: crashes reproducible only on real servers
  (client-class access in shared paths, empty-ItemStack packet encoding,
  missing sync channels) were found in live multiplayer testing and fixed.
- **LRTactical framework** rebuilt on NeoForge, including network sync,
  tooltips, HUD and cooldown overlays. `flash_shield` is not ported.

## Status

**R1 — first release.** Verified in single-player, LAN and dedicated-server
play (join/sync, combat, crafting, gun-pack hot reload, LRTactical items), with
test logs archived in the source repository. Not yet tested: hosting panels,
proxy networks, hybrid Bukkit+mods servers — issues from those environments
must first be reproduced on a plain NeoForge dedicated server.

## Requirements

| | |
|---|---|
| Loader | **NeoForge** |
| Required dependencies | **None** |
| Optional | Config UI, shaders, third-person animations, controller support, recipe viewers — see the compatibility matrix in the source repository |

> **NeoForge only.** Fabric builds (26.2 / 26.1.2 / 1.21.11) are provided by the
> sister project, not here.

## Installing gun packs

Gun packs go in **`.minecraft/tacz/`** (created on first launch). A zip or
folder is only recognised if **`gunpack.meta.json` exists at its root**;
otherwise it is silently skipped.

**Multiplayer:** install the same packs on **both server and client** — the
server provides gameplay data (synced over the network), the client provides
models/textures/sounds. After adding a pack, run `/tacz reload` on the server;
on the client press **F3+T** (no restart needed).

**Version predicate:** the mod reports `1.1.8` plus build metadata after `+`;
SemVer ignores build metadata, so packs requiring `"tacz": ">=1.1.8"` load
normally.

## Known limitation: packs requiring TacZ:Arcana

Packs that ship encrypted assets (`recursion/taczpack.dat`) require the
closed-source, Forge-only mod **TacZ:Arcana** to decrypt. No open TACZ build
can load them — entries and names appear, models/textures stay missing. Not a
defect of this port; cannot be fixed here.

## FAQ: REI/JEI cheat-give on a dedicated server yields purple items

If the recipe viewer is **not installed on the server**, its cheat-give sends a
bare item without data components, so the content id cannot be resolved.
Install the same viewer version on the server, take items from TaCZ's own
creative tabs, or `/give` with components. Same behaviour as the original mod.

## Feedback

Report issues via the **Issues** link on this page, using the provided
templates. Always attach the full `latest.log` — for multiplayer issues, from
both server and client. Please do not report issues of this port to the
original authors.

---

## 中文简介

本项目是《永恒枪械工坊:零》(TaCZ) 的非官方 NeoForge 移植，内置 LRTactical
战术装备框架（依赖 lrtactical 的内容包可完整使用；flash_shield 除外）。
无必装前置；联机需双端安装相同枪包，服务端 `/tacz reload`、客户端 F3+T 生效。
代码 GPL-3.0 开源（源码见本页 Source 链接），默认枪包资源 CC BY-NC-ND 4.0
原样承载。问题请经本页 Issues 链接反馈，勿打扰原作者。
按"原样"提供，不附带任何担保。
```

## ④ 字段与设置

| 字段 | 值 |
|---|---|
| License | GNU General Public License version 3 |
| Links | Source / Issues = 本仓库（**不放在描述里**） |
| Rewards Program | **关闭** |
| 文件上传 | 与 GitHub Release 同一 jar；Display Name `TaCZ Renovated 1.1.8+neoforge.26.1.2.R1`；Game Version 26.1.2；Loader NeoForge；Changelog 粘 CHANGELOG R1 条目 |
| Avatar | **[[ 自制 400×400，勿用 TACZ 官方素材 ]]** |

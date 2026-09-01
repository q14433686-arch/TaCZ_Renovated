# Modrinth 发布文案（TaCZ: Renovated）

> 项目级正文。结构与 CurseForge 文案统一，但 Modrinth 允许在正文直接提供源码、下载和
> Issues 链接。单次版本变化写入版本 Changelog，不累积到项目介绍。

## Project Title

```text
[UNOFFICIAL]TaCZ Renovated
```

## Summary

```text
An unofficial NeoForge community port of a customizable firearms mod for newer Minecraft releases, focused on gun-pack compatibility and extension support.
```

## Description

```markdown
# Unofficial NeoForge port

> **TaCZ: Renovated is an unofficial community port of Timeless & Classics Guns:
> Zero (TaCZ). It is not an official TACZ Dev Team release and has not been
> reviewed or endorsed by that team.**

TaCZ: Renovated provides **NeoForge** builds for Minecraft **26.2, 26.1.2 and
1.21.11**. It follows the public GPL source lineage of the original TaCZ,
Sh1roCu's Fabric port and our Fabric sister project. The goal is porting and
compatibility maintenance, not adding new firearms.

## Main features

- Firearms, attachments, optics, ammunition, workbenches and data-driven gun
  packs, including the upstream default gun pack.
- NeoForge-native registration, events and networking, plus branch-specific
  resource, GUI and rendering adaptations.
- Modern gun-pack loading and conversion for supported legacy layouts.
- A **partial LRTactical compatibility framework** covering melee items,
  consumables, detonators and several throwable behaviours.
- A **built-in TacZ Mesh Loader (TML)** for `poly_mesh` gun models (guns,
  attachments, ammo and blocks), with first-person and world-context GPU static
  baking, aperture-aware scope clipping and an in-game config page. Ported from
  [VellEagle/TacZMeshLoader](https://github.com/VellEagle/TacZMeshLoader)
  (GPL-3.0) via the Fabric sister project; see
  [docs/MESH_LOADER.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/docs/MESH_LOADER.md).
- **Scope picture-in-picture** (reprojection and second-world-render modes, off
  by default; classic whole-screen zoom remains the default) with in-scope arm /
  flash / body / text clipping and a low-power exemption below
  `ScopePipMinMagnification` (default 4x).
- Built-in JEI/REI ammo queries and workbench categories where available;
  optional integrations vary by release.

Third-party gun packs, tactical-equipment packs, optional mods and shader packs
must be verified against the exact Minecraft release. See each version's release
notes for tested status.

## Requirements

| Minecraft | Loader | Java | Required dependencies |
|---|---|---|---|
| 26.2 / 26.1.2 | NeoForge | 25+ | None |
| 1.21.11 | NeoForge | 21+ | None |

Files for different Minecraft releases cannot be mixed. There is no Forge or
Fabric build in this project. Fabric users should use the sister project
[TaCZ Refabricated Unofficial](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated).

## Installing gun packs

Place modern packs in `.minecraft/tacz/`. Zip files may be loaded directly, or
packs may be extracted as folders. `gunpack.meta.json` must be at the pack root;
do not add an extra wrapper directory inside a zip.

Place legacy-layout packs in `.minecraft/tacz_backup/`, keep a backup, and run
`/tacz convert`. Conversion cannot automatically repair every resource, recipe
or script difference. Judge by the internal layout rather than only the pack's
advertised Minecraft version.

In multiplayer, install the same packs on server and clients. The server supplies
gameplay data; clients supply models, textures, animations, sounds and language
files. Run `/tacz reload` on the server and press **F3+T** on clients after adding
a pack.

## LRTactical and Arcana

The integrated LRTactical code is a **partial compatibility framework**, not a
complete NeoForge release. It excludes the original complete art set and
`flash_shield`, and it does not guarantee every related content pack. Packs must
provide the assets they are licensed to distribute.

This project does not include
[TacZ:Arcana](https://www.curseforge.com/minecraft/mc-mods/tacz-arcana-timeless-and-classics-guns)
or implement its API or protected asset-loading process. Packs that explicitly
require Arcana are unsupported. Missing models or purple/black textures can also
be caused by directory nesting, resource paths, version constraints or incomplete
files, so the symptom alone does not establish an Arcana dependency.

## Known boundaries

- A scanned pack or visible entry is not proof that every model, animation,
  recipe and script is compatible.
- Scope rendering differs by branch and may produce different results with
  different graphics backends or shader packs.
- Hosting panels, proxy networks and hybrid Bukkit/mod servers are not uniformly
  supported. Reproduce problems on a plain NeoForge dedicated server first.

Use a minimal NeoForge environment when reporting issues and attach the complete
`latest.log` or crash report. Multiplayer reports need server and client logs.
Do not report this port's issues to upstream TaCZ, TaCZ Refabricated or
LRTactical authors.

## Links and credits

- [TaCZ: Renovated on CurseForge — Project ID 1663324](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-renovated)
- [Original project — MCModderAnchor/TACZ](https://github.com/MCModderAnchor/TACZ)
- [Direct upstream — Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated)
- [NeoForge port skeleton reference (GPL-3.0, auxiliary — rendering code not adopted) — MUKSC/TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)
- [Built-in TML upstream — VellEagle/TacZMeshLoader (GPL-3.0)](https://github.com/VellEagle/TacZMeshLoader)
- [LRTactical original — LesRaisins Tactical Equipements](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements)
- [Fabric sister project — TaCZ Refabricated Unofficial](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated)
- [Fabric sister project source](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
- [Source of this port](https://github.com/q14433686-arch/TaCZ_Renovated)
- [Downloads and release notes](https://github.com/q14433686-arch/TaCZ_Renovated/releases)
- [Issue tracker](https://github.com/q14433686-arch/TaCZ_Renovated/issues)

TaCZ, this port and the incorporated LRTactical code use GPL-3.0; the built-in TML
(built-in TML port) is GPL-3.0 from VellEagle/TacZMeshLoader. Default gun-pack
assets declare CC BY-NC-ND 4.0. LuaJ uses MIT; Commons Math uses Apache-2.0. Other code and assets keep their own licenses. Code licenses
do not automatically cover models, textures, animations or sounds; see
[LICENSE](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSE) and
[LICENSES.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSES.md).

The mod ID remains `tacz` to preserve gun-pack dependencies and save data. This
does not imply official status. Provided as-is, without warranty.
```

## 项目字段

| 字段 | 值 |
|---|---|
| License | `GPL-3.0-only` |
| Environment | Client and server |
| Loaders | NeoForge |
| Game versions | 26.2、26.1.2、1.21.11 |
| Required dependencies | 无 |
| Source | `https://github.com/q14433686-arch/TaCZ_Renovated` |
| Issues | `https://github.com/q14433686-arch/TaCZ_Renovated/issues` |
| Monetization | 关闭（默认枪包资源含 NC 条款） |

## 单个版本 Changelog 模板

```markdown
## [[完整版本号]]

**Minecraft [[版本]] · NeoForge [[版本]] · Java [[版本]]**

### Changes
- [[从对应分支 CHANGELOG / GitHub Release 摘取本次变化]]

### Verified in this build
- [[只列实际核验结果]]

### Known boundaries
- [[列该版本未实测或不支持项]]

Files for other Minecraft releases are not interchangeable. Back up worlds and
gun packs before upgrading.
```

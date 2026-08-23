# CurseForge 发布文案（TaCZ: Renovated）

> 项目级正文，参考本项目 CurseForge **Project ID 1663324** 的简洁结构整理。
> 项目正文不堆叠单次修复；每个文件的变化使用本文末尾的 Changelog 模板。
> CurseForge 描述中不放外部文件下载链接；源码和 Issues 使用项目 Links 字段。

## Project Name

```text
[UNOFFICIAL]TaCZ Renovated
```

## Summary

```text
An unofficial NeoForge community port of the TaCZ gun mod for newer Minecraft releases, focused on gun-pack compatibility and extension support.
```

## Description

```markdown
# Unofficial NeoForge port

> **TaCZ: Renovated is an unofficial community port of Timeless & Classics Guns:
> Zero (TaCZ). It is not an official TACZ Dev Team release and has not been
> reviewed or endorsed by that team.**

This project brings TaCZ to **NeoForge** on Minecraft **26.2, 26.1.2 and
1.21.11**. It is based on the public GPL source lineage of
[MCModderAnchor/TACZ](https://github.com/MCModderAnchor/TACZ),
[Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated), and
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial).
Its focus is porting and compatibility maintenance, not adding new firearms.

## Features

- TaCZ firearms, attachments, optics, ammunition, workbenches and data-driven
  gun packs, including the upstream default gun pack.
- NeoForge-native registration, events and networking, with resource, GUI and
  rendering adaptations for each supported Minecraft release.
- Direct loading of modern gun packs and conversion of supported legacy layouts.
- A **partial LRTactical compatibility framework** for melee items, consumables,
  detonators and several throwable behaviours.
- Built-in JEI/REI ammo queries and workbench categories where available;
  optional integrations vary by Minecraft release.

Exact compatibility and test status are listed on each uploaded file and in its
release notes. Third-party gun packs, tactical-equipment packs, optional mods and
shader packs must still be checked per version.

## Requirements

| Minecraft | Loader | Java | Required dependencies |
|---|---|---|---|
| 26.2 / 26.1.2 | NeoForge | 25+ | None |
| 1.21.11 | NeoForge | 21+ | None |

Files for different Minecraft releases are not interchangeable. This project
provides **NeoForge only**. For Fabric, use the sister project
[TaCZ Refabricated Unofficial](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated).

## Installing gun packs

Place modern gun packs in `.minecraft/tacz/`. Zip files may be loaded directly,
or packs may be extracted as folders. In both cases, `gunpack.meta.json` must be
at the pack root; do not wrap a zip in an extra directory.

Place legacy-layout packs in `.minecraft/tacz_backup/`, keep the originals, and
run `/tacz convert` in game. Conversion cannot repair every resource, recipe or
script difference automatically. Decide whether conversion is needed from the
pack structure, not only its advertised Minecraft version.

For multiplayer, install the same packs on server and clients. The server
supplies gameplay data; clients supply models, textures, animations, sounds and
translations. After adding a pack, run `/tacz reload` on the server and press
**F3+T** on clients.

## LRTactical and Arcana

The built-in LRTactical layer is a **partial compatibility framework**, not a
complete NeoForge distribution. It does not bundle the original art, does not
include `flash_shield`, and cannot guarantee that every LRTactical content pack
will work. Content packs must provide all assets they are licensed to distribute.

This project does not include **TacZ:Arcana** or implement its API or protected
asset-loading process. Packs that explicitly require Arcana are unsupported.
Missing models or purple/black textures alone do not prove an Arcana dependency;
incorrect directory nesting, resource paths, version constraints or incomplete
files can cause the same symptom.

## Known boundaries and issue reports

- Detection of a gun pack does not prove that all models, animations, recipes
  and scripts are compatible.
- Scope rendering is adapted separately for each branch; results can differ by
  graphics backend and shader pack.
- Hosting panels, proxy networks and hybrid Bukkit/mod servers are not covered
  by one compatibility guarantee. Reproduce issues on a plain NeoForge server.

When reporting a problem, first reproduce it in a minimal NeoForge environment
with this mod and attach the complete `latest.log` or crash report. For
multiplayer issues, include both server and client logs. Report port issues to
this project, not to TaCZ, TaCZ Refabricated or LRTactical authors.

## Credits and licensing

- Original: [MCModderAnchor / TACZ](https://github.com/MCModderAnchor/TACZ)
- Direct upstream: [Sh1roCu / TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated)
- NeoForge port skeleton reference (GPL-3.0, auxiliary — rendering code not adopted): [MUKSC / TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)
- LRTactical original: [LesRaisins Tactical Equipements](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements)
- Fabric sister project: [TaCZ Refabricated Unofficial](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated)
- This NeoForge port: source and issue tracker are available in this page's Links section

TaCZ, this port and the incorporated LRTactical code use GPL-3.0. The default gun
pack declares CC BY-NC-ND 4.0 for its assets. LuaJ uses MIT; Commons Math uses
Apache-2.0. Other code and assets retain their own
licenses. A code license does not automatically cover models, textures,
animations or sounds. See `LICENSE` and `LICENSES.md` in the source repository.

The mod ID remains `tacz` for existing gun-pack dependencies and save data. This
does not make the project official. Provided as-is, without warranty.
```

## 项目字段

| 字段 | 值 |
|---|---|
| License | GNU General Public License version 3 |
| Environment | Client & Server |
| Loader | NeoForge |
| Source | `https://github.com/q14433686-arch/TaCZ_Renovated` |
| Issues | `https://github.com/q14433686-arch/TaCZ_Renovated/issues` |
| Rewards Program | 关闭（默认枪包资源含 NC 条款） |
| Avatar | 使用本仓库原创 400×400 品牌图，不使用 TaCZ 官方图标 |

## 单个文件 Changelog 模板

```markdown
## TaCZ: Renovated [[完整版本号]]

**Environment:** Minecraft [[版本]] · NeoForge [[版本]] · Java [[版本]]

### Changes
- [[从该分支 CHANGELOG / Release 摘取本次新增、修复或变更]]

### Compatibility notes
- [[只写该文件已核验的枪包、可选模组、图形后端和联机状态]]
- [[未实测项目明确写 Not tested，不得写 Supported]]

### Upgrade notes
- Files for other Minecraft releases are not interchangeable.
- Back up worlds and gun packs before upgrading.
```

# Changelog

## Unreleased — 1.1.8+neoforge.26.2.0.r0

### Target

- Minecraft 26.2
- NeoForge 26.2.0.64 (release)
- Java 25
- Gradle 9.2.1 / ModDevGradle 2.0.144

### Changed

- Forward-ported the audited NeoForge 26.1.2 Beta-1 codebase to Minecraft/NeoForge 26.2.
- Migrated legacy NeoForge item handlers to transactional `ResourceHandler<ItemResource>` APIs.
- Migrated current-screen access to `Minecraft.gui`, retained NeoForge GUI-layer registration, and
  revalidated the minimal access transformer against the NeoForge 26.2 transformed compile classpath.
- Migrated custom rendering to 26.2 bind-group layouts, GPU formats, multiple color targets, vertex
  bindings, primitive topology, PreparedRenderType, Feature Rendering, PiP collectors, and shape outlines.
- Retained the OpenGL depth-aperture scope architecture, including Iris hand-pipeline classification
  and depth restore/mask shader bridge; updated comparisons for 26.2 reversed-Z.
- Added an explicit Vulkan fallback: no OpenGL depth-copy calls or GL-only custom pipeline
  registration; the opaque ocular is hidden and ordinary unmasked render types are used.
- Re-pinned optional 26.2 artifacts for Cloth, PAL, Controllable, Shoulder Surfing, JEI, REI and
  their compile dependencies.
- Updated Carry On compatibility to 2.11's `ItemStackTemplate#create()` rendering path.
- Added reflection-only First-person Model / Not Enough Animations handoff guards. Neither project
  currently publishes a NeoForge 26.2 file, so these guards are dormant rather than advertised support.
- Added `COMPATIBILITY.md` with source commits, artifacts and an explicit untested matrix.

### Removed

- Removed dead `GunPackProgressScreen`.
- Removed all use of NeoForge's deprecated-for-removal `IItemHandler` family.
- Removed redundant AT entries for members already public in 26.2.

### Known limitations / verification

- **No production JDK 25 Gradle build has run in this execution environment.**
- Dedicated-server `Done`, gun-pack load counts and the release jar are not yet verified.
- OpenGL, Iris and Vulkan require GPU testing; Vulkan has no depth-aperture effect.
- Optional compatibility entries are source/API-audited but not user PASS.
- Aperture and LRTactical are not included.

This entry must remain **Unreleased** until all gates in `docs/PORTING_STATUS.md` pass.

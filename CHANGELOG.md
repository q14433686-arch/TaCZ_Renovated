# Changelog

## Unreleased — 1.1.8+neoforge.26.2.0.r0

### Target

- Minecraft 26.2
- NeoForge 26.2.0.64 (release)
- Java 25
- Gradle 9.2.1 / ModDevGradle 2.0.144

### Changed

- Forward-ported the NeoForge 26.1.2 codebase to Minecraft/NeoForge 26.2, including the R1
  multiplayer fixes recovered after the original Beta-1 branch point.
- Changed both naturally-empty stacks in `ServerMessageGunDraw` to
  `ItemStack.OPTIONAL_STREAM_CODEC`, preventing tracking broadcasts from disconnecting players.
- Added `AttachmentsTagManager` and `RecipeFilterManager` to the network-cache listener list so
  attachment rules and block recipe filters reach multiplayer clients.
- Treats Iris' `Shader already assigned` response as successful existing classification.
- Moved gun, ammo, attachment and workbench `Item#getName(ItemStack)` overrides from client indices
  to common indices, preventing dedicated `/give` and display-name paths from loading client classes.
- Migrated legacy NeoForge item handlers to transactional `ResourceHandler<ItemResource>` APIs.
- Migrated current-screen access to `Minecraft.gui`, retained NeoForge GUI-layer registration, and
  revalidated the minimal access transformer against the NeoForge 26.2 transformed compile classpath.
- Migrated custom rendering to 26.2 bind-group layouts, GPU formats, multiple color targets, vertex
  bindings, primitive topology, PreparedRenderType, Feature Rendering, PiP collectors, and shape outlines.
- Replaced the interim OpenGL depth-aperture port with refab 26.2's stage-boundary off-screen ocular
  mask semantics, adapted to NeoForge pipeline and GUI-layer registration.
- Added backend-neutral mask target rendering, convex-hull ocular fill, sight/scope channel gating,
  unclipped ocular-ring redraw, inverse-clipped reticles, and shared gun/attachment/muzzle-flash clipping.
- Replaced the Iris depth-restore bridge with the refab linked-fragment mask branch and per-draw
  uniform/texture binding; shader replacements without a verified bridge fail open to ordinary rendering.
- Re-pinned optional 26.2 artifacts for Cloth, PAL, Controllable, Shoulder Surfing, JEI, REI and
  their compile dependencies.
- Updated Carry On compatibility to 2.11's `ItemStackTemplate#create()` rendering path.
- Added reflection-only First-person Model / Not Enough Animations handoff guards. Neither project
  currently publishes a NeoForge 26.2 file, so these guards are dormant rather than advertised support.
- Added `COMPATIBILITY.md` with source commits, artifacts and an explicit untested matrix.

### Removed

- Removed dead `GunPackProgressScreen`.
- Removed all use of NeoForge's deprecated-for-removal `IItemHandler` family.
- Removed the old raw-depth scope classes/mixins/shaders and their private `RenderType` constructor AT;
  retained only the three transformed gameplay members required by active 26.2 code.

### Known limitations / verification

- User-reported production JDK 25 `compileJava` and `build`: **PASS for commit `c40dab9`**.
  The subsequent scope-mask replacement changes the current HEAD and requires a fresh build.
- Dedicated-server `Done`, gun-pack load counts and the built jar's final contents are not yet verified.
- NeoForge's open ELS/Vulkan startup bug (`NeoForge#3230`) requires
  `config/fml.toml: earlyWindowControl=false`; the user reports Vulkan startup PASS with it disabled.
- The first Vulkan scope test exposed low-power reticles losing their mask when sight-body clipping was
  disabled; reticle containment and full-viewmodel clipping are now independent, pending rebuild/retest.
- Optional compatibility entries are source/API-audited but not user PASS.
- Aperture and LRTactical are not included.

This entry must remain **Unreleased** until all gates in `docs/PORTING_STATUS.md` pass.

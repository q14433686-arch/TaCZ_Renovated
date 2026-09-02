# 收枪（put-away）动画不渲染 · `keep()` 修复的 26.1.2（NeoForge）移植记录（2026-09-02）

> **状态**：本仓 `26.1.2`（NeoForge）侧**已落码**；编译门走 CI `compile-check`（本沙箱无 JDK/Gradle 工件，
> 无法本地编译）；**实机未验证**（沙箱无实机）。对外文案按 [`AGENTS.md`](../AGENTS.md) §2 写
> 「恢复 `keep()` 调用并修正其窗口守卫（待实测）」，**不得**写「已修复」。
>
> **货源**：姊妹项目（游戏语义权威）
> [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
> `26.1.2` 分支提交
> [`6a4c21c2`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/commit/6a4c21c2)
> （= 其 26.2 线 PR #87 的 26.1.2 移植；上游货源是外部 fork
> [`Legionoff/…GPT_Edition`](https://github.com/Legionoff/TaCZ_Refabricated_Unofficial_GPT_Edition)
> 的 `ca2b9fc`「Fix TACZ put-away animation rendering on Fabric 26.2」+ 姊妹项目维护者裁定的**两点加固**）。
> 姊妹侧完整论证见其 26.2(main) 线 `docs/lineage/SYNC_GUIDE_PUTAWAY_KEEP_20260902.md`。
>
> **性质**：同代码、同机制的语义移植件（本仓与姊妹项目在这 4 个文件上是同源代码，
> 差异只在加载器 API：NeoForge 事件总线 vs Fabric 注册表）。不是 GPU/纪元不可互抄件。

---

## 1. 症状与机制（为什么少了这一句就没有收枪动画）

本仓的第一人称视模由自己的 mixin 接管，「画哪把枪」的链条是：

```
ItemInHandRendererMixin#tacz$submitArmWithAnimatedItem（WrapOperation 包裹 renderArmWithItem）
  → FirstPersonAnimationCompat#getMainRenderStack(player)
    → KeepingItemRenderer.getRenderer().getCurrentItem()
      → keep 窗口内：tacz$KeepItem（旧枪）
      → 窗口外：    mainHandItem（vanilla @Shadow 字段，早已换成新枪）
  → geoRenderer.renderFirstPerson(... renderStack ...)
```

`LocalPlayerDraw#draw` 的时序是 `doPutAway(lastItem, putAwayTime)` → `doDraw(currentItem, …)`。
`doPutAway` 里 `tryExit` 触发 `INPUT_PUT_AWAY` 然后 `stateMachine.exit()` +
`setExitingTime(putAwayTime + 50)`；put_away 由 `AnimationController` 逐帧推进，
**前提是这把旧枪还在被提交渲染**。

没有人调 `keep()` 时：`getCurrentItem()` 只回落到 vanilla 的 `mainHandItem`（新枪）⇒
旧枪视模一帧都不再提交 ⇒ put_away 无处可画；同时新枪 `needReInit()` 立刻成立
（`!isInitialized() && exitingTime < now`，新枪是另一台状态机、`exitingTime` 默认 -1）⇒
`tryInit` 直接 `INPUT_DRAW`。观感就是「收枪动画被吞、切枪瞬间完成」。

## 2. 这不是本仓移植时删掉的：`keep()` 的注释是**继承**来的

`tryExit` 里那行 `keep(...)` 在直接上游 `Sh1roCu/TACZ-Refabricated` @ `1.21.1`
（`AnimateGeoItemRenderer.java`）与 `MCModderAnchor/TACZ` @ `1.20.1` 里**本来就是注释状态**，
姊妹项目三条分支同病同因。本仓 `AnimateGeoItemRenderer#tryExit` 与
`GunItemRendererWrapper#tryExit`（override 版）两处同样是注释 —— 本次**保持注释**，
只在两处补「不要打开」的说明：`keep()` **只能有一个调用点**。

`ItemInHandRendererMixin#tick` 的注入依旧**刻意留空**（其 javadoc 已解释：每 tick 强制写
`mainHandItem` / `mainHandHeight` 会打断切枪动画）。本次修复与它不冲突 ——
只在收枪那一刻写一次 `mainHandItem`，不再每 tick 钉死。

## 3. 本仓落码内容（4 文件，+60 / −4）

| 文件 | 改动 |
|---|---|
| `client/gameplay/LocalPlayerDraw.java` | import `KeepingItemRenderer`；`doPutAway` 里在 `tryExit` **之前**补带判定的 `keep(lastItem, putAwayTime)`（唯一现行调用点） |
| `client/renderer/item/AnimateGeoItemRenderer.java` | 新增 `hasInitializedStateMachine(ItemStack)`（与 `tryExit` 内部判定同源）；注释行补「不要打开」说明 |
| `client/renderer/item/GunItemRendererWrapper.java` | 同上的注释说明（override 版 `tryExit`） |
| `mixin/client/ItemInHandRendererMixin.java` | `keep()` 守卫语义修正（§4 加固 1）——只改 `@Unique` 方法体 |

**mixin 面**：目标类、`@Shadow` 字段（`mainHandItem` / `mainHandHeight` / `oMainHandHeight`）、
注入点（`renderHandsWithItems` 的 HEAD / RETURN、`renderArmWithItem` 的 WrapOperation、`tick` 的 HEAD）
**一个没动**，改的只有 `@Unique` 的 `keep` 方法体。26.1.2 未混淆，不涉 mappings。

**依赖面**：`ItemStack.isSameItemSameComponents` 本仓已在用（`api/item/IAnimationItem`、
`item/AmmoItem`、`lrtactical/item/ThrowableItem`），无新 API。

## 4. 两点加固（超出外部 `ca2b9fc` 原提交范围，与姊妹线一致）

### 加固 1：`keep()` 守卫 —— 从「窗口内一律忽略」改为「最新一次收枪接管」

改前（源自上游）：

```java
long time = System.currentTimeMillis() - tacz$KeepTimestamp;
if (time < tacz$KeepTimeMs) { return; }          // ← 窗口未过期就整条忽略
```

后果：连续快速切枪（A→B→C）时，第二次收枪**接管不了**窗口——上一把枪的剩余窗口继续生效，
第二把枪的 put_away 一帧都画不出来，窗口长度还比它需要的短。

改后：不同物品 → **接管**；同一物品且请求更长 → 延长；同一物品且请求更短 → 忽略
（保留原守卫里唯一良性的那一半：不截断正在播放的动画）。

```java
long now = System.currentTimeMillis();
boolean sameKeptItem = tacz$KeepItem != null
        && ItemStack.isSameItemSameComponents(tacz$KeepItem, itemStack);
if (sameKeptItem && now + timeMs <= tacz$KeepTimestamp + tacz$KeepTimeMs) { return; }
```

「接管不会用静止视模顶掉正在播放的动画」由加固 2 的调用点判定保证。

### 加固 2：调用点判定对齐上游 `isInitialized()` 语义

外部 `ca2b9fc` 的条件只是「`lastItem` 有 `AnimateGeoItemRenderer`」，比上游那两处注释
（写在 `stateMachine.isInitialized()` 之内）**宽**。差异场景 = 旧枪状态机从未初始化
（刚进世界、第三人称下切枪、上一把枪的窗口未过期所以这把从没被画过）：会开出一个
**没有 put_away 可播**的空窗口，表现为「旧枪静止一瞬再切新枪」，比不开更糟。

落地方式不是在 `doPutAway` 里重写状态机逻辑，而是把上游那条判定暴露成
`AnimateGeoItemRenderer#hasInitializedStateMachine(ItemStack)`（与 `tryExit` 内部判定同源）。
注意 `getStateMachine` 在 `GunItemRendererWrapper` 里是 override（按枪取
`GunDisplayInstance#getAnimationStateMachine`），所以枪械走的是**每把枪自己的**状态机。

### 波及面（行为扩大，非纯 bug 修复）

`doPutAway` 的入口判定是 `instanceof AnimateGeoItemRenderer`，而本仓内置 LRTactical 的
`MeleeItemRenderer` / `ThrowableItemRendererWrapper` / `ConsumableItemRenderer`
（`src/main/java/me/xjqsh/lrtactical/client/renderer/item/`）都继承它、都 override 了
`getStateMachine(stack)`、都**没有** override `tryExit` ⇒ 三族一并获得 keep 窗口。
它们此前同样从来没有 keep 窗口（同因），本次顺带覆盖 —— 属**行为扩大**，须单独实测（§5 第 7 项）。

## 5. 静态检查（本工作区实跑，2026-09-02）

| 检查 | 结果 |
|---|---|
| `python3 docs/check_mixin_registration.py` | rc=0（44 registered / 42 classes / 5 intentionally unregistered） |
| `python3 docs/check_lang_keys.py` | rc=0（HEAD 超集，320 字面量键全命中） |
| `python3 docs/check_mesh_config_parity.py` | rc=0（toml 19 / 面板 19 / 语言键 38，齐平） |
| `bash scripts/check_release_consistency.sh` | 一致（`gradle.properties` 未动 ⇒ 不触发 AGENTS.md §1 的六处同步） |
| `./gradlew compileJava` | 走 CI `compile-check`（沙箱无 JDK） |

> 姊妹侧 `ca083b5d`（面板语言键改名 `mesh_gpu_bake_budget` → `mesh_gpu_bake_budget_per_frame`）
> **本仓不适用**：本仓这三处从落地起就是 `…_per_frame`（`RenderClothConfig` + en_us/zh_cn），
> mesh parity 本来就是绿的。姊妹侧同期其余提交均为文档 / CI（`556dfea2`、`96066400`、
> `fb74b3fb`、`1519ae59`、`6016d708`、`3ac189a5`），无实质代码。

## 6. 落码之后必须实测的项（**目前全部未验证**）

1. **基本项**：切枪（枪→枪、枪→徒手/普通物品、普通物品→枪）能看到旧枪的收枪动画，
   且抬枪动画在其之后开始，不是同帧抢跑。
2. **keep 窗口的其他消费者**（本仓实拉：`CameraSetupEvent` 4 处、`FirstPersonRenderGunEvent` 1 处、
   `ScopePipRenderState` 3 处）：窗口内 `getCurrentItem()` 返回**旧枪**，
   故开镜相关要 A/B —— 开镜中切枪 / 切枪瞬间开镜 / 快速来回切两把带镜枪。
   本线是**深度孔径纪元**（`ScopePipRenderState`），与 26.2 的掩码/PIP 纪元不可互抄结论。
3. **光影（Iris）项**：本线手部相位门禁是 `ShaderCompat#shouldRenderInCurrentHandPhase(renderStack)`；
   收枪窗口内旧枪只应提交一遍（不闪烁、不双影）。另注意 mesh GPU 路径的
   `tacz$drawMeshGpuAfterHandFeatureFlush` 钩子在窗口内画的是旧枪。
4. **加固 1**：快速连切三把枪（A→B→C，间隔小于一把枪的 putAwayTime）——
   期望 A 的收枪播完 → C 的抬枪，B 被跳过；**不应**出现「静止的 B 挂在手上」或「A 的动画被截断」。
   再试 A→B→A，期望动画不被截短。
5. **加固 2**：刚进世界的第一次切枪、第三人称下切枪、拿枪掉线重连后立刻切枪 ——
   期望**不**出现旧枪静止一瞬的空窗口。
6. **`mainHandItem` 被写一次的副作用**：与 Viewmodel Changer / Hide Hands / SkyHands 等
   同样动 `ItemInHandRenderer` 的模组共存时，收枪窗口内不应出现双手/无手/错位。
7. **LRTactical 三族（行为扩大项）**：近战、投掷物、消耗品「用一半切走」时应能看到收招动画播完
   再切新物品；`getPutAwayTime` 的单位差异（近战/消耗品 tick×50、投掷物本身即毫秒）
   在窗口长度上应表现正常。

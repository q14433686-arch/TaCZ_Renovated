# 第六轮同步 Fabric 26.2 线（2026-09-02 · 收枪 `keep()` 动画修复）

日期：2026-09-02
作者：Arena agent（本仓 `arena/01a062df-tacz-renovated`，26.2 / NeoForge 26.2.0.64）
上一取货点：`dee2578d`（R5，2026-09-02；见 `REFAB_SYNC_0105E3E_R5_20260902.md`）
本次取货点：`a408eb00`（`q14433686-arch/TaCZ_Refabricated_Unofficial` 分支 `26.2(main)` 尖端，
Merge PR #87，Fabric 26.2 / Java 21）

> 方向：**Fabric 26.2（游戏语义权威线，AGENTS §0）→ 本仓 NeoForge 26.2**。只取游戏语义，
> 不复制 Fabric API 表面（§3 红线 3）。
>
> 证据级别：**静态（读码）+ 逐 hunk 比对**。本线 CI 编译门与实机**均未跑**（沙箱无 JDK），
> 按 AGENTS §2 只写「已移植、待编译/实机验证」，不写「已修好」。

## 1. `dee2578d..a408eb00` 的实质提交对账

逐 commit 读 `git show <sha>` 全文后分类：

| 提交 | 内容 | 本仓处置 |
|---|---|---|
| `169a525a` | fix(mesh)：高模枪身镜内裁剪此前从未生效（判据绘制期恒 false 时序 bug，改问帧快照） | **上轮已搬**。工作树已有 `ScopeMaskRenderer#viewmodelClipMaskThisFrame` + `ScopeBodyRenderTypes#maskReadyForViewmodelAtDraw/clipForViewmodelAtDraw` + `PolyMeshGpuRenderer` 两处绘制时判据与 log-once `GPU hand mesh pass: ocular clip ACTIVE`；记录见 `BUG_MESHGUNBODY_SCOPE_CLIP_RERENDER_20260902.md`（本仓该修复在 NeoForge 侧先落地，她提交信息「与 26.2 Neo 姊妹线 99253c5 同因同修」即指本仓） |
| `ffe45485` | fix(animation)：收枪 `doPutAway` 补回 `keep()`，让旧视模在 put-away 窗口内仍可被提交 | **已搬**（随 `32af4025` 终态一并落地） |
| `32af4025` | fix(animation)：`keep()` 守卫改「最新一次收枪接管」+ 调用点 `hasInitializedStateMachine` 对齐上游 `isInitialized()` 判定 | **已搬**（终态） |

其余（`997ade18`、`7ca71be6`、`ff701184`、`1ff84c1d`、`fcd3b4a5`、`bd234d0b` 等）为
docs / ci-log / 删 zip+log，无游戏语义，不搬。

## 2. 已搬件的代码落点（与她对表）

| 件 | 文件 | 关键符号 |
|---|---|---|
| 收枪 `keep()` 调用点 + `hasInitializedStateMachine` 判定 | `client/gameplay/LocalPlayerDraw.java` | `if (renderer.hasInitializedStateMachine(lastItem)) KeepingItemRenderer.getRenderer().keep(lastItem, putAwayTime)` |
| 状态机初始化判定暴露 | `client/renderer/item/AnimateGeoItemRenderer.java` | `hasInitializedStateMachine(ItemStack)` |
| 两处 `tryExit` 的「唯一调用点、不要打开」注释 | `AnimateGeoItemRenderer` + `GunItemRendererWrapper` | `// keep() 的唯一现行调用点在 LocalPlayerDraw#doPutAway` |
| `keep()` 守卫语义修正 | `mixin/client/ItemInHandRendererMixin.java` | `sameKeptItem && now + timeMs <= tacz$KeepTimestamp + tacz$KeepTimeMs` 才 return；否则「不同物品接管 / 同物品更长则刷新」 |

与她的终态**逐 hunk 一致**，仅两处刻意差异（见 §3）。

## 3. 刻意偏离（全部为线况差异，非语义差异）

1. **注释里的文档指路改成本线**：她注释写「26.2(main) 线的
   `docs/lineage/SYNC_GUIDE_PUTAWAY_KEEP_20260902.md`」。本仓无 `docs/lineage/`，
   同步取证在 `docs/records/`，故三处注释改写为「本线
   `docs/records/REFAB_SYNC_PUTAWAY_KEEP_R6_20260902.md`」——内容同义，只改路径。
2. **Fabric 表面不复制**：她的 `LocalPlayerDraw` 里 `ClientPlayNetworking.send` /
   `BlockableEventLoopAccessor#tacz$submitAsync` / `compat.fabric.BuiltinItemRendererRegistry`
   本仓对应位是 `ClientPacketDistributor.sendToServer` / `Minecraft#submit` /
   `client.renderer.item.BuiltinItemRendererRegistry`——本仓已有，未动。`keep()` 与
   `hasInitializedStateMachine` 本体无加载器专属代码，逐字同构。

## 4. 不搬清单（含理由）

| 姊妹件 | 理由 |
|---|---|
| `docs/lineage/SYNC_GUIDE_PUTAWAY_KEEP_20260902.md` + `HANDOFF_LEDGER.md` + `docs/patch/*.patch` | 她线内务。两份 `.patch` 目标是 refab 的 26.1.2 / 1.21.11，与本仓无关；本仓用本记录对账 |
| `fcd3b4a5` 六线 CI 上线清单 / `bd234d0b` CI 结果回填 / 各 ci-log | 文档与 CI 门禁；本仓 `.github/workflows/` 属仓库所有者人工流程（与前几轮口径一致，本轮不动 workflow） |

## 5. 继承/波及（与她对表）

- **LRTactical 三族行为扩大**：`MeleeItemRenderer` / `ThrowableItemRendererWrapper` /
  `ConsumableItemRenderer` 均继承 `AnimateGeoItemRenderer` 且 override `getStateMachine(stack)`
  （读码确认），此前同样无 keep 窗口，本次一并覆盖——这是**行为扩大**，不是纯 bug 修复，
  实测清单需单独过（§6 第 7 项）。
- **`tacz$KeepItem`/`mainHandItem` 写入的共存副作用**：与 Viewmodel Changer / Hide Hands /
  SkyHands 等同样动 `ItemInHandRenderer` 的模组共存时的表现，属待实测项（§6 第 6 项）。

## 6. 待验证（未跑前不得宣称已修）

1. **收枪动画**：切枪时旧枪 put_away 动画正常播放（不再瞬间完成 / 被吞）。
2. **收枪→抬枪次序**：窗口过期后新枪 `INPUT_DRAW` 正常触发。
3. **光影（Iris）**：收枪窗口内旧枪只提交一遍（不闪烁、不双影）。
4. **加固 1（守卫接管）**：快速连切三把枪 A→B→C（间隔 < putAwayTime），期望 A 收枪播完 →
   C 抬枪、B 被跳过，**不**出现「静止的 B 挂手」或「A 动画被截断」；同枪连续收放 A→B→A
   动画不被截短。
5. **加固 2（调用点判定）**：刚进世界第一次切枪、第三人称下切枪、掉线重连后立刻切枪，
   期望**不**出现旧枪静止一瞬的空窗口。
6. **`mainHandItem` 副作用**：与 Viewmodel Changer / Hide Hands / SkyHands 共存时收枪窗口内
   双手/无手/错位。
7. **LRTactical 三族**：近战 / 投掷 / 消耗品「用一半切走」时收招动画播完再切新物品；
   消耗品窗口来自 `getPutAwayTime() * 50L`，若窗口长短异常先查单位换算（不在本次改动内）。

## 7. 需要回给 Fabric 26.2 线的话

1. `dee2578d..a408eb00` 的 3 笔实质提交本仓已逐笔对账：`169a525a` 上轮已搬（本仓先落，
   即你提交信息所指的 Neo 姊妹线）；`ffe45485`+`32af4025` 已按终态等价落地（落点见 §2，
   逻辑逐字同构，仅注释里的文档路径与加载器表面不同）。
2. 你侧 `docs/lineage/SYNC_GUIDE_PUTAWAY_KEEP_20260902.md` 是 refab 线内务，本仓用
   `docs/records/REFAB_SYNC_PUTAWAY_KEEP_R6_20260902.md` 对账，不复制其文档树。
3. 本线 CI 门禁（workflow）按仓库所有者流程由人手动跟进，本轮不复制 `fcd3b4a5` 的
   六线 CI 清单。

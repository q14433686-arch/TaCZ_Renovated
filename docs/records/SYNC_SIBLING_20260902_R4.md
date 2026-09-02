# 姊妹线同步记录：2026-09-02 第四轮（R4）

- **对象**：姊妹项目 [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)（Fabric）
- **比对时点**：2026-09-02，姊妹线 tip
  - `1.21.11` = `6db3af93`（对应本分支的同世代线）
  - `26.1.2` = `a1e469b6`
  - `26.2(main)` = `a408eb00`
- **本线基线**：`cb8c8a84`（`arena/01a062f7-tacz-renovated`，源自 `1.21.11`）

## 1. 比对方法

1. `git log --since=2026-08-29 -- src` 逐分支列出 **只碰 `src/`** 的提交
   （docs / `ci-log:` / workflow 提交不产生代码差异，全部排除）。
2. 把姊妹 `1.21.11` 分支的 `src/main/java` 整树与本仓对拍（`diff -rq`），
   确认上一轮移植后两线仅剩 loader 侧差异（`cn.sh1rocu.tacz.compat.fabric` /
   `net.neoforged.*`、事件总线、`BuiltinItemRendererRegistry` 包位）。
3. 对本轮新增的每个 commit 读全量 diff，判定是否属于「实质性且适用于本世代」。

## 2. 判定结果

| 姊妹提交 | 分支 | 结论 | 处置 |
|---|---|---|---|
| `b8041ab9` fix(animation): restore put-away rendering on 1.21.11 | 1.21.11 | **实质、适用** | 已移植（4 文件） |
| `ca083b5d` fix(config): 面板语言键名跟随 toml 键蛇形 | 26.1.2 | **实质、适用**（本线含同一 Cloth 条目与语言键） | 已移植（3 文件） |
| `6a4c21c2`（26.1.2 的 put-away 移植）/ `ffe45485`+`32af4025`（26.2 原始两笔） | 26.1.2 / 26.2 | 与 `b8041ab9` 同一修复的兄弟线形态 | 由 `b8041ab9` 覆盖，不重复 |
| `169a525a` fix(mesh): 高模枪身镜内裁剪判据改问帧快照 | 26.2 | **不适用** | 该修复针对 26.2 的 `ScopeMaskRenderer` / `ScopeBodyRenderTypes` 架构；本线（与姊妹 1.21.11 线一致）走 **深度孔径 + `armClipped`/`shouldClipViewmodel`** 架构，无 `ScopeMaskGeometry` 绘制期清空的时序问题（姊妹线该文档亦明确写「`1.21.11` / `26.1.2` 不受影响」） |
| `3151adcd` / `dc24a2b7` / `a810f6ef`（26.2 的 PIP 烘焙裁定与回移植） | 26.2 | 已在 | 本线随 1.21.11 线 `237dc153` 已带（`PolyMeshGpuRenderer` 镜内不拒收 + 镜内清表 + `worldConsumedFrame` 只在主遍记） |
| `71ced107` / `96066400` / `fb74b3fb` / `1ff84c1d` 等 | 各线 | docs / CI | 不产生代码差异 |
| `2a86838a` / `39e49cb1` / `4f124ef5`（workflow） | 1.21.11 | 本仓已有等价 workflow（`build.yml` / `consistency.yml` / `compile-check.yml`） | 不动 |

## 3. 本轮改动清单

### 3.1 收枪动画（`b8041ab9` 等价移植）

- `client/gameplay/LocalPlayerDraw.java`：`doPutAway` 内、`tryExit` 之前，
  在 `renderer.hasInitializedStateMachine(lastItem)` 成立时调
  `KeepingItemRenderer.getRenderer().keep(lastItem, putAwayTime)`。**唯一调用点。**
- `client/renderer/item/AnimateGeoItemRenderer.java`：新增
  `hasInitializedStateMachine(ItemStack)`；`tryExit` 里注释掉的 `keep()` 加注保持注释。
- `client/renderer/item/GunItemRendererWrapper.java`：同上加注。
- `mixin/client/ItemInHandRendererMixin.java`：`keep()` 守卫改为「最新一次收枪接管」——
  仅在 `ItemStack.isSameItemSameComponents(tacz$KeepItem, itemStack)` 且
  `now + timeMs <= tacz$KeepTimestamp + tacz$KeepTimeMs` 时 return。

移植差异：姊妹线 import `cn.sh1rocu.tacz.compat.fabric.BuiltinItemRendererRegistry`，
本线为 `com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry`；
其余逐行等价（`KeepingItemRenderer`、`ItemStack` API 在两个 loader 下同名同签名）。

### 3.2 Cloth 语言键改名（`ca083b5d` 等价移植）

`compat/cloth/client/RenderClothConfig.java` 两处字面量 + `en_us.json` / `zh_cn.json`
各两个键名：`mesh_gpu_bake_budget` → `mesh_gpu_bake_budget_per_frame`。
显示文本、字段绑定、落盘键（toml `MeshGpuBakeBudgetPerFrame`）均未变。

## 4. 证据级别与实机口径

- 全部为**静态等价移植**（姊妹线同名文件逐行对照 + 本仓语法/JSON 校验）。
- **实机未验证**。收枪动画的实机判据（沿用姊妹线）：
  连续快速切换两把枪时，旧枪的 `put_away` 动画完整播放，不被打断、不异常加速、
  不出现「旧枪静止一瞬」；窗口结束后新枪的 `draw` 正常起。
- 语言键改名为机械改名，面板显示文本不变。

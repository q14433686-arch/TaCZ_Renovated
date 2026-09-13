# 同步取证：Fabric 1.21.11 线最新修复 → NeoForge 26.2（2026-09-13）

冻结记录。本轮从姊妹仓库 `q14433686-arch/TaCZ_Refabricated_Unofficial`
的分支 `arena/01a09a6c-tacz-refabricated-unofficial`（1.21.11 线，尖端 `469f646`）
取货，逐 commit 判定处置。版本号由 `1.1.8+neoforge.26.2.R3-hotfix` 升到
`1.1.8+neoforge.26.2.R3-hotfix2`。

## 0. 货源清单与处置

货源分支相对其上一次合并点 `680fbc81` 的增量共 4 个 commit：

| 货源 commit | 内容 | 本线处置 |
|---|---|---|
| `61ab4a09` | 修复第一人称手部错位：中和 vanilla 1.21.9+ 手臂 `zRot=±0.1` | **已移植**（§1） |
| `b3a01b14` | ci-log：`61ab4a09` 编译成功 | 不适用（对方 CI 产物） |
| `86d32db1` | 1.21.11 `mod_version` 升 `R3-hotfix2` | **等价执行**（§2，本线自己的版本串） |
| `469f646c` | ci-log：`86d32db1` 编译成功 | 不适用（对方 CI 产物） |

该分支再往前是 1.21.11 的 Hold My Items 5.x 兼容集
（`3164deb3` + `07681312` + `7daac625` + `ff9c3278`）——**登记未做**，见 §3。

Fabric 26.1.2 线已在 `44bb362a` 做过同一轮同步（移植 `61ab4a0` + 版本号
`R3-hotfix2` + 登记 HMI 未做）。本轮与其结论一致，是三线同形。

## 1. 已移植：第一人称手部错位（全枪械）

### 1.1 症状

第一人称下所有手枪整体偏左、手没握住枪；两把默认双管换弹时手部绑定/动画错位、
弹药悬浮在手上方。错位量恒定、非常规律。1.21.1 上游无此问题，
26.2 / 26.1.2 / 1.21.11 三线全复现 —— 这个「跨加载器、跨 MC 版本同时出现」本身
就指向 vanilla 侧的共同变更，而不是某一线的移植遗漏。

### 1.2 根因（货源指认，本线复核）

vanilla 在 1.21.1 → 1.21.9 的渲染重构里给 `AvatarRenderer#renderHand` 新增两行：

```java
model.leftArm.zRot  = -0.1F;   // 约 -5.7°
model.rightArm.zRot =  0.1F;   // 约 +5.7°
```

货源 commit 对 1.21.11 反编译源码逐行确认，并核对 1.21.9 / 1.21.10 / 26.1.2 同样存在；
1.21.1 的 `PlayerRenderer#renderArm` 没有这两行，手臂是笔直渲染的。

TACZ 全部枪模的手部定位（`righthand_pos` / `lefthand_pos`）都是按 1.21.1 的
`zRot=0` 姿态 authored 的。手臂网格绕肩部 pivot 凭空多转 ±5.7°，手相对枪恒定偏转。

**这不是「上游纪元机制失效」（AGENTS.md §3.8）**：失效类的特征是「只在 1.20.1/1.21.1
存在的机制在 26.2 没有基座」，处置是不修；本案相反 —— 是 vanilla **新增**了一笔写入，
覆盖在 TACZ 自己的手部定位之上，属普通缺陷，按正常流程修。

### 1.3 修法

`com.tacz.guns.util.RenderHelper#renderFirstPersonArm`（collector 重载）在每次
vanilla 手部调用之后、flush 之前调用新增的 `resetFirstPersonArmLean(avatar)`，
把两条手臂的 `zRot` 清零：

```java
private static void resetFirstPersonArmLean(AvatarRenderer<?> avatar) {
    if (avatar.getModel() instanceof PlayerModel playerModel) {
        playerModel.leftArm.zRot = 0.0F;
        playerModel.rightArm.zRot = 0.0F;
    }
}
```

时序依据（26.2 侧既有结论，本文件不新造）：`submitModelPart` 只拷贝**矩阵**
（`Pose#copy`），`ModelPart` 是**活引用**，旋转要到 `submitHandsWithItems` 之后的
`renderAllFeatures` 才被读取 —— 所以 submit 之后、flush 之前的写入决定最终姿态。
这条机制正是 `RenderHelper` 原注释里「第 5 轮：submit 之后绝不能还原 PlayerModel」
所依据的同一条。

两条必须一起清：vanilla 每次调用同时污染左右两条（调右手也写左臂），
连续两次提交时后一次会把前一条重新污染。

### 1.4 与本线既有结论的关系（防回滚）

- **与「第 5 轮」不矛盾**：第 5 轮禁止的是把 vanilla 整备好的 `visible`/pose 还原掉
  （会重现手臂残缺）；本轮中和的是 vanilla **新增**的一笔写入，且只动 `zRot`。
  同一机制、相反方向，是刻意的。代码注释里两处互相指认。
- **与镜内裁手无关**：`wrapForScopeClip` 只决定 collector 是否套代理（换 RenderType），
  与骨骼姿态无关；清零无条件执行，两种 collector 下都需要。
- **与 `PlayerModelMixin` 第 0 帧手臂归零不重叠**：那段只在
  `setupAnim` 的 `ageInTicks == 0` 分支生效，而 `renderHand` 的 `±0.1` 是
  `setupAnim` **之后**写的，覆盖不到。
- **不影响 vanilla 物品**：`renderFirstPersonArm` 只在 TACZ 接管 viewmodel 时被调用
  （`ItemInHandRendererMixin#tacz$submitArmWithAnimatedItem` 拦下 vanilla
  `submitArmWithItem` 之后），TACZ 接管的 flush 里不存在 vanilla 手臂提交
  （主手接管时副手被拦、TACZ 从不渲染副手）；纯 vanilla 的 flush 走不到这里。

### 1.5 本线适配差异

| 项 | 1.21.11 货源 | 本线（NeoForge 26.2） |
|---|---|---|
| 落点方法 | `RenderHelper#renderFirstPersonArm(5 参 collector 重载)` | 同名同签名的 collector 重载（本线只有这一个真正调用 vanilla 的重载） |
| 局部变量名 | `renderer` | `avatar` |
| 另一重载 | `@Deprecated` 空实现 | 同（`renderFirstPersonArm(player, arm, poseStack, light)`，注释写明 legacy） |
| 接管点名字 | `renderHandsWithItems` / `renderArmWithItem` | `submitHandsWithItems` / `submitArmWithItem` |
| 混淆 | 1.21.11 混淆，需 remap | 26.2 非混淆，直接用官方名 |

`PlayerModel` 的 import 为新增（`net.minecraft.client.model.player.PlayerModel`），
本线 `PlayerModelMixin` 已在用同一类名，路径无歧义。

### 1.6 验证状态（不得外推）

- **编译**：本沙箱无 JDK/Gradle 缓存，未本地编译。等 `compile-check` workflow 回写
  `build-reports/compile-java.log`。货源那条已过 1.21.11 线 CI 编译门，
  **不把对方 CI 写成本线 PASS**。
- **实机**：未执行。待跑清单：
  1. 第一人称手枪（默认包任一）：手掌与握把贴合、不再整体偏左；
  2. 双管霰弹换弹：手部与弹药对位，弹药不再悬浮；
  3. 左右手主手（`options.mainHand` 两档）各跑一遍；
  4. 双持/快速切枪连续提交两次，第二次不回退到偏转姿态；
  5. 手持 vanilla 物品（剑/方块/空手）第一人称手臂姿态与原版一致（回归）；
  6. 第三人称持枪与 GUI 缩略模型无变化（回归，`PlayerModelMixin` 路径）；
  7. 开镜（镜内裁手生效）下手臂仍正确裁剪且不偏转；
  8. 装 Player Animation Library / NEA 时第一人称手臂无异常（回归）。

## 2. 版本号：`R3-hotfix` → `R3-hotfix2`

命名沿用 R2 那一轮的规矩：`R<n>` → `R<n>-hotfix` → `R<n>-hotfix2`，小写、序号直接
接在 `hotfix` 后面，中间不放 `.` / `-` / `_`。理由有两条，与 Fabric 侧一致：

1. TaCZTweaks 按版本号字符串识别本项目；
2. SemVer 里多一个 `-` 会把后续内容拽进 pre-release 段，破坏「`>=1.1.8` 必须成立」
   这条枪包加载前提（本线 `gradle.properties` 注释里的既有 trap 说明同源）。

同步位置（AGENTS.md §1）：

- `gradle.properties`：`mod_version`，并补上上述命名格式注释；
- `README.md`：顶部版本句、支持表 26.2 行、第 7 节 SemVer 说明（3 处）、文档表
  CHANGELOG 行；
- `CHANGELOG.md`：新增 `1.1.8+neoforge.26.2.R3-hotfix2 — 2026-09-13（未发布）` 条目；
- `docs/PORTING_STATUS.md`：当前源码版本与版本基线段；
- `AGENTS.md`：当前版本行，并写入热修标签格式。

`src/main/templates/META-INF/neoforge.mods.toml` 用 `${mod_version}` 占位，无需改。
依赖版本未变，支持表的依赖格子与可选集成表不动。

自检：`bash scripts/check_release_consistency.sh`（见 §4）。

## 3. 登记未做：Hold My Items 5.x 兼容旁路

货源 `3164deb3` + `07681312` + `7daac625`（维护者 2026-09-12 已在 1.21.11 实机通过，
与 HMI 5.1 同装、手持枪械不再崩溃）。`ff9c3278` 的 `RawOutput.log` 是 1.21.11 的
intermediary 崩溃日志，不是修复本体，映射也与本线（非混淆）不符 —— 一并不同步。

**本线不预先移植**，沿用货源 `docs/HOLD_MY_ITEMS_COMPAT.md` §6.1 的判断：

- 机制：HMI 的 `com.holdmylua.source.mixin.safety.JavaMethodMixin` 在
  `org.luaj.vm2.lib.jse.JavaMethod#invokeMethod` 的 `RETURN` 处，把没有它私有
  `@Safe` 注解的方法返回值一律改写成 `LuaValue.NIL`；类加载器全 JVM 只有一份
  `JavaMethod`，于是 TaCZ 的 `context:xxx()` / `api:xxx()` 全部返回 nil，
  枪包状态机脚本崩在 `nil <= 0`。
- 代价/收益：探针判定健康（未装 HMI）时那套代码**什么都不做**，零收益；
  代价是多一个打进第三方库类的 mixin（不可静态验证的运行期面）+ 本线重跑整轮
  验证矩阵。
- 26.x 上 HMI 目前只有第三方反编译移植（`ThinkofRain1213/HMI-26.2`，保留 modid
  `holdmyitems` 与同一段 `safety.JavaMethodMixin`），官方是否出 26.x 版本未知。

**触发条件（满足任一条即移植）**：

1. 本线玩家日志出现同一签名：TaCZ 的 Lua 脚本报 `attempt to compare/index ... nil`，
   且同环境装有 HMI 或同类全局 Lua 沙箱；
2. HMI 官方发布 26.x 版本并保留该 safety mixin。

移植时需注意：mixin 目标是库类、不含任何 Minecraft 名字，可原样搬；但注册条目要落到
本线自己的 mixin 配置，且包名段要让 modid 过滤规则继续生效；若 HMI 的 modid 变了，
过滤规则跟着改。

## 4. 一致性自检

```bash
bash scripts/check_release_consistency.sh
bash scripts/check_release_consistency.sh --strict
```

结果记录在本轮提交信息中。脚本按 `mod_version` 字符串在 README（>=3 次）与
CHANGELOG（>=1 次）出现次数判定，`-hotfix2` 后缀不需要改脚本。

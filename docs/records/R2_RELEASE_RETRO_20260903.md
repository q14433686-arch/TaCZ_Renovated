# R2 发布复盘：24 小时内三个二进制共用一个名字（2026-09-03）

> 起因：维护者自述「R2 的 release 改了又改，构建文件换了三次」，要求查清频率与原因。
> 本文全部结论来自可查证据：GitHub Release/Events API、三分支 commit 史、
> Actions 流水记录、维护者上传的实机 `latest.log`（commit `fce3d76`）。
> 冻结快照，事后不改写。

## 1. 时间线（北京时间 UTC+8；证据栏为查证途径）

| 时间（9-2 → 9-3） | 事件 | 证据 |
|---|---|---|
| 06:05 | PR #29 合并 = R2 本体（渲染线 v1–v5 + 版本号 bump `1d01d14`） | commit `436486c` |
| **07:14–07:33** | **三线 R2 发布，jar 第①代上传**。当时仓库尚无 jar 构建 CI（只有 compileJava 门），此代为本地构建 | ReleaseEvent `published` ×3（Events API） |
| 23:56–00:03 | `build.yml` 紧急上线，7 分钟内 3 commit 连改（Create → 修分支名笔误 `26.2(main)`→`26.2` → 注释头重写），另改 compile-check | `f51d12f` `f4eaf8d` `31001d1` `bcbc1b6` |
| 00:32 / 00:33 / 00:42 | 三线合入收枪 put-away 动画修复（PR #32/#33/#34，同步姊妹线 `6a4c21c2`）→ **jar 第②代（第 1 次替换）**，首批 CI 产物 | build.yml 流水记录 |
| 01:51 | 维护者实机 LAN，第二名玩家 GOOSTL 加入即被踢：`Failed encoding custom payload neoforge:recipe_content: Empty ItemStack not allowed` | `latest.log` L573–651 |
| 01:55 | `latest.log` 网页上传至 26.1.2 分支 | `fce3d76`（"Add files via upload"） |
| 02:07–02:23 | 断连修复三线落地（PR #35/#36/#37，`GunSmithTableSerializer` 换 `ItemStack.OPTIONAL_STREAM_CODEC` + `Serializers` 实体数据同类修） | `ff348b0` 等 |
| **02:28:38–02:29:53** | **三线 jar 各替换（第 2 次替换），82 秒批量完成**，即现役资产 | 资产 `createdAt`（Release API） |

**频率**：发布 → 最终形态 19h15m；一个 tag 下 3 个世代；替换间隔 ≈17.3h 与 ≈1.9h；
三线并行 = 24h 内手动搬运 9 个 jar。

> 注：GitHub 不保留被替换资产的历史，API 只能看到现役一代。若维护者记忆为
> 「替换三次（四个世代）」，多出的一次已不可考（最可能是发布当晚的即时重传）。

## 2. 根因

### 两个真 bug（各触发一次替换）

1. **收枪动画被吞**（第②代）：上游继承缺陷——`LocalPlayerDraw#doPutAway` 的
   `keep()` 调用在上游即被注释，切枪瞬间完成。姊妹线 9-2 白天先修，当晚三线同步。
   单机可见，故发布次日即被抓到。
2. **LAN 断连**（第③代）：`GunSmithTableSerializer` 用非 optional 的
   `ItemStack.STREAM_CODEC`。**非 R2 回归**——该文件自 8-21 R0 初版（PR #3）后
   从未改动，雷埋了 12 天。它只在「配方全量同步给加入的玩家」时爆炸，
   即**必须有第二名玩家进世界才触发**；单机验证与渲染向实测永远踩不到。

### 一个流程放大器（为什么体感是「改了又改」）

- **三个不同二进制共用同一版本号与文件名**（`1.1.8+neoforge.26.1.2.R2` /
  `tacz-1.1.8+neoforge.26.1.2.R2.jar`），"R2" 成了移动目标，无法从文件名分辨世代，
  且无 sha256 留痕；
- 发布时**没有 jar 产物 CI**：第①代本地打包，`build.yml` 是热修中途才上线的——
  基础设施变更与两波热修压进同一个 24h；
- 三线并行、全手动上传：每波修复 ×3 搬运。

## 3. 已固化的改进（本次落地）

1. **`docs/publish/ci/release-assets.yml`（待上线稿）**：一键
   构建 → `--strict` 门禁 → 上传 → Release 正文自动追加世代记录
   （UTC 时间 / commit / 文件名 / sha256）；默认**禁止同名换弹**（同名资产存在即失败，
   指引先 bump `R2 → R2.1`；`allow_replace` 仅限传错当场重传）。
   上线与使用教程：[`../publish/ci/README.md`](../publish/ci/README.md)。
2. **热修必 bump build metadata**：`+` 后加段（SemVer build metadata）不影响
   枪包 `>=1.1.8` 检查——这是本仓自己在 CHANGELOG 头部论证过的规则的自然延伸。
3. **发布前冒烟增加「LAN 双人加入」项**：已写入 `RELEASE.md` §4 核对清单。
   本次断连类缺陷在单机日志零痕迹；R1 时代（8-21）的 LAN 测试结论不迁移到
   配方数据已变的 R2（RELEASE.md §1 本有「测试结论不跨版本继承」，跨世代同理）。

## 4. 遗留

- 26.2 / 1.21.11 两线的 `release-assets.yml` 适配与上线（要点见模板注释头）。
- ~~`GunSmithTableSerializer` 同类雷的全量排查~~ **已闭环（本轮扫描）**：全仓其余
  `ItemStack.STREAM_CODEC` 非 optional 用法共 6 处（`ServerMessageGunFire` /
  `GunFireSelect` / `GunMelee` / `GunReload` / `GunShoot` / `ServerMessageLevelUp`），
  均为「发送时必携真实枪械」语境；`ServerMessageGunDraw` 的注释即为在案证据
  （8-21 LAN 断连复盘时已逐字核对上游 1.21.1：Draw 用 OPTIONAL，
  Fire 系上游即非 OPTIONAL，**不得一并改动**）。与上游语义一致，不动。

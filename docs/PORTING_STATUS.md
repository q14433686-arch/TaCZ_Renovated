# 移植状态

目标版本：Minecraft **26.1.2** + NeoForge **26.1.2.97**（release）。

工作包①②③④⑤⑥已完成（首发范围）。Iris 为可选；无光影时走 vanilla depth-aperture。

## 未完成 / 未验证项（诚实清单）

> 本节记录**尚未达成**的兼容项，避免"文档说已完成、实际不工作"的落差。
> 最后更新：r20（2026-08-21）。

### ❌ Player Animation Library（PAL，第三人称动画）——四次尝试均未落地

用户实测结论：**未恢复兼容**（r17 报告一次，r20 后再次报告"依旧没做好"）。

| 轮次 | 做法 | 结果 |
|---|---|---|
| r17 | `compileOnly maven.modrinth:player-animation-library:1.2.5` | 死坐标（该项目 Modrinth version number 是 `1.2.5+26.1` 格式），PAL 类从未上编译 classpath |
| r18 | 随 r17 一起发布 | 同上 |
| r19 | PAL 改 CurseMaven 8454167；Controllable 用配置期 GitHub Releases 下载 | 用户构建在**配置期**崩：`PKIX path building failed`（Gradle JVM 不认代理环境下的 github.com 证书链） |
| r20 | 两个依赖全部改为 CurseMaven 坐标 + `libs/` 本地 jar 逃生舱，配置期零网络 | **用户反馈依旧没做好兼容**（缺构建/运行日志，失败环节未知） |

已核实的事实（不必重查）：
- PAL 26.1 发布是 **merged Fabric+NeoForge jar**（`PlayerAnimationLibMerged-*`），CurseForge 文件 8454167 = `1.2.5+26.1`（支持 26.1.2+2）；最新为 8674772 = `1.2.6+26.1`
- modid 两加载器均为 `player_animation_library`（源码库 `ZigyTheBird/PlayerAnimationLibrary` 分支 `26.1` 的 fabric.mod.json 与 neoforge.mods.toml 核实）
- 代码侧 `compat/playeranimator/**` 取自 refab 26.1.2，加载器适配三点已完成；挂载点（PlayerModelMixin / InnerThirdPersonManager / 9 个按键钩子）自移植初期就在位
- 用户 JVM 可达 cursemaven.com（REI 依赖自 WP03 起一直正常解析）

**当前未知（需用户提供才能继续）：**
1. r20 的 `gradlew build` 是否成功？失败请贴完整报错（尤其 `Could not find curse.maven:...` 字样）
2. 若构建成功：游戏里装的是哪个 PAL 文件（版本号/文件名）？`latest.log` 中搜索 `player_animation_library` 与 `tacz` 的加载行
3. 第三人称观察其他玩家持枪是否有任何 PAL 动画迹象

### ⚠️ Controllable（手柄绑定+震动）——未实测

代码（r18 起）与编译坐标（r20 起）就绪，API 对照 `MrCrayfish/Controllable@multiloader/26.1.2` 逐条验证过；但与 PAL 同链路，**在 PAL 问题水落石出之前不宣称可用**。CurseForge 文件页存在第三方分发警告，若 CurseMaven 拒发，需手动下载官方 jar 放 `libs/`（见 build.gradle 注释）。

### ✅ 已验证可用（用户 PASS）

- r15：Cloth Config 配置界面（T 键 + Mods 菜单，含无 Cloth 兜底）
- r16：爆头范围显示（F3+B）线渲染修复
- r17 前：配置界面崩溃（双 blur）修复等

### 其余兼容层状态

见 `docs/WP05_EVIDENCE.md` 兼容层盘点；其中 immediatelyfast（有据 no-op）、zoomify（NeoForge 无此 mod）、ar（无 26.1.2 版）为终态结论，非待办。

# WP-262-1 证据：26.2 构建骨架

日期：2026-08-21

## 前置闸门

### NeoForge 26.2 已进入 release

官方仓库：<https://github.com/neoforged/NeoForge>，分支 `26.2.x`。

- 稳定标记提交：`c13ea5b8000ee5333107f2be6416cc860c3f6d39`，提交标题
  `26.2.0 Stable`，日期 2026-08-09；tag `26.2.0-stable` 指向对应标记。
- 执行日分支 HEAD：`e973c1d1bbd2d2cf013b6df2b3c4c050f2b7d2f0`，日期
  2026-08-19；它是 `26.2.0` 基础 tag 后第 64 个 release commit。
- 官方 `Release` workflow run `32243488307` 在该 HEAD 成功结束。
- 由 NeoForge 的四段版本规则，当前 artifact 版本为 `26.2.0.64`；稳定标记之后且无
  `-beta` 后缀。

因此本包使用：

```properties
minecraft_version=26.2
minecraft_version_range=[26.2]
neo_version=26.2.0.64
```

### 官方 MDK 对齐

官方仓库：<https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle>，commit
`79215bb3398d047ef7a8588415278ac74b0966f8`（2026-08-16）。MDK 的固定 NeoForge
版本是当时的 `26.2.0.59`；本包按执行日官方 release HEAD 前滚到 `.64`。

| 骨架项 | MDK 26.2 | 本仓库 |
|---|---:|---:|
| Gradle wrapper | 9.2.1 | 9.2.1 |
| `net.neoforged.moddev` | 2.0.144 | 2.0.144 |
| Foojay resolver | 1.0.0 | 1.0.0 |
| Java toolchain | 25 | 25 |
| Minecraft range | `[26.2]` | `[26.2]` |
| 元数据模板 | `META-INF/neoforge.mods.toml` | 同；MDK 无 `moddedmc.mod.json` |
| `gradlew` mode | executable (`100755`) | 已修正为 `100755` |
| mappings/parchment | 无 | 无 |

有意保留的工程差异：

1. `rootProject.name = 'tacz'` 是项目名，不是模板漂移；
2. `neoForge.enable { version = ...; disableRecompilation = true }` 是一期已验证的
   ModDevGradle 2.x 设置，用来避免 2 GiB 沙盒执行 Vineflower 时 OOM；版本仍由同一个
   `neo_version` 属性提供；
3. jar-in-jar、本地库、可选兼容 compile classpath 与低内存 run 参数属于现有工程需求，
   不用空 MDK 覆盖。

### 上游语义与 primer

- NeoForged 26.2 primer：<https://docs.neoforged.net/primer/docs/26.2/>，GitHub source
  commit `df9c645b8fd73bfbcf4bacf793ba1d9430341d61`；执行前已通读完整 2638 行。
- 游戏语义上游 `q14433686-arch/TaCZ_Refabricated_Unofficial` 分支
  `26.2(main)`：commit `5a29159902f5dddf26cfd0cd0f0fa3b75fbe94e6`；已先读 `AGENTS.md`
  与 `docs/` 的 26.2/26.1.2 差异、渲染、第一人称、状态生命周期及兼容审计文档。
- 没有读取、下载或反编译 CurseForge `tacz-port` jar。

## 版本语义

```properties
mod_version=1.1.8+neoforge.26.2.0.R1
```

`1.1.8` 是 SemVer core；`+neoforge.26.2.0.R1` 是 build metadata，不参与枪包
`>=1.1.8` 的先后比较。没有使用 `-neoforge...` prerelease。

`neoforge.mods.toml` 的展示文字与模板来源注释已更新为 26.2；Iris 仍是 optional，
没有硬依赖 Aperture。Vulkan 为 26.2 实验 backend，另按 WP-262-3 矩阵验收。

## 静态验证

```text
git diff --check
# success

Gradle wrapper / ModDevGradle / Foojay / Java toolchain 与 MDK 表逐项相同
# checked
```

## 动态验收与 R1 定名

用户已对定名前同代码候选报告 build、`runServer`、生产专服与 L0-L3 PASS。随后只把
version metadata 定名为 R1，逻辑代码不变。最终 R1 产物仍需快速执行：

```bash
./gradlew clean build --no-configuration-cache
./gradlew runServer --no-configuration-cache
```

确认 Mod List 为：

```text
Timeless and Classics Zero 1.1.8+neoforge.26.2.0.R1 (tacz)
```

并检查最终 jar 文件名和 `neoforge.mods.toml` 均已展开 R1。

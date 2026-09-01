# 开发指南

面向想构建、修改或贡献本仓库的开发者。玩家请看 [README](../README.md) 与
[COMPATIBILITY.md](COMPATIBILITY.md)。**AI 协作者先读根目录 [`AGENTS.md`](../AGENTS.md)**
（会话级强制规则），再读本文与 [`../CHARTER.md`](../CHARTER.md)。

## 环境与构建

- **JDK 21**（Minecraft 1.21.11：Mojang 随游戏分发 Java 21；`build.gradle` 的 toolchain 同此）
- Gradle Wrapper 9.2.1（仓库自带，勿升级）
- **不要配置 mappings / parchment / Yarn**——1.21.11 是混淆版本，官方 Mojang 映射已由 ModDevGradle 自动接好（`gradle.properties` 注释）；不要再叠加任何映射层。

```bash
./gradlew build        # 产物在 build/libs/
./gradlew runClient    # 客户端
./gradlew runServer    # 专用服务端（无 GUI 环境的冒烟手段）
```

低内存环境可在 `build.gradle` 保持 `disableRecompilation = true`；本机开发建议改回
`false` 以挂上 Minecraft 反编译源码。

## 版本号（红线）

`mod_version` 必须是 `1.1.8+neoforge...`（SemVer **build metadata**）。
**禁止** `1.1.8-neoforge...`：`-` 是 pre-release，排序在 1.1.8 之前，
枪包的 `>=1.1.8` 检查会静默失败。

## 项目规则

| 文档 | 内容 |
|---|---|
| [`../CHARTER.md`](../CHARTER.md) | 工作合同：版本决策、参考仓库边界、事实来源层级、**洁净室条款** |
| [`records/`](records/) | 各工作包的 API 证据与冒烟记录（冻结快照，见下） |
| [`../LICENSES.md`](../LICENSES.md) | 依赖与许可证清单、Jar-in-Jar 说明 |
| [`../CHANGELOG.md`](../CHANGELOG.md) | 版本历史 |

三条最重要的：

1. **洁净室**：禁止以任何形式接触 CurseForge `tacz-port`（guilhermez1989）的 jar。
2. **API 必须有证据**：任何非平凡 API 调用要能指认 `类#方法(签名)` + 来源
   （层级见宪章第 3 节），并记入对应的 records 文档。
3. **参考边界**：游戏语义抄 Fabric **1.21.11** 分支（姊妹仓库 `arena/01a05db2*` 线），加载器习语抄 MUKSC 1.21.1，
   **MUKSC 的渲染代码一行不抄**（宪章第 2 节的表格是完整版）。

## 进度与记录约定

- **进度不进 README。** 历史看 git log 与 `CHANGELOG.md`；发版时更新 changelog。
- **改 `mod_version` 必须同步 README 与 CHANGELOG**，自检：
  `bash scripts/check_release_consistency.sh`（发布/合并前加 `--strict`）。
  检查点清单见 [`../AGENTS.md`](../AGENTS.md) §1。
- `docs/records/` 是**冻结审计快照**（API 证据、冒烟日志、决策与踩坑记录），
  完成后不回头改写；其中的交叉引用可能指向当时的旧文件名。
- 面向人的活文档只有：README、CHANGELOG、`docs/COMPATIBILITY.md`、本文件。
  改动影响到玩家可见行为时同步更新前三者。

## 谱系

双亲结构：语义主线来自姊妹项目（Fabric），加载器习语参考 MUKSC（辅，26.2 起退场）。

```
MCModderAnchor/TACZ  (1.20.1 Forge, 官方源头)
        │
        └── Sh1roCu/TACZ-Refabricated  (1.21.1 Fabric)
                  │
                  └── q14433686-arch/TaCZ_Refabricated_Unofficial
                      (Fabric 26.2 / 26.1.2 / 1.21.11 —— 姊妹项目)
                            │
                            │  游戏语义（主）
                            ▼
                ┌─────────────────────────────────┐
                │  本仓库  TaCZ: Renovated         │
                │  (NeoForge 1.21.11，modId=tacz) │
                └─────────────────────────────────┘
                            ▲
                            │  加载器习语参考（辅；渲染代码零采用）
                  MUKSC/TACZ-1.21.1  (NeoForge 1.21.1)
```

全谱系 GPL-3.0：衍生合法，义务是发布二进制必须同步提供完整对应源码并保留版权声明。
项目显示名 **TaCZ: Renovated** 只是品牌；modId、包名、版本号串不随名字变
（命名决策：[`records/NAMING_DECISION.md`](records/NAMING_DECISION.md)）。

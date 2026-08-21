# 26.2 开发指南

目标：Minecraft 26.2 + NeoForge 26.2.0.64 + Java 25。当前版本：
`1.1.8+neoforge.26.2.0.R1`。

## 1. 工具链

- JDK 25；确认 `java -version` 与 Gradle launcher JVM 都是 25。
- Gradle Wrapper 9.2.1；不要使用系统旧 Gradle 替代 wrapper。
- ModDevGradle 2.0.144。
- 26.1+ 未混淆，不添加 mappings / Parchment / Yarn。

```bash
./gradlew --version
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew build --no-configuration-cache
```

## 2. 运行配置

```bash
./gradlew runServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
```

生产专服不能由 `runServer` 代替；按 [`DEDICATED_SERVER_TEST.md`](DEDICATED_SERVER_TEST.md)
执行 L0-L4。NeoForge Vulkan 客户端需先在实例 `config/fml.toml` 设置：

```toml
earlyWindowControl=false
```

这是 NeoForge#3230 的 ELS workaround，不属于 TACZ 修复。

## 3. 代码权威

1. Minecraft 26.2 未混淆 classfile/source。
2. NeoForge 26.2 primer 与官方 sources。
3. `TaCZ_Refabricated_Unofficial` `26.2(main)`：只取游戏语义，不复制 Fabric API 表面。
4. 本仓 26.1.2 R1：NeoForge 加载器习语和多人稳定基线。
5. MUKSC/TACZ-1.21.1：必要时只参考加载器习语，渲染禁止复制。

禁止接触 CurseForge `tacz-port` jar。非平凡 API 必须在 evidence/records 中写出类、方法、
descriptor 与来源。

## 4. 双端与网络纪律

- `@OnlyIn(Dist.CLIENT)` 不作为 26.1+ dedicated 类加载保护。
- 覆写 vanilla 双端方法时，方法体不得依赖 client-only index/renderer 类。
- 网络 ItemStack 可能为 EMPTY 时使用 optional codec；必为非空的消息不要机械改动。
- 资源管理器若实现 `INetworkCacheReloadListener`，必须确认同时进入 reload 和 network list。

快速审计：

```bash
grep -rn "TimelessAPI.getClient\|ClientIndexManager" src/main/java --include="*.java"
grep -R "ItemStack.STREAM_CODEC\|ItemStack.OPTIONAL_STREAM_CODEC" -n src/main/java/com/tacz/guns/network
```

## 5. 渲染纪律

- 普通 scope 路径为阶段边界离屏 ocular mask，不保留 raw-depth 平行实现。
- mask geometry 在 submit 阶段收集，在 `PreparedFrame#executeSolid` 前一次性绘制。
- 低倍 sight：reticle-only mask；高倍 scope：full-viewmodel mask。
- Iris 自定义 pipeline 必须分类到 HAND，并在每个非 scope draw 重置 mask mode。
- OpenGL 正常不等于 Vulkan/Iris PASS；每个 backend 单独记录结果。

调试 mask：

```toml
ScopeMaskDebug=true
```

左上角应出现随瞄具移动的白色 ocular 投影。

## 6. 版本与模板

`mod_version` 使用：

```properties
mod_version=1.1.8+neoforge.26.2.0.R1
```

`+` 是 build metadata；禁止改成 `-neoforge`。改版本后必须运行：

```bash
bash scripts/check_release_consistency.sh --strict
```

`neoforge.mods.toml` 整个文件（包括注释）都会经过 Groovy template engine。注释中不要写
未知的字面量 dollar-brace；一致性脚本会检查所有实际 placeholder 是否存在于
`gradle.properties`。

## 7. 提交前检查

```bash
bash scripts/check_release_consistency.sh --strict
git diff --check
python -m json.tool src/main/resources/tacz.mixins.json
python -m json.tool src/main/resources/tacz.iris.mixins.json
python -m json.tool src/main/resources/tacz.carryon.mixins.json
```

没有用户实测就不要在 README、CHANGELOG 或兼容矩阵写 PASS。构建、专服、GPU、可选 Mod
和发布归档是相互独立的闸门。

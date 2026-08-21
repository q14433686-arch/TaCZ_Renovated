# 给 26.2 前滚分支（01a023e5）的同步说明 —— 2026-08-21

> 本文件面向执行 26.2 前滚的 AGENT。你的基线如果早于 Beta-2（提交 `5d2358d`），
> 就缺少三轮多人联机实测抓出的全部修复。**26.2 必须从 Beta-2 起跳，不是 Beta-1。**

## 一、最快同步路径

PR #6（`arena/01a023bf-...` → `main(26.1.2)`）合并后：

```bash
git fetch origin
git merge 'origin/main(26.1.2)'     # 或 rebase，取决于你的分支纪律
```

若你改动已大、只想拿代码修复，cherry-pick 这三个（按序）：

| 提交 | 内容 | 26.2 上是否仍适用 |
|---|---|---|
| `09a0edd` | ① `ServerMessageGunDraw` 两字段改 `ItemStack.OPTIONAL_STREAM_CODEC`（空栈踢全员）；② `AttachmentsTagManager`/`RecipeFilterManager` 接回 `registerNetwork`（否则联机客户端方块索引全灭、配件允装失效）；③ IrisCompat "already assigned" 视为成功 | ①② **必须**；③ 视 Iris 26.2 版行为再验 |
| `eea0b59` | mods.toml 模板注释里不得出现字面量 dollar-brace（Groovy 模板引擎连注释一起求值） | **必须**（26.2 MDK 同机制） |
| `3b19477` | 四个物品类 `getName` 覆写改走 common 索引（原引用 client 索引，专服 `/give` 即 `NoClassDefFoundError` 崩服） | **必须** |

## 二、比提交更重要的三条经验（前滚时会反复踩）

1. **26.1+ 上 `@OnlyIn(Dist.CLIENT)` 只是文档，不是保护。** 老 Forge dist cleaner
   会剥离成员，NeoForge 26.1 起只警告。你在 26.2 前滚渲染/GUI 时会大量搬运带
   @OnlyIn 的覆写——凡覆写 vanilla 双端方法的，一律按无注解审查方法体里的
   client 类引用。审计命令：
   `grep -rn "TimelessAPI.getClient\|ClientIndexManager" src/main/java --include="*.java"`
   （排除 client 包后逐个判断执行路径；详见 records/SERVER_TEST_20260821_DEDICATED.md）
2. **新写/搬运网络消息时，ItemStack 字段先问一句：会不会是 EMPTY？**
   会 → `OPTIONAL_STREAM_CODEC`。上游哪些消息该用哪个，refab 26.1.2
   `ServerMessageGunDraw#write` javadoc 有逐条对照。
3. **单机跑通 ≠ 完成。** 本轮四个致命 bug 全部只在多人下现形。26.2 的验收必须
   包含 `docs/DEDICATED_SERVER_TEST.md` 的 L0-L2（headless 可做）+ L3 实机矩阵。

## 三、你可能还没见过的文档（都在 Beta-2 基线里）

- `docs/PORT_262_BRIEF.md`——**你的工单**（差异映射、权威边界、WP-262 切分）
- `AGENTS.md`——会话级规则（版本号一致性门禁、不得声称未实现）
- `docs/records/SERVER_TEST_20260821_*.md`——四份联机实测记录（根因与证据链）
- `CHANGELOG.md` Beta-2 条目——修复全景

## 四、版本号

你的起步版本串应为 `1.1.8+neoforge.26.2.0.r0`，基于 **Beta-2** 的代码。
改 `gradle.properties` 后跑 `bash scripts/check_release_consistency.sh --strict`。

# 给 26.2 前滚 AGENT 的同步说明（复制本文全文给他即可）

> 更新：2026-08-21（26.1.2 R1 定名后）。本会话（01a023bf）保持开启，**本分支可直接读取，
> 不必等 PR #6 合并**。
>
> 26.2 后续也定名为 R1；本文中的目标版本示例已统一为最终 26.2 R1 metadata，分支名保持原样。

## 0. 你要同步的源

```
仓库：q14433686-arch/TaCZ-Renovated
分支：arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0   ← 26.1.2 线的 R1 基线（最新提交即是）
```

```bash
git fetch origin arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0
```

你的基线如果切自 `4d2edc1`（Beta-1 时代的 main），就缺少三轮多人联机实测抓出的
全部修复。**26.2 必须从 R1 起跳**（R1 = 该分支 `5d2358d` 切版 + 后续文档收尾，
开发期曾用标签 Beta-2，代码同物）。

## 1. 同步方式二选一

**A（推荐）**：把你的 26.2 分支直接 rebase / merge 到本分支最新提交上——
文档、脚本、修复一次拿齐。

**B（你已大改、怕冲突）**：只 cherry-pick 三个代码修复，再手动抄文档：

| 提交 | 内容 | 26.2 上是否仍适用 |
|---|---|---|
| `09a0edd` | ① `ServerMessageGunDraw` 两字段 `ItemStack.OPTIONAL_STREAM_CODEC`（空栈踢全员）；② `AttachmentsTagManager`/`RecipeFilterManager` 接回 `registerNetwork`（否则联机客户端方块索引全灭、配件允装失效）；③ IrisCompat "already assigned" 视为成功 | ①② **必须**；③ 视 Iris 26.2 版行为再验 |
| `eea0b59` | mods.toml 模板注释禁写字面量 dollar-brace（Groovy 引擎连注释一起求值） | **必须** |
| `3b19477` | 四个物品类 `getName` 改走 common 索引（原引用 client 索引，专服 `/give` 即 NoClassDefFoundError 崩服） | **必须** |

另有版本号提交 `5d2358d`（Beta-2）与 `b9de5e0`（定名 R1）只影响 26.1.2 线元数据，
你不用 pick——你的版本串直接从 `1.1.8+neoforge.26.2.0.R1` 起步。

## 2. 三条经验（比提交更重要，前滚时会反复踩）

1. **26.1+ 上 `@OnlyIn(Dist.CLIENT)` 只是文档，不是保护**（dist cleaner 不再剥离成员）。
   你前滚渲染/GUI 会大量搬带注解的覆写——凡覆写 vanilla 双端方法的，一律按无注解
   审查方法体内的 client 类引用。审计命令：
   `grep -rn "TimelessAPI.getClient\|ClientIndexManager" src/main/java --include="*.java"`
2. **网络消息里的 ItemStack 字段先问一句：会不会是 EMPTY？** 会 → OPTIONAL codec。
   refab `ServerMessageGunDraw#write` javadoc 有上游逐条对照。
3. **单机跑通 ≠ 完成。** 26.1.2 线的四个致命 bug 全部只在多人下现形。26.2 的验收
   必须包含本分支 `docs/DEDICATED_SERVER_TEST.md` 的 L0-L2（headless 可做）
   + L2.5 枪包专项 + L3 实机矩阵。

## 3. 必读文档（都在本分支）

- `docs/PORT_262_BRIEF.md` —— 你的工单（差异映射、权威边界、WP-262 切分）
- `AGENTS.md` —— 会话规则（版本一致性门禁、不得声称未实现）
- `docs/DEDICATED_SERVER_TEST.md` —— 测试预案（L0-L4 + L2.5）
- `docs/records/SERVER_TEST_20260821_*.md` —— 五份实测记录（根因与证据链）
- `CHANGELOG.md` R1 条目 —— 修复全景

## 4. 版本号红线

起步 `1.1.8+neoforge.26.2.0.R1`；`+` 后是 build metadata，**禁止 `-`**。
改 `gradle.properties` 后跑 `bash scripts/check_release_consistency.sh --strict`。

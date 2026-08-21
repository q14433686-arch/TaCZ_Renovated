# 联机实测记录 #1 —— 2026-08-21 LAN 双端踢出

> 冻结快照。首次多人场景实测（预案 `docs/DEDICATED_SERVER_TEST.md` 的 L3 前哨：
> **单机开放局域网**，非专用服务器）。结果：**FAIL——加入 0.7 秒后双端断连**。
> 本记录含根因分析、修复与复测清单。

## 环境（取自用户上传 log，仓库 `main(26.1.2)` 分支 `latest.log`）

- MC 26.1.2 + NeoForge 26.1.2.97，tacz `1.1.8+neoforge.26.1.2.Beta-1`
- 场景：GIG 开单机世界 → mcwifipnp 开放局域网 → GOOSTL 加入
- 环境为大型整合包：sodium、iris 1.11.3+mc26.1.2、xaero、REI、cloth、PAL、
  carryon、shouldersurfing、firstperson、immediatelyfast 等
- 第三方枪包：`ciblr`、`murasamet`、`cib`（+ 默认包）

## 故障 1（致命，断连根因）：Draw 消息空栈编码崩溃

```
Failed encoding custom payload tacz:server_gun_draw:
  EncoderException: Empty ItemStack not allowed
→ Connection ERROR → GOOSTL lost connection → 单机服务器随退出停止 → 双端全踢
```

- **根因**：`ServerMessageGunDraw.STREAM_CODEC` 对 previous/current 两个 ItemStack
  用了 `ItemStack.STREAM_CODEC`（不允许 EMPTY）。首次切枪无"上一把"、空手切换、
  丢弃手持物时字段天然为 EMPTY；消息为 tracking 广播，一次空栈踢掉视野内所有人。
- **证据**：③ refab 26.1.2 `ServerMessageGunDraw#write` javadoc 详载同一移植回归，
  并逐字核对上游 1.21.1 用 `OPTIONAL_STREAM_CODEC`；明确警告同目录
  Fire/FireSelect/Melee/Reload/Shoot 五个消息上游即非 OPTIONAL，不得一并改。
- **修复**：两字段改 `ItemStack.OPTIONAL_STREAM_CODEC`（仅此消息）。

## 故障 2（功能残废）：RECIPE_FILTER / ATTACHMENT_TAGS 漏出同步包

```
Failed to parse data from network for BLOCK_INDEX with id tacz:gun_smith_table
  JsonParseException: there is no corresponding data file   （全部方块索引同此）
```

- **根因**：`RecipeFilterManager` 与 `AttachmentsTagManager` 均实现
  `INetworkCacheReloadListener`，但接线只走了 `register.accept(...)`、未入
  `listeners` → `getNetworkCache()` 永远不打包 RECIPE_FILTER 与 ATTACHMENT_TAGS。
  客户端 `CommonBlockIndex.checkData` 查 filter 必空 → 所有 BLOCK_INDEX 解析失败
  （工作台在联机客户端不可用）；ATTACHMENT_TAGS 缺失另致客户端配件允装判断
  静默失效。
- **证据**：③ refab 26.1.2 `CommonAssetsManager#reloadAndRegister`——其 `register(...)`
  助手把这两个管理器一并加入 listeners（全同步），仅 lootInjection/script 不同步；
  默认包 `data/blocks/*.json` 均带 `filter` 字段（本仓库资源核实）。
- **修复**：两管理器改走 `registerNetwork(...)`，对齐 refab 接线。

## 故障 3（启动 WARN，非致命）：Iris 重复 assignPipeline

```
[Iris] Found fine program match for minecraft:pipeline/entity_cutout: HAND_CUTOUT
IllegalStateException: Shader already assigned ← IrisCompat.assignPipelineToIris
```

- **根因**：Iris 1.11.3+mc26.1.2 对常见 entity 管线自动分类；我们再手动 assign 即抛
  "already assigned"。目的已达成却记 WARN 且走"vanilla pipeline"分支。
- **修复**：识别该异常视为成功（保留 Iris 的分类，降为 info 日志）。

## 未定案（复测时优先确认）

- **宿主端（GIG）也报了故障 2 的解析失败**——宿主 `CommonAssetsManager.INSTANCE`
  应非空、查询应命中，静态分析未能完全解释该侧报错机制。修复同步缺口后复测：
  若宿主仍报错，则 `CommonAssetsManager.get()` 的回落逻辑或调用时序仍有第二个问题。
- 故障 1 修复后才能看到后续链路（枪包同步完成度、射击/换弹转播）——本次测试
  在 0.7 秒即断连，L3 矩阵实质上一行都未开测。

## 复测清单

1. 重建后重跑同一 LAN 场景：加入不断连、双端日志无 `server_gun_draw` 编码错误、
   无 BLOCK_INDEX 解析错误、宿主端亦干净；
2. 双端打开枪械工作台并各合成一件（验证 RECIPE_FILTER 到位）；
3. 配件允装（拖入不允许的配件应被拒——验证 ATTACHMENT_TAGS 到位）；
4. 装 Iris 端启动日志出现 "already classified by Iris" info 而非 WARN；
5. 通过后再推进 `DEDICATED_SERVER_TEST.md` 的 L2（真实专用服务器）与 L3 全矩阵。

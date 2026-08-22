# 联机实测记录 #2 —— 2026-08-21 LAN 复测 PASS

> 冻结快照。对 `SERVER_TEST_20260821_LAN.md` 三项修复的复测。
> log 证据：仓库 `main(26.1.2)` 分支 `latest.log`（commit 7f15d5d，2398 行）。

## 结果：PASS（用户实测 + log 双确认）

| 复测项 | 结果 | log 证据 |
|---|---|---|
| 加入不断连 | ✅ | GOOSTL（local 通道）与 GIG（127.0.0.1）先后登入，无 lost connection |
| `server_gun_draw` 编码错误 | ✅ 0 命中 | `grep -c "Empty ItemStack"` = 0 |
| BLOCK_INDEX 解析错误 | ✅ 0 命中 | `grep -c "Failed to parse data from network"` = 0 |
| Iris 改走成功分支 | ✅ | `already classified by Iris ... keeping Iris assignment` info ×3 管线 |
| 游戏内异常 | 用户报告"没什么异常" | — |

## 未定案关闭

记录 #1 中"宿主端也报解析错误、机制未定位"——修复 RECIPE_FILTER/ATTACHMENT_TAGS
同步缺口后宿主端错误一并消失。经验结论：宿主客户端处理 SyncGunPack 时同样走了
网络 cache 查询路径，缺口一补双端同愈。不再单独追查。

## 覆盖面备注（诚实边界）

- 本轮场景 = **内置服务器 + LAN/本地隧道**（本次连接含 `local:E:` 通道）。
- **仍未覆盖**：生产 jar + 真实专用服务器（预案 L2）、L3 完整 11 行矩阵
  （本轮只验了加入与基础游玩，射击转播/工作台合成/配件允装/断线重连等
  未逐行打钩）、其他服务器形态（见预案「形态矩阵」节）。

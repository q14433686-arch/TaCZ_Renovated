# 联机实测记录 #4 —— 2026-08-21 专用服务器（L2+L3）PASS

> 冻结快照。log 证据：`main(26.1.2)` 分支 `latest.log` + `debug.log`（commit 641b8a8）。

## 环境

- **真实专用服务器**：NeoForge 26.1.2.97 安装器 `--install-server`，`D:\tacz_srv`，
  DedicatedServer，生产 jar（Mod List：`TaCZ: Renovated 1.1.8+neoforge.26.1.2.Beta-1 (tacz)`）
- 双客户端 GOOSTL / GIG 经 127.0.0.1 加入，游玩约 3 分钟（含成就触发、切创造）

## 判据核对

| 判据 | 结果 | 证据 |
|---|---|---|
| 起服到 Done | ✅ | `Done (0.316s)` |
| 生产 jar 身份与新显示名 | ✅ | Mod List 行 |
| 枪包装载 | ✅ | `guns=54 ammo=24 attachments=99 blocks=3 recipes=173` |
| getName 崩溃回归（/give 系） | ✅ 未复现 | 全 log 无 NoClassDefFoundError |
| LAN 轮修复回归 | ✅ | 无 Empty ItemStack / 无网络解析错误 |
| 双客户端加入与游玩 | ✅ | 两次 login、无异常断连 |
| 用户侧 L2/L3 测试点 | ✅ | 用户报告"要测试的点全 PASS，暂未发现问题" |

计数说明：`blocks=3 recipes=173`（早期 WP04 冒烟为 4/182）——当前默认包
`data/blocks/` 实为 3 个文件（ammo_workbench / gun_smith_table / attachment_workbench，
仓库资源核实），差异属 Beta-1 基线内容调整（LR 撤回期），非本轮回归。

噪音甄别：结尾两条 `Exception caught in connection: SocketException: Connection reset`
= 玩家直接关闭客户端的正常 TCP 断开（20:17:15，与两名玩家 Disconnected 同刻），非 mod 问题。
`RecipeManager empty ingredients` 警告为已知无害项（CHANGELOG 已知问题在案）。

## 结论

- 预案 **L2 全判据通过**；**L3 用户报告 PASS**（形态矩阵第 1 行关闭）。
- 联机故障三轮（LAN 踢出 → LAN 复测 → 专服 /give 崩）全部闭环，
  据此**切版 Beta-2**（更名 + 四组修复）。
- 后续形态（面板/代理/混合服）按 L4 矩阵择机推进，均非 Beta-2 阻塞项。

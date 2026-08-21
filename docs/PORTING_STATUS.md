# 移植状态

目标版本：Minecraft **26.1.2** + NeoForge **26.1.2.97**（release）。
当前版本：**1.1.8+neoforge.26.1.2.Beta-1**（r1-r30 为开发历史，Beta-1 起为稳定基线）。

工作包①②③④⑤⑥已完成（首发范围）。Iris 为可选；无光影时走 vanilla depth-aperture。

## 版本基线

- **Beta-1（当前）**：撤回工作包⑦（LRTactical），功能基线 = r23（cloth/PAL/controllable/shouldersurfing
  全兼容 + 全部已 PASS 修复）。LR 代码全部移除，踩坑点归档于 `WP07_LRTACTICAL_PLAN.md`。
- r1-r30：开发历史（详见 git log 与各 WP 文档）。

## 未实现项（诚实清单）

> 最后更新：Beta-1（2026-08-21）。

### ❌ LRTactical 内置（WP⑦）——已撤回，未实现

- r26 立项实施（104 文件 + 31 资源迁入，形态 B），r26→r30 三轮修复后**仍有未定位的启动崩溃**，
  项目决定撤回全部代码（Beta-1）。
- **现状**：LR 内容包可被发现（`GunPackLoader` 软 provides 保留），但四个基础物品
  （throwable/melee/detonator/consumable）与全部行为**不存在**——LR 包装上后道具不可用。
- 决策记录、撤回原因、**全部踩坑点（13 条，含注册表冻结/ID_MAPPER 私有化/readMap 歧义等）**
  与重启前置条件：见 `docs/WP07_LRTACTICAL_PLAN.md`。
- 重启条件：定位 r30 崩溃日志 + 优先 DeferredRegister 完整重写 init 包。

### 其余兼容层终态

| 兼容层 | 状态 |
|---|---|
| cloth / playeranimator(PAL) / controllable / shouldersurfing | ✅ 活（用户 PASS） |
| carryon / firstperson / iris / shader / jei / rei / recipeviewer | ✅ 活 |
| justzoom | ⏸ 有据不做（无上游先例，项目决定不原创） |
| zoomify | ⏸ NeoForge 无此 mod |
| immediatelyfast | ⏸ 有据 no-op（26.x 无需集成） |
| ar | ⏸ 无 26.1.2 版 |
| **lrtactical** | **❌ 未实现（已撤回，见 WP07 文档）** |

## 已验证可用（用户 PASS 清单）

- r15：Cloth Config 配置界面（T 键 + Mods 菜单，含无 Cloth 兜底）
- r16：爆头范围显示（F3+B）线渲染修复
- r17-r20：PAL 第三人称动画（根因：默认包 3 个动画文件漏拷，r22 修复后 PASS）
- r23：Shoulder Surfing Reloaded 5.x（插件 + 准星）
- r24/r25：JustZoom 原创适配后按项目决定撤销

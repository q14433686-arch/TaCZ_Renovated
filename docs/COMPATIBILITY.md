# 兼容性

适用版本：Minecraft 26.1.2 + NeoForge 26.1.2.x。本 mod **无必装前置**。

## 枪包

- 兼容按 TaCZ **1.1.8** 制作的枪包，`tacz >= 1.1.8` 依赖检查照常通过。
- 默认枪包随 jar 附带，首次启动解压到 `游戏目录/tacz/`。
- **依赖 `lrtactical` 的枪包**：可以装载，枪械部分正常；但近战/投掷物/引爆器/消耗品等
  LR 道具**不可用**（LRTactical 框架未内置，撤回记录见
  [`records/WP07_LRTACTICAL_PLAN.md`](records/WP07_LRTACTICAL_PLAN.md)）。

## 可选 mod

| Mod | 状态 | 验证版本 | 说明 |
|---|---|---|---|
| Cloth Config | ✅ 可用 | 26.1.154 | 图形配置界面（T 键 / Mods 菜单）。缺席时 T 键给下载链接，Mods 页显示提示屏 |
| Player Animation Library | ✅ 可用 | 1.2.5+26.1 | 第三人称持枪/换弹动画 |
| Controllable | ✅ 可用 | 0.26.0 | 手柄开火/瞄准/换弹按键 |
| Shoulder Surfing Reloaded | ✅ 可用 | 5.0.10 | 越肩视角插件与准星 |
| Iris | ✅ 可用 | — | 光影下的瞄具与第一人称渲染（反射接入，不装则不加载） |
| JEI | ✅ 可用 | 29.29.0.77 | 配方查看 |
| REI | ✅ 可用 | 26.1.2 对应版 | 配方查看 |
| Carry On | ✅ 可用 | — | 搬运枪械工作台等方块 |
| FirstPerson Model | ✅ 可用 | — | 第一人称身体模型共存 |
| ImmediatelyFast | ✅ 无需适配 | — | 26.x 渲染架构下无需集成，装了也不冲突 |
| Just Zoom | ❌ 不适配 | — | 无上游先例，项目决定不做原创适配 |
| Zoomify | — | — | 26.1 线无 NeoForge 版，无从适配 |
| Accelerated Rendering | — | — | 无 26.1.2 Feature Rendering 版，兼容层禁用 |

「✅ 可用」= 在真实客户端环境通过用户实测（PASS），或为无 GUI 依赖的纯逻辑层。

## 已知问题

- **联机覆盖面**：LAN 与真实专用服务器（生产 jar + 双客户端）已实测通过
  （2026-08-21，R1）；面板服、代理网络、混合服未测试，进度见
  [`DEDICATED_SERVER_TEST.md`](DEDICATED_SERVER_TEST.md) L4 形态矩阵。
- 启动日志中原版 `RecipeManager` 对工作台配方报 `empty ingredients` 警告：无害，
  实际合成走 mod 内部管线。
- 发现新问题请按 [README](../README.md#反馈-bug) 的格式反馈。

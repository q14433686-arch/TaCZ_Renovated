# 更新日志

版本号格式：`1.1.8+neoforge.26.1.2.<标签>`。`+` 之后是 SemVer build metadata，
因此枪包的 `tacz >= 1.1.8` 依赖检查照常通过（**禁止**改用 `-`，那是 pre-release，会静默不满足 `>=1.1.8`）。

## Unreleased

### 品牌

- 新增本仓库原创 `icon.png` / `logo.png`（青色四段瞄具环 + 铜色 R），并写入
  `neoforge.mods.toml` 的 `logoFile`。未使用官方 TaCZ 图标（CC BY-NC-ND 4.0
  禁止再创作），也与 Fabric 姊妹项目那套官方原图区分开。

### 文档

- 重写 CurseForge、Modrinth 与 MC 百科发布正文：按项目级长期说明与版本级
  Changelog 分层，删除冗长、重复及过度承诺的内容。
- MC 百科文案改为可直接使用的 BBCode，并为本项目 CurseForge 页面（Project ID
  1663324）、原始项目、直接上游、源码、Release、Issues 与许可文件补齐跳转链接。
- `docs/publish/RELEASE.md` 改为覆盖 26.2、26.1.2、1.21.11 的内部更新规范，明确
  活文档 → Release notes → 平台 Changelog 的同步顺序及“测试结论不得跨分支继承”。

## 1.1.8+neoforge.26.1.2.R1 — 2026-08-22

首个发布版。三条战线在同一版本收口，全部经用户实机验收
（LAN 双轮 + 专用服务器 L2/L3 + 枪包专项 + LR 单机/专服专项，
records/SERVER_TEST_*、records/LR2_INVENTORY.md 全程台账）。

### 更名

- **项目更名：TaCZ: Renovated**（原"TaCZ NeoForge 26.1.2（非官方移植）"）。
  只改显示名，**modId 仍为 `tacz`**，枪包兼容不受影响。
  决策记录：`docs/records/NAMING_DECISION.md`。

### 新增：LRTactical 内置层（WP-LR2）

- throwable/melee/detonator/consumable 四类基础物品、五类投掷行为
  （explode/sticky/smoke/stun/effect-cloud）、数据装载与网络同步
  （独立载荷通道 `lr1`）、反馈层（三类 tooltip / 使用进度 HUD / 分类冷却遮罩）。
- 依赖 `lrtactical` 的内容包**完整可用**。
- 范围界定：flash_shield 未含（独立子系统 + ARR 美术，与姊妹项目同边界）；
  原作美术零打包，道具模型/贴图由内容包提供；无内容包时显示原版占位模型。
- 实现要点：LR init 包全量 DeferredRegister 重写（根治 WP07 A 类注册时序坑，
  当年 r30 的未定位启动崩溃未复现——E-13 闭案）；本仓补齐 NeoForge 路径必需的
  `items/*.json` 与 `particles/smoke_cloud.json`（本仓实现所需，非上游缺陷）。

### 修复（联机战役，Beta-1 → R1）

- **专服致命**：四个物品类（枪/弹药/配件/工作台）的 `getName` 覆写调用 client 索引，
  `/give` 等服务端路径触发即 `NoClassDefFoundError` 崩服（26.1 起 NeoForge 不再按
  `@OnlyIn` 剥离成员，上游祖传写法失效）。改走 common 索引。
- **联机致命**：`ServerMessageGunDraw` 空 ItemStack 编码崩溃——加入/空手切枪把视野内
  所有玩家踢下线。改 `ItemStack.OPTIONAL_STREAM_CODEC`（上游 1.21.1 与 refab 同款）。
- **联机功能**：RECIPE_FILTER 与 ATTACHMENT_TAGS 漏出网络同步包——联机客户端方块索引
  全部解析失败（工作台不可用）、配件允装判断静默失效。接回 `registerNetwork`。
- **Iris**：Iris 1.11.3 已自动分类 entity 管线时不再误报 WARN，保留 Iris 分类。
- **构建**：mods.toml 模板注释中的字面量 dollar-brace 炸毁 `generateModMetadata`。

### 文档

- 文档体系对齐姊妹项目规范：README 重写、`AGENTS.md`、一致性自检脚本、
  专用服务器测试预案（`docs/DEDICATED_SERVER_TEST.md`，L0-L4 + L2.5 枪包专项）。
- 联机枪包指引：双端安装职责、服务端 `/tacz reload`、客户端新增包按 F3+T 重载。

### 已知事项

- Fabric 姊妹项目的 getName 模式待其作者顺手核查（NeoForge 侧已实证，Fabric 未实测）。
- 面板服/代理网络/混合服未测试（L4 矩阵在案，非阻塞项）。

## 1.1.8+neoforge.26.1.2.Beta-1 — 2026-08-21

首个稳定基线。此前的 r0–r30 为开发迭代，历史见 git log，各阶段验收证据见 `docs/records/`。

### 包含

- 完整枪械玩法：物品/方块/配方注册、网络同步、弹道、枪包装载
  （默认枪包：枪械 54、弹药 24、配件 99、方块 4、配方 182）
- 渲染：depth-aperture 瞄具、第一人称 Feature Rendering、工作台 BER
- 可选 Iris 光影兼容（无 Iris 时不加载任何相关代码）
- 可选 mod 兼容：Cloth Config、Player Animation Library、Controllable、
  Shoulder Surfing Reloaded、JEI / REI、Carry On、FirstPerson Model
  （矩阵见 `docs/COMPATIBILITY.md`）

### 相对 r30 的变更

- **移除**：LRTactical 内置框架（r26 立项，三轮修复后仍有未定位的启动崩溃，撤回；
  决策与踩坑记录见 `docs/records/WP07_LRTACTICAL_PLAN.md`）。
  枪包依赖检查对 `lrtactical` 的软放行保留。

### 已知问题

- 依赖 `lrtactical` 的枪包：枪械可用，LR 道具（近战/投掷/引爆器/消耗品）不可用。
- 启动日志中原版 `RecipeManager` 对工作台配方有 `empty ingredients` 警告——无害，
  实际合成走 mod 内部管线。

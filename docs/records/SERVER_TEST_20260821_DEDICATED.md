# 联机实测记录 #3 —— 2026-08-21 专用服务器（L2）首跑：/give 崩服

> 冻结快照。预案 L2 首次执行：生产 jar + NeoForge 安装器专用服务端。
> 结果：**起服成功，`/give` 即崩**——正是 L2 设计要抓的"dedicated 无客户端类"故障类。

## 现象与调用链（用户侧诊断 + 本仓库源码逐行核实）

```
GiveCommand.giveItem() → ItemStack.getDisplayName() → getItemName()
  → GunSmithTableItem.getName()          （GunSmithTableItem.java:43，已核实）
    → TimelessAPI.getClientBlockIndex()  （TimelessAPI.java:77，已核实）
      → ClientIndexManager / client 索引类 → 服务端类加载失败 → 崩服
```

## 根因（比表面诊断多一层）

- `getName` 是**双端公共方法**（/give 回显、容器标题、聊天 hover、命名铁砧等
  服务端路径都会调用），四个物品类的覆写却直接调 client 索引。
- **为什么以前"安全"**：上游 1.20.1 Forge 的 dist cleaner 会真的把
  `@OnlyIn(Dist.CLIENT)` 成员从服务端剥离，覆写在 dedicated 上不存在 → 走原版实现。
  **26.1 NeoForge 不再剥离、仅警告**（records/WP04 早有记载）——祖传注解失效，
  潜伏 bug 引爆。属"上游模式在新加载器语义下失效"，非本仓库新引入。
- 姊妹项目 refab 26.1.2 同文件为同款写法（`@Environment(EnvType.CLIENT)` 同样
  不剥离）——**Fabric 侧 dedicated 同样潜伏此崩溃，建议回报上游修复**。

## 全仓库同类病灶清查（2026-08-21）

| 位置 | 判定 | 处置 |
|---|---|---|
| `AbstractGunItem#getName` / `AmmoItem#getName` / `AttachmentItem#getName` / `GunSmithTableItem#getName` | **必崩**（服务端可达） | ✅ 已修：改走 common 索引（`getCommonXxxIndex().getPojo().getName()`——与 client 索引读同一份 index json，翻译键一致；client 渲染聊天组件时自行翻译） |
| `AmmoItem#appendHoverText` | 安全（tooltip 仅 client 管线调用；HotSpot 惰性解析，未执行的调用指令不触发类加载） | 保留，备案 |
| `GunItemDataAccessor#getAimingZoom`（调 getClientAttachmentIndex） | 安全（调用者全在 client：CameraSetupEvent/渲染器） | 保留，备案；若未来服务端要算 zoom（如散布），必须先改 common 路径 |
| `LaserColorUtil` | 安全（调用者 HSVSliderGroup/BeamRenderer 均 client） | 保留，备案 |

## 复测（在专服上）

1. `/give @s tacz:modern_kinetic_gun`、弹药、配件、工作台各一次——不崩、
   聊天回显显示正确译名；
2. 通过后继续 L2 其余判据与 L3 全矩阵（见预案）。

## 经验沉淀（写给 26.2 工单与未来包）

**26.1+ 上 `@OnlyIn(Dist.CLIENT)` 只是文档，不是保护。** 双端公共方法的覆写体内
不得出现 client 类引用；需要 dist 分支时用"独立 client 类 + 运行期 Dist 判断"
（本仓库 ClientPacketBridge 即该模式）。审查上游/refab 代码时，凡见 @OnlyIn/@Environment
标注的 vanilla 方法覆写，一律按无注解审查。

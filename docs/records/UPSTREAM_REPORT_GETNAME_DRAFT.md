# 上游回报文案存档（2026-08-21）

> 目标：q14433686-arch/TaCZ_Refabricated_Unofficial（Fabric 姊妹项目）新建 issue。
> 措辞已按对方 AGENTS.md §2 纪律自审：NeoForge 侧为实证事实，Fabric 侧明确标注未实测。
> 以下为可直接粘贴的 issue 正文。

---

**标题**：`[潜伏崩溃] 物品 getName 覆写引用 client 索引——dedicated 服 /give 可致崩服（NeoForge 姊妹项目已实证并修复，Fabric 侧同款代码待复现）

**正文**：

## 背景

NeoForge 26.1.2 姊妹移植（TaCZ-Renovated，同谱系）在专用服务器实测中发现并修复了
一个崩服问题。经比对，本仓库 `26.1.2` 分支存在**逐行同款**的代码模式，特此回报。

## NeoForge 侧实证（已复现 + 已修复 + 复测 PASS）

真实专用服务器上执行 `/give` 即崩，调用链（与源码逐行核实）：

```
GiveCommand.giveItem() → ItemStack.getDisplayName() → getItemName()
  → GunSmithTableItem#getName()            ← @OnlyIn(Dist.CLIENT) 覆写
    → TimelessAPI.getClientBlockIndex()
      → ClientIndexManager → client 索引类 → NoClassDefFoundError → 崩服
```

根因两层：

1. `getName` 是**双端公共方法**——`/give` 回显、容器标题、聊天 hover、命名铁砧等
   服务端路径都会调用，不能假设只在客户端执行；
2. 这个写法在 1.20.1 上游"安全"仅仅因为**老 Forge 的 dist cleaner 会把
   `@OnlyIn(Dist.CLIENT)` 成员真的从服务端剥离**（覆写不存在 → 走原版实现）。
   NeoForge 26.1 起不再剥离、仅警告，祖传注解一夜之间失效。

## 为什么 Fabric 侧大概率同样命中（未实测，请先复现）

- fabric-loader 对 `@Environment(EnvType.CLIENT)` **从不剥离成员**，该注解始终只是
  文档标记——即本模式在 Fabric dedicated 上从 1.21.1 时代起就应是潜伏状态；
- 本仓库 `26.1.2` 分支 `GunSmithTableItem#getName` 为同款实现
  （`@Environment(EnvType.CLIENT)` + `TimelessAPI.getClientBlockIndex`），
  已与 NeoForge 侧崩溃现场逐行比对；
- **我方未在 Fabric dedicated 上实测**，不预设结论。建议复现步骤：
  Fabric 专服 + 本 mod，控制台或 OP 执行
  `/give @s tacz:modern_kinetic_gun`（枪/弹药/配件/工作台各一次）。

## 波及面（按 NeoForge 侧清查结果，供对照）

必修（服务端可达的 vanilla 覆写）：

- `AbstractGunItem#getName`
- `AmmoItem#getName`
- `AttachmentItem#getName`
- `GunSmithTableItem#getName`

审计方法：`grep -rn "TimelessAPI.getClient\|ClientIndexManager" src/main/java --include="*.java"`
排除 client 包后逐个判断执行路径。以下类别经判定**安全**（方法体含 client 调用但
仅被 client 管线执行；JVM 惰性解析下未执行的调用指令不触发类加载）：
`appendHoverText`、`getAimingZoom`（调用者全在渲染/相机侧）、`LaserColorUtil`。

## 修复参考（NeoForge 侧已复测 PASS 的版本）

统一改走 common 索引——与 client 索引读的是**同一份 index json、同一个翻译键**，
客户端渲染聊天组件时自行翻译，双端行为一致、无需 dist 分支：

```java
// 以 GunSmithTableItem 为例；其余三处同构
public Component getName(ItemStack stack) {
    Identifier blockId = this.getBlockId(stack);
    var blockIndex = TimelessAPI.getCommonBlockIndex(blockId);
    if (blockIndex.isPresent() && blockIndex.get().getPojo().getName() != null) {
        return Component.translatable(blockIndex.get().getPojo().getName());
    }
    return super.getName(stack);
}
```

对照提交：TaCZ-Renovated `3b19477`（修复）、`5d2358d`（专服复测 PASS 后切版）；
实测记录 `docs/records/SERVER_TEST_20260821_DEDICATED*.md`。

## 复测判据

dedicated 上 `/give` 四类物品：不崩，聊天回显显示正确译名（而非原始注册名）。

## 附注

- `1.21.11` 分支与直接上游 Sh1roCu/TACZ-Refabricated（1.21.1 Fabric）为同一代码
  谱系，理论上同样适用，均未核实——若本仓库复现成立，或可继续向上游回报。
- 反向致谢：本仓库 `ServerMessageGunDraw` 的 OPTIONAL_STREAM_CODEC javadoc 与
  `CommonAssetsManager` 的同步接线，是 NeoForge 侧另外两处联机修复的直接证据来源。

# refab 三分支回哺计划（作者视角）—— getName 潜伏崩溃

> 2026-08-21。取代 `UPSTREAM_REPORT_GETNAME_DRAFT.md`（原为对外 issue 口径；
> 因 refab 与本仓库同属一人，改写为直接维护计划）。
> 三分支源码已于本日联网逐文件核实，非推测。

## 一、核查结果（2026-08-21，raw.githubusercontent 逐文件拉取）

| 分支（现版本） | getName 病灶 | Draw OPTIONAL 修复 | common 索引 getter |
|---|---|---|---|
| `26.2(main)`（R2） | ✅ **确认在**：`GunSmithTableItem#getName`、`AbstractGunItem#getName` 均为 `@Environment(CLIENT)` + `getClientXxxIndex`（逐行核）；Ammo/Attachment 两文件按同构推定，修复时顺核 | ✅ 已有（javadoc 源头分支） | ✅ 有（同文件大量使用 `getCommonGunIndex`） |
| `26.1.2`（R2） | ✅ 确认在：`GunSmithTableItem`、`AbstractGunItem:288` | ✅ 已有 | ✅ 有 |
| `1.21.11`（R2） | ✅ 确认在：`GunSmithTableItem`（其余推定同构） | ✅ 已有 | 推定有，修复时核 |

关键事实链：

1. **fabric-loader 从不剥离 `@Environment` 成员**——该注解在三条分支上都只是文档；
2. `getName` 是双端公共方法，`/give` 回显、容器标题、聊天 hover、命名铁砧等
   服务端路径都会调用；
3. NeoForge 姊妹项目（TaCZ-Renovated）已在真实专服**实证同款代码崩溃**
   （`/give` → `NoClassDefFoundError`，records/SERVER_TEST_20260821_DEDICATED.md），
   修复后复测 PASS；
4. Fabric 侧崩溃**尚未实测复现**——修复前先在 Fabric dedicated 上跑一次
   `/give`，这决定 changelog 的合法措辞（见第四节）。

## 二、修复方案（三分支同一配方）

四处 `getName`（AbstractGunItem / AmmoItem / AttachmentItem / GunSmithTableItem）
改走 common 索引，删除 `@Environment(CLIENT)`：

```java
public Component getName(ItemStack stack) {
    Identifier id = this.getXxxId(stack);
    var index = TimelessAPI.getCommonXxxIndex(id);
    if (index.isPresent() && index.get().getPojo().getName() != null) {
        return Component.translatable(index.get().getPojo().getName());
    }
    return super.getName(stack);
}
```

- common 与 client 索引读同一份 index json、同一翻译键，客户端渲染聊天组件时
  自行翻译——显示行为不变，无需 dist 分支（NeoForge 侧对照提交 `3b19477`）。
- 若某分支 common 索引未暴露 `getPojo()`/name，补个 getter 即可。
- **refab 独有的追加审计**：LR 内置框架（`me.xjqsh.lrtactical.*`）与
  `cn.sh1rocu.*` 扩展里的物品类同样可能覆写 `getName`/其他双端方法——
  审计 grep 要扫全源码树，不只 `com.tacz`：
  `grep -rn "getClient\|ClientIndexManager\|client.resource" src/main/java --include="*.java" | grep -v "/client/"`
- 判"安全可留"的标准：方法仅被 client 管线调用（tooltip/渲染/相机），惰性解析下
  不触发类加载——`appendHoverText`、`getAimingZoom`、laser 颜色工具属此类。

## 三、分版本建议

### 26.2_R3（主线，优先做）

- 范围：四处 getName + LR/扩展审计结果。
- 附加：可顺带核一件小事——NeoForge 侧发现 `ListPackCommand` 存在但未接进
  `RootCommand`；查 refab 侧是否接线，确认是谁的移植遗漏（不阻塞 R3）。
- 验收：Fabric 26.2 dedicated `/give` 四类物品（+若干 LR 物品）不崩、译名正确。

### 26.1.2_R3（证据最直接，紧随其后）

- 范围同上。此分支与 NeoForge 实证环境同版本线，代码逐行同款，风险论证最硬。
- 验收同上（Fabric 26.1.2 dedicated）。

### 1.21.11_R3（收尾）

- 范围同上。分支特有注意（对齐你自己的 AGENTS §3）：
  - 混淆分支，但本修复**纯 mod 代码、不涉 mixin 目标**——两个 verify 脚本
    不是本次门槛；Loom remap 风险低；
  - "编译通过 ≠ 运行期安全"在该分支有五次前科——dedicated 实机跑一次 `/give`
    不能省。

### 三分支共同的发布纪律（你自己的规矩）

- 每分支改版本号 → README **6 处**同步；合并/发布前
  `bash scripts/check_release_consistency.sh --all --strict`；
- 跨分支复制修复代码/README 段落时逐句核版本号与分支名（历史事故 ×2）；
- Release 正文首行环境行照旧。

## 四、changelog 措辞（AGENTS §2 红线）

- **若 Fabric dedicated 复现成功**：可写"修复：专用服务器上 /give 或任何需要
  服务端读取物品显示名的路径（容器标题/聊天 hover/铁砧）触发崩溃"；
- **若未复现、仅按结构修复**：只能写"加固：消除物品显示名路径上对客户端专属类
  的引用（NeoForge 姊妹项目已实证同款代码在专服崩溃）"——不写"修复了崩溃"。

## 五、执行顺序建议

1. 任一分支先做 Fabric dedicated `/give` 复现实验（10 分钟，定措辞）；
2. 26.2(main) 修复 + 全树审计 → R3；
3. cherry-pick/平移到 26.1.2、1.21.11，各自实机验证 → R3；
4. 三分支 README/Release 按一致性脚本收尾。

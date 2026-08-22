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

## 六、追记（2026-08-21 深夜）：修复后暴露的第二层问题——专服物品全部"无数据化"

> 作者实测反馈：getName 修复后专服不崩了，但 `/give`/拿取的枪全是紫黑**平面贴图**
> （连模型都没有）、名字只剩 `.name` 尾巴；工作台只有枪械工作台正常，其余显示
> `tacz.XXX` 原始键；LR 手雷只能拿到无功能的"测试手雷"。单机/局域网均正常。

### 定性：getName 修复是**起了作用的**

修复前这条路径是**直接崩服**（NoClassDefFoundError）——崩溃把一切下游问题都遮住了。
现在不崩，才第一次看到 refab 专服路径的真实状态。这不是修复失效，是揭盖。

### 根因判断（证据齐全，指向单点）

**症状组合 = 无数据 ItemStack（dataless）**，三条症状同源：

1. TaCZ 架构里枪/工作台变体/LR 道具都是"一个注册物品 + 数据组件（GunId/BlockId/…）"。
   裸物品没有 id 组件 → `tacz:dynamic_item` 动态模型无从取显示 → **平面缺失贴图
   （不是紫黑模型，正因为连模型都查不到）**；getName 查不到索引 → 原始键；
   LR 手雷回落到注册基体"测试手雷"，自然无功能。
2. "只有枪械工作台正常" 是自证：它是工作台方块物品的**无数据默认形态**
   （`getBlockId` 无 NBT 时回落 `EMPTY_BLOCK_ID` → 默认=枪械工作台），
   其余工作台是 BLOCK_INDEX 变体、必须带 BlockId——裸拿必炸型。
3. **为什么只有专服炸**：物品的正确形态来自创造物品栏（`fillItemCategory` →
   `getAllCommonXxxIndex()`）。单机/局域网时 `CommonAssetsManager.INSTANCE`
   在同一 JVM，标签构建时数据就位。专服时客户端标签在**枪包网络缓存到达之前**
   就构建完了（且 refab `doSync` 对远程连接先 `clearInstance()`）——标签里装进
   的全是裸注册物品；之后 REI/创造栏拿取、乃至"give"（REI 作弊给予给的就是
   标签里的那个栈）全部继承 dataless。
4. **实锤对照**（2026-08-21 拉取 `26.2(main)` 源码逐行核实）：refab
   `ServerMessageSyncGunPack#doSync` = clearInstance → fromNetwork →
   ClientIndexManager.reload → RecipeViewer 刷新，**缺少创造标签重建**；
   NeoForge 姊妹项目同处理器多出 `CreativeModeTabs.tryRebuildTabContents` 段
   （`ClientPacketHandlers.onSyncGunPack:181-198`），并有实现细节：
   `tryRebuildTabContents` 对相同输入会**静默跳过**，需先翻转一次 permission
   入参使旧构建失效、再按真实权限重建。NeoForge 专服 L2/L3 全 PASS 正是
   建立在这段之上。

### 给 refab 侧的诊断清单（先证后改）

1. **10 秒定案**：F3+H 高级提示，比对专服拿到的枪 vs 单机拿到的枪的组件/NBT——
   专服的应缺 GunId。缺 = dataless 路线确认。
2. `doSync` 后打印缓存计数（gunIndex 条数）：>0 说明同步本身健康、纯属标签
   过期；=0 则还要查 Fabric 侧发送链路（`SYNC_DATA_PACK_CONTENTS` →
   sendToClientPlayer 在专服 join 时是否触发）。
3. 交叉验证：专服上用枪械工作台**合成**一把枪（配方数据走服务端）——合成的枪
   应完全正常，与 /give 的坏枪对照，即证"栈数据"根因而非渲染管线。
4. 修复方向（由 refab 侧自行实施）：`doSync` 末尾补创造标签重建；参考实现
   注意 permission 翻转技巧，否则重建静默 no-op。LR 物品同机制受益，
   修完后单独复验 LR 数据同步通道。

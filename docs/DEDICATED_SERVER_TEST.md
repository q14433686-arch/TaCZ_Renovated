# 26.2 专用服务器与多人测试预案（L0-L4）

> 活文档。26.1.2 R1 已完成 LAN、真实专服与枪包专项，但这些 PASS 不能自动继承到
> 26.2。当前 26.2 HEAD 已重写网络触点、transfer API 与 scope rendering；必须重新执行
> L0-L3。本预案按成本从低到高排列，跳层等于放走对应故障类。R1 历史证据位于
> `docs/records/SERVER_TEST_20260821_*.md`。

执行者：项目发起人的 JDK 25 / GPU 环境；结果必须记录当前 commit 与完整版本矩阵。

> 当前执行状态（2026-08-22）：用户报告的 **L0-L3 PASS** 对应 LR 合入前核心候选，冻结
> 记录为 `docs/records/SERVER_TEST_20260821_262_R1.md`。当前 R1 已前滚 LRTactical 的
> 代码、资源、AT、mixin 与 payload，必须从 L0 起重跑，并增加下述 LR 专项；26.1.2 的
> LR 单机/专服 PASS 只作为源基线。

---

## L0. 构建后静态自检（1 分钟，不用起游戏）

```bash
./gradlew build
JAR=build/libs/tacz-1.1.8+neoforge.26.2.R1-hotfix.jar   # 以实际文件名为准
unzip -l "$JAR" | grep -E "META-INF/jarjar/|luaj|commons-math3"
unzip -l "$JAR" | grep -E "tacz.*mixins.json|lrtactical.mixins.json|accesstransformer.cfg"
unzip -l "$JAR" | grep -E "me/xjqsh/lrtactical/|assets/lrtactical/|data/lrtactical/"
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "version=|modId="
```

判据：

- [ ] `META-INF/jarjar/` 下有 **luaj** 与 **commons-math3**（缺 = 生产必炸
      `NoClassDefFoundError: org/luaj/vm2/LuaError`，见 `LICENSES.md` 前科）；
- [ ] 四个 mixin json（`tacz` / `tacz.iris` / `tacz.carryon` / `lrtactical`）与 AT 文件在 jar 内；
- [ ] LR Java、assets、data、items/models/particles JSON 全部在 jar 内，且无来源不明二进制美术；
- [ ] mods.toml 中 `version` 已展开为 `1.1.8+neoforge...`（**不得**残留 `${mod_version}`，
      **不得**是 `-neoforge`）。

## L1. 开发环境 runServer（10 分钟）

```bash
./gradlew runServer --no-daemon     # 首次需在 run/ 接受 eula
```

判据（对照 `docs/records/` 各期冒烟口径）：

- [ ] Mod List 出现 `Timeless and Classics Zero 1.1.8+neoforge.26.2.R1-hotfix (tacz)`；
- [ ] 日志有 TaCZ 与 LR payload 注册行、枪包装载行及
      `LRTactical built-in layer (NeoForge 26.2 R1 candidate) registered`；
- [ ] LR throwable/melee/consumable 三类 index reload 完成，登入同步不报未知 payload；
- [ ] 到 `Done`，`stop` 干净退出；
- [ ] 全程无 `NoClassDefFoundError`（dedicated classpath 没有
      `LocalPlayer`/`Minecraft`——LR 与 TaCZ 的 S2C 处理都必须留在 client bridge 后面）。

这层抓：注册/网络/资源加载在 server dist 的类加载问题。抓不到：jarJar、生产 mixin/AT。

## L2. 生产 jar + 真实 NeoForge 服务端（30 分钟，纯 headless）

**这是最常被跳过、恰恰最容易炸的一层**——开发 classpath 与生产模块类加载器
行为不同（jarJar、mixin 服务发现、AT 应用、默认枪包从 jar 而非目录导出）。

```bash
# 1. 官方安装器装服务端（版本必须为 26.2.0.64 release）
java -jar neoforge-26.2.0.64-installer.jar --installServer srv
# 2. 部署
cp build/libs/tacz-*.jar srv/mods/
echo "eula=true" > srv/eula.txt
# 3. 启动（内存按机器调）
cd srv && java -Xmx2G -jar <安装器生成的启动 jar 或 run.sh> nogui
```

判据：

- [ ] 启动到 `Done`，mod 列表含 `tacz`，版本串正确；
- [ ] `srv/tacz/` 出现默认枪包导出（`GetJarResources` 的 **jar 内路径**读取，
      开发环境走的是目录路径，这里是第一次真正测到）；
- [ ] 日志枪包计数与 L1 一致；
- [ ] 控制台执行 `/tacz` 系列命令（list pack 等）：需要玩家上下文的子命令应
      **报错可读**而非堆栈；
- [ ] 放入一个依赖 `lrtactical` 的第三方内容包：LR index/recipe/script 装载；投掷、近战、
      消耗品与引爆器服务端逻辑可执行；flash_shield 缺失是明确范围边界；
- [ ] 两客户端观察 LR 投掷实体 tracking、分类冷却与索引同步，不踢出、不串 id；
- [ ] `stop` 干净退出，重启第二次（验证导出目录已存在时的幂等）。

## L2.5 枪包装载专项（专服上验证第三方枪包）

> 机制先明确：**服务端是逻辑权威**——`服务端根/tacz/` 里的包被装载后，common 数据
> （索引/数据/配方/filter/tags）经网络同步给客户端；**客户端仍需本地安装同一个包**
> 提供显示资产（模型/贴图/音效/语言文件）。没有"列包"命令（ListPackCommand 未接线，
> 见下），判据以日志为准。

### A. 双端安装（正常路径）

1. 包（zip 或解压目录，根须有 `gunpack.meta.json`）放 `服务端根/tacz/`；
   客户端同一包放 `.minecraft/tacz/`；重启服务端（或在线 `/tacz reload`，需 OP）。
2. 服务端日志判据：
   - `Found N possible gunpack(s)`，N 含新包；
   - `gun pack loaded: guns=... ammo=...` 计数增量与包内容一致；
   - **无 BLOCK_INDEX / RECIPE_FILTER 解析错误**（第三方包的自定义工作台方块
     正好压在 R1 修复的同步路径上，是最好的回归探针）。
3. 进服判据：创造物品栏/配方查看器可见包内枪械；`/give` 包内枪不崩、
   聊天回显正确译名（验 common 同步 + getName 修复）；开枪有伤害（服务端逻辑）、
   有模型音效（客户端资产）；包内配方可在工作台合成。

### B. 不对称安装（R1 基线已确认；26.2 必须重跑）

R1 证据：`docs/records/SERVER_TEST_20260821_GUNPACK.md`。

| 场景 | R1 实测表现（26.2 待确认） |
|---|---|
| 只装客户端 | 无事发生（服务端不认包内 id）；服务端补装后 `/tacz reload` 即正确加载 |
| 只装服务端 | 紫黑方块 + 名字只剩 `....name` 原始翻译键（显示资产不走网络同步）；**不崩、不踢** |

### C. 在线热重载（R1 基线已确认；26.2 必须重跑）

- R1 服务端侧：双客户端在线时 `/tacz reload` 干净重载并全员重同步，无踢出、无解析错误。
- R1 客户端侧：本地新增包后按 F3+T 即加载，无需重启；`/tacz reload` 只负责服务端
  common 同步。26.2 必须重新验证两条路径。

### D. 版本谓词

包内 `gunpack.meta.json` 声明 `tacz >= 1.1.8` 的，26.2 R1（`+` build metadata）
应照常通过；遇到写了奇怪谓词的包，记录其完整谓词再下结论。

### E. LRTactical 内容包

1. 双端安装至少一个含 throwable/melee/consumable 的 LR 内容包；
2. 登录后名字、tooltip、模型与配方正确，服务端三类 index 与客户端一致；
3. 分别验证普通/粘性/烟雾/闪光/效果云/C4、左右键近战、消耗品与遥控引爆；
4. 服务端 `/tacz reload` 后在线客户端重新收到 LR index，实体 tracking 与 cooldown 无残留；
5. 缺客户端显示资产时可降级但不崩；flash_shield 条目明确记录为范围外，不伪装支持。

### 已知遗留（备案）

- `ListPackCommand.java` 存在但未在 `RootCommand` 接线——无列包命令。
  是否为移植遗漏待对照 refab 后定（backlog，不阻塞测试）。

## L3. 双客户端联机矩阵（需要两个真实客户端）

准备：同机跑法——`srv/server.properties` 设 `online-mode=false`，
用启动器/Prism 开两个实例（第二个用离线账户名）。异机 LAN 更接近真实。

**每行都对应一段真实网络代码路径，逐行打钩并记录：**

| # | 场景 | 验证的代码路径 | 结果 |
|---|---|---|---|
| 1 | 双客户端加入服务器 | 枪包/配方网络同步（configuration payload + `OnDatapackSyncEvent`）；双方枪械/配方一致；无 payload mismatch 踢出 | |
| 2 | 纯原版客户端尝试加入 | 必需 payload 协商行为——**记录实际表现**（拒绝信息是否可读），不预设结论 | |
| 3 | A 开火，B 旁观 | `ServerMessageGunFire` tracking 转播：B 端第三人称动画、音效、枪口火光 | |
| 4 | A 击中 B（含爆头） | 服务端弹道判定、伤害/爆头倍率、击退（`LivingKnockBackEvent`） | |
| 5 | 换弹 / 切开火模式 / 瞄准 / 匍匐 | `IGunOperator` 状态同步，B 视角姿态正确 | |
| 6 | 工作台全流程 | 放置 → 开 GUI（menu payload）→ 消耗材料合成一把枪（服务端配方校验 `TableRecipe`） | |
| 7 | 断线重连 | 弹匣余弹、配件状态恢复（数据持久化路径） | |
| 8 | `/tacz reload`（服务器执行） | 资源重载 + 全员重同步 | |
| 9 | 双端装 PAL 后重跑 #3/#5 | 第三人称动画在**联机**下（r17-r22 的 PASS 场景是否含联机未记录，需补验） | |
| 10 | 开 F3+B 后重跑 #4 | 爆头盒线渲染（r16 修复）在联机下的回归 | |
| 11 | （可选）netem/clumsy 加 100ms+ 延迟重跑 #3-#5 | 高延迟下开火/换弹手感与状态回滚 | |
| 12 | A 投掷各类 LR 道具，B 旁观 | `IEntityWithComplexSpawn`、实体 tracking、烟雾/闪光/效果云同步 | |
| 13 | A 连续使用分类冷却道具 | `ServerMessageCustomCooldown` 只发所属玩家；B 不串冷却 | |
| 14 | A 用 LR 近战攻击 B | C2S prepare、服务端索敌/冷却/伤害权威、双方动画/HUD | |
| 15 | 在线 `/tacz reload` 后重跑 #12-#14 | LR 三类 index S2C 重同步、旧状态不泄漏 | |

## L4. 服务器形态矩阵（L2/L3 通过后按优先级推进）

> 每种形态只测它**独有**的故障面；已被上一形态覆盖的不重复。
> 未测的形态对外一律说"未测试"，不说"支持/不支持"。

| 优先级 | 形态 | 独有故障面 | 怎么测 | 状态 |
|---|---|---|---|---|
| **1** | 内置服务器 + LAN/本地隧道 | 双客户端基础同步 | 跑 L3 全矩阵 | ❌ 26.2 未测；26.1.2 R1 基线见 records #1/#2 |
| **2** | **真实专用服务器**（NeoForge 安装器 `--installServer`） | 生产类加载、dedicated dist、jar 内资源导出、纯远程同步 | 本预案 **L2** + 两客户端 **L3** | ⚠️ LR 合入前核心候选 PASS；当前 artifact 与 LR 专项待重跑 |
| 3 | 面板服/托管商 | 受限内存/面板注入的启动脚本与 JVM 参数 | 任选一家面板装同一 jar，重点看启动内存与枪包导出目录写权限 | ❌ 未测 |
| 4 | 离线模式混跑（online-mode=false） | UUID 体系差异 → 按 UUID 键控的玩家持久化数据（弹匣余弹/配件） | 同一玩家离线名/正版各进一次，检查数据不串号 | ⚠️ 本轮 LAN 实为离线模式，未专项验证 |
| 5 | 代理网络（Velocity modern forwarding 后挂 NeoForge 后端） | 握手/自定义 payload 过代理、跨服切换后的枪包重同步 | Velocity + 两个后端服，切服后开工作台/开枪 | ❌ 未测 |
| 6 | 混合服（NeoForge + Bukkit 插件：Youer/MohistNeo、Arclight） | 混合核以 mixin 改写 PacketDistributor 与事件桥——正打在本 mod 的 tracking 广播与事件链上；只承诺尽力而为 | **先核查是否存在 26.2 构建**；有则跑 L3 #1/#3/#4/#6 | ❌ 未测；无 26.2 构建则标“生态未达，暂不适用” |
| 7 | Geyser/基岩互通 | 基岩客户端无本 mod，仅枪械实体/音效的观感 | 低优先 | ❌ 未测 |

**对外口径规则**：README/COMPATIBILITY 只在对应行 ✅ 后才可声明该形态可用；
混合服即使通过也标注"尽力而为"（混合核自身的 mod 兼容即为尽力而为）。

## 记录与归档

- 每次执行留：服务器 `logs/latest.log`、两个客户端 `latest.log`、
  环境行（MC / NeoForge / 本 mod / 可选 mod 全版本）、逐行勾选结果；
- 结果归档到 `docs/records/`（如 `SERVER_TEST_<日期>.md`），
  **未执行的行明确标"未测"，不许留空糊弄**（AGENTS.md §2）；
- 全矩阵通过后，`COMPATIBILITY.md` 与 README 才允许出现"支持专用服务器"表述——
  在那之前对外口径一律"专用服务器未完成实测"。

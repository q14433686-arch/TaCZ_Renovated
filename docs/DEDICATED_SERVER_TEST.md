# 专用服务器测试预案（L1/L2/L3）

> 活文档。Beta-1 的已知盲区：至今所有用户 PASS 都来自单机客户端场景，
> **专用服务器三层均未在真实环境验证过**（开发期 `runServer` 冒烟见 `records/`，
> 但那是开发 classpath，不等于生产）。本预案按成本从低到高排列，
> 每层写明"这层独有能抓到什么"——跳层等于放走对应故障类。

执行者：项目发起人本机，或有 JDK 25 + 外网的 AGENT 环境。
（Arena 沙盒 2026-08-21 核实：无 JDK、无直连外网，只能做文档与静态分析。）

---

## L0. 构建后静态自检（1 分钟，不用起游戏）

```bash
./gradlew build
JAR=build/libs/tacz-1.1.8+neoforge.26.1.2.Beta-1.jar   # 以实际文件名为准
unzip -l "$JAR" | grep -E "META-INF/jarjar/|luaj|commons-math3"
unzip -l "$JAR" | grep -E "tacz.*mixins.json|accesstransformer.cfg"
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "version=|modId=" 
```

判据：

- [ ] `META-INF/jarjar/` 下有 **luaj** 与 **commons-math3**（缺 = 生产必炸
      `NoClassDefFoundError: org/luaj/vm2/LuaError`，见 `LICENSES.md` 前科）；
- [ ] 三个 mixin json（`tacz` / `tacz.iris` / `tacz.carryon`）与 AT 文件在 jar 内；
- [ ] mods.toml 中 `version` 已展开为 `1.1.8+neoforge...`（**不得**残留 `${mod_version}`，
      **不得**是 `-neoforge`）。

## L1. 开发环境 runServer（10 分钟）

```bash
./gradlew runServer --no-daemon     # 首次需在 run/ 接受 eula
```

判据（对照 `records/` 各期冒烟口径）：

- [ ] Mod List 出现 `TaCZ: Renovated 1.1.8+neoforge.26.1.2.Beta-1 (tacz)`；
- [ ] 日志有 payload 注册行与枪包装载行（Beta-1 基线：`guns=54 ammo=24 attachments=99
      blocks=4 recipes=182`，数字变了要能解释）；
- [ ] 到 `Done`，`stop` 干净退出；
- [ ] 全程无 `NoClassDefFoundError`（dedicated classpath 没有
      `LocalPlayer`/`Minecraft`——历史崩溃类，S2C 处理必须留在 `ClientPacketBridge` 后面）。

这层抓：注册/网络/资源加载在 server dist 的类加载问题。抓不到：jarJar、生产 mixin/AT。

## L2. 生产 jar + 真实 NeoForge 服务端（30 分钟，纯 headless）

**这是最常被跳过、恰恰最容易炸的一层**——开发 classpath 与生产模块类加载器
行为不同（jarJar、mixin 服务发现、AT 应用、默认枪包从 jar 而非目录导出）。

```bash
# 1. 官方安装器装服务端（版本必须 26.1.2.x release）
java -jar neoforge-26.1.2.97-installer.jar --install-server srv
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
- [ ] 放入一个依赖 `lrtactical` 的第三方枪包 → 服务器**不崩**、枪械部分装载
      （LR 道具不可用是预期行为，见 README §2）；
- [ ] `stop` 干净退出，重启第二次（验证导出目录已存在时的幂等）。

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

## L4. 服务器形态矩阵（L2/L3 通过后按优先级推进）

> 每种形态只测它**独有**的故障面；已被上一形态覆盖的不重复。
> 未测的形态对外一律说"未测试"，不说"支持/不支持"。

| 优先级 | 形态 | 独有故障面 | 怎么测 | 状态 |
|---|---|---|---|---|
| — | 内置服务器 + LAN/本地隧道 | 双客户端基础同步 | 已按 L3 前哨执行 | ✅ 基础通过（2026-08-21，records #2；完整 11 行矩阵未逐行打钩） |
| **1** | **真实专用服务器**（NeoForge 安装器 `--install-server`） | 生产类加载（jarJar/mixin/AT）、dedicated dist 无客户端类、jar 内资源导出、无宿主玩家的纯远程同步 | 本预案 **L2** + 连两个客户端跑 **L3 全矩阵** | ❌ 未测（**最重要的缺口**） |
| 2 | 面板服/托管商 | 受限内存/面板注入的启动脚本与 JVM 参数 | 任选一家面板装同一 jar，重点看启动内存与枪包导出目录写权限 | ❌ 未测 |
| 3 | 离线模式混跑（online-mode=false） | UUID 体系差异 → 按 UUID 键控的玩家持久化数据（弹匣余弹/配件） | 同一玩家离线名/正版各进一次，检查数据不串号 | ⚠️ 本轮 LAN 实为离线模式，未专项验证 |
| 4 | 代理网络（Velocity modern forwarding 后挂 NeoForge 后端） | 握手/自定义 payload 过代理、跨服切换后的枪包重同步 | Velocity + 两个后端服，切服后开工作台/开枪 | ❌ 未测 |
| 5 | 混合服（NeoForge + Bukkit 插件：Youer/MohistNeo、Arclight） | 混合核以 mixin 改写 PacketDistributor 与事件桥——正打在本 mod 的 tracking 广播与事件链上；业界共识兼容为"尽力而为"，多数 mod 作者拒绝混合服支持 | **先核查是否存在 26.1.2 构建**（Arclight 的 NeoForge 线现到 1.21.1；Youer 版本线需查）；有则跑 L3 矩阵 #1/#3/#4/#6 四行 | ❌ 未测；无 26.1.2 构建则标"生态未达，暂不适用" |
| 6 | Geyser/基岩互通 | 基岩客户端无本 mod，仅枪械实体/音效的观感 | 低优先 | ❌ 未测 |

**对外口径规则**：README/COMPATIBILITY 只在对应行 ✅ 后才可声明该形态可用；
混合服即使通过也标注"尽力而为"（混合核自身的 mod 兼容即为尽力而为）。

## 记录与归档

- 每次执行留：服务器 `logs/latest.log`、两个客户端 `latest.log`、
  环境行（MC / NeoForge / 本 mod / 可选 mod 全版本）、逐行勾选结果；
- 结果归档到 `docs/records/`（如 `SERVER_TEST_<日期>.md`），
  **未执行的行明确标"未测"，不许留空糊弄**（AGENTS.md §2）；
- 全矩阵通过后，`COMPATIBILITY.md` 与 README 才允许出现"支持专用服务器"表述——
  在那之前对外口径一律"专用服务器未完成实测"。

# TaCZ: Renovated 26.2 兼容矩阵

适用构建：`1.1.8+neoforge.26.2.R2`（**R2 期快照**；TML / PIP 的专项说明见根 README §2 与
[`docs/MESH_LOADER.md`](docs/MESH_LOADER.md)）

游戏 / 加载器：Minecraft 26.2 / NeoForge 26.2.0.64  
核验日期：2026-08-22（2026-09-02 补：Iris 行实机反馈）

> **状态纪律**：下表的“API 已核”表示发布文件、坐标与源码签名已核对，**不等于游戏内
> PASS**。旧 L0-L3 回执覆盖 LR 合入前的 26.2 核心候选；当前 R2 候选必须
> 重跑构建、专服与多人。26.1.2 的 LR 单机/专服 PASS 也不自动继承到 26.2。

## 内置 LRTactical

| 范围 | 本仓接入 | 当前状态 |
|---|---|---|
| 四类物品与五类投掷行为 | throwable / melee / detonator / consumable；explode / sticky / smoke / stun / effect-cloud | 26.2 source/API 已核；未实机 |
| 数据与网络 | index/data/recipe/filter/Lua；独立 `lr1` payload；登录/重载同步 | NeoForge 26.2 API 已核；未实机 |
| 客户端反馈 | tooltip、使用进度 HUD、分类冷却、耳鸣、实体/动态物品渲染 | 26.2 mixin/事件目标已核；未实机 |
| 明确排除 | flash_shield；原作 ARR 美术 | 不实现/不打包，不宣称支持 |

来源与 descriptor 证据见
[`docs/records/LR_R1_SYNC_26_2_20260822.md`](docs/records/LR_R1_SYNC_26_2_20260822.md)。

## 依赖钉选

| Mod | 26.2 NeoForge 构建 | 本仓接入 | 当前状态 |
|---|---|---|---|
| Cloth Config | `26.2.155`，`me.shedaniel.cloth:cloth-config-neoforge:26.2.155` | T 键 / Mods 配置页；缺失时下载提示 | 坐标与 API 已核；未实机 |
| Player Animation Library (PAL) | `1.2.6+26.2` merged Fabric+NeoForge，Curse file `8674798` | 第三人称枪械动画 | 源码 API 已核；未实机 |
| Controllable | `0.26.1` NeoForge 26.2，Curse file `8403602` | 手柄绑定、连射轮询、开火震动 | 源码 API 已核；未实机；运行时另需其 Framework 依赖 |
| Shoulder Surfing Reloaded | `5.0.7` NeoForge 26.2，Curse file `8445037` | v5 plugin event、双手枪械 adaptive aim、准星 | plugin/API/发现机制已核；未实机 |
| JEI | `30.24.0.176`，`mezz.jei:jei-26.2-neoforge:30.24.0.176` | 工作台、配件/弹药查询、subtype | 30.24 source/API 已核；未实机 |
| REI | `26.2.820` NeoForge，Curse file `8271756` | 工作台、配件/弹药查询、subtype、同步后 reload | source/API 已核；未实机 |
| Architectury API | `21.0.2` NeoForge | REI 26.2.820 的编译/运行依赖 | 按 REI 26.2 source 原始 pin |
| Iris | `1.11.2` NeoForge 26.2 | 反射 API、HAND/HAND_TRANSLUCENT、shadow、linked-fragment mask bridge | OpenGL source/API 已核；实机反馈 2026-09-02（用户，Iris 1.11.2 + ComplementaryUnbound r5.8.1）：PIP 二次渲染与目镜掩码孔径裁切行为确认（两项均非逐条矩阵 PASS） |
| Carry On | `2.11.0` NeoForge 26.2 | 多格工作台 root/companion、放置预检、携带模型 BlockId | 2.11.0 descriptor 已核；未实机 |
| First-person Model | **无 NeoForge 26.2 文件**；2.7.2 只有 Fabric 26.2，NeoForge 止于 26.1.2 | 反射 ActivationHandler 已按 2.7.2 API 预留 | 当前不列为可安装兼容；桥保持 dormant |
| Not Enough Animations | **无 NeoForge 26.2 文件**；1.12.4 的 NeoForge 文件止于 26.1.2 | 直接手臂提交 guard 已按 1.12.4 API 预留 | 当前不列为可安装兼容；桥保持 dormant |
| Punchy! | 有 NeoForge 26.2：`2.7d`，Curse file `8697217` | 可选 `@Pseudo` mixin 走 Punchy 既有 blacklist / 让出路径 | API/target 已核；未实机。无 Punchy 时 plugin 不应用 mixin |

## 源码证据

- **PAL 1.2.6+26.2**：tag `v1.2.6+26.2` →
  `7d2a480808962608018ea77b23fdebe9baaa3ea8`。已核
  `PlayerAnimationFactory.FactoryHolder#registerFactory`、
  `PlayerAnimationAccess#getPlayerAnimationLayer`、controller/fade/adjustment/loader API。
- **Controllable 0.26.1+26.2**：tag `v0.26.1+26.2` →
  `7333428d29464db914750eac2a039c22102e3e65`。已核 `ButtonBinding` 构造器、
  `InGameContext` protected 构造器、`OnPressAndReleaseHandler#create`、
  `Controllable#getBindingRegistry/#getController`、`Controller#isButtonPressed/#rumble`。
- **Shoulder Surfing 5.0.7**：tag `26.2-5.0.7` →
  `ab65e01733dbe1ae70fba90bc2744c1682018539`。已核
  `IShoulderSurfingPlugin#register(IEventBus)`、
  `ComputePlayerAimStateEventHandler` 与 NeoForge `PluginLoaderNeoForge` 对
  `shouldersurfing_plugin.json` 的扫描。
- **JEI 30.24 source line**：commit
  `886b3644c62f4c18ffa22a23a0de0e1130e2f507`（`specificationVersion=30.24.0`）。
  本仓 34 个 JEI import 均在 26.2 source 存在；NeoForge 端继续通过
  `RecipesReceivedEvent` 启动/刷新。
- **REI 26.2.820**：branch commit
  `2be20928abd9f1164fd9fd251268041c036b580f`。本仓 REI import 均存在；额外
  `me.shedaniel.math` 类型由 Cloth/REI 依赖提供；`reloadPlugins(MutableLong,ReloadStage)`
  两参入口存在。该 source 明确 pin Cloth `26.2.155`、Architectury `21.0.2`。
- **Iris 26.2**：branch commit
  `8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a`。已核 API revision 3、
  `assignPipeline`、`isRenderingShadowPass`、HandRenderer 三个查询，以及
  `ShaderCreator#link` 的 fragment-source 参数位置。
- **Carry On 2.11.0**：tag `v2.11.0` →
  `b82a8ccfe8b4a9af98b7485826c2162e8faaae81`。已核：
  `PickupHandler#tryPickUpBlock(ServerPlayer,BlockPos,Level,BiFunction)`、
  `PlacementHandler#tryPlaceBlock(ServerPlayer,BlockPos,Direction,BiFunction)`、
  `CarryRenderHelper#getRenderItemStack(Player)->ItemStackTemplate`，以及
  `CarriedObjectRender#drawBlock` 紧接 `.create()` 的调用点。
- **FPM / NEA API**：FPM 2.7.2 tag
  `eef8f91206c9f0ad1681111235c0d802349f986a` 的
  `FirstPersonAPI#registerPlayerHandler(Object)` / `ActivationHandler#preventFirstperson()`；
  NEA 1.12.4 tag `dd7e5e191839de8044b8bc942304e2b1ead7950f` 的
  `NEAnimationsLoader.INSTANCE.playerTransformer` /
  `PlayerTransformer#renderingFirstPersonArm(boolean)`。当前只是未来兼容预留，因为无
  NeoForge 26.2 发布文件。
- **Punchy! 2.7d NeoForge 26.2**：本体 ARR、无公开源码，未下载/反编译 jar。class/method
  目标由公开兼容层交叉核对：姊妹项目
  `TaCZ_Refabricated_Unofficial` 的 Punchy `@Pseudo` mixin、Scorched Guns NeoForge
  `top.ribs.scguns.mixin.client.compat.punchy.*`，以及 Epic Fight Compat
  `dev.khanhtimn.efcompat.mixins.punchy.*`（直接 import `punchy.client.render.PunchyArmRenderer`、
  `punchy.client.state.HandEquipStateMachine`、`punchy.client.state.MovementStateMachine`，
  并 wrap `wasItemBlacklisted` / `renderFirstPerson`）。本仓只取“持 TACZ viewmodel 时让出”
  的游戏语义，不复制 Fabric API 表面。`@Pseudo + require=0`：目标改名时无 Punchy 实例不崩，
  有 Punchy 时仍需按矩阵复测。

## 图形后端边界

| 后端 | 状态 |
|---|---|
| OpenGL（无 Iris） | 阶段边界离屏 ocular mask 已接入；GPU 未实测 |
| OpenGL + Iris 1.11.2 | HAND pipeline 分类、linked-fragment dormant branch 与逐 draw mask uniform bridge 已接入；GPU 未实测。本轮两处改动：① mask uniform/采样器改为 `trySetup` RETURN + `iris$setupState` RETURN 双写入点，不再依赖与 Iris 的 mixin 应用顺序；② 凸包孔径填充改为斜率空间、不再读投影 UBO（旧实现开光影后必抛 `Buffer is not readable` 而每帧回退描摹）。**复测**：2026-09-02
用户实机（Iris 1.11.2 + ComplementaryUnbound r5.8.1）开镜与 PIP 二次渲染行为确认；
逐条矩阵未跑 |
| Vulkan | `earlyWindowControl=false` 后用户启动 PASS；低倍准星 containment 报告 FAIL，已拆分 reticle-only/full-viewmodel mask 修复，当前 HEAD 待复测 |
| 其他 shader replacement / Aperture | 没有已核 bridge 时走普通未掩码回退；未作为硬依赖接入 |

## 其他兼容层

- **ImmediatelyFast**：26.2 已没有旧 `ImmediatelyFastApi#getBatching` 接口；Feature
  Rendering 下不需要手动断批。本仓 hook 是有意 no-op，不应描述成“修复/加速支持”。
- **Accelerated Rendering**：没有已核的 26.2 Feature Rendering API；加速路径强制关闭，
  普通渲染不受影响。
- **Zoomify**：没有 NeoForge 26.2 构建，hook 为 no-op。
- **TaCZ Tweaks（`tacztweaks`）**：第三方 addon，本仓不接入、不依赖。
  `2.14.2+neoforge.26.2.Beta-1`（源码 `q14433686-arch/TaCZTweaks_Unofficial` 分支
  `26.2-neoforge`）已逐文件核对：不含任何渲染 / 光影 / 掩码代码，也没有 Iris mixin。
  它自己不含渲染代码，但进入 mod 列表会改变 mixin config 应用顺序，
  而本仓的镜内裁剪原先在该顺序上是脆的（Iris 与本仓都在 `trySetup` RETURN 注入），
  故「装上它才坏」是真实因果链、只是不经过它的代码。已改为与顺序无关，取证见
  [`docs/records/SCOPE_MASK_HULL_SLOPESPACE_20260827.md`](docs/records/SCOPE_MASK_HULL_SLOPESPACE_20260827.md)。
- **Carry On**：`tacz:target` / `tacz:statue` 仍在黑名单；多格工作台不在黑名单，依赖
  optional mixin 做原子 root/companion 处理。

## 必须执行的用户矩阵

1. 无可选 Mod：客户端、专服、进服、枪包同步、工作台。
2. Cloth only：T 键、Mods 配置页、保存；无 Cloth 的提示回退。
3. PAL only：第三人称持枪、切枪、趴姿→站立、淡出重复。
4. Controllable + Framework：绑定、按住连射、换弹/近战/瞄准、各 fire mode 震动。
5. Shoulder Surfing 5.0.7：双手枪判定、adaptive aim、free-look、准星。
6. JEI only / REI only / JEI+REI：默认包、第三方包、远程同步后刷新、工作台 catalyst。
7. Iris 1.11.2：无光影/有光影、HAND solid/translucent、shadow、mask mode 泄漏、水/粒子/云。
8. Carry On 2.11.0：A/B/C 工作台任一半格搬起、完整放下、阻挡时原子失败、BlockId 模型。
9. Vulkan：阶段边界 target 切换、mask debug 预览、无 device loss、镜身/准星/火光裁剪。
10. LRTactical：单机与专服分别验证 tooltip/HUD、投掷/近战/消耗品、烟雾/闪光、
    index 同步、实体 tracking、分类冷却及至少一个 LR 内容包。
11. Punchy! 2.7d：普通工具动画仍由 Punchy 控制；枪/刀/手雷无第二套手臂、无
    walk/sprint/camera-lag 叠层，右手贴在枪上；收起后恢复。另测开镜灵敏度与无
    Punchy 时同量级。光影目镜裁切不在本轮修复范围内。

完成上述测试前，只能写“API/坐标已核”，不能写“兼容 PASS”。

# TaCZ NeoForge 26.2 兼容矩阵

适用构建：`1.1.8+neoforge.26.2.0.r0`  
游戏 / 加载器：Minecraft 26.2 / NeoForge 26.2.0.64  
核验日期：2026-08-21

> **状态纪律**：下表的“API 已核”表示发布文件、坐标与源码签名已核对，**不等于游戏内
> PASS**。本执行环境没有可用的 JDK 25 / Gradle 依赖下载通道和 GPU；所有运行矩阵仍需
> 用户实机完成。未实际测试的项目不会标成 PASS。

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
| Iris | `1.11.2` NeoForge 26.2 | 反射 API、HAND/HAND_TRANSLUCENT、shadow/depth hand shader | OpenGL source/API 已核；未实机 |
| Carry On | `2.11.0` NeoForge 26.2 | 多格工作台 root/companion、放置预检、携带模型 BlockId | 2.11.0 descriptor 已核；未实机 |
| First-person Model | **无 NeoForge 26.2 文件**；2.7.2 只有 Fabric 26.2，NeoForge 止于 26.1.2 | 反射 ActivationHandler 已按 2.7.2 API 预留 | 当前不列为可安装兼容；桥保持 dormant |
| Not Enough Animations | **无 NeoForge 26.2 文件**；1.12.4 的 NeoForge 文件止于 26.1.2 | 直接手臂提交 guard 已按 1.12.4 API 预留 | 当前不列为可安装兼容；桥保持 dormant |

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
  `assignPipeline`、`isRenderingShadowPass`、HandRenderer 三个查询与 ShaderCreator
  fragment ordinal。
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

## 图形后端边界

| 后端 | 状态 |
|---|---|
| OpenGL（无 Iris） | depth-aperture 代码/API 已移植；GPU 未实测 |
| OpenGL + Iris 1.11.2 | pipeline 分类与 hand shader bridge 已核源码；GPU 未实测 |
| Vulkan | 只承诺**不执行 OpenGL depth copy**；瞄具走无 depth/unmasked 降级；未实测 |
| Aperture | 未发布/未接入；不在本期支持范围 |

## 其他兼容层

- **ImmediatelyFast**：26.2 已没有旧 `ImmediatelyFastApi#getBatching` 接口；Feature
  Rendering 下不需要手动断批。本仓 hook 是有意 no-op，不应描述成“修复/加速支持”。
- **Accelerated Rendering**：没有已核的 26.2 Feature Rendering API；加速路径强制关闭，
  普通渲染不受影响。
- **Zoomify**：没有 NeoForge 26.2 构建，hook 为 no-op。
- **Carry On**：`tacz:target` / `tacz:statue` 仍在黑名单；多格工作台不在黑名单，依赖
  optional mixin 做原子 root/companion 处理。

## 必须执行的用户矩阵

1. 无可选 Mod：客户端、专服、进服、枪包同步、工作台。
2. Cloth only：T 键、Mods 配置页、保存；无 Cloth 的提示回退。
3. PAL only：第三人称持枪、切枪、趴姿→站立、淡出重复。
4. Controllable + Framework：绑定、按住连射、换弹/近战/瞄准、各 fire mode 震动。
5. Shoulder Surfing 5.0.7：双手枪判定、adaptive aim、free-look、准星。
6. JEI only / REI only / JEI+REI：默认包、第三方包、远程同步后刷新、工作台 catalyst。
7. Iris 1.11.2：无光影/有光影、HAND solid/translucent、shadow、瞄具 depth/水/粒子/云。
8. Carry On 2.11.0：A/B/C 工作台任一半格搬起、完整放下、阻挡时原子失败、BlockId 模型。
9. Vulkan：启动不调用 GL、瞄具降级不黑屏、warning 只出现一次。

完成上述测试前，只能写“API/坐标已核”，不能写“兼容 PASS”。

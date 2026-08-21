# TaCZ NeoForge 26.2 changelog（草稿）

> 仅记录已经落入本分支的改动。未执行的实机/兼容矩阵不得写成 PASS。

> **冻结过程记录**：本文件保留 WP-262 分包迁移过程，不作为当前发行说明。
> 对外版本变更以根 `CHANGELOG.md` 为准；当前状态以 `docs/PORTING_STATUS.md` 为准。
> 文中“待重建”等措辞反映当时阶段，已被后续 L0-L3 用户 PASS 取代；最终版本随后定名 R1。

## Unreleased — `1.1.8+neoforge.26.2.0.R1`

### WP-262-0（仍基于 26.1.2）

- 将弹药来源、工作台扣料与相关公开 helper 从 NeoForge 已标记
  `forRemoval` 的 `IItemHandler` API 迁移到事务式
  `ResourceHandler<ItemResource>` API。
- 退弹入包改用原版 `Inventory#placeItemBackInInventory(ItemStack)`；背包满时仍会掉落实体。
- 删除无引用的 `GunPackProgressScreen`。
- commit `c40dab9` 曾获用户 JDK 25 build PASS；当前 scope-mask 替换后的 HEAD 需重建。

### WP-262-1

- 构建目标前滚到 Minecraft 26.2 + NeoForge 26.2.0.64（release）。
- 版本设为 `1.1.8+neoforge.26.2.0.R1`，保持 `1.1.8` SemVer core 与枪包依赖兼容。
- 对齐官方 MDK-26.2：Gradle 9.2.1、ModDevGradle 2.0.144、Foojay 1.0.0、
  Java 25、`[26.2]` 精确游戏范围，并修正 `gradlew` 可执行位。
- 瞄具主路径已改为 26.2 阶段边界离屏 ocular mask；当前 HEAD 构建与服务端启动待重跑。

### WP-262-2

- 将全部直接 `Minecraft.screen` / `Minecraft#setScreen` 使用迁到 26.2 的
  `Minecraft.gui.screen()` / `Gui#setScreen(...)`。
- HUD/准星取消继续走 NeoForge `RenderGuiLayerEvent.Pre`，不注入 vanilla `Hud` 私有流程。
- 交互提示颜色从已移除的 `ChatFormatting#getColor()` 迁到 `TextColor#getValue()`。
- 逐条重验 AT；scope 改用公开 `RenderType#create` 后删除构造器 widening，只保留三个
  真实 transformed classpath 所需的 gameplay 访问条目。
- Mixin compatibility level 对齐 Java 25；注册与 common mixin 目标完成静态 descriptor 核验。
- 专服 `Done` 与枪包装载数字仍待可运行环境验证。

### WP-262-3

- 采用 refab 26.2 已知解：提交阶段收集 ocular，`FeatureRenderDispatcher#executeSolid` 前
  在阶段边界一次性绘制无 depth 的 RGBA8 离屏 mask。
- 镜身/枪身/非瞄具配件/枪口火光在镜内 discard，准星反向约束在镜内；加入凸包填充、
  sight/scope 通道门禁与 `ocular_ring` 普通 RenderType 重画。
- 七条 mask-aware pipeline 经 NeoForge `RegisterRenderPipelinesEvent` 注册；普通路径不调用
  GL API，可进入 OpenGL/Vulkan backend。
- 删除旧 raw-depth copy/restore 类、GL encoder mixin、fragment shader 与 private RenderType AT。
- Iris 改为 linked-fragment dormant mask branch + 每 draw uniform/texture binding；没有已核 bridge
  的 shader replacement 安全回退普通渲染。
- PiP、shape outline、hand 方法与 GameRenderer camera getter 继续使用 26.2 API。
- 当前只有静态 classfile/API/scratch 检查；scope-mask 替换后的 JDK 25 build 与
  OpenGL/Iris/Vulkan GPU 矩阵未执行。

### WP-262-4

- 重钉 26.2 可选编译坐标：Cloth 26.2.155、PAL 1.2.6+26.2、Controllable 0.26.1、
  Shoulder Surfing 5.0.7、JEI 30.24.0.176、REI 26.2.820、Architectury 21.0.2。
- Carry On 兼容对齐 2.11.0：携带渲染的 BlockId 恢复移到
  `ItemStackTemplate#create()` 后的 mutable stack。
- 补全 FPM ActivationHandler 与 NEA direct-arm 反射 guard；执行日两者均没有
  NeoForge 26.2 发布文件，因此桥保持 dormant，不宣称可安装兼容。
- 新增 `COMPATIBILITY.md`，逐项区分“发布/API 已核”与“未实机”，并列出最终用户矩阵。
- 所有可选 Mod 运行矩阵仍未执行，不声明 PASS。

### WP-262-5（发布准备，受阻）

- 新建根 `CHANGELOG.md` 的 Unreleased R1 条目；README、LICENSES、PORTING_STATUS 更新到
  26.2，且显式保留当前 HEAD“待重建/未实测/未发布”状态。
- README 不再充当逐工作包进度表；详细状态只放 `docs/PORTING_STATUS.md`。
- 没有发布 jar：当前 scope-mask HEAD、专服、GPU 与可选 Mod 矩阵未通过，发布闸门保持关闭。

# TaCZ NeoForge 26.2 changelog（草稿）

> 仅记录已经落入本分支的改动。未执行的实机/兼容矩阵不得写成 PASS。

## Unreleased — `1.1.8+neoforge.26.2.0.r0`

### WP-262-0（仍基于 26.1.2）

- 将弹药来源、工作台扣料与相关公开 helper 从 NeoForge 已标记
  `forRemoval` 的 `IItemHandler` API 迁移到事务式
  `ResourceHandler<ItemResource>` API。
- 退弹入包改用原版 `Inventory#placeItemBackInInventory(ItemStack)`；背包满时仍会掉落实体。
- 删除无引用的 `GunPackProgressScreen`。
- 构建验收待在具备 JDK 25 且可下载 Gradle/依赖的环境补跑；当前不声明编译或实机 PASS。

### WP-262-1

- 构建目标前滚到 Minecraft 26.2 + NeoForge 26.2.0.64（release）。
- 版本设为 `1.1.8+neoforge.26.2.0.r0`，保持 `1.1.8` SemVer core 与枪包依赖兼容。
- 对齐官方 MDK-26.2：Gradle 9.2.1、ModDevGradle 2.0.144、Foojay 1.0.0、
  Java 25、`[26.2]` 精确游戏范围，并修正 `gradlew` 可执行位。
- 保留 OpenGL/depth-aperture 为本期瞄具主路径；没有宣称 Vulkan 或 Aperture 支持。
- 服务端启动验收仍待可联网的 JDK 25 环境执行。

### WP-262-2

- 将全部直接 `Minecraft.screen` / `Minecraft#setScreen` 使用迁到 26.2 的
  `Minecraft.gui.screen()` / `Gui#setScreen(...)`。
- HUD/准星取消继续走 NeoForge `RenderGuiLayerEvent.Pre`，不注入 vanilla `Hud` 私有流程。
- 交互提示颜色从已移除的 `ChatFormatting#getColor()` 迁到 `TextColor#getValue()`。
- 逐条重验 AT；仅保留仍为 private 的 `RenderType(String, RenderSetup)` 构造器条目，
  删除四个 26.2 已公开成员的冗余 widening。
- Mixin compatibility level 对齐 Java 25；注册与 common mixin 目标完成静态 descriptor 核验。
- 专服 `Done` 与枪包装载数字仍待可运行环境验证。

### WP-262-3

- 将自定义 pipeline 迁到 26.2 的 `BindGroupLayout`、多 color target、vertex binding、
  `PrimitiveTopology` 与 `GpuFormat` API，并通过 NeoForge `RegisterRenderPipelinesEvent` 注册。
- 保留 OpenGL depth-aperture；`RenderType#draw` operation 边界迁到
  `PreparedRenderType` + 26.2 八参数 GL encoder draw 边界。
- 对齐 26.2 reversed-Z compare、depth bias 与 GLSL aperture 深度判据。
- Vulkan 下不构造 GL-only pipeline，瞄具改走隐藏 opaque ocular 的未掩码降级，并记录一次 warning；
  不声明 Vulkan depth/光影或 Aperture 支持。
- PiP 改为框架传入 `SubmitNodeCollector`；爆头框改走官方 shape-outline feature；
  第一人称 hand 方法、GameRenderer camera getter 全部对齐 26.2 descriptor。
- Iris 1.11.x 反射入口按 26.2 source 重验，恢复 shadow-pass 查询，并保留
  HAND/HAND_TRANSLUCENT pipeline 分类与 hand fragment depth 分支。
- 当前只有静态 classfile/API 检查；JDK 25 全仓构建及 OpenGL/Iris/Vulkan GPU 矩阵未执行。

### WP-262-4

- 重钉 26.2 可选编译坐标：Cloth 26.2.155、PAL 1.2.6+26.2、Controllable 0.26.1、
  Shoulder Surfing 5.0.7、JEI 30.24.0.176、REI 26.2.820、Architectury 21.0.2。
- Carry On 兼容对齐 2.11.0：携带渲染的 BlockId 恢复移到
  `ItemStackTemplate#create()` 后的 mutable stack。
- 补全 FPM ActivationHandler 与 NEA direct-arm 反射 guard；执行日两者均没有
  NeoForge 26.2 发布文件，因此桥保持 dormant，不宣称可安装兼容。
- 新增 `COMPATIBILITY.md`，逐项区分“发布/API 已核”与“未实机”，并列出最终用户矩阵。
- 所有可选 Mod 运行矩阵仍未执行，不声明 PASS。

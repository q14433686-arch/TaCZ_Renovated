# 更新日志

版本号格式：`1.1.8+neoforge.<mc>.<标签>`。`+` 后是 SemVer build metadata，不参与
`>=1.1.8` 排序；禁止改用 `-neoforge...` pre-release。

## Unreleased

（R2 定名之后的增量写在里；发布时并入下一版条目。）

### 修复：二次渲染（Iris）镜内误画视模 —— 「高模枪枪身仅二次渲染时被高倍镜裁切」（2026-09-02）

- **症状**：高模枪的枪身在仅开 `ScopePipRerender` 时被「高倍镜」裁出孔径
  形状的孔；重投影形态无；1.21.11 / 26.1.2 线无（没有二次渲染 PIP）。
- **根因**：Iris 把手部渲染搬进 `LevelRenderer#render` 内部（实机日志调用
  栈 + Iris 1.11.2 源码双证）⇒ 二次渲染的镜内那一遍（又一次
  `levelRenderer.render`）**自带一趟手部 pass**：视模立方体按窄投影画进
  镜内画面，并被同一遍刚画的窄投影孔径掩码裁孔；合成把它贴进镜片。
  旧防线（`renderAfterSolid` 的镜内闸）只挡了 mesh 表、漏了立方体。
- **修法**：新 `IrisHandRendererMixin` 在 `HandRenderer.renderSolid` /
  `renderTranslucent` HEAD 取消镜内那一遍的手部 pass（镜内画面按定义
  只有放大后的世界）；首次跳过打一行 log 兼作注入匹配证据。另加
  `maskReadyForViewmodel` 一次性 gate 诊断（每种「判定 × 帧形态」组合
  一行，封顶 8 行）供无光影形态的复测取证。
- 完整排查（含「裁剪链本身模式无关」的穷举结论与未决项）见
  `docs/records/BUG_MESHGUNBODY_SCOPE_CLIP_RERENDER_20260902.md`；
  实机判据见 `docs/MESH_LOADER.md` §5.2 第 18 条。
  **证据级别：静态闭环 + Iris 源码/实机日志核对；CI 与实机未跑，不宣称已修。**

### 同步 Fabric 26.2 线 `arena/01a05e3e`（tip `dee2578d`，2026-09-02 对账，R5）

> 游戏语义权威线（AGENTS §0）自 R4 取货点 `bf5bc5a` 又前进 6 笔实质提交
> （+ 探针/文档），逐 commit 核对本线基线后等价移植 5 件、判 4 项不适用；
> 完整对照与回执见 `docs/records/REFAB_SYNC_0105E3E_R5_20260902.md`，
> 验证判据见 `docs/MESH_LOADER.md` §2.8 / §5.2 第 13-17 条 / §5.4 第 7 条。
> **证据级别：静态闭环 + 逐字节基线比对 + NeoForge 源码联网核对；本线 CI 编译门
> 与实机均未跑，不宣称已修。**

- **mesh 枪身开镜目镜裁剪**（她 `7227ff99`，26.1.2 线 `ee77059` 同款缺口）：
  collector 枪身走 `clipForViewmodel` 的 SCOPE_MASK 管线，GPU 手部表画的 mesh
  枪身却从不经过那次替换 ⇒ 枪管穿进镜内画面。新 `LIT_CLIPPED_PIPELINE`
  （无光影裸 pass）+ 光影侧手部表过 `clipForViewmodel`（与立方体枪身同一份
  替换），两路共用 `maskReadyForViewmodel(true)` 判据、与立方体裁剪同开同关；
  世界表不裁。
- **PIP 二次渲染：镜内那遍世界表「各自登记、各自画、画完即清」**（她
  `3151adcd`→`dc24a2b7`，与 1.21.11 `237dc153` / 26.1.2 `db360639` 同因同修）：
  26.2 每遍 `LevelRenderer#render` 都会把本帧提交节点重画一次 ⇒ 镜内那遍
  **确实重新提交**世界 mesh 枪（她自己的哨兵日志被用户实机 latest.log 推翻
  了第一轮的「不适用」裁定）。删 `shouldSubmitGpuWorld` 的镜内拒收、
  `renderWorldAfterSolid` 镜内画完即清表，修掉「二次渲染镜头里高模枪退化成
  立方体、退镜/关二次渲染就正常」；两条 log-once 常驻供回报对表。
- **开镜距离补偿**（她 `08869095`）：两道 poly 距离闸门（48 / 16 格）原按裸眼
  距离判定，4x 镜下观感只剩 12 / 4 格 ⇒ 举镜看到的掉落物/第三人称 mesh 枪
  几乎必然是立方体。闸门阈值乘 `ScopePipRenderer.currentDetailZoom()`
  （`1+(zoom-1)·progress`，收镜回 1），`Throwable` 守卫不连坐。
- **纹理 render pass 外预解析**（她 `99b15b28`，26.1.2 线 `2ae4c29` 踩坑）：
  懒加载的贴图在 pass 内上传被拒 ⇒ 全 GPU 提交的枪贴图永远加载不上、每帧报错
  + 紫黑。`drawList` 改在 `createRenderPass` 之前预解析 `viewsByTexture`，
  失败按纹理去重打日志。
- **预热窗口挡 `allChanged`**（她 `99b15b28`，26.1.2 线 `d3f0fdc` 实机崩溃链）：
  `LevelExtractorScopePassMixin` 的取消闸扩到 `isBuildingScopePipeline()`
  预热构建窗口，防 Voxy 主栈改绑瞄具管线 ⇒ 主画面远景永久错乱。
- 交叉印证（上轮已搬，本轮补证）：跨包合成 `tacz:nbt` 的肇事材料样本
  （`{"type":"tacz:nbt",…,"items":"tacz:attachment"}`）与上轮 `RecipeCompat`
  改写覆盖的形状逐字吻合；`PartialNbtIngredient` 与她的 `TaczNbtIngredient`
  已逐语义对读等价。

**明确不搬**（理由见 records 文档 §2）：FCAP 保存断桥桥接（本线原生
NeoForge `ModConfigSpec.save()` 落盘链路已接好并经 R2 实机，FCAP 断层不存在；
已联网核 NeoForge 源码）、`tacz:nbt` 注册一等 ingredient（改写路径全覆盖，
`PartialNbtIngredient` 语义等价）、空闲释放拒释放熔断（本线从未引入空闲释放
实验入口）、日志级别（保留与 1.21.11 姊妹线一致的 `warn`）。**遗留另案**：
世界语境贴图与第一人称不同源（少 `/uv/` ⇒ missing-texture 兜底，她侧同样
未修）。版本号**未动**（仍 `1.1.8+neoforge.26.2.R2`）⇒ README 无需跟改。

### 同步姊妹 1.21.11 线 `arena/01a05e43`（tip `5fa0963`，2026-09-01 对账）

> 姊妹线 `41319d7`（其自 Fabric 线 `01a05db2` 的同步）逐 commit 核对本线现状后等价移植；
> 完整对照与「不搬清单」见 `docs/records/SYNC_SIBLING_0105E43_20260901.md`。
> **证据级别：源码级静态闭环（含动画三文件逐字节比对）；本线 CI 编译门与实机均未跑，不宣称已修。**

- **tooltip 纯查表**（姊妹 `c9b8ba1`，对齐本线 `ec51f556`）：
  `ClientAttachmentItemTooltip` / `ClientBlockItemTooltip` 不再走 `I18n.get`
  （「查表 + `String.format`」的格式化接口，枪包把 `tooltipKey` 写成含 `%` 的内联串时
  返回 `"Format error: ..."`），改用 `Language.getInstance().getOrDefault(...)` 纯查表。
  `PapiManager` 本线早已同修（`ec51f556`），与姊妹 tip 逐字等价，未动。
- **`tacz:nbt` 跨包材料**（Fabric 线 `e1aad10` 第 1 件，经姊妹线等价改写）：
  枪包升级工具产出的 `tacz:nbt`（`items` 常为单字符串、`partial` 布尔）本线此前不认、
  整条配方解析失败。`RecipeCompat.normalizeCustomIngredient` 现在把 `tacz:nbt` 改写为
  已注册的 `neoforge:ingredient_type=tacz:partial_nbt`（`strict = !partial`，缺省 strict，
  与本线 `PartialNbtIngredient.CODEC` 的 `optionalFieldOf("strict", false)` 吻合），
  `items` 字符串→数组；旧的无 type `{item+nbt}` 写法不再**静默丢弃 nbt**，
  改写为 partial 宽松子集语义并 INFO 记一笔；`GunSmithTableIngredient` 解析失败的 WARN
  带上规范化形态 + 原文，catch 面加 `LinkageError`。（改写逻辑 13 组 JSON 用例独立模拟
  全 PASS，Java 编译待 CI。）
- **`/tacz overwrite` 落盘**（等价姊妹 `cd14a2a` 第 4 项）：命令行开关绕过了 Cloth 面板
  的 savingRunnable，改完重启回默认；现在 `PreLoadConfig.spec.save()` 显式落盘
  （本线原生 NeoForge `ModConfigSpec#save` = `loadedConfig.save()`，无需 FCAP 那套
  ConfigPersist）。
- **lang 补 2 键**（en/zh，取值与姊妹线逐字一致）：`attribute.name.tacz.bullet_resistance`
  （被 `ModAttributes` 实际引用，此前属性名显示原始键）与 `commands.tacz.arguments.enum.invalid`
  （本线暂无 Java 引用，按三线键表对齐防缺键）。

**明确不搬**（对照姊妹 `41319d7`，理由见 records 文档）：P0-a functionalTasks 手动 flush
（本线 `super.submit` → `BedrockModel#submit` 自带无条件 `submitFunctionalTasks`，
姊妹线系其自回放架构特有病）、P1 镜内文字掩码裁剪全套（本线已有更完整独立实现：
`ScopeTextSubmitter`/`ScopeTextRenderTypes`/`scope_text_final.fsh`/`ScopeFinalRingOverlay`
延迟路径 + Iris 桥）、动画两修（本线三文件与姊妹 tip 逐字节相同，无差异可搬）、
LR「幽灵使用」/耳鸣资源（姊妹 `81dfb50`/PR #24 的全部内容本线已逐件在位，无差异可搬）、
姊妹 CI workflow `.yml`（按仓库所有者流程由人手动跟进）。版本号**未动**（仍 `1.1.8+neoforge.26.2.R2`）⇒ README 无需跟改。

## 1.1.8+neoforge.26.2.R2 — 2026-09-01（待发布命令）

### 目标环境

- Minecraft 26.2
- NeoForge 26.2.0.64 release
- Java 25
- Gradle 9.2.1 / ModDevGradle 2.0.144

### 品牌

- 接入 26.1.2 定稿的原创品牌标：`icon.png`（512² 方形头像）与 `logo.png`
  （1280×360 Mods 横幅），青色四段瞄具环 + 铜色 R。`neoforge.mods.toml` 写入
  `logoFile="logo.png"` 与 `logoBlur=false`；用 `scripts/generate_branding.py`
  重新生成，逐字节与 26.1.2 一致。不使用官方 TaCZ 图标（CC BY-NC-ND 4.0
  禁止再创作），决策快照见 `docs/records/BRANDING.md`。

### 修复

- 同步姊妹分支 `arena/01a04e96` 的检视动画两连修（`4aa8d7b` + `12d6f3c`）：
  `stopAnimation` 原先只停轨道上的当前 runner，而带过渡时长启动的动画在过渡完成前
  是挂在旧 runner 的 `transitionTo` 上的 —— 开镜时检视（脚本 `inspect.transition`
  的打断分支必然落在 0.2 秒过渡窗口内）停的是早已停止的旧残骸，检视动画成了无主
  僵尸，状态机已回 idle、挂在 inspect 态上的打断手段全部失联，只有切枪/丢枪能救。
  改法分两步且必须一起拿：先让 stop 连坐 `transitionTo`；再引入 runner 出生序号与
  `AnimationStateMachine#trigger` 的转移前快照，豁免「本次 trigger 刚启动的后继
  动画」—— 否则检视中换弹的换弹动画会被 exit 的 stop 当场误杀。
  三文件基线已与姊妹侧逐字比对确认为其修复前状态。**源码级同步，未实机。**

- 修复枪包内联占位符被 `String.format` 解析成 `Format error: …`（`c1b687b`）：
  display json 的 `text_show` 走 `PapiManager#getTextShow`。上游 1.20.1 是
  `I18n.language.getOrDefault(textKey)` —— 纯查表，查不到原样返回键。26.2 的
  `I18n.language` 字段没了，移植时误换成 `I18n.get(textKey)`，而它**不等价**
  （26.2 `I18n` 字节码实读）：查表之后还要 `String.format`，抛
  `IllegalFormatException` 时返回 `"Format error: " + s`。对「把显示串直接内联进
  `text_key`」的枪包是致命的（MK5HD 的 `"%ammo_count%"` 不是语言键）：查表落空
  原样返回 → `%a...` 被当格式说明符 → 抛异常 → 垃圾串在前，而下面 PAPI 的占位符
  循环照常把 `%ammo_count%` 换成弹药数，真数字缀在最后。改回纯查表语义：
  `Language.getInstance().getOrDefault(textKey)`。**未实机。**

- 修复三处 26.2 编译错误：`PapiManager` 的 `Language` 类名（26.2 是
  `net.minecraft.locale.Language`，不是 `net.minecraft.client.resources.language.Language`）
  与 `GameRendererMixin` 漏 import（`bf2a16f`）；`ScopeFinalRingOverlay` 的方法名
  （26.2 是 `getModelViewMatrixCopy`，`ce24ef8`）；`Ordered` 与 `SubmitNodeCollector`
  在 26.2 是**平级类型**、`instanceof` 恒假（`30966ea`，修 `475ea40` 的编译错误）。

- 修复开光影后镜内裁切整体失效（含低倍镜准星溢出目镜）—— **draw 时的 uniform /
  采样器状态被覆盖**。Iris 的 `MixinGlCommandEncoder` 也在 `GlCommandEncoder#trySetup`
  的 RETURN 注入，并在那里调用 `ExtendedShader#iris$setupState`
  （`_glUseProgram` + `ProgramSamplers#update()` + `ProgramUniforms#update()`），
  而本仓写 `tacz_ScopeMaskMode` 的 hook 挂在**同一个 RETURN 点**、
  `IrisExtendedShaderMixin` 又在 `iris$setupState` 的 RETURN 把它无脑写回 0。
  两个处理器的先后由 mixin config 应用顺序决定，一旦本仓排在 Iris 之前，
  mode 就被写回 0 且本 pass 内无人再写 —— 镜身与准星一起不裁。
  改为**两个写入点**（`trySetup` RETURN + `iris$setupState` RETURN，
  pass 在 `trySetup` HEAD 记录），谁最后跑都得到正确值，与 mixin 顺序无关；
  并修掉「拿 A 程序的 uniform location 去 `glUniform1i` 当前程序」的静默无效写入。
  取证（含 Iris 钉死 commit `8f3a7a3` 的 `MixinGlCommandEncoder` 原文）与复测清单见
  `docs/records/SCOPE_MASK_HULL_SLOPESPACE_20260827.md`。**未实机。**

- 修复开光影后镜内裁切整体失效（含低倍镜准星溢出目镜）：`ScopeMaskRenderer` 的
  凸包孔径填充原先要把投影 UBO 读回 CPU，开光影后该读回必抛
  `IllegalStateException: Buffer is not readable`（本仓 `latest.log` 实录，Iris
  `1.11.2+mc26.2`），于是凸包每帧回退逐立方体描摹，板条目镜的孔径没被填上。
  改为在**光线斜率空间**求凸包（`NDC = (P00·x/-z, P11·y/-z)` 是保凸包的正系数
  轴向缩放），写回时整片扇面共用一个视深度 —— 不再需要投影矩阵与其逆，
  也去掉每帧 64B 的 GPU 读回；并补上姊妹仓已有、本仓缺失的近平面炸包保护
  （`SLOPE_SANITY_LIMIT`）。`ScopeMaskHullFill=false` 仍是即时回退开关。
  取证与复测清单见 `docs/records/SCOPE_MASK_HULL_SLOPESPACE_20260827.md`。
  **源码级修复，沙箱无 JDK 无法编译，未实机。**

- 修复 LRTactical 长按使用状态分叉，并补齐耳鸣音效与药效图标资源。

- 修复 26.2 首次生产编译暴露的 FOV event、HUD tick、AvatarRenderer descriptor 与三个
  transformed member AT 问题。
- 回流 26.1.2 R1 多人修复：`ServerMessageGunDraw` 的可空栈改 optional stream codec，
  防止首次/空手切枪的 tracking 广播踢出玩家。
- 将 `AttachmentsTagManager` 与 `RecipeFilterManager` 接入 network-cache listener，恢复
  RECIPE_FILTER / ATTACHMENT_TAGS 的联机同步。
- 四个双端 `Item#getName(ItemStack)` 改用 common index，避免 dedicated `/give` 路径加载
  client 类并崩服。
- Iris 已自动分类 pipeline 时，将 `Shader already assigned` 视为成功而不是兼容失败。
- 修复 `neoforge.mods.toml` 注释中的未知 dollar-brace 被 Groovy template engine 求值的问题。
- 低倍 sight 的 reticle containment 与 full-viewmodel clipping 拆分：低倍使用
  reticle-only mask，高倍使用完整镜身/枪身/配件/火光 mask。
- 安装 Punchy! 时右手脱离枪身、枪+手臂整体摆幅过大：按姊妹项目语义接入可选 mixin，
  持枪期间让 Punchy 的独立手臂与位移矩阵让出给 TACZ 第一人称状态机。
- Punchy! 开镜灵敏度过高：改走客户端 aiming 进度与 KeepingItem，并挡住 Punchy
  自己的 look/FOV 补偿。光影目镜裁切（BUG1）的尝试已撤回。未实机。
- 投掷物静止拉栓反复抖动：官方手雷脚本用字面量 `idle` 表示取消拔销，移植层却把近战
  专用的 `INPUT_IDLE` 每 tick 打给投掷物，两者撞名。位移 tick 改回只驱动近战。
- 跟官方 0.4.3 能跟的契约：烟雾粒子改采环境光（邻格回退、最低 2，不再全亮
  `0xF000F0`）；可预燃投掷物在手上炸改为 `prepare + 完整 lifeTime`（26.2 仍先
  `stopUsingItem` 再 `onThrow`）；display 增加 `display_offset` /
  `entity_transform`；消耗品补 `ConsumableItemRenderer` 与 display 通道。
  tooltip 自定义描述本仓已有，未改。未实机。
- 可预燃满进度后 `life` 被夹到 0：实体 tick 改为 `life >= 0` 才超时引爆，
  `0` 当帧炸，C4 `-1` 仍不超时。未再被用户打回。
- `5f6b9e7` 曾给开镜 `xBob` 乘 `1/zoom`、并加宽 Iris HAND 片元注入。用户复测
  高倍目镜仍不裁、开镜滞后仍在。相关代码已回退到 `305bed1`。审计与下一任
  提示词：`docs/records/SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md`（§0 清点本分支已落地
  的 Punchy / 0.4.3 / fuse / display，禁止当下任空白仓）。不得标 PASS。

### 新增：LRTactical 内置层

- 从 26.1.2 R1 稳定尖端前滚 throwable / melee / detonator / consumable 四类基础物品、
  explode / sticky / smoke / stun / effect-cloud 五类投掷行为、LR 数据/配方/Lua 装载与
  `lr1` payload 同步。
- 接入三类 tooltip、使用进度 HUD、id-keyed 分类冷却遮罩、耳鸣压音量、实体渲染与
  26.2 item-model 分流；无内容包时使用 vanilla 占位资源。
- 26.2 专项修正：受保护 `SimpleParticleType` 构造、即时药效方法拼写、Java 25 mixin、
  `GuiGraphicsExtractor` / `SoundEngine` 注入点复核与 `Player#canCriticalAttack` AT。
- 不含 flash_shield，不打包 LRTactical 原作 ARR 美术。26.1.2 源基线已有单机/专服 PASS，
  但 26.2 LR 实机矩阵仍待执行。

### 新增：镜内画中画（Scope PIP，默认关闭）

- 从姊妹分支 `TaCZ_Refabricated_Unofficial` 的 `26.2(main)`（尖端 `fcaa2b8`，上一同步点
  `7f6d1bf`）按本仓 NeoForge 表面重写接入：重投影模式（默认）+ 实验性二次渲染模式
  （`ScopePipRerender`），以及 Iris / Voxy / Sodium / PhysicsMod 四条兼容层。
  仅 `client.*` 包、不引入 Fabric 注解；`FabricLoader` → `ModList`，ForgeConfigAPIPort →
  `ModConfigSpec`，管线走 `RegisterRenderPipelinesEvent`。
- 新增 12 个 `ScopePip*` 选项，默认值与值域**逐项跟随姊妹分支**（`ScopePipEnable=false`、
  `ScopePipAllowShaderPacks=false`），便于两边 A/B 对照。关闭时运行路径与合入前逐位等价；
  运行期任何异常自我停用并退回整屏变焦。
- 同步姊妹分支 `5a96423` + `c74b34b`：新增倍率下限闸门 `ScopePipMinMagnification`
  （默认 4.0，1.0~100.0）—— 当前档位倍率低于该值时 PIP 让位、回经典整屏变焦。
  低倍镜（2×/3×）下整屏变焦观感本就自然，而 PIP 每帧要付一次全屏拷贝（二次渲染
  模式下是整遍世界重画）。组合镜按当前档位判定，切档自动跟随；`inactiveReason()`
  与 `wantsIrisComposite()` 两处加闸门即可覆盖四个时机（已逐行核对调用点）。
   已接入游戏内菜单与中英文文案。**未编译、未实机。**
- 同步姊妹分支 `9df8718`：新增 `ScopePipRerenderInterval`（1~4，默认 1）—— 二次渲染
  模式下镜内那一遍世界每 N 帧才真跑一次，其余帧复用离屏纹理里的上一帧成品。
  这是**光影下唯一砍得到大头的杠杆**：光影 + 二次渲染的帧率对半，根因是整条 Iris
  管线每帧跑两遍，N=2 直接把那份额外开销减半；代价是转视角时镜内内容滞后 N-1 帧，
  镜外主画面永远满帧率。复用有两道守卫：target 重建代数必须一致（否则新纹理内容
  未定义），帧差必须 < N（帧序号只在 `GameRenderer#extract` HEAD 递增，每帧恰一次，
  不受 Iris 一帧两趟手部 pass 影响）。
  同时把 `ScopePipResolutionScale` 的**真实作用域**写进配置注释与 tooltip，并在两条
  不消费它的路径（重投影模式、光影下）各打一条一次性日志 —— 免得玩家调了没反应
  当成 bug 报。该旋钮只在「二次渲染 + 无光影」下生效。
- 镜内合成的边界**只有着色器里的软掩码约束**（掩码为假即 discard），与姊妹分支一致。
- 含姊妹分支 `052e600` 的修复：只有主画面的 `LevelRenderer.submitNodeStorage` 需要保留
  提交节点，Iris `ShadowRenderer` 那份专用存储每帧照常清空；此前无差别拦截会让
  Iris 阴影队列永不释放（每开镜帧沉积 ~3.7 个 Submit/DrawCommand，地板 ~7 FPS）。
  取证与复测协议：`docs/records/REFAB_SCOPE_PIP_FPS_DECAY_20260829.md`。
- 按裁决**不同步**那批实验装置（`ScopePipResourceProbe`、`ScopePipDebugGpuMem`、
  `ScopePipReleaseIdlePipeline`）；它们的调查记录随同步存档。完整取舍与未验证清单：
  `docs/records/REFAB_SCOPE_PIP_SYNC_20260830.md`。
- 实机两轮后**移除**移植时自加的「目镜包围盒 → 硬件剪裁（scissor）」：
  它要拿手持那一遍的投影把斜率包围盒换算成屏幕 NDC，而 26.2 上 CPU 侧拿不到它 ——
  ① `renderItemInHand` 第三参数实测是**视图矩阵**（`m00 ∝ cos(yaw)`，随朝向胀缩
  且在两半球变号，于是朝南那半边被切成随朝向变化的矩形）；
  ② 改用 `CameraRenderState#projectionMatrix` 后变成**恒矩形**，说明它是世界投影、
  比手持那一遍更宽（手持用的是更窄的 FOV）；
  ③ `RenderSystem#getProjectionMatrix()` 在 26.2 已不存在，投影 UBO 读回在有光影时
  必抛 `Buffer is not readable`。既然无法被正确计算，就不再保留：相关字段、换算、
  scissor 调用与矩阵采集全部删除。完整的两轮取证与「想加回来唯一正确的做法
  （在着色器里用环境 Projection uniform 做斜率空间判定）」见
  `docs/records/REFAB_SCOPE_PIP_SYNC_20260830.md` §5.5。
- **源码级移植，未经本仓编译；上面这条移除同样未经实机复验**（沙箱无 JDK / Maven 源
  与游戏）。禁止写 PASS。

### 新增：镜内裁手（同步姊妹 `94179d4`，她侧已 PASS）

- 高倍镜掩码就绪时，第一人称手臂改走「镜内 discard」管线：手臂的 RenderType 是
  `AvatarRenderer#renderHand` 内部自己挑的 `entityTranslucent(skin)`（字节码实读），
  调用点无法直接换 —— 用 collector 动态代理在提交穿过时把那个 RenderType 原地替换。
  判据是 identity 比较：`RenderTypes.entityTranslucent` 按贴图 memoize，同皮肤恒同实例。
- 管线复用火光那条 `FLASH_TRANSLUCENT_CLIPPED_PIPELINE`（同 blend / snippet / define），
  但 RenderSetup **不能**复用 `create(...)` 助手 —— vanilla `entityTranslucent` 的 setup
  比 `entityCutout` 多 `affectsCrumbling()` + `sortOnUpload()`，少了会出现二层袖压一层臂
  的错序。
- 掩码未就绪（低倍镜 / 光影 / 配置关闭）时原样返回真 collector：最坏回到「镜内见手臂」
  的既有行为，绝不画错模型。
- **源码级同步，未经本仓实机验证。**

### 新增：镜内文字（同步姊妹 `9d03659`，她侧已 PASS）

- 瞄具上的文字（MK5HD 弹药计数 / "AMMO" 标签一类）此前走 vanilla 字体管线 ——
  `TextFeatureRenderer` 内部从 `GlyphRenderTypes` 三件套里挑 RenderType，
  调用方无任何注入点，只能靠「开镜到 0.35 才显示」的门禁治标。
- 改为徒手走 `Font#prepareText → visit` 后门拿字形几何，塞进新的 `scope_text`
  裁剪管线（vanilla TEXT 配方 + `SCOPE_MASK`，语义同准星：只保留镜内）。
  多页字体图集按页分组，每页复用 `ScopeMaskTextureHandle` 的空壳纹理思路。
- 任一环不可用（总开关关 / 光影 / 掩码 target 失败）即回退 vanilla `submitText`，
  行为退回「开镜才显示」的已验证现状，**绝不丢字**。枪身上的文字不受影响
  （`clipToScopeMask=false`）。
- 已知边界：第三方 ttf / unihex 灰度字体走回退路径（不裁但也不裂）。
- **源码级同步，未经本仓实机验证。**

### 新增：内置 TacZ Mesh Loader（TML，`model_type=mesh` 枪包）

来源：VellEagle/TacZMeshLoader `1.21.1_fabric` v0.1.7（GPL-3.0，已登记
`LICENSES.md`），经姊妹分支 `arena/01a04e96` 的 `8c6ad27` 落地，按她的
`SYNC_GUIDE_RENOV_262` §3 二段式推进。维护者确认本仓用户确有 `model_type=mesh`
枪包后，由「暂不移植」翻为立项。

- **第 0 步（安全子集）**：纯 collector 路径、无 GPU 烘焙。四个模型
  （枪 / 配件 / 弹药 / 方块）+ 四个装载 mixin（`GunDisplayInstance` /
  `ClientAttachmentIndex` / `ClientAmmoIndex` / `ClientBlockIndex` —— 目标全是本仓
  自有类，不是第三方表面，故 mixin 配置沿用 `required=true` + `defaultRequire=1`：
  装载点改名就 fail-fast，而不是静默丢功能）+ 独立 mixin 配置
  `tacz.mesh.mixins.json`。枪的弹匣走双通道：主遍历 exclude
  `additional_magazine` 子树，立方体弹匣交给本仓 26.2 原生的 `IMirrorGeometry`，
  poly 弹匣按该节点变换补画。
- **近距全模豁免**（`2ee701d`）：无 LOD 低模的高模（36 万顶点级）在玩家眼前的
  第三人称 / 掉落物 / 展示台会被世界顶点预算整层拦掉、只剩立方体。改为在
  `MeshWorldFullDetailDistance`（默认 16 格）内免顶点预算，预算只保护远处与密集
  场景。FIXED / HEAD 是双面语境（既在枪匠桌 GUI 预览、又在世界展示台雕像 / 展示框
  / 背枪），只有非 GUI 那一侧允许豁免，否则 36 万顶点会被全量画进图标。
- **第 1 步：第一人称手部 GPU 静态烘焙**（`728ca3c` —— 取姊妹四笔的**最终形态**，
  不逐笔照搬，因为后三笔全是对第一笔的修正）：骨骼本地顶点一次烘进常驻 VBO
  （ENTITY 格式、light 烘进 UV2），每帧只写 O(骨骼) 次 `DynamicTransforms` ——
  成本从 O(顶点) 降到 O(骨骼)。关掉的四个上游 PR（#33 / #69 / #70 / #71-72）的
  教训逐条落地：HAND 表只收 `ScopeMaskRenderer.isInHandPass()` 期间的提交
  （不是 `transformType.firstPerson()`，世界 / GUI / 掉落物才不会漏进世界 pass）；
  绘制挂在 `executeSolid` 之后（不在任何 render pass 内，此时立方体深度已就绪）；
  换弹 `additional_magazine` 恒走 collector（`mirrorRoot` 矩阵语义不同）；
  translucent 骨骼留在 collector 走排序混合；任何异常即本会话停用并回退 collector。
  光影下默认走 vanilla `RenderType.prepare()` + `PreparedRenderType.drawFromBuffer`
  + `ENTITY_CUTOUT`，让 Iris 按 HAND 程序接管 —— 裸 GPU pass 会绕过光影拦截；
  `MeshGpuUnderShaders` 是诊断用强开。绘制时自乘 `getModelViewMatrixCopy()`
  （collector 路径是 MV_draw × pose_submit 两层，GPU 路径原先只写了后者，
  表现为「朝向恒北」）；光影开关翻转立即失效重烘（旧 VBO 在新布局下属性错位会
  拉伸，一帧都不能再画）。
- **手部路径六连修**（`2a408c7`，同步她 08-31）。**第 1 条是本仓已带病发船的真
  bug**：法线弹栈时序 —— `drawListViaRenderType` 在 `prepare()` 之后、
  `drawFromBuffer` 之前就弹了 MV 栈，而光影包的 `gl_NormalMatrix` 是 Iris 在
  **绘制执行那一刻**从 RenderSystem MV 栈顶取的逆转置（Iris 26.2
  `ExtendedShader.iris$setupState` 源码实读），**不走 `prepare()` 快照**；弹早了
  ⇒ 栈顶只剩 MV_draw，`pose_bone` 旋转层丢失 ⇒ 顶点法线（骨骼本地系）转到错误
  方向 ⇒ 光影下反光的光源关系错乱。位置不受影响（`ModelViewMat` 走 `prepare()`
  快照），所以肉眼容易漏。弹栈移到 `drawFromBuffer` 之后。另五条：两处 catch 补接
  `LinkageError`（Iris / Sodium 升级后签名变了抛的是 `NoSuchMethodError`，
  Error 不是 Exception，漏接 = 崩游戏而不是回退 collector）；GPU 失败不再回写
  `MeshyConfig`（渲染线程写配置文件既不安全，也会把一次瞬时故障固化成用户看不懂的
  持久设置，只置会话标志）；逐帧比对 `ENTITY.getVertexSize()`，stride 变即整代失效
  （原来只认光影开关翻转，可别的 mod 改顶点格式同样会让旧 VBO 被按新 stride 解读）；
  手部消费点带 `RenderSystem.outputColorTextureOverride` 时跳过并清表；
  `PolyMesh` 退化面（零面积）不再写。
- **第 2 步：世界语境 GPU 烘焙**（`ba59ff5` + 修正 `f5fa1a1`，`MeshGpuWorld`
  **默认开**）：第三人称（别人手里的枪）/ 掉落物 / 展示框 / 展示台共用同一套常驻
  骨骼 VBO，每把枪每帧往 `WORLD_DRAWS` 登记 O(骨骼) 个矩阵，在世界帧图的
  `PreparedFrame.executeSolid` RETURN 处统一绘制。
  * 消费点不是 `renderAllFeatures`：26.2 的世界实体 pass 根本不经过它
    （`LevelRenderer.render` 的帧图 lambda **直调** `executeSolid`，偏移 177）；
    而 `renderLevel` 偏移 560 那次 `renderAllFeatures` 在 MV 栈 pop 之后 ——
    在那里画 = 丢掉相机旋转整层 = 枪固定在视角空间（她实测复现；与第 1 步当年
    丢 MV_draw 层是同一个病，这次是丢在栈已空的地方）。
  * NeoForge 表皮差异只此一处：她的 `ScreenRenderTracker` 用 Fabric 的
    `ScreenEvents.beforeExtract/afterExtract` 精确框住 Screen 提取窗口（不能用
    「有菜单开着」或时间戳窗口 —— 那会一开背包全场景跌回 collector，上游 TML
    记载过同款事故）。NeoForge 没有等价事件，改挂 vanilla 的
    `Screen#extractRenderState`（挂点存在性由本仓 `GunRefitScreen extends Screen`
    覆写该方法并调 `super` 编译通过**自证**），并用**深度计数**而不是布尔 ——
    子类覆写里调 `super` 时，super 那次的 RETURN 会把布尔清掉。
  * 光照档 LRU（`MeshGpuLightCacheSize` 默认 4，逐出的 VBO 延迟一帧释放，本帧
    绘制表可能还引用它）与每帧烘焙额度（`MeshGpuBakeBudgetPerFrame` 默认 4，
    病理场景回退 collector 而不是逐帧「逐出—重烘」打摆；额度与缓存容量解耦：
    缓存是显存开销、额度是每帧 CPU / 上传开销）；世界 GPU 失败不再拖垮已实测过的
    手部路径，两者各有独立会话标志。
- 适配（仅三处，其余逐字相同）：去掉 Fabric 的 `@Environment(EnvType.CLIENT)`；
  `ForgeConfigSpec` → `ModConfigSpec`；缓存失效监听器从 Fabric 的
  `ResourceManagerHelper` 改为 NeoForge 的 `AddClientReloadListenersEvent`
  （与 `PlayerAnimatorCompat` 同一范式）。配置面按维护者硬性惯例**同时接 TOML 与
  Cloth**（比姊妹侧多一整个 mesh 分区与中英文文案）。
- 验证：第 1 步由维护者 2026-08-31 实机复测 `docs/MESH_LOADER.md` §5.2 第 8–12 条
  全通过；第 2 步由维护者 2026-09-01 报告**实机 PASS**。
- 仍未量化：GPU 烘焙的**帧率收益数字一个都没有**，只有「成本从 O(顶点) 降到
  O(骨骼)」这个机制性结论。多人满屏高模枪的 fps 对比是最该出数字的地方。

### 变更

- 从包含多人修复与 LRTactical 的完整 NeoForge 26.1.2 R1 稳定基线前滚到 26.2；不是重写。
- 将已 removal 的 `IItemHandler` 路径迁到事务式 `ResourceHandler<ItemResource>`。
- 当前 screen 访问迁到 `Minecraft.gui`；HUD、文本颜色、PiP、shape outline、hand API 与
  Feature Rendering 对齐 26.2。
- 用 refab 26.2 已验证的阶段边界离屏 ocular mask 取代临时 OpenGL raw-depth 方案：
  convex-hull fill、sight/scope 分组、`ocular_ring` 普通重画、反向准星裁切、视模与火光裁切。
- 普通 mask 只使用 `TextureTarget` / `RenderPass` backend 抽象，可进入 OpenGL/Vulkan。
- Iris 改用 HAND pipeline 分类、linked-fragment dormant branch 与逐 draw uniform/texture binding。
- 版本 metadata 定名为 `1.1.8+neoforge.26.2.R1`。

### 工程

- 新增编译验证配方 `docs/ci/compile-check.yml`（**暂存在 docs/ci/，需项目成员移入
  `.github/workflows/` 才生效** —— 沙箱凭据无 `workflows` 权限，直接推会被拒）：
  编译在 GitHub Actions 上跑，日志经
  Contents API 写回分支的 `build-reports/compile-java.log`，供网络受限的环境用
  `gh api contents` 读回 —— 配方随姊妹分支 `arena/01a04e96` 同步（她的 v3，
  v2 的 commit 回推曾因 push 竞争多次失败）。只在本仓 `arena/**` 分支触发，
  `paths-ignore: build-reports/**` 防死循环；`.gitignore` 把该目录排除在提交之外。
- 编译验证配方已从 `docs/ci/` 暂存区**落进 `.github/workflows/compile-check.yml`
  并生效**：编译在 GitHub Actions 上跑，日志经 Contents API 写回分支的
  `build-reports/compile-java.log`，供网络受限的环境用 `gh api contents` 读回。
  只在本仓 `arena/**` 分支触发，`paths-ignore: build-reports/**` 防死循环。
- 新增**收尾用 changelog 流程**（`scripts/generate_changelog.sh` +
  `.github/workflows/changelog.yml`，手工触发）：按 conventional-commit 前缀
  从 `git log` 生成条目草稿（自动剔除 `ci-log` 回推提交），可下载为 artifact
  或回推到 `docs/records/`。**本版条目是人工对照 40 笔提交写的**，脚本从下一版起
  作为收尾骨架使用。
- 版本号一致性门禁（`consistency.yml`）已含**全仓 markdown 相对链接核验**，
  文档搬移不会再留下断链。

- 记录姊妹分支 `arena/01a04e96` 的同步取舍：内置 Mesh Loader（`8c6ad27`，第三方
  `VellEagle/TacZMeshLoader` 的 poly_mesh 渲染，GPL-3.0）**已由维护者确认需求后立项**——
  本仓用户确用 `model_type=mesh` 枪包，此前「暂不移植」的裁定作废。按姊妹
  `SYNC_GUIDE_RENOV_262` §3 的二段式推进：第 1 段（安全子集：纯 collector 路径、无 GPU
  烘焙）分三批，已落地第一批（8 个与加载器无关的核心文件，896 行，**只编译不生效**）；
  第 2 段（GPU 静态烘焙）等第 1 段实机 PASS 再动。许可已登记进 `LICENSES.md`。
  见 `docs/records/REFAB_SYNC_01A04E96_R2_20260830.md` §4.3。

### 修复

- **光影下镜内文字完全不裁（穿出目镜）**：光影下裁剪的执行者不是我们的着色器 ——
  管线被 `assignPipeline` 归入 Iris 的 HAND 程序后，光影包的手部着色器整条替换
  `scope_text.fsh`，真正干活的是注入进光影着色器的 `tacz_ScopeMaskMode` 分支，
  而它的开关值由 `IrisScopeMaskState#resolveMode` **按管线 location 查表**给出。
  那张表只登记了镜身/火光（mode 1）与准星（mode 2），`scope_text_clipped`
  漏登 —— 镜内文字是后来才加的管线，当时注释还写着「光影下掩码整体禁用、
  走不到这里」，而那是 Iris 桥落地**之前**的旧政策。现在 `scope_text_clipped`
  登记为 mode 2（与准星同侧：discard 镜外），并修正那两处过时注释。
- **光影下镜内文字被画成黑块**：与上面同一条管线、同一处 assign 的第二个后果。
  注入分支只做 `discard`、不接管着色，而文字用的是 **TEXT 顶点格式**
  （`POSITION_TEX_LIGHTMAP_COLOR`，没有 Normal），光影包按实体格式取法线与
  lightmap，语义对不上；我们 fsh 里那句 `color.a < 0.1 → discard` 也不执行，
  于是每个字形是一整块实心方块。修法：光影下文字**根本不进 Iris 管线**，
  延后到 `LevelRenderer#render` 之后、与遮光环共用最终覆盖层重画，并改用
  `pipeline/scope_text_final`（配方相同，但**不 assign 给 Iris**，因此由我们
  自己的着色器执行）。无光影路径逐字不变。
- **开镜渐开超过目镜（MARK5 / `scope_mk5hd` 等）**：掩码凸包的离群阈值在
  2026-08-27 改用斜率空间时**量级换算错了**。`NDC = P × slope`，70° FOV / 16:9
  下 `P00≈0.80`、`P11≈1.43`，屏幕边缘的斜率只有 0.7~1.25，而阈值取了 16.0，
  等价 `|NDC| ≈ 13~23` —— 比姊妹仓 26.2 的 `NDC_SANITY_LIMIT = 2.0` 松 6~11 倍
  （变焦会放大 P，差得更多）。这一段多出来的点撑大凸包，掩码于是大于真实通光
  孔径，开镜渐开长满的那一下就越过目镜 —— 且只有本仓有。现在：绝对阈值
  16.0 → 3.0（屏幕内的目镜顶点 NDC ≤ 1，永远到不了这个上限，只挡伪影不伤孔径），
  并新增与投影无关的自适应剔除（相对中位数半径 ×5、相对中位数深度 ×0.25）；
  剔除到不足 3 点则回退逐立方体描摹。
- **光影下开镜，目镜遮光环被镜内画中画的合成整片盖掉（对着光时表现为「遮光环变
  半透明」）**：光影路径的合成跑在 `LevelRenderer#render` 之后 —— Iris 把整个手部
  pass 搬进了那里 —— 也就是**画在手持之后**，于是把案例⑨ 救回来的物理目镜框
  （`ocular_ring`）重新盖掉；无光影时合成在阶段边界、画在手持之前，所以只有光影
  路径有这个问题。机制随 1.21.11 邻链 `2710c7c` 移植（排队 → 合成之后用「无掩码
  + 无雾」的原版 entity 管线重画），并按 26.2 改了三处（不自建
  `FeatureRenderDispatcher`、不调 `SubmitNodeStorage#endFrame`、刷新点不是 Iris 的
  `finalizeLevelRendering`）。**本机编译 + 实机 PASS（用户 2026-08-30 复测）** —— 对着光源开镜，遮光环恢复为不透明黑环，位置与大小均正确。见
  `docs/records/SCOPE_RING_IRIS_OVERLAY_20260830.md`。

### 可选兼容

- 重钉 Cloth Config、PAL、Controllable、Shoulder Surfing、JEI、REI、Architectury 的 26.2
  artifact。
- Carry On 对齐 2.11 的 `ItemStackTemplate#create()` 渲染路径。
- First-person Model / Not Enough Animations 只有反射 handoff 预留；核验日没有 NeoForge
  26.2 发布文件，不作为可安装兼容宣传。
- Punchy!：持有带模型的 TACZ/LR viewmodel 时走其官方 blacklist 让出路径，取消独立手臂
  与 walk/sprint/camera-lag 叠层；普通物品仍由 Punchy 控制。未实机。
- ImmediatelyFast hook 为明确 no-op；Accelerated Rendering 强制关闭；Aperture 未硬依赖接入。

### 移除

- 删除无引用的 `GunPackProgressScreen`。
- 删除旧 `IItemHandler` family 使用。
- 删除 raw-depth scope 状态机、GL encoder mixin、旧 shader 与 private `RenderType` 构造器 AT。

### 验证

- 用户对 **LR 合入前** 的 26.2 核心候选报告 JDK 25 build 与专服/多人 L0-L3 PASS；
  冻结记录：`docs/records/SERVER_TEST_20260821_262_R1.md`。
- 26.1.2 R1 的 LR 层已有用户单机与专服 PASS；该结果只作为源基线，不外推到 26.2。
- NeoForge Vulkan 需在 `config/fml.toml` 设置 `earlyWindowControl=false` 绕过仍开放的
  NeoForge#3230 ELS 问题；关闭后用户报告 Vulkan 启动 PASS。
- 当前 LR-integrated R1 只完成源码/API 静态前滚，尚无 JDK 25 生产构建或实机 PASS。
- 内置 Mesh Loader 第 1 步（第一人称手部 GPU 烘焙）：维护者 2026-08-31 实机复测
  `docs/MESH_LOADER.md` §5.2 第 8–12 条**全部通过**。
- 内置 Mesh Loader 第 2 步（世界语境 GPU 烘焙）：维护者 2026-09-01 报告**实机
  PASS**（未逐条回报 §5.4 九项矩阵 —— 若要作为发版依据需补齐逐条结果）。
- 镜内画中画（Scope PIP）**全条线仍只有源码级同步 + CI 编译，无实机结论**（默认
  关闭，关闭时运行路径与合入前逐位等价）；镜内裁手 / 镜内文字在姊妹侧已 PASS，
  本仓仍未实机。
- 官方 0.4.3 调查、留给下一轮（本轮不做）：
  - TACZ 第三人称枪口锁定：官方 LR 自己的 player_animator 旋转层会给所有玩家手臂加 pitch；
    本仓没有这套层。TACZ PAL 的 `PalRotationAdjustment` 已对 `is3rdFixedHand` 跳过手臂，
    官方那条「枪口被锁」的病根这里不存在。下一轮只有在接入 LR 旋转层时才需要复做豁免。
  - 近战第三人称 player_animator：官方是独立 upper/lower/rotation 层 + display 的
    `third_person_animation` + 攻击/idle 监听，体积大、要内容包动画、可能和 TACZ PAL
    抢层。本轮只读完源码，不接入。

### 发布前仍需完成

- 当前 LR-integrated R1 jar 的 clean build、L0 与 Mod List/`Done` 复核；
- LR 单机与生产专服专项（同步、实体 tracking、冷却、近战、烟雾/闪光）；
- L2.5 第三方枪包及 LR 内容包明确确认；
- OpenGL / Iris / Vulkan 完整 GPU scope-mask 矩阵；
- 可选 Mod 逐项用户结果；
- metadata、license、source tag 与 source archive 最终一致性检查。
- 内置 Mesh Loader 第 2 步的 §5.4 九项矩阵逐条回报（`MeshGpuWorld` 默认开，
  这一项是 R2 唯一默认生效的新 GPU 面）。
- 内置 Mesh Loader：GPU 烘焙的帧率收益数字（至少一组「多人满屏高模枪」开/关对比）。
- Scope PIP / 镜内裁手 / 镜内文字的实机矩阵（当前默认关或仅在姊妹侧 PASS）。

R2 条目在收到项目发起人明确发布命令前**不视为已发布**；发布时把上方日期改为
实际发布日期，并把 `## Unreleased` 里的增量并入下一版条目。
未收到明确命令时：**不 merge、不打 tag、不创建 Release、不上传 jar。**

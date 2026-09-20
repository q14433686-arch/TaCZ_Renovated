# 26.3 移植追踪（NeoForge / TaCZ_Renovated）

> 起始：2026-09-21，分支 `arena/01a0c084-tacz-renovated`，基线 `b2d8ed3`（26.2 R3-hotfix2 内容）。
> 依据：refab 26.3 线维护者的《26.3 移植指南》+ 姊妹项目 `TaCZ_Refabricated_Unofficial` `26.3` 分支实际 diff。
> 参照源码：本地 `/home/user/refab_263`（refab 26.3，含全部实机验证 docs）。
>
> **验证纪律**：本沙箱无法访问 Maven（只能连 github.com），本地无法编译/进游戏。
> 编译验证依赖 GitHub Actions `compile-check.yml`（runner 出网可拉 NeoForge maven）。
> 每条 render 改动的「实机验证」仍需在本地完整 build + 双遍（无光影/Iris）playtest，未做的一律标「仅编译」。

## 图例
- ✅ 已移植 + 期望 CI 编译通过
- 🔧 已移植，仅编译级信心（未实机）
- ⏳ 进行中
- ⬜ 未开始
- ⛔ 阻塞/需上游

## 依赖与地基（§1）
| 项 | 状态 | 备注 |
|---|---|---|
| gradle.properties 版本号（MC/Neo/JEI/Cloth/mod_version） | ⬜ | Neo 26.3.0.7-beta；JEI/Cloth/Iris NeoForge 26.3 构件号自查 |
| neoforge.mods.toml 依赖区间 | ⬜ | |
| 摘除 REI/Zoomify/ShoulderSurfing IMPL、保留门面 | ⬜ | README 措辞「禁用」非「修复」 |

## 移植方法（已确立）
- refab 26.2(main)→26.3 的 128 文件 diff 逐一映射：52 个可干净 `git apply`（已套用），
  35 个 reject（两仓实现分歧，逐一手工）。参照 `/home/user/refab_263`（Fabric 26.3）+
  `/home/user/neoforge_263`（NeoForge 26.3.x patches，核实 vanilla/NeoForge 侧签名）。
- 本地无法编译，推分支后靠 GitHub Actions `compile-check.yml` 真实编译验证。

## Mojang 侧（§2，加载器无关）
| 项 | 状态 | 备注 |
|---|---|---|
| §2.1 blaze3d→renderpearl 包迁移 | ✅ | 全仓完成（RenderTarget/TextureTarget 保留 blaze3d.pipeline，refab 亦未动）；withSampler→withUniform(COMBINED_IMAGE_SAMPLER) 已改 |
| §2.2 API 小改名 | ✅ | mulPose→rotate 全仓；input keys/openUri/PushReaction/codec()/block；readMap→BufMapCodec；bindTexture→setUniform；setPipeline(P)→setPipeline(getCompiledPipeline(P)) 全仓完成 |
| §2.3 第一人称拆分 FirstPersonHandsAndItems* | ✅ | 新建 2 mixin + 删 ItemInHandRendererMixin + KeepingItemRenderer + 调用点；mixin json 已改 |
| §2.4 render pass 归属倒置 | ✅ | FeatureRenderDispatcherMixin（prepareFrame RETURN 锚点 + executeSolid(RenderPass) 描述符 + static 处理器）已 clean；PolyMeshGpuRenderer.renderAfterSolid/renderWorldAfterSolid/drawList/drawViaRenderTypeCore 全部两阶段 externalPass 化（StagedVertexBuffer.ExecuteInfo + drawFromBuffer(info,pass) + drawListInto 抽出）；outputColorTextureOverride 闸门按 26.3 删除 |
| §2.5 投影 UBO 不可读回 | ✅（N/A） | renov 的 ScopeMaskRenderer 早已改为 slope-space 凸包，全仓无 getProjectionMatrixBuffer().map 读回 → **不需要** GameRendererProjectionAccessor，已从 mixin json 移除该误加条目 |
| §2.6 shader shaderc/SPIR-V 方言 | ⬜ | 6 GLSL（仅运行期编译影响，编译无关；待实机验证阶段核） |
| §2.7 管线预热 ScopePipelinePrewarm | ⏳ | renov 用自身 ScopePipRenderer.prewarmShaderPipelineIfNeeded；refab 的独立 ScopePipelinePrewarm 类不移植 |
| §2.8 loot 表 schema 重写 + 双掉落 bug | ✅ | 6 loot 表重写 + AbstractGunSmithTableBlock 去手工掉落 |
| §2.2 DamageCooldown（damageCooldownTime+lastHurt） | ✅ | DamageCooldownUtil + Accessor 已建，调用点 clean |
| GameRendererMixin 5 注入点重写 | ✅ | render 描述符去 DeltaTracker/Matrix4fc → 26.3 真签名；renderItemInHand/renderLevel 注入改「空形参 CallbackInfo」；partialTick 改 this.minecraft.getDeltaTracker() |
| 版本翻 26.3 | ✅ | gradle.properties: minecraft_version=26.3 / neo_version=26.3.0.7-beta / mod_version=1.1.8+neoforge.26.3.R1 |

## Iris 侧（§3）
| 项 | 状态 | 备注 |
|---|---|---|
| §3.3 mode 判定重做（声明式采样器） | ⚠️ 仅编译-兼容/行为待验 | renov 现走 `glPipeline.info().getLocation()`（resolveModeUncached）与 `glRenderPass.samplers` 反射（resolveMaskTextureId）；两者在 26.3 renderpearl 后端**可能恒失败**（.info()/named samplers 字段已随后端重构移除，见 §3.2 dead-ends）。renov 均有 try/catch 兜底 + ScopeMaskTarget.current() 回退 → **不崩，但光影下镜身/准星裁剪可能静默失效**。refab 的解法（IrisFrontendRenderPassMixin + FrontendRenderPass.uniforms 按名判 mode）为 refab-only 架构，renov 无对应 noteFrontendPass 基础设施；该 mixin 的 json 误加条目已移除。**需实机（开 Iris 26.3）验证 mode 是否命中，若失效再移植前端-pass 方案。** |
| §3.4 掩码 pass 雾污染 | ⬜ | 待实机验证阶段核 |
| Iris mixin 改名/改包 | ✅ | 包迁移已随 §2.1 完成；无 .bindTexture/withSampler 残留 |

## 行为 bug（§4）
| 项 | 状态 | 备注 |
|---|---|---|
| §4.1 JEI 重启方式 | ⬜ | |
| §4.3 recipe codec 惰性解析 | ⏳ | renov 已有惰性基础设施，仅需改 serializer codec |
| §4.5 GPU 烘焙首帧回退 | ⬜ | |
| §4.6 高模 GPU 消费点位置 | ⬜ | |
| §4.7 Iris 高模法线 rebind | ⬜ | |
| §4.9 配方同步（NeoForge OnDatapackSyncEvent） | ⬜ | 机制与 Fabric 不同 |

## 备注：两仓差异观察
- renov 用 `net.minecraft.resources.Identifier` 别名（297 处），与 refab 的 `Identifier` 一致；无 `ResourceLocation` 直用。
- renov 的 `GunSmithTableIngredient` 已具备 `JsonElement` 惰性构造 + `resolve()`，领先 refab-old。
- 共享代码（com.tacz/me.xjqsh）两仓高度同源；Fabric loader 层（cn.sh1rocu）需翻译成 NeoForge 事件/AT。

# 移植状态

目标版本：Minecraft **26.3** + NeoForge **26.3.0.7-beta**（beta 钉选）。
当前源码版本：**1.1.8+neoforge.26.3.R1**（未发布）。
状态：**26.3 全量端口已完成源码层移植（编译级，实机验收全部未做）**；26.2 线
`R3-hotfix` 已发布（2026-09-09），`R3 / R2 / R1 / R1-hotfix` 亦已发布，26.3 线以
未发布的 R3-hotfix2 源码为直接母本。本页按工作包记录历史矩阵；发布封口的验证
状态以 CHANGELOG / docs/records 为准，本文未把未跑项标为 PASS。

> 最后更新：2026-09-21（26.3 移植日）。本文只记录诚实状态；README 不作为逐包进度日志。

## 26.3 工作包（当前线）

> 移植依据：姊妹仓 26.3 分支的
> [`docs/lineage/PORT_GUIDE_26_3_FOR_RENOVATED_NEOFORGE_20260921.md`](lineage/PORT_GUIDE_26_3_FOR_RENOVATED_NEOFORGE_20260921.md)
> （已快照入仓）。执行计划见
> [`docs/investigations/WP263_0_PORT_PLAN_20260921.md`](investigations/WP263_0_PORT_PLAN_20260921.md)。
> **下表「已落地」= 源码已移植 + 静态核对；全部条目实机状态 = 未实测。**

| 工作包 | 已落地 | 尚缺验收（全部未实测） |
|---|---|---|
| WP-263-0 计划与证据 | 依赖矩阵 / API 核验表 / 七阶段执行序（WP263_0_PORT_PLAN） | — |
| WP-263-1 依赖与包迁移 | NF 26.3.0.7-beta / JEI 31.0.0.5 / Cloth 26.3.158 钉选；renderpearl 包迁移全量 | CI 编译绿 |
| WP-263-2 渲染重构 | pass 归属倒置（§2.4）、第一人称拆分（§2.3）、投影 accessor（§2.5）、shader 方言（§2.6）、管线预热（§2.7） | 八朝向跟手 / 开镜掩码 / PIP / 水下雾 |
| WP-263-3 高模 | PolyMeshGpuRenderer 26.3 化（externalPass / ExecuteInfo / 强制重绑） | 无光影 + 光影高模全矩阵 |
| WP-263-4 Iris | mode 标记采样器判别、setupDraw/空形参注入、FrontendRenderPass 配对 | 光影下裁剪 / 首次开镜不崩 |
| WP-263-5 数据与同步 | 战利品表 schema + 迁移层、配方 codec 延迟解析、sendRecipes、RecipeViewerReloadBridge | 挖工作台 / 旧枪包进存档 / 专服 JEI |
| WP-263-6 兼容门面 | REI / Controllable / SSR 禁用（IMPL 排除、plugin json 存档） | 上游 26.3 构件后回补 |
| WP-263-7 发布准备 | README / CHANGELOG / 版本一致性脚本通过 | 实机验收 + 发布闸门（RELEASE_CHECKLIST） |


## 26.2 工作包

| 工作包 | 已落地 | 尚缺验收 |
|---|---|---|
| WP-262-0 transfer 卫生 | `IItemHandler` for-removal 调用迁到 `ResourceHandler<ItemResource>`；删除死屏幕 | R1 最终 compile warning 复核 |
| WP-262-1 构建骨架 | MC 26.2 / NF 26.2.0.64 / Java 25 | LR-integrated R1 clean build / L0 / L1 |
| WP-262-2 非渲染 | Gui/HUD/AT；R1 network/getName 回流；LR payload/reload/dedicated 接线 | 当前 HEAD L2/L3/L2.5 |
| WP-262-3 渲染 | stage-boundary ocular mask、Feature/PiP/Gizmo/hand API、Iris bridge | OpenGL/Iris/Vulkan 完整 GPU 矩阵 |
| WP-262-4 可选兼容 | 26.2 坐标重钉、Carry On 2.11、FPM/NEA dormant bridge、Punchy 让出 mixin、矩阵文档 | `COMPATIBILITY.md` 全部游戏内项目 |
| WP-262-LR | 26.1.2 R1 的 109 Java LR 层、资源、四处接线；粒子/药效/mixin/item-model 对齐 26.2 | LR 单机、专服、多人、内容包专项 |
| WP-262-5 发布准备 | README/CHANGELOG/LICENSES/状态文档 | **发布 jar 与源码包被上述构建/实测闸门阻塞** |

基础证据：`docs/investigations/WP262_0_EVIDENCE.md` 至 `docs/investigations/WP262_5_EVIDENCE.md`。LR 前滚证据：
`docs/records/LR_R1_SYNC_26_2_20260822.md`。

## 已有 PASS 的适用范围

用户曾对 LR 合入前的 26.2 核心候选报告 build、L0-L3 **PASS**；冻结回执：
`docs/records/SERVER_TEST_20260821_262_R1.md`。该结果继续证明原有 TaCZ 核心逻辑，但不能
代表当前增加 LR 代码/资源/AT/mixin/payload 后的完整 artifact。

26.1.2 R1 尖端 `6020a5cf1dd02c356f797557f6323b0d430b75e1` 的 LR 层已有用户单机与
专服 PASS。它是此次前滚的稳定源基线，不是 26.2 实测结果。

## 版本基线

- **26.2 R3-hotfix2（当前工作树，未发布）**：R3-hotfix + 第一人称手部错位修复
  （中和 vanilla 1.21.9+ 手臂 `zRot=±0.1`，同步自 Fabric 1.21.11 线 `61ab4a0`）；
  编译门待 CI 回写、实机未验。
- **26.2 R3-hotfix（2026-09-09 发布）**：R3 + Iris scope-mask Fix-A / JEI·REI 枪匠台配方与
  Ammo Query / 移交单可见 bug 四项。
- **26.2 R3（2026-09-07 发布）**：R2 + string 库热修 / 创造搜索 / 死处理器接线 / mesh 裁剪时序；从完整 26.1.2 R1 前滚，不是重写。
- **26.1.2 R1（完整功能基线）**：LAN、真实专服、枪包及 LR 单机/专服专项实测通过；
  其 PASS 不能自动继承为 26.2 PASS。
- **Beta-1 / b9de5e0 / r1–r30**：历史或不含 LR 的中间基线，不再作为完整起点。

## 当前明确边界

### 图形后端

- OpenGL ocular mask：stage-boundary target / body-reticle-viewmodel clip 已接入，**未 GPU 实测**。
- OpenGL + Iris 1.11.2：per-draw uniform bridge 已接入；2026-09-09 以
  `ShaderCreator#link` 内 fragment `createShader` 的 `@ModifyArgs` 取代旧的全程序/
  跨方法桥，默认 `HAND_ONLY`，另有 `ALL` / `OFF` 对照与 per-program sampler-unit
  安全层。用户 2026-08-26 的「高倍目镜未裁」是旧实现的历史反馈，不能用来判定
  Fix-A；JDK 25 CI `compileJava` + full build 已通过，仍待实机三档矩阵。见
  `docs/records/REFAB_SYNC_262_MAIN_1B4AF9F_20260909.md`。
- 开镜视角滞后：给 `xBob` 乘瞄准系数被用户与姊妹仓两边否决，已回到官方未缩放
  `* 0.1`。高倍/组合镜症状仍在。同一审计文档。
- Vulkan：设置 `config/fml.toml: earlyWindowControl=false` 后用户启动 **PASS**；低倍准星
  mask 报告 FAIL，已拆分 reticle/body mask 状态修复，当前 HEAD 待复测。
- 其他 shader replacement / Aperture：没有已核 bridge 时未掩码回退。

### LRTactical 内置层

已落地：

- throwable / melee / detonator / consumable；
- explode / sticky / smoke / stun / effect-cloud；
- LR index/data/recipe/filter/Lua、`lr1` payload、登录/重载同步；
- tooltip、使用进度 HUD、分类冷却、耳鸣与动态物品/实体渲染；
- 26.2 `SimpleParticleType`、即时药效、item model、GuiGraphicsExtractor/SoundEngine mixin
  与 `Player#canCriticalAttack` AT 适配。

明确不含：flash_shield 与原作 ARR 美术。当前状态是 **source/API 已核，26.2 未实测**。

### 可选 Mod

所有发布文件、source commit、pin 与未实测标记见根目录
[`COMPATIBILITY.md`](../COMPATIBILITY.md)。当前没有任何 26.2 可选兼容项被标记为用户 PASS。

## 发布阻塞项

在以下项目全部完成前，仍不得发布 R2 或声称移植完成：

1. 当前 R2 候选：JDK 25 clean build、L0、Mod List 与 `Done`；
2. 当前 artifact：L2 生产专服、L3 双客户端与 L2.5 枪包专项；
3. LR 专项：单机/专服的同步、tracking、冷却、近战、投掷、烟雾/闪光及 LR 内容包；
4. 客户端：无可选 Mod 的完整枪械/资源回归；
5. GPU：OpenGL、Iris、Vulkan scope-mask 矩阵；
6. `COMPATIBILITY.md` 可安装项目逐行用户实测或继续明确“未实测”；
7. 发布 jar 的 metadata/license 与对应源码 tag/归档一致性最终复核。

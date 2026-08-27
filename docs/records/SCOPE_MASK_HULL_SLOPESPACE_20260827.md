# 开光影后镜内裁切失效 — 两处独立病灶（draw 时 uniform/采样器状态 + 凸包掩码读回）

日期：2026-08-27
本仓分支：`arena/01a0457c-tacz-renovated`（基线 `3106dea`）
状态：**源码级修复，未实机**。GPU / 光影包 / 多人均未复测，禁止写 PASS。

---

## 1. 用户报的现象

> 安装 `tacztweaks-2.14.2+neoforge.26.2.Beta-1.jar` 后，开启光影开镜时镜内裁切直接
> 失效，低倍镜准星也不再被限制在目镜内。同为 26.2 的
> `TaCZ_Refabricated_Unofficial-26.2-main-` 没有这个问题。

## 2. tacztweaks 自己不含渲染代码（已逐文件核对）

`tacztweaks-2.14.2+neoforge.26.2.Beta-1` 的源码是
`q14433686-arch/TaCZTweaks_Unofficial` 分支 `26.2-neoforge`
（`gradle.properties` 里 `mod_version=2.14.2+neoforge.26.2.Beta-1`，与用户给的
文件名逐字相同）。核对结论：

- 全仓 `src/main/java` + `src/main/kotlin` 搜
  `RenderSystem|RenderType|LevelRenderer|GameRenderer|ItemInHand|GlCommandEncoder|iris|Iris`
  —— **零命中**。它不含任何渲染、光影、掩码代码。
- `tacztweaks.mixins.json` 列出的 40 个 mixin 目标全部是 gameplay / 音效 / 数据 /
  GUI；渲染相关的只有三个，且都与瞄具无关：
  `crawl.AvatarRendererMixin`（趴姿玩家模型）、
  `gun.EntityBulletRendererMixin`（曳光弹）、
  `tweaks.RenderCrosshairEventMixin`（只是按开关 cancel `renderHitMarker`）。
- 它没有 Iris mixin，没有碰 `ShaderCreator` / `ExtendedShader` / `GlCommandEncoder`，
  也没有引用任何 `com.tacz.guns.client.render.*` 类
  （`import com.tacz.*` 全量清单里没有 render/scope 包）。

**但这不等于它与症状无关。** 它自己不会关掉裁剪，可是它进入 mod 列表会改变
mixin config 的应用顺序，而本仓的镜内裁剪在那个顺序上是**脆的**（§3）。
所以「装上它才坏、卸掉又好」是真实的因果链，只是链条不经过它的代码。

> 本文先前一版曾写成「tacztweaks 是相关不是因果、真正开关只是光影本身」——
> 那句是错的，已按 §3 的取证更正。

## 3. 病灶 A（主因）：draw 时的 uniform / 采样器状态被 Iris 覆盖，且修复依赖 mixin 顺序

用户判断正确。`IrisScopeMaskState` 写 `tacz_ScopeMaskMode` 与掩码采样器的时机，
和 Iris 重新绑定程序 / 采样器的时机，**挂在同一个注入点上**。

Iris 的 `MixinGlCommandEncoder`（已按本仓钉死的取证 commit
`8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a` 逐字核对，blob `a919d34`）：

```java
@Inject(method = "trySetup", at = @At("RETURN"))
private void iris$setupState(GlRenderPass glRenderPass, Collection<String> collection,
                             CallbackInfoReturnable<Boolean> cir) {
    if (glRenderPass.pipeline.program() instanceof IrisProgram is && !is.iris$isSetUp()) {
        ...
        is.iris$setupState(glRenderPass.samplers, ...);
        programsToClear.add(is);
    }
}

@Inject(method = "finishRenderPass", at = @At("HEAD"))
private void iris$clearState(CallbackInfo ci) {
    programsToClear.forEach(IrisProgram::iris$clearState);   // → isSetup = false
}
```

而 `ExtendedShader#iris$setupState` 的方法体里是
`GlStateManager._glUseProgram(getProgramId())` → `this.samplers.update()` →
`uniforms.update()` → …，**本仓的 `IrisExtendedShaderMixin` 又挂在它的 RETURN 上，
把 `tacz_ScopeMaskMode` 无脑写回 0**。

本仓写 mode 的 `IrisGlCommandEncoderMixin` 同样挂在 `trySetup` 的 RETURN 上。于是：

| mixin 应用顺序 | 一次 setup 内的实际时序 | 结果 |
|---|---|---|
| Iris 在前，tacz 在后 | `iris$setupState`（含我们的 reset→0）→ 我们写 mode=1/2 | 裁剪正常 |
| **tacz 在前，Iris 在后** | 我们写 mode=1/2 → `iris$setupState`（`_glUseProgram` + `samplers.update()` + 我们的 reset→**0**） | **整个 pass 不裁** |

第二种顺序下，同一 pass 内后续 draw 的 `trySetup` 对同一条管线不再触发 setup，
没有人再把 mode 写回去 —— 镜身（mode 1）与准星（mode 2）**一起**失效，
正是「开光影开镜，镜内裁切直接失效，低倍镜准星也不再被限制在目镜内」。

**两个 RETURN 处理器的先后由 mixin config 的应用顺序决定，而它随已安装 mod 集合变化。**
这就是「装了 tacztweaks 才坏、卸掉又好」的合理机制 —— tacztweaks 自己没有一行渲染代码
（§2），但它进入 mod 列表后足以改变这个顺序。

> 无法从日志证明用户那次会话落在哪一种顺序：`Iris scope-mask bridge active (mode=1…)`
> 只说明我们**写过** mode=1，不能说明它有没有随后被写回 0。所以修法是
> **让它与顺序无关**，而不是去赌顺序。

### 顺带修掉的一处真实错误

旧 `applyToGlRenderPass` 在 `GL_CURRENT_PROGRAM` 为 0 时，退回
「从 `glRenderPass.pipeline.program()` 取 `programId`」，然后拿**那个程序**的
uniform location 去调 `glUniform1i`。`glUniform1i` 只作用于当前绑定的程序，
而 location 是**按程序**分配的 —— 这是把 A 程序的 location 写进 B 程序（或写进空气），
静默无效。现在没有当前程序就直接放弃，交给下面的程序级 hook 补写。

### 修法：两个写入点，谁最后跑都对

1. `IrisGlCommandEncoderMixin` 新增 `trySetup` **HEAD** 注入，记下当前 `GlRenderPass`
   （HEAD 一定早于任何 RETURN 处理器）；
2. `trySetup` RETURN 照旧写一次；
3. `IrisExtendedShaderMixin` 在 `iris$setupState` RETURN 不再写 0，改为
   `applyToShaderProgram(this)` —— 用第 1 步记下的 pass 解析出正确 mode 并写入。
   该点位于 `_glUseProgram(getProgramId())` 与 `samplers.update()` **之后**，
   所以只要 Iris 做了 setup，我们就是本轮最后写入者。

`applyToShaderProgram` 额外校验 `GL_CURRENT_PROGRAM == programId`，不一致就跳过并
一次性告警，绝不跨程序写 location。非镜身/准星管线仍然写 0，防泄漏语义不变。

## 4. 病灶 B（次因，独立存在）：凸包掩码的投影 UBO 读回（`latest.log` 实录）

`ScopeMaskRenderer#writeHullFill` 要在 CPU 上做目镜投影的 2D 凸包，就必须拿到
本 pass 实际使用的投影矩阵。26.2 的 `RenderSystem` 没有 `getProjectionMatrix()`，
旧实现于是去读 GPU 侧的投影 UBO：

```java
try (GpuBufferSlice.MappedView view = RenderSystem.getProjectionMatrixBuffer().map(true, false)) {
    proj.set(view.data());
} catch (Exception e) { /* 一次 warn，然后 return false */ }
```

开光影后这条读回**必然抛异常**：

```
[Render thread/WARN] [com.tacz.guns.GunMod/]: [TACZ Scope] Hull-fill: could not read back
    the projection UBO; this entry falls back to legacy per-cube tracing.
java.lang.IllegalStateException: Buffer is not readable
    at com.mojang.blaze3d.opengl.GlBuffer$Direct.map(GlBuffer.java:101)
    at com.mojang.blaze3d.buffers.GpuBufferSlice.map(GpuBufferSlice.java:28)
    at com.tacz.guns.client.render.scope.ScopeMaskRenderer.writeHullFill(ScopeMaskRenderer.java:405)
    ...
    at net.irisshaders.iris.pathways.HandRenderer.renderSolid(HandRenderer.java:119)
```

后果链：

1. `writeHullFill` 每帧 `return false` ⇒ 凸包填充**在有光影时从来没跑过**；
2. 掩码退回逐立方体描摹（同一条日志：`Ocular mask drawn: 288 indices from 7 batches`
   ≈ 12 个 cube 的裸几何）；
3. 对**板条拼玻璃**的目镜（AUG 3 条十字、elcan 8 片竖板、lpvo 细十字），
   掩码只剩板条本身，孔径内部没有掩码 ⇒ 镜身/视模不裁、准星的反向裁剪也约束不住；
4. 用户看到的就是「开光影开镜，镜内裁切直接失效，低倍镜准星溢出目镜」。

同一份日志也说明**桥本身是通的**（`Iris scope-mask bridge active (mode=1, ...)`），
所以坏的是掩码**内容**，不是 discard 机制 —— 这也解释了为什么无光影时正常：
那时 UBO 读回成功，凸包把孔径填上了。

### 修法：在光线斜率空间做凸包，彻底不读投影

标准透视投影（Minecraft `Matrix4f#perspective`，`m32 = -1` ⇒ `clip.w = -z`）下：

```
NDC.x = P00 * x / -z = P00 * slopeX      slopeX = x / -z
NDC.y = P11 * y / -z = P11 * slopeY      slopeY = y / -z
```

`P00`、`P11` 恒正 ⇒「斜率 → NDC」是正系数轴向缩放，是**保凸包**的仿射双射。
于是：

- 凸包直接在斜率空间求，**不需要投影矩阵**；
- 写回绘制空间也不需要逆投影：斜率 `(sx, sy)` 在任意深度 `d > 0` 上都对应同一个
  NDC 点，取 `(sx*d, sy*d, -d)` 即可。整个扇面共用一个 `d`（取目镜顶点的平均视深度，
  夹到 `HULL_DEPTH_MIN = 0.1`，即近平面 0.05 之外），掩码 target 无深度附件，
  `d` 不参与遮挡；
- 扇面与逐立方体描摹走**同一条**投影管线 ⇒ 两者天然对齐，不再有
  「读回的投影 ≠ pass 实际投影」这一类错位；
- 顺带去掉每帧 64B 的 GPU 读回。

同时补上姊妹仓 26.2 已有、本仓缺失的**近平面炸包保护**：擦过近平面的顶点会让斜率
飙到 ±1000，一个点就能把凸包撑满全屏、掩码「全屏为真」。斜率阈值取
`SLOPE_SANITY_LIMIT = 16`（合法目镜投影的斜率上界 ≈ `aspect * tan(fov/2)`，
最宽 FOV + 最宽屏也就 ~3；伪影是 100 量级）。

改动文件：

- `src/main/java/com/tacz/guns/compat/iris/IrisScopeMaskState.java`
  —— 新增 `setCurrentPass` / `applyToShaderProgram` / `writeScopeMaskState`，
  删除 `resetShaderProgram`，修掉跨程序写 location 的错误；
- `src/main/java/com/tacz/guns/mixin/client/iris/IrisGlCommandEncoderMixin.java`
  —— 新增 `trySetup` HEAD 注入；
- `src/main/java/com/tacz/guns/mixin/client/iris/IrisExtendedShaderMixin.java`
  —— RETURN 改为 `applyToShaderProgram`；
- `src/main/java/com/tacz/guns/client/render/scope/ScopeMaskRenderer.java`
  —— `writeHullFill` 重写、`emitNdc*` → `emitSlope*`、删除 `loggedProjReadFailure`
  与 `GpuBufferSlice` 依赖、成功日志增加 hull/traced 计数。

`ScopeMaskHullFill=false` 仍然是即时回退开关，语义不变。

## 5. 本轮验证到什么程度

**没有做实机，也没有做成编译。** 沙箱里没有 JDK、没有 Gradle 发行版，
`services.gradle.org` / `api.adoptium.net` / GitHub release 资产主机全部不可达
（HTTP 000），`./gradlew build` 无法执行。已做的是：

- 结构核对：注释/字面量剥离后括号配平通过；新方法齐备，
  `emitNdcAsQuad` / `emitNdcVertex` / `loggedProjReadFailure` / `GpuBufferSlice`
  全部确认已从文件中消失；
- 数学核对（脚本，2000 组随机 FOV/aspect/板条目镜）：
  斜率空间凸包按 `(P00, P11)` 映射后与「先投影再求凸包」的顶点集**逐点一致**
  （最大偏差 4.4e-16）；`(sx*d, sy*d, -d)` 在 `d ∈ {0.1, 0.37, 1, 250}` 上
  重投影回 `(P00*sx, P11*sy)`，误差 2.2e-16。
  这验证的是改动所依赖的恒等式，**不是**跑了改动后的代码。

**必须由用户执行的复测**（有光影 / 无光影各一遍，且**装与不装 tacztweaks 各一遍**）：

0. 装 tacztweaks + 开光影 + 高倍/组合镜/低倍各开一次镜：镜内裁切与准星约束应与
   不装 tacztweaks 时**完全一致**。这是本轮的主验收项；
   若日志出现 `Iris program setup ran with a different program bound`，
   说明 `iris$setupState` 的调用点与本假设不符，请把日志发回来；
1. 日志里**不再出现** `Hull-fill: could not read back the projection UBO`；
2. `Ocular mask drawn: ... (hull-filled entries=N, traced-fallback entries=M)`
   中 `N > 0`；若 `N` 恒为 0 且 `M` 等于批次数，说明凸包仍未生效，本文结论作废；
3. AUG / elcan / lpvo（低倍档）/ HAMR（高低倍两档）开镜：镜内不见镜筒内壁与枪管穿镜，
   准星被约束在目镜内；
4. 关闭 `ScopeMaskHullFill` 后行为回到逐立方体描摹（回退开关仍有效）。

### 残留风险

- 本仓的程序级 hook 挂在 `ExtendedShader`。若 Iris 另有其它 `IrisProgram` 实现类
  被用于第一人称手部，那条路径只剩 `trySetup` RETURN 一个写入点，
  在「tacz 在前」的顺序下仍可能被覆盖。未逐个核对 Iris 的 `IrisProgram` 实现清单。
- `trySetup` 是「每次 draw 调用」还是「仅管线切换时调用」，属于 Mojang 侧代码，
  本沙箱拿不到 `GlCommandEncoder` 源码，未取证。修法对两种语义都成立
  （两个写入点覆盖了两种时序），但这一点是推断不是取证。

## 6. 本轮**没有**做的事

- 没有碰 `IrisShaderCreatorMixin` / `IrisGlCommandEncoderMixin` /
  `IrisExtendedShaderMixin`（`docs/records/SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md` §5
  已把「再 hook 一次 link」列为禁区）；
- 没有把 `shouldDisableScopeMaskUnderShaderPack` 扩成 Iris；
- 没有搬姊妹仓的 `ScopePipRenderer` / `IrisScopePipelineCompat` /
  `VoxyScopePipelineCompat`（Fabric 表面 + PIP 是另一条工作包，本仓 PR #17 已关闭未合）；
- 没有动开镜视模 bob 或约束公式（问题 3 的禁区不变）。

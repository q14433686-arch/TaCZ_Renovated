# 交接：给 `TaCZ_Refabricated_Unofficial` 的镜内裁剪做「顺序无关」加固

> 来源：`TaCZ_Renovated` 分支 `arena/01a0457c-tacz-renovated`，PR #26，commit `5c02c5f`（修复）
> 与 `ef456ed`（横查与更正）。取证记录见本仓
> `docs/records/SCOPE_MASK_HULL_SLOPESPACE_20260827.md` §3 / §7。

---

## 0. 先说性质：**这是加固，不是修一个正在发生的 bug**

NeoForge 侧（`TaCZ_Renovated`）实测：开光影 + 开镜，镜内裁切整体失效（含低倍镜准星
溢出目镜）。已定位并修复，用户实机 **PASS**。

**Fabric 侧（你们）用户实测不发作。** 但你们的代码带着**同一个对 mixin 注册顺序的
依赖**，只是当前落在好的那一行。本任务的价值是消除这个依赖，避免将来 loader 的
mod 排序一变就复现同样的失效。

所以：**不要把它当成 bug 修复来写 CHANGELOG，也不要声称修好了用户报的某个现象。**
措辞用「消除对 mixin 注册顺序的依赖」这类加固口径。

---

## 1. 已核实的事实（可直接引用，不必重查）

1. **`TaCZ_Renovated` @ `26.2(main)` 与你们 `26.2(main)` 的两个 mixin 文件逐字节相同。**
   `git diff` 结果：
   - `src/main/java/com/tacz/guns/mixin/client/iris/IrisGlCommandEncoderMixin.java` —— **0 行差异**
   - `src/main/java/com/tacz/guns/mixin/client/iris/IrisExtendedShaderMixin.java` —— **0 行差异**
   - `src/main/java/com/tacz/guns/compat/iris/IrisScopeMaskState.java` —— 186 行差异，
     **全部**是你们多出的反射结果缓存（`cachedPassClass` / `cachedPipelineField` /
     管线→mode 记忆），语义等价，与顺序无关。

2. **Iris 确实也在 `GlCommandEncoder#trySetup` 的 RETURN 注入。**
   在 `IrisShaders/Iris` 分支 `26.2`（tip = `8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a`，
   `compare` 结果 status `identical`、ahead/behind 均 0）上，
   `common/src/main/java/net/irisshaders/iris/mixin/MixinGlCommandEncoder.java`
   （blob `a919d34`）共 3 个注入：`trySetup @ HEAD`、`trySetup @ RETURN`、
   `submitRenderPass @ HEAD`。其中 RETURN 那个是：

   ```java
   @Inject(method = "trySetup", at = @At("RETURN"))
   private void iris$setupState(GlRenderPass glRenderPass, Collection<String> collection,
                                CallbackInfoReturnable<Boolean> cir) {
       if (glRenderPass.pipeline.program() instanceof IrisProgram is && !is.iris$isSetUp()) {
           ...
           is.iris$setupState(glRenderPass.samplers, ...);   // _glUseProgram + samplers.update() + uniforms.update()
       }
   }
   ```

   而你们的 `IrisExtendedShaderMixin` 挂在 `iris$setupState` 的 RETURN，
   无条件把 `tacz_ScopeMaskMode` 写回 0。

3. **NeoForge 侧那台机器上的实际顺序是坏的**（`latest.log`，
   `iris-neoforge-1.11.2+mc26.2.jar`）：

   ```
   19:28:19.083  [TACZ Scope] Iris compat mixin config loaded: package=com.tacz.guns.mixin.client.iris
   19:28:19.094  [mixin] Reference map 'iris.refmap.json' for mixins.iris.json could not be read
   19:28:19.094  [mixin] Reference map 'iris.refmap.json' for mixins.iris.vertexformat.json ...
   19:28:19.094  [mixin] Reference map 'iris.refmap.json' for mixins.iris.compat.sodium.json ...
   ```

   tacz 的 `tacz.iris.mixins.json` 比 Iris 的三个 config 早 11 ms 注册 →
   `trySetup` RETURN 上 tacz 处理器在前、Iris 在后 → mode 被写回 0 且本 pass 内
   无人再写。

4. **你们为什么不发作：未知，且不要猜。** 没有 Fabric 侧启动日志。可能是
   Fabric Loader 的 mixin config 注册顺序把 iris 排在 tacz 之前，也可能 Fabric 版
   Iris 构建与 `26.2` tip 有差异。**接手后请自己取证**（见 §5），不要照抄 NeoForge 的结论。

### 失效时序（两行表，坏的是第二行）

| mixin 注册顺序 | 一次 setup 内的时序 | 结果 |
|---|---|---|
| Iris 先、tacz 后 | `iris$setupState`（含你们的 reset→0）→ 你们写 mode=1/2 | 裁剪正常 |
| **tacz 先、Iris 后** | 你们写 mode=1/2 → `iris$setupState`（`_glUseProgram` + `samplers.update()` + 你们的 reset→**0**） | **整个 pass 不裁** |

---

## 2. 要做的改动

原则：**两个写入点，谁最后跑都对**，从而与注册顺序无关。

### 2.1 两个 mixin 文件 —— 有现成补丁，已验证可干净打上

`docs/handoff/scope-mask-order-independence.patch`（本仓内，63 行）。
已对你们 `26.2(main)` 跑过 `git apply --check` → **exit 0**。

改动内容：
- `IrisGlCommandEncoderMixin` 新增 `trySetup` **HEAD** 注入，记录当前 `GlRenderPass`
  （HEAD 一定早于任何 RETURN 处理器，所以无论 Iris 的 RETURN 处理器何时跑，
  pass 都已经记下）；
- `IrisExtendedShaderMixin` 在 `iris$setupState` RETURN 改为调
  `IrisScopeMaskState.applyToShaderProgram((Object) this)`，
  不再无条件写 0。

### 2.2 `IrisScopeMaskState` —— 只能照描述改，补丁打不上（你们多了缓存层）

你们 `26.2(main)` 上的相关行号（供定位，改完会变）：

| 行 | 内容 |
|---|---|
| 81 | `int modeLocation = GL20C.glGetUniformLocation(programId, UNIFORM_MODE);` —— 在 `resetShaderProgram` 内 |
| 121 | `int programId = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);` —— 在 `applyToGlRenderPass` 内 |
| 125 | `Object glProg = invokeNoArgs(glPipeline, "program");` —— **要删的退回分支** |
| 133 / 143 | `modeLocation` / `samplerLocation` 的 `glGetUniformLocation` |

需要做四件事：

1. **新增 `private static Object currentPass;` 与 `public static void setCurrentPass(Object)`**，
   由 2.1 的 HEAD 注入调用。
2. **把 `resetShaderProgram` 换成 `applyToShaderProgram(Object shader)`**：
   取 `getProgramId(shader)`；**校验 `GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM) == programId`**，
   不一致就跳过并一次性告警（`glUniform1i` 只作用于当前程序，uniform location 又是
   按程序分配的，跨程序写等于写进别的程序或写进空气）；一致则用
   `resolveMode(currentPass)` 解析出 mode 并写入。
   —— 你们已有 mode 缓存，`resolveMode(currentPass)` 应当命中它，别绕开缓存。
3. **抽一个 `writeScopeMaskState(int programId, int mode, Object glRenderPass)`**，
   让 `trySetup` RETURN 与 `iris$setupState` RETURN 两个调用点共用同一份写入逻辑，
   保证「最后跑的那个」写的是同一套状态。非镜身/准星管线仍然写 0，
   **防泄漏语义不要动**。
4. **删掉 `programId = getProgramId(invokeNoArgs(glPipeline, "program"))` 这条退回分支。**
   没有当前程序就直接 return，交给 2.2 的程序级 hook 补写。
   理由同上：拿 A 程序的 location 去 `glUniform1i` 当前程序是静默无效写入。

写入顺序保持：先 `glUniform1i(mode)`、再 `glUniform1i(sampler)`、再绑纹理、
最后 `glActiveTexture(GL_TEXTURE0)` 把活跃单元还回去。
Iris 的 `ProgramSamplers#update()` 只重绑它自己的单元
（`IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS = {0,1,2}`，其余从 3 起顺序分配），
且跑在你们之前，所以你们这次绑定是本轮最后写入者。

> 参考实现：`TaCZ_Renovated` @ `arena/01a0457c-tacz-renovated`，
> commit `5c02c5f`，文件 `src/main/java/com/tacz/guns/compat/iris/IrisScopeMaskState.java`。
> 那份**没有**你们的缓存层，抄的时候把缓存补回去，别把性能优化丢了。

---

## 3. 禁用清单（同样适用，别踩）

来自 `TaCZ_Renovated` 的 `docs/records/SCOPE_IRIS_VIEWLAG_AUDIT_20260826.md`，
这些在本仓已经试过并确认是死路：

- 不要按瞄准进度 / 变焦缩放 `xBob` / `yBob` / root offset；
- 不要再加 `ShaderCreator` / `GlShader` / `ProgramBuilder` 的编译期钩子；
- 不要把 `shouldDisableScopeMaskUnderShaderPack()` 扩到 Iris；
- 不要复制 `IrisScopePipelineCompat` / `VoxyScopePipelineCompat`；
- 不要在 submit 中途无保护地切换 `RenderPass`（VK_ERROR_DEVICE_LOST）；
- 不要用 `require=0` 的新 mixin 去「探测」——本仓已有一次因此静默失效的教训。

另外：**本任务不需要碰 `IrisShaderCreatorMixin`。**
它的 `@ModifyVariable(method="link", argsOnly=true, index=5, require=0)` 已在
`8f3a7a35` 上核对过 `ShaderCreator.link` 签名共 8 个参数、index 5 确为
`String fragment`，索引是对的。但注意它是**同一类静默失效点**：索引一旦漂移，
dormant 分支不进 fragment 源码 → uniform 不存在 → `glGetUniformLocation` 返回 −1 →
直接 return，**症状与本任务要修的一模一样且日志无报错**。排查时先看日志里有没有
`[TACZ Scope] Injecting dormant scope-mask branch …`。

---

## 4. 不要顺手做的事

- 不要改 `unit = Math.max(15, GL_MAX_TEXTURE_IMAGE_UNITS - 1)`。
  Iris 的 `ProgramSamplers.Builder` 从 `nextUnit=0` 起、跳过保留单元 `{0,1,2}`、
  逐个 +1，`remainingUnits = maxTextureUnits - 3`，耗尽时抛
  `No more available texture units`。只有光影包用到 **≥29 个动态采样器**才会占到
  单元 31 相撞。已知低危，本仓也没改。
- 不要动 `ScopeMaskRenderer` 的凸包算法。本仓那边另有改动（脱离投影 UBO 读回、
  改斜率空间），**那是独立的第二个病灶，与本任务无关**，你们那边是否需要同步
  由你们自己判断，别混在一个 PR 里。

---

## 5. 验证门槛

必做：

1. `bash scripts/check_release_consistency.sh --strict`（如果你们仓里有这个脚本）
   或你们自己的等价门禁，必须绿。
2. 三个改动文件做括号/字符串配平与 import 使用性核对；确认
   `resetShaderProgram` 全仓无残留引用。
3. **自己取证 Fabric 侧的 mixin 注册顺序**：拿一份你们实机的启动日志，
   找 tacz 的 `tacz.iris.mixins.json` 与 Iris 的 mixin config 各自的注册时刻，
   确认当前落在哪一行。这条结论要写进你们的记录文档 —— 本任务不预设答案。

不得声称：

- 不得声称「修好了用户报的镜内裁切失效」—— 用户没在你们这边报过这个现象。
- 不得在没实机的情况下把兼容层任何一行标成 PASS。
- 沙箱里如果同样没有 JDK，**如实写「未编译」**，别把静态核对说成编译通过。

---

## 6. 一句话版本

给 `IrisGlCommandEncoderMixin` 加一个 `trySetup` HEAD 注入记下当前 `GlRenderPass`；
把 `IrisExtendedShaderMixin` 在 `iris$setupState` RETURN 的「无条件写 0」换成
「按记下的 pass 写正确 mode」；删掉 `applyToGlRenderPass` 里
「没有当前程序就从 `pipeline.program()` 取 programId」那条退回分支。
这样 `trySetup` RETURN 与 `iris$setupState` RETURN 两个点谁最后跑都写对，
不再依赖 tacz 与 Iris 的 mixin config 注册先后。

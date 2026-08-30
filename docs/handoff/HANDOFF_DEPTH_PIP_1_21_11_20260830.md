# 深度版镜内画中画（PIP）—— 1.21.11 原型设计

日期：2026-08-30
来源：本仓 26.2 分支「遮光环被 PIP 合成盖掉」一役的跨分支取证
对象：本仓 `1.21.11` 分支（depth-aperture 架构），原型验证后再谈 26.1.2
状态：**设计稿，未实现**。已知怎么做（孔径信号怎么来、时序怎么排），
有两处 API 未取证（见 §6）—— 这正是原型阶段要解决的。

> 本会话被钉在 26.2 分支，不能动 `1.21.11`。§7 的提示词是给**开在 1.21.11 上的
> 新会话**用的。

---

## 1. 与 26.2 那套的根本差别

26.2 的 PIP 靠**离屏掩码纹理**（`ScopeMaskTarget`）判孔径；depth-aperture 分支
**没有掩码**，而且按姊妹仓库的交接约定，也不该把掩码架构搬过来。

但 depth-aperture 分支有**等价的孔径信号**：`ScopeDepthCopyState` 手上有两张
可采样的深度纹理，孔径判定就是一句比较。**这条分支不需要掩码也能做 PIP。**

## 2. 孔径信号：1.21.11 上现成的八行

`scope_reticle_mask.fsh`（1.21.11）已经是「采两张深度纹理判孔径」，逐字可复用：

```glsl
uniform sampler2D tacz_WorldDepthSampler;      // 目镜绘制【前】的世界深度（步骤 1）
uniform sampler2D tacz_ApertureDepthSampler;   // 目镜绘制【后】拷下来的深度（步骤 3）
const float TACZ_MASK_EPSILON = 1.0e-6;

vec2 wUv = gl_FragCoord.xy / max(vec2(textureSize(tacz_WorldDepthSampler, 0)), vec2(1.0));
vec2 aUv = gl_FragCoord.xy / max(vec2(textureSize(tacz_ApertureDepthSampler, 0)), vec2(1.0));
float wd = texture(tacz_WorldDepthSampler, wUv).r;
float ad = texture(tacz_ApertureDepthSampler, aUv).r;
if (!(ad < wd - TACZ_MASK_EPSILON)) discard;   // 孔径外丢弃
```

`ScopeDepthCopyState` 的头注释把整套流程写得很清楚：
BACKUP（存世界深度）→ 不可见目镜写近深度 → APERTURE_COPY（拷下来）→ 镜身正常画
（孔径内的像素被深度判掉）→ RESTORE（写回世界深度）→ MASK（准星比较两张深度）。

关键点：**APERTURE_COPY 是在镜身绘制之前拷的**，所以那份深度里只有「世界深度 +
目镜的近深度」，不会被枪身污染 —— 判据干净。

需要新增的：给 `ScopeDepthCopyState` 加两个访问器，把 `WORLD_TARGET` /
`APERTURE_TARGET` 的纹理交给合成阶段绑定（两者现在都是 `private static final`）。

## 3. 时序：与 26.2 相反，而且必须靠最终覆盖兜住

**这是整个设计里最容易搞错的一点**：26.2 的掩码在阶段边界就画好了，所以合成可以
排在手持**之前**；depth-aperture 分支的孔径信号是**手持那一遍里**才产生的
（目镜要先光栅化、再拷深度），所以**合成只能排在手持之后**。

于是合成会把手持画进孔径里的东西全部盖掉 —— 也就是**准星和目镜框**。
好在 1.21.11 上已经有现成解法：`ScopeFinalOverlayState`（排队 + 延后重画，
目前只在 Iris 路径 flush）。只要把 flush 提前到「我们的合成之后」，就天然成立：

```
世界渲染
  → 拷贝主画面（此时还没有枪）
  → 手持那一遍（目镜写近深度 → 深度拷贝；镜身被深度裁出孔径；准星/目镜框排队）
  → 〔Iris：composite / final pass〕
  → 【PIP 合成】按深度判据把放大后的世界贴进孔径
  → 【最终覆盖 flush】准星 + 目镜框重画到合成之上   ← 1.21.11 已有
```

需要改动：把 `ScopeFinalOverlayState` 的 flush 从「仅 Iris」扩展到**两条路径都
flush**（不开光影时也要在合成之后补一遍）—— 这是对 1.21.11 既有工作代码的侵入，
是本次改动的主要回归风险，务必保留 `ScopeLateReticleState` 的旧路径作对照。

## 4. 其余部件（与 26.2 同构，可照搬语义）

- **拷贝**：`ScopePipTarget` 那类离屏 target，在「世界画完、视模开画之前」抓一份。
  26.2 的实现可直接抄语义（注入点：世界渲染结束处）。
- **重投影**：倍率就是瞄具倍率，采样点 `center + (uv − center) / Z`，与 26.2
  `scope_pip.fsh` 同一份数学；Catmull-Rom 重建 + 按倍率加权的钝化蒙版可整段复用
  （同一个仓库，直接读 26.2 分支那份 `scope_pip.fsh`）。
- **FOV**：PIP 的全部意义是「镜外保持 1×」，所以要把整屏变焦的 FOV 抑制掉，
  改由着色器放大 —— 与 26.2 的 `suppressesWorldFovZoom()` 同职责。
- **闸门与回退**：`ScopePipEnable` 默认关；任一环不满足即走整屏变焦；
  运行期异常自我停用并退回整屏变焦（这条在 26.2 上是救命设计，必须保留）。
- **配置**：与 26.2 的 12 个 `ScopePip*` 对齐，便于跨分支 A/B。

## 5. 分步计划

1. **先补访问器**：`ScopeDepthCopyState` 暴露两张深度纹理（`WORLD_TARGET` /
   `APERTURE_TARGET` 现在都是 `private static final`）。同时把 Iris 分支里那份
   「私有世界深度拷贝」的触发条件从 `ScopeFinalOverlayState.hasPendingReticles()`
   放宽成「有待重画准星 **或** 本帧 PIP 生效」—— 否则 Iris 下 PIP 拿不到世界深度
   （见 §6.2）。这一步仍然不改任何可见行为。
2. **最小验证**：写一个只画纯品红的全屏 pass，用 §2 的判据，确认「只有孔径被涂红」
   —— 这一步不过，后面全是空转（26.2 上 `ScopePipDebugPaintLens` 就是干这个的）。
3. **接拷贝与重投影**：倍率传 1.0 先验证「孔径里是世界、且没有缩小的枪」。
4. **接倍率**：验证放大倍率与瞄具倍率一致。
5. **接最终覆盖**：准星 + 目镜框回到画面最上层（含不开光影那条路径）。
6. **接 FOV 抑制**：镜外回到 1×。
7. **配置化 + 回退**。

## 6. 未取证 / 未验证（诚实清单）

1. **深度纹理怎么绑进我们自己的 RenderPass**：准星那条路是把它们绑进 RenderType
   （走 vanilla 的管线绑定），而合成是我们自己开的 `RenderPass#bindTexture`，
   要的是 `GpuTextureView`。`DepthTextureTarget` 能否直接给出可绑定的 view —— **未经证实**，
   可能要加一层。
2. **Iris 下的世界深度 —— 已经找到现成机制，只剩「怎么绑」这一个未知数。**
   我原本担心「Iris 下世界深度是 `depthtex2`，我们自己的裸 pass 绑不到」。
   读 `ScopeDepthCopyState`（1.21.11）时发现 **1.21.11 已经为 final-overlay 解决了同一件事**：

   ```java
   // R11 final-overlay shaders intentionally run after Iris has stopped binding
   // depthtex2. When a frozen final reticle exists, take one private copy now;
   // it is the same pre-ocular world depth but remains sampleable after final
   // composite. Ordinary Iris paths keep using depthtex2 without this blit.
   if (ScopeFinalOverlayState.hasPendingReticles()) {
       boolean copied = copyCurrentDepth(WORLD_TARGET, "final-overlay world depth");
       worldDepthIdentity = copied ? captureDepthIdentity() : null;
       if (!copied) { maskWorldValid = false; }
   }
   ```

   也就是说：**只要把这份私有拷贝的触发条件从「有待重画准星」放宽到
   「有待重画准星 **或** PIP 本帧生效」**，PIP 合成就能拿到一份在 final composite
   之后仍然可采样的世界深度 —— 而且它拷的就是 `WORLD_TARGET`，正是 §2 判据里
   `tacz_WorldDepthSampler` 读的那张。判据一行都不用改。
   剩下的未知数只有 §6.1（怎么把 `DepthTextureTarget` 的纹理绑进我们自己开的
   `RenderPass`）。拷失败时 `maskWorldValid = false`，PIP 必须跟着退回整屏变焦。
3. 26.2 的经验：合成管线**不要**声明 DepthStencilState（不测深度也不写深度），
   否则后续画的准星会被判成遮挡。
4. 本环境无 JDK / 无 Maven 源，**一行都没编译过**。

## 7. 【可直接粘贴】给 1.21.11 会话的提示词

```
在本分支（1.21.11，depth-aperture 架构）上做「深度版镜内画中画（PIP）」原型。
本仓 26.2 分支已经有一版 PIP（靠离屏掩码判孔径）并实机验证；本分支没有掩码、
按交接约定也不该搬掩码架构，但可以用深度信号做等价实现。

【孔径信号：本分支现成的八行，逐字可用】
scope_reticle_mask.fsh 已经在做「采两张深度纹理判孔径」：
  uniform sampler2D tacz_WorldDepthSampler;     // 目镜绘制前的世界深度
  uniform sampler2D tacz_ApertureDepthSampler;  // 目镜绘制后拷下来的深度
  float wd = texture(tacz_WorldDepthSampler, wUv).r;
  float ad = texture(tacz_ApertureDepthSampler, aUv).r;
  if (!(ad < wd - 1.0e-6)) discard;             // 孔径外丢弃
ScopeDepthCopyState 的头注释把流程写全了（BACKUP → 目镜写近深度 → APERTURE_COPY →
镜身被深度裁出孔径 → RESTORE → MASK）。APERTURE_COPY 在镜身绘制之前拷，
所以那份深度里只有「世界深度 + 目镜近深度」，判据干净。
需要给 ScopeDepthCopyState 加两个访问器，把 WORLD_TARGET / APERTURE_TARGET
的纹理交给合成阶段绑定（现在都是 private static final）。

【时序：与 26.2 相反，这是最容易搞错的一点】
26.2 的掩码在阶段边界就画好了，合成可以排在手持【之前】；本分支的孔径信号是
手持那一遍里才产生的（目镜要先光栅化、再拷深度），所以合成只能排在手持【之后】。
于是合成会盖掉手持画进孔径里的准星和目镜框 —— 解法是本分支已有的
ScopeFinalOverlayState（排队 + 延后重画，目前只在 Iris 路径 flush）：
把它改成「PIP 合成之后也 flush」（不开光影那条路径同样要补一遍）。
  世界 → 拷贝主画面 → 手持（深度拷贝；准星/目镜框排队）
      → 〔Iris: composite/final〕→ PIP 合成 → 最终覆盖 flush

【其余部件照搬 26.2 的语义（同一个仓库，直接读 26.2 分支）】
- ScopePipTarget 那类拷贝：在「世界画完、视模开画之前」抓一份；
- scope_pip.fsh 的重投影数学（倍率=瞄具倍率，采样点 center+(uv-center)/Z，
  Catmull-Rom + 按倍率加权的钝化蒙版）可整段复用；
- suppressesWorldFovZoom()：PIP 的意义是镜外保持 1×，要抑制整屏 FOV 变焦；
- 闸门与回退：ScopePipEnable 默认关；任一环不满足走整屏变焦；运行期异常
  自我停用并退回整屏变焦（26.2 上是救命设计，必须保留）；
- 配置与 26.2 的 12 个 ScopePip* 对齐，便于跨分支 A/B。

【分步（每步都要用户实机确认再进下一步）】
1) 先只加访问器，不改行为；
2) 写「只涂纯品红」的全屏 pass，用上面的判据，确认只有孔径被涂红
   （26.2 的 ScopePipDebugPaintLens 就是干这个的）—— 这步不过后面全是空转；
3) 接拷贝与重投影，倍率传 1.0，验「孔径里是世界、且没有缩小的枪」；
4) 接倍率；5) 接最终覆盖（含不开光影那条路径）；6) 接 FOV 抑制；7) 配置化。

【已知的 / 未取证的】
1) Iris 下的世界深度【已有现成机制，不用自己造】：ScopeDepthCopyState 的 Iris 分支里
   已经有这么一段（注释原文照抄）：
     // R11 final-overlay shaders intentionally run after Iris has stopped binding
     // depthtex2. When a frozen final reticle exists, take one private copy now;
     // it is the same pre-ocular world depth but remains sampleable after final
     // composite. Ordinary Iris paths keep using depthtex2 without this blit.
     if (ScopeFinalOverlayState.hasPendingReticles()) {
         boolean copied = copyCurrentDepth(WORLD_TARGET, "final-overlay world depth");
         worldDepthIdentity = copied ? captureDepthIdentity() : null;
         if (!copied) { maskWorldValid = false; }
     }
   所以只要把触发条件放宽成「有待重画准星 或 本帧 PIP 生效」，PIP 合成就有一份
   在 final composite 之后仍可采样的世界深度，而且就是判据里 tacz_WorldDepthSampler
   读的那张 WORLD_TARGET，判据一行都不用改。拷失败时 maskWorldValid=false，
   PIP 必须跟着退回整屏变焦。
2) 【仍未取证】深度纹理怎么绑进我们自己开的 RenderPass —— 准星那条路是绑进
   RenderType 走 vanilla 绑定，我们要的是 GpuTextureView；DepthTextureTarget
   能否直接给出可绑定的 view 未证实，可能要加一层包装。
   这一条要是走不通，就先用不开光影路径做原型，Iris 路径后置。
另：合成管线不要声明 DepthStencilState（不测不写深度），否则后续准星被判遮挡 ——
这是 26.2 上踩过的。

【要求】按仓库规矩：先取证再动手，源码级实现要自审并标注未验证项，不要写 PASS，
每步等用户实机反馈。动到 ScopeFinalOverlayState 的 flush 时机属于侵入既有工作
代码，请保留 ScopeLateReticleState 的旧路径作对照，并给出回归复测清单。
```

## 8. 为什么先做 1.21.11 而不是 26.1.2

1.21.11 上 `ScopeFinalOverlayState` 已经存在（26.1.2 还没有），而深度版 PIP 的
时序**依赖**这套最终覆盖来兜住准星与目镜框。所以正确顺序是：
**先把 26.1.2 补到 1.21.11 的水平（见 `HANDOFF_26_1_2_CATCHUP_20260830.md`），
再在 1.21.11 上做深度版 PIP 原型，通过后再往 26.1.2 搬。**

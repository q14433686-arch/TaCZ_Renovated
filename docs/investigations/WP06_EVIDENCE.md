# 工作包⑥ 证据清单

语义：Fabric 26.1.2 depth-aperture 的 Iris HAND 注入（`ShaderCreator` fragment 休眠分支）。
加载器：NeoForge 26.1.2.97。禁止抄 MUKSC 渲染。Iris 不在编译 classpath。

## 加载器 API（② loader-11.0.15 javap）

| 调用 | 证据 |
|---|---|
| `FMLLoader#getCurrent()` / `#getLoadingModList()` / `LoadingModList#getModFileById(String)` | ② loader-11.0.15 javap。`LoadingModList.get()` 已 `@Deprecated(forRemoval)`；mixin plugin 阶段 `ModList.get()` 尚未就绪 |
| `ModList#isLoaded(String)` | ② 运行期 `IrisCompat` 反射闸门 |
| `IrisApi#assignPipeline(RenderPipeline, IrisProgram)` | Fabric 26.1.2 公开 API，本 port 经 `Class.forName` 调用 |
| `HandRenderer#isActive` / `#isRenderingSolid` / `#isHandTranslucent` | 同上，反射 |

## 实现要点

- `ShaderCompat`：调用方只依赖这一层。26.2 Aperture 换后端时不改瞄具/第一人称代码。
- `tacz.iris.mixins.json`：`required=false` + `IrisCompatMixinPlugin`。无 Iris 时不应用、不炸 dedicated。
- `IrisDepthRestoreShaderMixin`：`@Mixin(targets="...ShaderCreator")`，编译期零 Iris 类型。只给 HAND 程序注入 `tacz_DepthRestoreMode` / `tacz_ScopeMaskMode` 休眠分支。
- 第一人称 solid/translucent 相位闸：`ShaderCompat.shouldRenderInCurrentHandPhase`。
- 自定义 pipeline 仍在 `ScopeRenderTypes` 静态初始化里 `assignPipeline(..., HAND / HAND_TRANSLUCENT)`。

未接触 `tacz-port` jar。无 GPU，Iris 实机矩阵留给有显示器的环境。

# TaCZ NeoForge 26.2（非官方移植）

> **非官方。请勿向原作者（MCModderAnchor / Serene Wave Studio）报 bug。**
> 目标：**Minecraft 26.2 + NeoForge 26.2.0.x（release）+ Java 25**。
> 当前源码版本：**`1.1.8+neoforge.26.2.0.r0`**。

Timeless and Classics Zero 的 NeoForge 26.2 前滚移植。26.1.2 Beta-1 是唯一代码出发点；
本仓库保持公开源码与可审计谱系。

**当前 r0 尚未发布：** 26.2 classfile、NeoForge/Iris/兼容 Mod 源码与静态 API 检查已完成，
但本执行环境无法运行生产 JDK 25 Gradle 构建，也没有 GPU。`compileJava`、专服 `Done`、
OpenGL/Iris/Vulkan 与可选 Mod 游戏矩阵全部仍待执行。未实测内容不会标成 PASS。

## 谱系与许可

```text
MCModderAnchor/TACZ                         1.20.1 Forge 官方源
        │
        ├── Sh1roCu/TACZ-Refabricated      1.21.1 Fabric
        │       └── q14433686-arch/TaCZ_Refabricated_Unofficial
        │               26.2 Fabric（游戏语义权威）
        │
        └── MUKSC/TACZ-1.21.1              1.21.1 NeoForge
                        （仅加载器习语参考；其渲染禁止照抄）
                                │
                                └── 本仓库 NeoForge 26.1.2 → 26.2
```

- 代码：GPL-3.0-only；发布二进制时必须同步提供完整对应源码并保留版权声明。
- 原版资源：CC BY-NC-ND 4.0。
- 第三方依赖与资源许可见 [`LICENSES.md`](LICENSES.md)。
- 工作规则与洁净室红线见 [`CHARTER.md`](CHARTER.md)。

## 26.2 渲染边界

- **OpenGL**：保留并前滚 depth-aperture 瞄具；可选 Iris 1.11.x 走反射 API 与
  HAND/HAND_TRANSLUCENT shader bridge。
- **Vulkan（实验）**：不执行 OpenGL depth copy；瞄具走隐藏 opaque ocular 的未掩码降级，
  并记录一次说明日志。
- **Aperture**：未作为已发布依赖接入，本期不支持。

这意味着 r0 不应被描述为“Vulkan 瞄具/光影已兼容”。

## 可选 Mod

26.2 artifact、源码签名、明确缺口与待执行矩阵见
[`COMPATIBILITY.md`](COMPATIBILITY.md)。特别注意：执行日 First-person Model 与 Not Enough
Animations 均没有 NeoForge 26.2 发布文件；代码中的反射桥只是 dormant 预留。

## 开发环境

- JDK **25**（官方 26.2 MDK toolchain）
- Gradle Wrapper **9.2.1**
- ModDevGradle **2.0.144**
- Minecraft **26.2** / NeoForge **26.2.0.64**
- 26.1+ 游戏本体未混淆：**不要配置 mappings / parchment / Yarn**

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew build --no-configuration-cache
./gradlew runServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
```

证据记录位于 `docs/WP262_*_EVIDENCE.md`；当前诚实状态见
[`docs/PORTING_STATUS.md`](docs/PORTING_STATUS.md)。

## 版本号

`mod_version` 必须保持 `1.1.8+neoforge...`：`+` 后是 SemVer build metadata，不参与枪包
`>=1.1.8` 的版本排序。

**禁止**写成 `1.1.8-neoforge...`；`-` 会把它变成低于正式 `1.1.8` 的 pre-release，
导致部分枪包依赖检查静默失败。

## 洁净室

禁止接触 CurseForge 项目 `tacz-port`（作者 guilhermez1989）的 jar：不下载、不反编译、
不参考其代码或声称源自该 jar 的片段。

<!-- release-version: 1.1.8+neoforge.26.1.2.R3-hotfix2 -->
# TaCZ: Renovated — Minecraft 26.1.2 / NeoForge（R3-hotfix2）

> **非官方社区移植，不是 TaCZ 官方发布，也未获 TACZ Dev Team 审核或背书。
> 本移植的问题请提交到本仓库，不要打扰原作者。**
>
> 本公告只覆盖相对于已发布 **R3** 的热修。R3 的实机验收结论不自动延伸到本次新增改动。

## 环境

- Minecraft：**26.1.2**
- NeoForge：**26.1.2.x**（开发基于 **26.1.2.97**）
- Java：**25+**
- Mod：**`1.1.8+neoforge.26.1.2.R3-hotfix2`**
- 必需前置：**无**

不同 Minecraft 版本的文件不能混用；这不是 1.21.11 的版本。

## 本次变化（相对 R3）

### 修复

- **工作台配方启动日志刷屏**：将枪械工作台的自定义配方标记为 special，消除
  `Recipe tacz:… can't be placed due to empty ingredients` 的无意义 WARN；工作台、JEI/REI
  仍直接使用原有的 `TableRecipeManager`，配方列表与合成逻辑不变。
- **格洛克 17 缺失 raise 音效报错**：删除默认枪包动画中对不存在
  `p24_pi_golf17_stockskel_raise` 音效的引用，切枪时不再输出对应的 `Missing gun sound resource`。
- **Iris 手部管线无效 WARN**：移除对已注册 vanilla `ENTITY_*` / `ITEM_*` 管线的重复分配调用，
  不再输出 `Found perfect program match … HAND_CUTOUT`；Iris 原有的按 draw 手部/实体管线分派
  和 TaCZ 的 scope / mesh 自定义管线保持不变。这是删除无效调用，不是新增光影兼容声明。
- **高模枪在 Iris 光影下检视偏黑、反射异常**：GPU mesh 渲染在光影启用时改为每根骨骼独立
  `RenderPass`，使 Iris 为每根骨骼重新设置法线/模型视图状态并通知对应 albedo/PBR 材质；无光影
  时仍保持单批次。保留 VBO 缓存、MV push/pop、scope mask 与资源准备流程。

详细机制、适用性与复测清单见本线
[CHANGELOG](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/CHANGELOG.md)、
[可见日志修复记录](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/docs/records/VISIBLE_BUGS_39JqB2p_2612_20260908.md)
及 [mesh 光影记录](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/docs/records/MESH_GPU_IRIS_PASS_LIFETIME_2612_20260908.md)。

## 验证范围与已知边界

- 上述代码已通过本线 CI 的 compile-check / 全量 build；新增
  `./gradlew meshRenderPassTest` 已挂入 `check`，覆盖 RenderPass 生命周期的模型回归。
  该回归不是实际 Iris / OpenGL 集成测试。
- **本热修尚无维护者实机 PASS。** 发布前仍须在最终构建上测试：启动日志、格洛克 17 切换、
  Iris 下第一人称枪体/抛壳/火光、多材质高模枪检视、scope/PIP，以及无光影回归和性能。
- 已发布 R3 的实机 PASS 只适用于其包含的 R3 修复；不能据此宣称本热修、所有枪包、
  所有光影包或所有部署形态已验证。
- 本次不处理「开光影世界全透明」问题；它与本次日志修复无关，仍需单独跟进。

## 安装与兼容范围

将对应版本的 jar 放入 `mods/`，先备份世界和枪包。现代枪包放入 `tacz/`；
旧布局包备份后放入 `tacz_backup/` 并执行 `/tacz convert`。联机枪包需双端安装，
服务端执行 `/tacz reload`，客户端新增包按 F3+T 重载。

不支持明确依赖 TacZ:Arcana 的内容。LRTactical 不含 flash_shield 或原作完整美术资源。
混合服、代理与面板环境的问题应先在原生 NeoForge 专服复现；兼容范围见本线
[兼容矩阵](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/docs/COMPATIBILITY.md)。

## 许可与来源

- [源码与对应 tag](https://github.com/q14433686-arch/TaCZ_Renovated) ·
  [问题反馈](https://github.com/q14433686-arch/TaCZ_Renovated/issues)
- [原始 TaCZ](https://github.com/MCModderAnchor/TACZ) ·
  [直接上游](https://github.com/Sh1roCu/TACZ-Refabricated) ·
  [Fabric 姊妹语义主线](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
- [NeoForge 移植骨架参考（GPL-3.0）](https://github.com/MUKSC/TACZ-1.21.1)，未采用其渲染代码。
- [内置 TML 上游（GPL-3.0）](https://github.com/VellEagle/TacZMeshLoader)

代码 GPL-3.0-only；默认枪包资源 CC BY-NC-ND 4.0；LuaJ 为 MIT，commons-math3 为
Apache-2.0。完整说明见
[LICENSES.md](https://github.com/q14433686-arch/TaCZ_Renovated/blob/26.1.2/LICENSES.md)。
Release 的 Source code 归档必须对应所挂二进制；热修使用新版本与新 tag，不覆盖旧资产。

# 26.2 R2 发布检查清单

目标版本：`1.1.8+neoforge.26.2.R2`。R1 的归档清单见
[`records/RELEASE_CHECKLIST_26_2_R1.md`](records/RELEASE_CHECKLIST_26_2_R1.md)（未关闭项
已结转进本页）。本页全部阻塞项关闭 **且** 收到项目发起人明确发布命令之前，CHANGELOG
的 R2 条目不得视为已发布。

## A. 版本与构建

- [x] `gradle.properties`、README、CHANGELOG、metadata 与文档版本均为 R2。
- [x] `bash scripts/check_release_consistency.sh --strict` 退出 0。
- [ ] JDK 25 clean build（`./gradlew build`）在本分支跑通。
- [ ] `git diff --check`、全部 mixin JSON 与枪包 JSON 解析通过。

## B. L0 产物

- [ ] jar 文件名为 `tacz-1.1.8+neoforge.26.2.R2.jar`。
- [ ] `META-INF/neoforge.mods.toml` 已展开 R2，无未知 placeholder。
- [ ] jar 内含 tacz / iris / carryon / lrtactical / mesh 五份 mixin JSON 与 AT。
- [ ] jar-in-jar metadata 中有 LuaJ 和 Commons Math。
- [ ] LICENSE、第三方许可说明与最终 jar 内容一致；`LICENSES.md` 已登记
      TacZMeshLoader（GPL-3.0）。

## C. 专服与多人

- [ ] LR-integrated `runServer`：Mod List、TaCZ/LR payload、索引加载与 `Done`。
- [ ] 生产专服：无 client class load、资源/AT/mixin/jarJar 正常。
- [ ] L3 双客户端基础矩阵（含**别人手里的 mesh 枪** —— 第 2 步默认开，这条是必测项）。
- [ ] L2.5 第三方枪包与 LR 内容包专项明确确认。

## D. LRTactical

- [ ] 单机：创造栏、三类 tooltip、动态/占位模型与 F3+T。
- [ ] throwable：普通/粘性/烟雾/闪光/效果云/C4，实体与粒子不崩。
- [ ] melee / consumable / detonator：左右键、冷却、治疗/效果、遥控引爆。
- [ ] HUD/音频：使用进度、分类冷却遮罩、致盲、耳鸣与音量恢复。
- [ ] 专服：三类 index 登录/重载同步、实体 tracking、S2C 冷却、C2S 近战权威结算。

## E. 客户端与 GPU

- [ ] 无可选 Mod 的完整枪械、工作台、同步、资源重载回归。
- [ ] OpenGL 无 Iris：ocular mask、低/高倍准星、ring、配件、火光。
- [ ] OpenGL + Iris 1.11.2：HAND solid/translucent、shadow、water/fog/particles、mode reset。
- [ ] Vulkan（`earlyWindowControl=false`）：mask target、resize/reload、无 device loss。
- [ ] 低倍 reticle-only mask 与高倍 full-viewmodel mask 反复切换无状态残留。

## F. 内置 Mesh Loader（R2 新增面，第 2 步默认开）

- [ ] `docs/MESH_LOADER.md` §5.2 第 8–12 条（第 1 步）复测 —— 维护者 2026-08-31 已
      复测通过，发版前再确认一次未被第 2 步改动影响。
- [ ] `docs/MESH_LOADER.md` §5.4 九项矩阵（第 2 步）**逐条回报** —— 维护者 2026-09-01
      报告实机 PASS，但未逐条回报；作为发版依据需补齐，尤其：
      开背包时同屏世界 mesh 枪不掉 collector、多人满屏高模枪、光影下世界枪照明。
- [ ] 至少一组帧率对比数字（多人满屏高模枪，`MeshGpuWorld` 开 / 关）。
- [ ] `MeshGpuWorld=false` 与合入前行为一致（回退路径仍然有效）。
- [ ] mesh 枪包与立方体枪包混装、重载、`F3+T` 无残留。

## G. Scope PIP / 镜内裁手 / 镜内文字

- [ ] Scope PIP 实机矩阵（默认关闭：关闭时与合入前逐位等价这一条必须先确认）。
- [ ] Scope PIP 开启态：重投影与二次渲染两模式、Iris / Voxy / Sodium / PhysicsMod。
- [ ] 镜内裁手、镜内文字：姊妹侧已 PASS，本仓仍需一轮实机（含光影下文字不黑块、
      MARK5 开镜渐开不越目镜）。

## H. 源码与发布

- [ ] 工作树干净，最终 commit 已记录。
- [ ] **收尾 changelog**：跑 `scripts/generate_changelog.sh`（或触发
      `changelog` workflow）生成条目草稿，**人工核对**后并入 `CHANGELOG.md`
      —— 脚本只按提交前缀归类，不负责判断机制描述是否准确。
- [ ] CHANGELOG 的 R2 条目日期改为实际发布日期，`## Unreleased` 增量并入下一版。
- [ ] tag 名、jar 名、source archive 名与 R2 一致。
- [ ] GPL 对应源码与二进制同时提供。
- [ ] 项目发起人已明确下达发布命令。

未收到明确命令时：**不 merge、不打 tag、不创建 Release、不上传 jar。**

# 26.2 R1 发布检查清单（已归档）

> **归档件**：R1 已于 2026-08-22 发布，本页不再维护。R2 的现行清单是
> [`docs/RELEASE_CHECKLIST.md`](../RELEASE_CHECKLIST.md)。保留此页只为追溯
> R1 当时哪些项关了、哪些没关（未关项已结转进 R2 清单）。

目标版本：`1.1.8+neoforge.26.2.R1-hotfix`。当前 CHANGELOG 必须保持 **Unreleased**，直到本页
所有阻塞项关闭并得到项目发起人明确发布命令。

## A. 版本与构建

- [x] `gradle.properties`、README、CHANGELOG、metadata 与文档版本均为 R1。
- [x] `bash scripts/check_release_consistency.sh --strict` 退出 0。
- [ ] LRTactical 合入后重新执行 JDK 25 clean compile/build。
- [x] `git diff --check`、全部 mixin JSON 与 LR JSON 解析通过。

## B. L0 产物

- [ ] jar 文件名为 `tacz-1.1.8+neoforge.26.2.R1-hotfix.jar`。
- [ ] `META-INF/neoforge.mods.toml` 已展开 R1，无未知 placeholder。
- [ ] jar 内含 tacz / iris / carryon / lrtactical mixin JSON 与 AT。
- [ ] jar 内含 `me/xjqsh/lrtactical/**`、`assets/lrtactical/**` 与 `data/lrtactical/**`。
- [ ] jar-in-jar metadata 中有 LuaJ 和 Commons Math。
- [ ] LICENSE、第三方许可说明与最终 jar 内容一致；没有 LRTactical 原作 ARR 美术。

## C. 专服与多人

历史证据（不等于当前 HEAD）：

- [x] LR 合入前核心候选 L1 `runServer` 用户 PASS。
- [x] LR 合入前核心候选 L2 NeoForge 26.2.0.64 生产专服用户 PASS。
- [x] LR 合入前核心候选 L3 基础双客户端层级用户 PASS。

当前 artifact：

- [ ] LR-integrated R1 `runServer`：Mod List、TaCZ/LR payload、索引加载与 `Done`。
- [ ] LR-integrated R1 生产专服：无 client class load、资源/AT/mixin/jarJar 正常。
- [ ] L3 双客户端基础矩阵重跑。
- [ ] L2.5 第三方枪包与 LR 内容包专项明确确认。

## D. LRTactical

- [ ] 单机：创造栏、三类 tooltip、动态/占位模型与 F3+T。
- [ ] throwable：普通/粘性/烟雾/闪光/效果云/C4，实体与粒子不崩。
- [ ] melee / consumable / detonator：左右键、冷却、治疗/效果、遥控引爆。
- [ ] HUD/音频：使用进度、分类冷却遮罩、致盲、耳鸣与音量恢复。
- [ ] 专服：三类 index 登录/重载同步、实体 tracking、S2C 冷却、C2S 近战权威结算。
- [ ] 至少一个第三方 LR 内容包双端安装通过；flash_shield 明确不在范围内。

## E. 客户端与 GPU

- [ ] 无可选 Mod 的完整枪械、工作台、同步、资源重载回归。
- [ ] OpenGL 无 Iris：ocular mask、低/高倍准星、ring、配件、火光。
- [ ] OpenGL + Iris 1.11.2：HAND solid/translucent、shadow、water/fog/particles、mode reset。
- [ ] Vulkan（`earlyWindowControl=false`）：mask target、resize/reload、无 device loss。
- [ ] 低倍 sight reticle-only mask 与高倍 full-viewmodel mask 反复切换无状态残留。
- [ ] LR HUD/实体/动态物品渲染不破坏 scope-mask 与 Feature Rendering。

## F. 可选 Mod

逐项按 [`../COMPATIBILITY.md`](../COMPATIBILITY.md) 更新为用户结果；未测试可保持“未实测”，
但不得写 PASS：

- [ ] Cloth Config
- [ ] PAL
- [ ] Controllable + Framework
- [ ] Shoulder Surfing Reloaded
- [ ] JEI / REI / 同装
- [ ] Iris
- [ ] Carry On

FPM/NEA 没有 NeoForge 26.2 文件，不列入可安装验收。

## G. 源码与发布

- [ ] 工作树干净，最终 commit 已记录。
- [ ] 对应 source archive 与构建源码逐文件一致。
- [ ] CHANGELOG 从 Unreleased 改为实际发布日期。
- [ ] tag 名、jar 名、source archive 名与 R1 一致。
- [ ] GPL 对应源码与二进制同时提供。
- [ ] 项目发起人已明确下达发布命令。

未收到明确命令时：**不 merge、不打 tag、不创建 Release、不上传 jar。**

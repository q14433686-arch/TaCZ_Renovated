# 26.2 R1 发布检查清单

目标版本：`1.1.8+neoforge.26.2.0.R1`。当前 CHANGELOG 必须保持 **Unreleased**，直到本页
所有阻塞项关闭并得到项目发起人明确发布命令。

## A. 版本与构建

- [ ] `gradle.properties`、README、CHANGELOG、metadata 与文档版本均为 R1。
- [ ] `bash scripts/check_release_consistency.sh --strict` 退出 0。
- [ ] R1 定名后重新执行 clean compile/build（版本 metadata 变化后的最终产物）。
- [ ] `git diff --check` 与 mixin JSON 解析通过。

## B. L0 产物

- [ ] jar 文件名为 `tacz-1.1.8+neoforge.26.2.0.R1.jar`。
- [ ] `META-INF/neoforge.mods.toml` 已展开 R1，无未知 placeholder。
- [ ] jar 内含 tacz / iris / carryon mixin JSON 与 AT。
- [ ] jar-in-jar metadata 中有 LuaJ 和 Commons Math。
- [ ] LICENSE、第三方许可说明与最终 jar 内容一致。

## C. 专服与多人

- [x] L1 `runServer` 用户 PASS（定名前同代码候选）。
- [x] L2 NeoForge 26.2.0.64 生产专服用户 PASS（定名前同代码候选）。
- [x] L3 基础双客户端层级用户 PASS（无逐行日志，不外推可选行）。
- [ ] R1 最终 jar 快速复核 Mod List 版本与 `Done`。
- [ ] L2.5 第三方枪包专项明确确认。

## D. 客户端与 GPU

- [ ] 无可选 Mod 的完整枪械、工作台、同步、资源重载回归。
- [ ] OpenGL 无 Iris：ocular mask、低/高倍准星、ring、配件、火光。
- [ ] OpenGL + Iris 1.11.2：HAND solid/translucent、shadow、water/fog/particles、mode reset。
- [ ] Vulkan（`earlyWindowControl=false`）：mask target、resize/reload、无 device loss。
- [ ] 低倍 sight reticle-only mask 与高倍 full-viewmodel mask 反复切换无状态残留。

## E. 可选 Mod

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

## F. 源码与发布

- [ ] 工作树干净，最终 commit 已记录。
- [ ] 对应 source archive 与构建源码逐文件一致。
- [ ] CHANGELOG 从 Unreleased 改为实际发布日期。
- [ ] tag 名、jar 名、source archive 名与 R1 一致。
- [ ] GPL 对应源码与二进制同时提供。
- [ ] 项目发起人已明确下达发布命令。

未收到明确命令时：**不 merge、不打 tag、不创建 Release、不上传 jar。**

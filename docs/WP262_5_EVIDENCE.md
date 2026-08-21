# WP-262-5 证据：发布准备与阻塞闸门

日期：2026-08-21

## 已完成的发布准备

- 根 `README.md` 的目标、源码版本、工具链和渲染边界更新到 26.2；删除一期逐包进度表，
  不把 README 当工作日志。
- 根 `CHANGELOG.md` 新增 `Unreleased — 1.1.8+neoforge.26.2.0.R1` 条目。
- `LICENSES.md` 更新到 Minecraft 26.2 / NeoForge 26.2.0.64 / MDK-26.2，并列出
  jar-in-jar 与可选 compile-only 许可。
- `docs/PORTING_STATUS.md` 改为 26.2 真实状态与发布阻塞清单。
- `COMPATIBILITY.md` 保持每行“API 已核 / 未实测”的明确区分。
- `mod_version` 仍为 `1.1.8+neoforge.26.2.0.R1`，没有引入 `-` prerelease。

## 尚未通过，故发布被阻塞

用户已在 Windows JDK 25 / Gradle 9.2.1 环境对定名前同代码候选报告 L0-L3 **PASS**，
覆盖 build、jar 基础内容、`runServer`、真实生产专服和双客户端基础联机层级。冻结回执：
`docs/records/SERVER_TEST_20260821_262_R1.md`。随后只修改版本 metadata 与文档。

以下发布闸门仍未通过：

- 最终 R1 jar 的 build、L0 与 Mod List 版本快速复核；
- L2.5 第三方枪包专项未被本次回执单独确认；
- OpenGL / Iris / Vulkan 完整 GPU 矩阵；
- `COMPATIBILITY.md` 可选 Mod 逐项用户 PASS；
- metadata/license 与对应源码 tag/source archive 的最终一致性检查。

## 发布结论

**本工作树是未发布 R1 候选，不是可发布成品。** 未收到明确命令时不 merge、不打 tag、
不创建 Release、不上传 jar，也不把 CHANGELOG 的 Unreleased 改成日期版本。剩余步骤统一由
`docs/RELEASE_CHECKLIST.md` 管理。

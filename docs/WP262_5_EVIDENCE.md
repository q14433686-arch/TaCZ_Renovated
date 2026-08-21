# WP-262-5 证据：发布准备与阻塞闸门

日期：2026-08-21

## 已完成的发布准备

- 根 `README.md` 的目标、源码版本、工具链和渲染边界更新到 26.2；删除一期逐包进度表，
  不把 README 当工作日志。
- 根 `CHANGELOG.md` 新增 `Unreleased — 1.1.8+neoforge.26.2.0.r0` 条目。
- `LICENSES.md` 更新到 Minecraft 26.2 / NeoForge 26.2.0.64 / MDK-26.2，并列出
  jar-in-jar 与可选 compile-only 许可。
- `docs/PORTING_STATUS.md` 改为 26.2 真实状态与发布阻塞清单。
- `COMPATIBILITY.md` 保持每行“API 已核 / 未实测”的明确区分。
- `mod_version` 仍为 `1.1.8+neoforge.26.2.0.r0`，没有引入 `-` prerelease。

## 尚未通过，故发布被阻塞

当前沙盒仍没有生产 JDK 25 依赖下载通道或 GPU。2026-08-21 用户在 Windows 的真实
JDK 25 / Gradle 9.2.1 环境首次运行 `gradlew build`，26.2 artifact 生成成功并到达
`compileJava`，随后报告 9 个错误。本分支已依据该真实输出修复 AT、FOV pass、HUD tick
与 AvatarRenderer descriptor，但**修复后尚未重跑**。

因此以下发布闸门仍没有通过：

```bash
./gradlew clean compileJava --warning-mode all --no-configuration-cache
./gradlew build --no-configuration-cache
./gradlew runServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
```

也没有：

- `build/libs/tacz-1.1.8+neoforge.26.2.0.r0.jar`；
- jar-in-jar / metadata / license 的最终二进制检查；
- 专服 `Done` 与枪包装载数；
- OpenGL / Iris / Vulkan GPU 矩阵；
- `COMPATIBILITY.md` 用户 PASS。

## 发布结论

**本工作树是未发布 r0 候选，不是可发布成品。** 不创建 release、不上传 jar、不把
CHANGELOG 的 Unreleased 改成日期版本。待外部可运行环境完成全部闸门后，再执行：

1. 修复真实编译/启动错误；
2. 将兼容矩阵逐行更新为用户结果；
3. 检查 jar 内容与源码一致；
4. 给 CHANGELOG 条目定发布日期；
5. 同步发布 jar 与对应源码归档。

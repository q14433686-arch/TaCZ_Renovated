# 工作台与双格拾取修复：首轮 CI 快照（2026-10-27）

- PR [#66](https://github.com/q14433686-arch/TaCZ_Renovated/pull/66) 首个人类源码提交：`581daaacc8e21ef55259480c804442e17b455fbe`；按用户要求版本仍为 `1.1.8+neoforge.1.21.11.R3-hotfix4`。
- 该提交的 PR 检查：[完整 Gradle build](https://github.com/q14433686-arch/TaCZ_Renovated/actions/runs/40435936947)、[compile-check](https://github.com/q14433686-arch/TaCZ_Renovated/actions/runs/40435936957)、[consistency](https://github.com/q14433686-arch/TaCZ_Renovated/actions/runs/40435936998) 均成功。完整构建的 `Full build` 与 `Upload jars` 步骤均成功；`build.gradle` 的 `check` 依赖包括新增的工作台解析、拾取桩及模拟目标 Mixin 注入测试。本次环境无法读取完整构建日志的下载地址，因此不引用单个任务的详细输出。
- 同一提交的 push 检查中的 [compile-check](https://github.com/q14433686-arch/TaCZ_Renovated/actions/runs/40435911061) 也成功；其 bot 按工作流将 `build-reports/compile-java.log` 回写为 `49ac635005e3cc0073870a9721cb539241987660`，没有修改源码或版本。
- GitHub 将 bot 回写后对应的 PR 构建、编译及一致性工作流标为 `action_required`，不是测试失败。本记录和更新日志是后续的人类文档提交，用于重新触发 PR 检查；本记录不预言其结果。
- 本快照只证明首轮构建与检查，**没有在真实 Minecraft、Carry On 或用户枪包中实机验证**。问题分析和待测矩阵见[工作台崩溃审计](WORKBENCH_PICKUP_CRASH_12111_20261027.md)；版本决定见[版本记录](WORKBENCH_PICKUP_VERSION_12111_20261027.md)。

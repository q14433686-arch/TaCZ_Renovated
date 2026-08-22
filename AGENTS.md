# AGENTS.md — 给 AI 助手的仓库规则

> 本文件供 AI 编码助手在**每次会话开始时**自动读取。人类协作者也适用。
>
> **如果你是 AI：本文件的规则优先于你的默认行为。开始改动前请完整读完。**
> 涉及移植/写代码时，还必须读完 [`CHARTER.md`](CHARTER.md)（工作合同，红线全集）。

---

## 0. 本仓库是什么

**TaCZ: Renovated** —— TaCZ（Timeless & Classics Guns: Zero）的**非官方** NeoForge 移植，
GPL-3.0。当前单分支：Minecraft **26.1.2** + NeoForge 26.1.2.x，Java **25**，未混淆。
26.2 移植筹备中（[`docs/PORT_262_BRIEF.md`](docs/PORT_262_BRIEF.md)）。

- 游戏语义权威：姊妹项目 [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)（Fabric 26.x）。
- **modId 永远是 `tacz`**——枪包依赖检查钉死的。改的是显示名，不是 id
  （命名决策：[`docs/records/NAMING_DECISION.md`](docs/records/NAMING_DECISION.md)）。

## 1. 【强制】改版本号 = 必须同步改 README 与 CHANGELOG

只要改动 `gradle.properties` 的 `mod_version`，**必须在同一改动（或同一分支收尾前）**同步：

1. `README.md` 顶部「本仓库当前源码版本为 **`…`**」行；
2. `README.md` §1 支持环境表的「本 mod」行；
3. `README.md` §5「版本约束」段的构建元数据示例；
4. `CHANGELOG.md` 新版本条目。

**自检命令：**

```bash
bash scripts/check_release_consistency.sh            # 只报告（恒退出 0）
bash scripts/check_release_consistency.sh --strict   # 发布/合并门禁，不一致退出 1
```

`--strict` 返回非 0 时**不得声称任务完成，也不得发布**。

CI 模板位于 [`docs/publish/ci/consistency.yml`](docs/publish/ci/consistency.yml)
（一致性门禁 + 文档链接核验），需由**仓库所有者**复制到
`.github/workflows/consistency.yml`——AI 助手的 token 无 `workflows` 权限，无法代劳
（2026-08-21 推送被拒实证，与姊妹项目 AGENTS 记载一致）。

## 2. 【强制】不得声称未实际实现的东西

写 CHANGELOG、Release notes、README 或对外文案时：

- **区分「绕开/禁用」与「修复/支持」**。本仓库的真实例子：
  - `ARCompat.shouldAccelerate()` 直接 `return false`——这是**禁用**，不是兼容；
  - Zoomify / ImmediatelyFast 是 **no-op**（前者 NeoForge 无此 mod，后者 26.x 无需集成）；
  - LRTactical 是**已撤回**——任何文案不得暗示"支持 LR 内容包"。
- 兼容性、实测结果没有实际验证过就不要写，或明确标注"未实测"
  （`docs/COMPATIBILITY.md` 的口径：✅ = 用户 PASS 或纯逻辑层）。
- 版本号必须以 `gradle.properties` / 实际构建为准，禁止凭记忆写。

## 3. 红线速查（详见 CHARTER.md）

1. **洁净室**：禁止以任何形式接触 CurseForge `tacz-port`（guilhermez1989）的 jar。
2. **API 证据**：任何非平凡 API 要能指认 `类#方法(签名)` + 来源层级（宪章 §3），
   证据写入 `docs/records/`。凭训练数据写 API = 打回。26.1+ 未混淆，没有借口。
3. **参考边界**：游戏语义抄姊妹项目 Fabric 分支（不抄 Fabric API 表面）；
   MUKSC/TACZ-1.21.1 只参考加载器习语，**渲染一行不抄**。
4. **SemVer**：`mod_version` 必须 `1.1.8+neoforge...`（`+` build metadata）。
   **禁止 `-`**（pre-release 会让枪包 `>=1.1.8` 检查静默失败）。
5. **进度不进 README**。历史 = git log + `CHANGELOG.md`；每包证据 = `docs/records/`。

## 4. 文档地图

| 文件 | 性质 |
|---|---|
| `README.md` / `CHANGELOG.md` / `docs/COMPATIBILITY.md` / `docs/DEVELOPMENT.md` | **活文档**，行为变化时同步更新 |
| `AGENTS.md`（本文件）/ `CHARTER.md` | 规则，改动需项目发起人同意 |
| `docs/PORT_262_BRIEF.md` | 二期移植工单（交接文档） |
| `docs/records/` | **冻结审计快照**，完成后不回头改写 |

## 5. 会话结束前的自检

- [ ] 动过 `mod_version` → README 三处 + CHANGELOG 已同步，`--strict` 通过
- [ ] 写过对外文案 → 无未经验证的"修复/支持"声明；禁用没写成兼容
- [ ] 新增非平凡 API 调用 → 证据已入 `docs/records/`
- [ ] 从姊妹项目（Fabric）复制过内容 → 已逐句核对加载器差异、版本号、目录名
- [ ] 没有把进度表写回 README

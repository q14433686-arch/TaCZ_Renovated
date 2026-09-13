# docs/publish/ci/ —— workflow 模板与上线说明

本目录存放 `.github/workflows/` 正式件的同源模板及待上线稿。
**模板文件本身不会运行**；修改后必须同步正式件。按本仓库已有权限分工，普通代码/文档
从工作分支合入，workflow 由具备工作流写权限的维护者在 GitHub 网页端创建或更新。
维护者已于 **2026-09-07** 将 `release.yml` 添加到默认分支与本工作分支，已核对正式件
与模板一致、GitHub Actions 注册为 active；**本轮不需要再次复制**。上轮 App 缺少
`workflows` 权限的情况仍作为历史记录保留，今后若需改工作流仍由有权限的维护者更新。
定义上线、CI 通过与实际发布上传分别记账，当前状态见
[`R3 签收记录`](../../records/R3_CONFIRMATION_2612_20260907.md)。

## 模板清单

| 模板 | 正式件 | 状态 | 用途 |
|---|---|---|---|
| [`consistency.yml`](consistency.yml) | `.github/workflows/consistency.yml` | 已上线 | 版本一致性与文档链接 |
| [`release.yml`](release.yml) | `.github/workflows/release.yml` | **已由维护者上线，与模板一致** | 新 tag 构建 → 发布门禁 → 创建 Release（默认草稿）→ commit/sha256 留痕 |
| [`release-assets.yml`](release-assets.yml) | `.github/workflows/release-assets.yml` | 待上线的旧资产维护方案 | 对已存在的 Release 上传资产与留痕，不负责创建 Release |

`build.yml` / `compile-check-2612.yml` 仍由维护者直接在网页端维护，本次不改。

## 工作流部署与后续维护

本轮正式件已经就位，勿重复创建。以下步骤供首次部署到其他环境或日后更新时参考；
每个新版本（包括 R3-hotfix2）都须合入配套代码、版本文档与验收记录后，才能为最终源码创建发布 tag。

1. **发布 commit 必须包含配套代码/文档**，尤其是
   [`verify_release.py`](../../../scripts/verify_release.py)、
   [`test_release_gate.py`](../../../scripts/tests/test_release_gate.py)、
   [`test_script_globals.py`](../../../scripts/test_script_globals.py) 及其 Lua 断言文件、
   [`RELEASE_NOTES.md`](../RELEASE_NOTES.md)。不要只复制 workflow 而漏掉它调用的脚本。
2. GitHub 仓库页面选择默认分支 **`26.1.2`**；已有正式件时编辑它，首次部署才新建文件。
3. 文件名填 **`.github/workflows/release.yml`**。
4. 把 [`release.yml`](release.yml) **全文**复制进去并保存；不要粘 1.21.11 的原版，
   本版使用 Java 25，并修正了 CLI 参数、tag/版本检查与本线的资产留痕。
5. 在 Actions 中确认出现 `release` 与 **Run workflow**；这一步才叫上线。

GitHub 的 `workflow_dispatch` 要求 workflow **先存在于默认分支**；之后才可用 Run workflow
的分支选择或 `--ref` 选择执行哪一版 workflow。不是“只要任意分支有文件就能触发”。
依据：[GitHub 手动运行工作流文档](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)。

## 新版本首发 / 热修（推荐路径）

1. 按 [`RELEASE.md`](../RELEASE.md) §4 完成本线发布前核对，尤其是 **LAN 双人加入**。
2. 确定新 `mod_version`，同步 README 三处与 CHANGELOG，`--strict` 通过。
   已发布 R3 的实机 PASS 按维护者本轮确认记账；R3-hotfix2 的新增改动仍须单独验收，
   今后也不能跨版本自动继承。
3. 重写 [`RELEASE_NOTES.md`](../RELEASE_NOTES.md)：准确环境、本次变化、本构建验证边界、
   来源与许可；首行 `release-version` 必须是最终完整版本号。R3-hotfix2 已完成正文和版本头的
   初稿，仍不得擅自填入未经确认的专项 PASS。
4. 为**包含上述文件与版本改动的发布 commit** 创建并推送新 tag；此时不要先建 Release。
   tag 格式为 `26.1.2_` + build metadata 尾段，例如 `26.1.2_R3-hotfix2` 或 `26.1.2_R3.1`。
   已发布的 R3 tag 与二进制不得拿来承载尚未发布的修复。
5. Actions → `release` → Run workflow，分支选 **26.1.2**：
   - `tag`：上一步已存在的新 tag；
   - `title`：可留空或自定；
   - `draft`：**默认勾选**，先产生私有草稿；完成复核后再在网页发布。
     只有已经完成本线发布验收时，才取消勾选直接公开。
6. 核对输出 jar 名、正文准确性、资产世代记录中的完整 commit 与 sha256；
   发布后再回填 README 下载链接，其他平台仍需按各平台规则单独上传。

R3-hotfix2 发布 CLI（**先合入并创建对应 tag，再执行；本次合并不自动发布**）：

```bash
gh workflow run release.yml --ref 26.1.2 \
  -f tag=26.1.2_R3-hotfix2 -f title='TaCZ: Renovated — 26.1.2 R3-hotfix2' -f draft=true
```

`--ref` 指 workflow 所在分支；**真正构建的代码来自输入 tag**，不是该分支的最新 HEAD。
工作流的构建、校验、上传全在 GitHub runner，不要求沙箱先下载产物。

### 常见阻断（不要靠绕过门禁解决）

- **Draft / wrong release-version**：若正文还是草稿或引用另一版，先人工复核正文及版本。
- **Tag/version mismatch**：tag、`gradle.properties` 或游戏版本线不一致；别用其他分支的 tag。
- **Release already exists**：包括已有草稿；流程不覆盖它，也没有 `--clobber`。
  若失败仅留下尚未公开、从未交付的空草稿，可核对后删除该空草稿再重试；
  已有公开/交付资产的热修必须使用新版本、新 tag，不能同名替换。
- **Tag moved during the build**：tag 在构建期间被移动，必须查清来源，不能上传当前 jar。
- **L0 artifact gate**：缺 jar、元数据、mixin、AT 或内嵌库时会失败，不以另一个 jar 代替。
- **权限失败**：由仓库维护者检查 workflow 的 `contents: write` 和仓库 Actions 策略；
  不要把凭据写入 workflow、聊天或版本库。

## 旧 release-assets 模板的边界

[`release-assets.yml`](release-assets.yml) 仍保留，供维护者处理**已存在 Release 的资产维护**。
需要使用时同样复制为 `.github/workflows/release-assets.yml` 后才会运行；它不会自动创建
Release。此模板不是本次新版本首发的必装步骤。

- 同名守卫、版本一致性与世代留痕仍须保留；`allow_replace` 仅用于传错文件的当场纠正，
  不能用于已交付版本的热修。
- **新热修改用上述新 tag 路径**，不要把新源码构建挂到旧 tag 上，却声称旧 tag 的
  Source code 归档就是该二进制的完整对应源码。
- 不把旧资产模板的 `ref` 输入与新 `release` 的 tag 锁定流程混为一谈。

## 本地回归与边界

```bash
python3 -m unittest discover -s scripts/tests -p 'test_release_*.py' -v
bash scripts/check_release_consistency.sh --strict
# 最终版本/tag/正文准备好且已构建后：
python3 scripts/verify_release.py --tag 26.1.2_R3-hotfix2 --artifact
```

Python 3.11+ 即可运行发布门禁回归；它使用临时合成 ZIP 检查正常/异常产物结构，
**不构建、不运行 Minecraft**。独立 Lua 回归另需 JDK 25。二者都不取代实机或最终 tag 的
完整 Gradle build / L0 验收。R2 的 LAN 与同名资产教训见
[`R2 发布复盘`](../../records/R2_RELEASE_RETRO_20260903.md)。

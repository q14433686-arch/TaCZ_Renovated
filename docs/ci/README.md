# CI 配方暂存区

沙箱里的 GitHub App **没有 `workflows` 权限**，直接推 `.github/workflows/*` 会被
remote 拒（`refusing to allow a GitHub App to create or update workflow … without
`workflows` permission`）。所以新配方与配方改动一律**先落在本目录**，由项目成员在
网页端（或本地有权限的仓库）移入 `.github/workflows/` 后生效。

本目录的文件是**待上线稿**，不是 `.github/workflows/` 的镜像 —— 两者不一致时以
`.github/workflows/` 为「已生效」，以本目录为「期望状态」。

## 上线状态表

| 配方 | 本目录 | `.github/workflows/` | 状态 |
|---|---|---|---|
| `compile-check.yml` | 已更新 | 旧版 | **待上线**：本次给 `paths-ignore` 加了 `docs/**` 与 `**/*.md` |
| `changelog.yml` | 已新增 | **不存在** | **待上线**：新建，收尾用 CHANGELOG 草稿生成器 |

## compile-check.yml 待上线的改动

```yaml
    # 防止“日志回推 commit”再次触发本流程造成死循环
    # docs/** 与 *.md 不会改变编译结果，跳过（文档改动由 consistency 流程守着：
    # 它跑版本号门禁 + 全仓 markdown 相对链接核验）
    paths-ignore:
      - 'build-reports/**'
      - 'docs/**'
      - '**/*.md'
```

**未上线前的后果**：纯文档提交仍会触发一轮 2 分钟左右的编译验证，并回推一条
`ci-log` 提交 —— 不影响正确性，只是白跑。

## changelog.yml 上线后怎么用

1. Actions → `changelog` → `Run workflow`；
2. `since_ref` 留空即取「与主干 26.2 的分叉点」，也可填某个 tag / commit；
3. `version` 填版本串（例如 `1.1.8+neoforge.26.2.R2`）；
4. 默认**只出 artifact**，下载后人工核对；勾 `commit_back` 才回推到 `docs/records/`。

本地不跑 Actions 时等价命令：

```bash
bash scripts/generate_changelog.sh            # 默认起点
bash scripts/generate_changelog.sh d0c69a8 --version 1.1.8+neoforge.26.2.R2
```

脚本只按提交前缀归类，**不判断机制描述是否准确，也不含实机验证状态** ——
并入 `CHANGELOG.md` 前必须逐条人工补。

# docs/publish/ci/ —— workflow 模板目录（正式件同源镜像）

> 本目录存放 `.github/workflows/` 正式件的**同源模板**。维护纪律（AGENTS.md §1 已有先例）：
> **改动先改这里，再镜像到正式件**；正式件由维护者在 GitHub 网页端上线/更新
> （沙箱 Agent 的 token 无 `workflows` 权限，推不动 `.github/workflows/` 下的文件）。

## 模板清单

| 模板 | 正式件 | 状态 | 用途 |
|---|---|---|---|
| [`consistency.yml`](consistency.yml) | `.github/workflows/consistency.yml` | 已上线 | 版本号一致性门禁 + 文档链接核验 |
| [`release-assets.yml`](release-assets.yml) | `.github/workflows/release-assets.yml` | **待上线** | Release 资产：构建 → 校验 → 上传 → 世代留痕 |

（`build.yml` / `compile-check-2612.yml` 由维护者直接在网页端迭代，本目录暂无镜像；
如需改动建议也先落模板再镜像，收敛到同一套纪律。）

## release-assets.yml 上线教程（一次性，约 2 分钟）

1. GitHub 网页 → 仓库 → 切到 **`26.1.2` 分支** → `Add file` → `Create new file`；
2. 文件名填 `.github/workflows/release-assets.yml`；
3. 把 [`release-assets.yml`](release-assets.yml) **全文**（含注释头）粘贴进去，commit 到 `26.1.2`；
4. 其它两线各自适配后同样上线（适配要点见模板注释头：26.2 改 `ref` 默认值；
   1.21.11 改 `ref` 默认值 + `java-version: '21'`；两线均需核对
   `scripts/check_release_consistency.sh` 是否存在）。

> workflow_dispatch 只认「文件所在分支」：上线到哪条分支，Run workflow 时就选哪条分支。

## 使用教程

### 场景 A：新版本首发（如 R3）

1. 按 [`../RELEASE.md`](../RELEASE.md) §4 走完发布前核对（版本号、CHANGELOG、`--strict`）；
2. GitHub 网页建 tag + Release（正文按 RELEASE.md §3 模板写），**不要手动传 jar**；
3. Actions → `release-assets` → Run workflow（branch 选 `26.1.2`）：
   - `tag` = 刚建的 tag（如 `26.1.2_R3`）；
   - `ref` = 默认 `26.1.2`（或钉死某个 commit SHA）；
   - `allow_replace` = 不勾；
4. 跑完后到 Release 页核对：资产已挂上，正文末尾多了「资产世代记录」一行
   （UTC 时间 / 构建 commit / 文件名 / sha256）。

### 场景 B：发布后热修（R2 那晚的正确姿势）

1. 修复合入发布分支后，**先 bump build metadata**：`gradle.properties` 的
   `mod_version` 尾段 `...R2` → `...R2.1`（`+` 之后是 SemVer build metadata，
   枪包 `>=1.1.8` 检查不受影响；**禁止用 `-`**）；
2. 按 AGENTS.md §1 同步 README 三处 + CHANGELOG，`--strict` 必须过（CI 会再拦一次）；
3. Actions → `release-assets` → Run workflow：`tag` 填**原 Release 的 tag**，
   `allow_replace` 不勾——新文件名（`tacz-...R2.1.jar`）不与旧资产冲突，直接并存；
4. 旧世代资产可留可删：留着则玩家侧新旧可辨（文件名即世代），删掉则页面干净，
   世代记录表里都有案可查；
5. Release 正文顶部手动补一句「R2.1 修复了 XXX，请下载新文件」。

### 纪律（从 R2 复盘固化下来的三条）

1. **禁止同名换弹**：同一个文件名只允许对应一个二进制。workflow 的同名守卫会拦，
   `allow_replace=true` 仅限「传错文件当场重传」这一种情况；
2. **每个上线二进制必须可溯源**：世代记录表（commit + sha256）由 workflow 自动维护，
   不要手工编辑该表；
3. **发布前冒烟必须含 LAN 双人加入**：R2 的断连 bug（`neoforge:recipe_content`
   编码空 ItemStack 爆炸）只在第二名玩家加入时触发，单机与渲染向实测零痕迹。
   复盘全文见 [`../../records/R2_RELEASE_RETRO_20260903.md`](../../records/R2_RELEASE_RETRO_20260903.md)。

# AGENTS.md — 给 AI 助手的仓库规则

> 本文件供 AI 编码助手在每次会话开始时读取。涉及移植或写代码时，还必须完整阅读
> [`CHARTER.md`](CHARTER.md)。两者与用户下发的 26.2 工单共同构成工作合同。

## 0. 本仓库是什么

**TaCZ: Renovated 26.2 candidate**：从 NeoForge 26.1.2 R1 前滚到
Minecraft **26.2** + NeoForge **26.2.0.64** + Java **25** 的非官方 GPL-3.0 移植。

- modId 永远是 `tacz`；枪包依赖已绑定该 id。
- 当前版本：`1.1.8+neoforge.26.2.0.R1`，仍为 Unreleased candidate。
- 代码谱系基线：26.1.2 R1 分支
  `arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0`，尖端
  `6020a5cf1dd02c356f797557f6323b0d430b75e1`；该稳定基线包含多人修复与
  LRTactical 内置层，不能再把 Beta-1 `4d2edc1` 或旧 `b9de5e0` 当作完整功能基线。
- 26.1.2 的 LR 单机/专服 PASS 不自动继承到 26.2；当前 LR-integrated HEAD 必须重跑
  build、L0-L3、L2.5 与 LR 专项。
- 游戏语义权威：`TaCZ_Refabricated_Unofficial` 的 `26.2(main)`；只取游戏语义，
  不复制 Fabric API 表面。

## 1. 版本一致性门禁

修改 `gradle.properties` 的 `mod_version` 时，必须在同一分支收尾前同步：

1. `README.md` 顶部当前源码版本；
2. `CHANGELOG.md` 当前条目；
3. 任何 active release/status 文档中的精确版本串。

运行：

```bash
bash scripts/check_release_consistency.sh
bash scripts/check_release_consistency.sh --strict
```

`--strict` 非 0 时不得声称完成、合并或发布。版本必须使用 `+` build metadata；禁止
`1.1.8-neoforge...` 形式的 pre-release。

## 2. 不得声称未验证的内容

- 绕开、禁用、fallback、no-op 不得写成修复或支持。
- 编译 PASS 不等于专服、GPU、可选 Mod 或多人联机 PASS。
- `COMPATIBILITY.md` 中只有用户实际完成的行才能写 PASS；其余明确“未实测”。
- 当前 scope-mask、Vulkan、Iris 和低倍准星修复均以对应状态文档为准，不从旧 commit
  的 PASS 外推到新 HEAD。

## 3. 红线速查

1. **洁净室**：禁止下载、接触、反编译或参考 CurseForge `tacz-port` jar。
2. **API 证据**：非平凡 API 必须指认类、方法、descriptor 和来源，写入 evidence/records。
3. **参考边界**：refab 只取游戏语义；MUKSC 渲染一行不抄。
4. **双端安全**：26.1+ 不把 `@OnlyIn(Dist.CLIENT)` 当类加载保护。覆写 vanilla 双端方法时，
   方法体不得引用 client-only 类。审计：
   ```bash
   grep -rn "TimelessAPI.getClient\|ClientIndexManager" src/main/java --include="*.java"
   ```
5. **网络 ItemStack**：字段可能为 EMPTY 时必须使用 optional codec；不能把同目录其他
   必为非空的消息机械改掉。
6. **多人门禁**：当前 HEAD 必须执行 `docs/DEDICATED_SERVER_TEST.md` 的 L0-L2、L2.5
   与 L3；单机正常不等于完成。
7. **进度不进 README**：工作包与证据放 `docs/`；README 只写用户需要的当前边界。

## 4. 文档地图

| 文件 | 性质 |
|---|---|
| `README.md` / `CHANGELOG.md` / `COMPATIBILITY.md` | 用户入口、发行说明、兼容状态 |
| `docs/README.md` | 文档索引与类型说明 |
| `docs/DEVELOPMENT.md` | 构建、运行、权威边界与开发纪律 |
| `docs/PORTING_STATUS.md` | 当前候选状态与发布闸门 |
| `docs/DEDICATED_SERVER_TEST.md` / `docs/GUNPACKS.md` | L0-L4 验收与枪包指南 |
| `docs/RELEASE_CHECKLIST.md` | R1 发布前逐项门禁 |
| `docs/WP262_*_EVIDENCE.md` | 26.2 工作包证据 |
| `docs/records/` | 冻结审计快照及 26.1.2 R1 回流证据 |
| `CHARTER.md` / 本文件 | 规则 |

## 5. 会话结束前自检

- [ ] 版本信息一致，必要时 `--strict` 通过
- [ ] 没有把未执行项写成 PASS
- [ ] 新 API 有证据；从 refab 复制的内容已去除 Fabric 表面
- [ ] 双端方法与 EMPTY ItemStack 已审计
- [ ] 当前分支仍是固定 Arena 分支，未切换或创建其他分支

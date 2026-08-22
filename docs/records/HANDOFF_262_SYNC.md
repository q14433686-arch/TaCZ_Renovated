# 给 26.2 前滚 AGENT 的同步说明（复制本文全文给他即可）

> 更新：2026-08-22。**基线 = R1（含 LRTactical 内置层，已通过
> 单机 + 专用服务器双重验收）。** 本分支可直接读取，不必等 PR 合并。

## 0. 你要同步的源

```
仓库：q14433686-arch/TaCZ-Renovated
分支：arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0
基线：R1 = 分支最新提交（mod_version = 1.1.8+neoforge.26.1.2.R1，含 LR）
```

```bash
git fetch origin arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0
git merge FETCH_HEAD        # 或 rebase；R1 收版后不再有开发中提交混入
```

之前钉在 `b9de5e0` 的指示**作废**：LR-dev 开发期已结束，分支尖端即稳定基线 R1。
若你已基于 b9de5e0 做了大量工作，merge 会带入的增量 = LR 内置层（`me/xjqsh/**` 全树 +
四处 tacz 接线 + 资源）+ R1 后的文档，无其它代码变动。

## 1. R1 基线相对提交 b9de5e0 的增量（你的 26.2 前滚范围随之扩大）

- `me.xjqsh.lrtactical.*`（约 105 java）：LR 战术装备框架，NeoForge 习语
  已全量改写（DeferredRegister / 事件面 / PayloadRegistrar `lr1` 通道）。
- tacz 侧接线四处：`GunMod` 构造器、`GunModClient` enqueueWork、
  mods.toml `[[mixins]]`（lrtactical.mixins.json）、AT（`Player#canCriticalAttack`）。
- 资源：`assets/lrtactical/**`（items json / particles json / lang / scripts /
  display）、`data/lrtactical/**`。

## 2. LR 层的 26.2 前滚专项注意（除 PORT_262_BRIEF 全部条目外新增）

1. **`SimpleParticleType` 构造器 26.2 变 protected**（WP07 B-9）——
   `me.xjqsh.lrtactical.init.ModParticleTypes` 直接 `new`，26.2 上必须改
   工厂/匿名子类（该文件 javadoc 已预埋提示）。
2. LR 的 mixin 两个（`GuiGraphicsExtractorMixin` 冷却遮罩、`SoundEngineMixin`
   耳鸣压音量）目标是 vanilla 类——26.2 的 Gui/Hud 重组（PORT_262_BRIEF §4-G）
   与音频系统变动需逐一重验注入点。
3. LR 反馈层（tooltip/HUD/粒子）跑在 GuiGraphicsExtractor 与 Feature Rendering
   体系上——26.2 渲染大改（§4-B/D/F）会波及，前滚时与 tacz 渲染层同批处理。
4. LR 网络消息三条无 ItemStack 字段（EMPTY 纪律天然过）；
   `ServerMessageSyncLrPack` 的 readMap/writeMap 已是显式 lambda（B-8）。

## 3. 三条经验（不变，前滚时会反复踩）

1. **26.1+ 上 `@OnlyIn(Dist.CLIENT)` 只是文档不是保护**——凡覆写 vanilla 双端
   方法，一律按无注解审查方法体内的 client 类引用。
2. **网络消息里的 ItemStack 字段先问一句：会不会是 EMPTY？** 会 → OPTIONAL codec。
3. **单机跑通 ≠ 完成**——验收必须包含 `docs/DEDICATED_SERVER_TEST.md` 的
   L0-L2 + L2.5 + L3；R1 的 LR 层就是按这套完成专服验收的。

## 4. 必读文档（都在本分支）

- `docs/PORT_262_BRIEF.md` —— 你的工单（差异映射、权威边界、WP-262 切分）
- `docs/WP_LR2_BRIEF.md` + `docs/records/LR2_INVENTORY.md` —— LR 层的完整
  实现台账（前滚 LR 时照着改动清单走）
- `AGENTS.md` —— 会话规则（版本一致性门禁、不得声称未实现）
- `CHANGELOG.md` R1 条目 —— 修复与新增全景

## 5. 版本号红线

起步 `1.1.8+neoforge.26.2.0.r0`，基于 **R1** 代码；`+` 后是 build metadata，
**禁止 `-`**。改 `gradle.properties` 后跑
`bash scripts/check_release_consistency.sh --strict`。

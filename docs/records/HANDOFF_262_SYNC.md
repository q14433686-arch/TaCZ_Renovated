# 给 26.2 前滚 AGENT 的同步说明

> 更新：2026-08-22。**基线 = 26.1.2 R1（含 LRTactical 内置层，来源分支已通过
> 单机 + 专用服务器双重验收）。** 本分支可直接读取，不必等 PR 合并。
>
> 本文是 handoff 快照；26.2 的实际执行结果与待测边界见
> `LR_R1_SYNC_26_2_20260822.md` 和 `../PORTING_STATUS.md`。

## 0. 同步源

```text
仓库：q14433686-arch/TaCZ-Renovated
分支：arena/01a023bf-tacz-1-1-8-neoforge-26-1-2-r0
提交：6020a5cf1dd02c356f797557f6323b0d430b75e1
版本：1.1.8+neoforge.26.1.2.R1
范围：多人修复 + 更名 + LRTactical 内置层
```

此前钉在 `b9de5e0` 的指示作废：LR-dev 已结束，来源分支尖端才是稳定 R1 基线。
相对 `b9de5e0` 的代码增量是 LR 内置层（`me/xjqsh/**`、四处 tacz 接线与资源）。

## 1. LRTactical 增量

- `me.xjqsh.lrtactical.*`：throwable / melee / detonator / consumable，五类投掷行为，
  DeferredRegister、事件面与独立 `lr1` payload；
- tacz 接线：`GunMod`、`GunModClient`、`neoforge.mods.toml` mixin 声明、
  `Player#canCriticalAttack` AT；
- 资源：`assets/lrtactical/**` 与 `data/lrtactical/**`；
- 明确排除：flash_shield 与原作 ARR 美术。

## 2. 26.2 前滚专项

1. `SimpleParticleType#<init>(Z)V` 在 vanilla 26.2 为 protected：使用工厂或匿名子类；
2. `GuiGraphicsExtractorMixin` 冷却遮罩与 `SoundEngineMixin` 耳鸣压音量必须逐个重验目标；
3. tooltip/HUD/粒子与动态物品/实体渲染必须随 26.2 Gui/Feature Rendering 同批验证；
4. 三条 LR 网络消息没有 `ItemStack` 字段；`ServerMessageSyncLrPack` 保持显式
   `readMap` / `writeMap` lambda；
5. 26.1.2 LR PASS 不自动继承为 26.2 PASS。

具体 descriptor 与来源见 `LR_R1_SYNC_26_2_20260822.md`。

## 3. 三条经验

1. 26.1+ 的 `@OnlyIn(Dist.CLIENT)` 不是 dedicated 类加载保护；
2. 网络 `ItemStack` 先判断是否可能 EMPTY，可能则使用 optional codec；
3. 单机跑通不等于完成，必须执行 L0-L3、L2.5 与 LR 专服专项。

## 4. 必读文档

- 来源分支 `docs/PORT_262_BRIEF.md`；
- 来源分支 `docs/WP_LR2_BRIEF.md` 与 `docs/records/LR2_INVENTORY.md`；
- 本分支 `AGENTS.md` / `CHARTER.md`；
- 本分支 `docs/records/LR_R1_SYNC_26_2_20260822.md`；
- 本分支 `docs/DEDICATED_SERVER_TEST.md`。

## 5. 版本号红线

本分支已由项目发起人定名为 `1.1.8+neoforge.26.2.0.R1`，不得因来源 handoff 中旧的
`r0` 起步示例而回退。`+` 后是 build metadata，禁止改成 `-` pre-release。

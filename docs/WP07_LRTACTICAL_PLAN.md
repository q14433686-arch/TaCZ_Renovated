# 工作包⑦（立项）：LRTactical 兼容框架内置（形态 B）

> 2026-08-21 商讨定案。本文件是决策记录 + 实施计划，动工前按此执行。

## 决策

**形态 B：LR 兼容框架内置进 tacz 主 mod**（与 refab 26.1.2 一致），不另立独立 mod。

理由：
- refab（语义权威）即为内置形态；其 LR 框架与 tacz 深度共生（引用 tacz 内部类
  `BlockTransformParser`/`BedrockPart`/`AnimateGeoItemRenderer`/`JsonDataManager` 等），
  独立成 mod 需先在 tacz 开插件化 SPI，工程量大且收益低；
- NeoForge 无 Fabric 的 `provides` 字段，依赖冒充本就依赖 tacz 枪包加载器的软特判（已就位），
  独立 mod 并不能带来额外机制收益。

## 许可与红线（商讨确认）

- LR **代码公开（GPL-3.0）**，经 refab 谱系可审计 → 允许移入，沿用 GPL-3.0；
- LR **美术资源 ARR** → **绝不打包**任何模型/贴图/动画/音效；refab 内置的 31 个 lrtactical
  资源文件已核：26 json + 5 lua，**零美术文件**，内容包/玩家自带美术；
- `flash_shield` 不移植、不注册空壳物品（对齐 refab）；
- 命名与免责：文档注明"非官方 LR 兼容框架，部分兼容，问题归本仓库"。

## 范围（对照 refab 26.1.2 实测）

| 项 | 规模 | 说明 |
|---|---|---|
| Java | **150 个文件**（`me.xjqsh.lrtactical.*` 完整包，原作者包名） | init 9 / util 7 / entity 7 / melee 6 / collision 6 / network 5 / item 5 / tooltip 5 / 渲染 5 / api 5 / resource 4 / throwable 8+ … |
| 资源 | 31 文件（26 json + 5 lua） | display/blocks、scripts、lang、recipe_filters |
| 已就位 | `GunPackLoader` 软 provides（`lrtactical` 按 0.3.0 放行） | NeoForge 侧依赖检查等价物 |
| 已就位 | `GunSmithTableResultSerializer` 惰性 custom items（LR 物品兼容注释已在） | — |

## 实施计划（分四阶段）

1. **阶段一·骨架**：搬入 `me.xjqsh.lrtactical` 包树 + 31 资源；NeoForge 化入口
   （Fabric 入口 → `@Mod`/`@EventBusSubscriber` 事件订阅、`DeferredRegister` 注册、
   Fabric payload → NeoForge `CustomPacketPayload` 网络）。
2. **阶段二·数据面**：resource/manager 四个加载器接进 `ClientAssetsManager`/`CommonAssetsManager`
   的 reload 管线；LR 命名空间资源随主 mod 资源栈加载。
3. **阶段三·行为面**：entity（ThrowableItemEntity 等）、item（Consumable/Detonator/Melee）、
   tooltip、渲染器逐块通电；逐类对照 26.1.2 API（沿用宪章 §3 证据规则，记录 WP07 证据表）。
4. **阶段四·验收**：装 LR 内容包（自带美术）实测投掷物/消耗品/引爆器/近战五类投掷物；
   更新 `PORTING_STATUS.md` 与 `WP07_EVIDENCE.md`。

## 明确不做

- flash_shield；
- 打包/再分发 LR 美术；
- 独立 LR mod（除非未来 tacz 出插件化 SPI 后重议）。

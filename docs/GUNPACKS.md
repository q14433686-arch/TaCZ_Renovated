# 26.2 枪包安装与兼容指南

适用版本：`1.1.8+neoforge.26.2.0.R1`。

## 双端职责

| 位置 | 目录 | 负责内容 |
|---|---|---|
| 服务端 | `<server>/tacz/` | common index、枪械属性、配方、filter、attachment tags 与逻辑权威 |
| 客户端 | `<instance>/tacz/` | 模型、贴图、声音、语言与其他显示资产 |

正常多人使用必须双端安装同一枪包。枪包根目录或 zip 根必须含 `gunpack.meta.json`。

## 重载

- 服务端数据与在线客户端 common 同步：由 OP 执行 `/tacz reload`。
- 客户端运行时新增本地显示包：按 **F3+T**。
- `/tacz reload` 不替代客户端资源重载；F3+T 也不让服务端发现只存在客户端的逻辑数据。

## 不对称安装的预期

- **只装客户端**：服务端不认识包内 id；通常没有逻辑效果。
- **只装服务端**：逻辑数据可同步，但客户端缺模型/语言时可能显示紫黑资源或原始翻译键；
  底线是不崩溃、不踢出。

这些是 26.1.2 R1 的已知基线；26.2 R1 仍须按 L2.5 单独确认。

## 版本谓词

推荐：

```json
{
  "dependencies": {
    "tacz": ">=1.1.8"
  }
}
```

本 Mod 的 `1.1.8+neoforge.26.2.0.R1` 中，`+` 后是 build metadata，不影响
`>=1.1.8`。不要把依赖写成只接受某个开发分支名，也不要假设 `-neoforge` 与 `+neoforge`
排序相同。

## LRTactical 边界

本仓库没有内置 LRTactical 的 throwable、melee、detonator、consumable 基础物品与行为。
依赖 LRTactical 的枪包只能期待普通枪械部分；LR 专属道具不可用。发现包不等于完整支持。

## 故障判读

| 现象 | 优先检查 |
|---|---|
| 加入即断线，日志含 `Empty ItemStack not allowed` | 双端是否使用含 OPTIONAL Draw codec 修复的当前版本 |
| `BLOCK_INDEX` / `RECIPE_FILTER` 解析失败 | 服务端是否同步 filter/tag cache；双端版本是否一致 |
| `/give` 导致专服加载 client 类 | 是否使用 common-index `getName` 修复版本 |
| 有伤害但紫黑模型/无语言 | 客户端缺同一枪包显示资产 |
| 客户端新增包不生效 | F3+T；服务端新增包另执行 `/tacz reload` |
| `tacz >= 1.1.8` 被拒绝 | 检查本 Mod 是否误用 `-` pre-release 版本号 |

## 验收

完整 L2.5 清单见 [`DEDICATED_SERVER_TEST.md`](DEDICATED_SERVER_TEST.md)：

- 默认包与至少一个第三方包；
- 双端正常安装；
- 客户端-only / 服务端-only；
- 在线 `/tacz reload`；
- 客户端 F3+T；
- `/give`、射击、工作台合成和版本谓词。

# WP-262-4 证据：26.2 可选兼容与坐标重钉

日期：2026-08-21

## 公开发布文件核验

| 项目 | 26.2 NeoForge 结论 | 构建 pin |
|---|---|---|
| Cloth Config | 有 release：26.2.155 | `me.shedaniel.cloth:cloth-config-neoforge:26.2.155` |
| PAL | 有 merged Fabric+NeoForge release：1.2.6+26.2 | Curse `8674798` |
| Controllable | 有 NeoForge beta：0.26.1 | Curse `8403602` |
| Shoulder Surfing | 有 NeoForge release：5.0.7 | Curse `8445037` |
| JEI | 有 NeoForge beta：30.24.0.176（2026-08-19） | `mezz.jei:jei-26.2-neoforge:30.24.0.176` |
| REI | 有 NeoForge beta：26.2.820 | Curse `8271756` |
| Architectury | REI source pin：21.0.2 | `dev.architectury:architectury-neoforge:21.0.2` |
| Iris | 有 NeoForge release：1.11.2 | 反射兼容，无 compile dependency |
| Carry On | 有 NeoForge release：2.11.0 | optional mixin/反射，无 compile dependency |
| First-person Model | **无 NeoForge 26.2**；2.7.2 只有 Fabric 26.2 | 不加依赖，反射桥 dormant |
| Not Enough Animations | **无 NeoForge 26.2**；1.12.4 NeoForge 止于 26.1.2 | 不加依赖，反射桥 dormant |

公开页面：

- <https://www.curseforge.com/minecraft/mc-mods/cloth-config/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/player-animation-library/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/controllable/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/shoulder-surfing-reloaded/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/jei>
- <https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/iris-shaders>
- <https://www.curseforge.com/minecraft/mc-mods/carry-on/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/first-person-model/files/all>
- <https://www.curseforge.com/minecraft/mc-mods/not-enough-animations/files/all>

未下载、未接触 `tacz-port` jar。

## API 证据

完整 commit/descriptor 表写入根目录 `COMPATIBILITY.md`。本包直接核对的公开源码：

- PAL `v1.2.6+26.2`：`7d2a480808962608018ea77b23fdebe9baaa3ea8`；
- Controllable `v0.26.1+26.2`：`7333428d29464db914750eac2a039c22102e3e65`；
- Shoulder Surfing `26.2-5.0.7`：`ab65e01733dbe1ae70fba90bc2744c1682018539`；
- JEI 30.24 source line：`886b3644c62f4c18ffa22a23a0de0e1130e2f507`；
- REI 26.2：`2be20928abd9f1164fd9fd251268041c036b580f`；
- Iris 26.2：`8f3a7a35d780fe80c8cd3c8517f3fa3c4df3f18a`；
- Carry On `v2.11.0`：`b82a8ccfe8b4a9af98b7485826c2162e8faaae81`；
- First-person Model `2.7.2`：`eef8f91206c9f0ad1681111235c0d802349f986a`；
- Not Enough Animations `1.12.4`：`dd7e5e191839de8044b8bc942304e2b1ead7950f`。

### 代码调整

1. Gradle 的 7 个 26.1 compile pin 全部换成 26.2 loader-correct artifact。
2. Controllable 0.26.1 的 binding/context/handler/controller/rumble 描述符与本仓调用一致。
3. PAL 1.2.6 的 factory/controller/fade/adjustment/loader 描述符与本仓调用一致。
4. Shoulder Surfing 5.0.7 的 v5 event/plugin API 与
   `shouldersurfing_plugin.json` NeoForge 扫描机制一致。
5. Carry On 2.11 的 `getRenderItemStack` 返回不可变 `ItemStackTemplate`；修复点移到
   `CarriedObjectRender#drawBlock` 中紧邻 `.create()` 的 redirect，避免把 mutable stack
   错写回 template。
6. First-person bridge 从空壳补成 FPM ActivationHandler + NEA direct-arm guard；使用
   NeoForge `ModList`，无硬依赖。因两者无 NeoForge 26.2 文件，当前不会被列为可安装 PASS。
7. `RenderHelper` 的直接 AvatarRenderer 左/右手提交用 try/finally 包住 NEA guard。
8. REI `reloadPlugins(MutableLong,ReloadStage)` 两参入口、JEI NeoForge
   `RecipesReceivedEvent` 启动顺序已按 26.2 source 重验。

## 静态验证边界

- 外部 API import 对 source tree 检查：JEI 34 个 import 全存在；REI API import 全存在，
  `me.shedaniel.math.Point/Rectangle` 来自其 Cloth/REI 依赖。
- Carry On 三个 mixin target 与 2.11 source 方法名/参数逐项相符。
- Java 语法与 JSON/mixin 配置检查会在提交前执行。

本沙盒仍不能执行生产 JDK 25 Gradle build，也没有安装这些 Mod 启动客户端。因此这里不把
任何一行标成用户 PASS。运行矩阵见 `COMPATIBILITY.md`。

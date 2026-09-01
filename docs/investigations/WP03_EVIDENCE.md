# 工作包③ 证据清单

骨架：MUKSC `NetworkHandler` + `PayloadRegistrar` + `PacketDistributor`。
内容语义：Fabric 26.1.2 字段布局；ID 用 `Identifier.fromNamespaceAndPath`。

## 加载器 API（26.1.2.97 sources）

| 调用 | 证据 |
|---|---|
| `RegisterPayloadHandlersEvent` 实现 `IModBusEvent` | ⑤ `RegisterPayloadHandlersEvent.java` |
| `event.registrar(String version)` → `PayloadRegistrar` | ⑤ 同上 |
| `PayloadRegistrar#playToServer(Type, StreamCodec, IPayloadHandler)` | ⑤ `PayloadRegistrar.java` |
| `PayloadRegistrar#playToClient(...)` | ⑤ 同上 |
| `PayloadRegistrar#configurationToServer/Client` + `executesOn(HandlerThread.NETWORK)` | ⑤ 同上 |
| `IPayloadContext#enqueueWork(Runnable)` / `#player()` / `#reply` / `#finishCurrentTask` | ⑤ `IPayloadContext.java` |
| `PacketDistributor.sendToPlayer/TrackingEntityAndSelf/AllPlayers/InDimension` | ⑤ `PacketDistributor.java` |
| `RegisterConfigurationTasksEvent#register` + `ICustomConfigurationTask` | ⑤ 同包 |
| `CustomPacketPayload.Type(Identifier)` | ① 26.1.2 |
| `StreamCodec.composite` / `ByteBufCodecs.VAR_LONG|FLOAT|INT` / `Identifier.STREAM_CODEC` | ① |
| `FriendlyByteBuf#readIdentifier/writeIdentifier` | ①（handshake） |
| `IGunOperator.fromLivingEntity` 在 mixin 前返回 NoOp | 避免 C2S 在 ④ 之前 ClassCast |

## 专用服务端约束

26.1 Dist cleaner **不再**按 `@OnlyIn` 剥成员，但 **dedicated server classpath 没有 `LocalPlayer`/`Minecraft`**。
S2C 处理若写在与 codec 同一 class，注册 `::handle` 会在服务端 `NoClassDefFoundError: LocalPlayer`。
因此 S2C `handle` 目前是空 `enqueueWork`；C2S shoot/reload 仍 `sendToTrackingEntityAndSelf` 发出 `ServerMessageGunFire/Shoot/Reload`。
客户端真正 `NeoForge.EVENT_BUS.post` 放到 ④（`IGunOperator` mixin + client handler 隔离类）。

## 冒烟（dedicated server）

```
TaCZ NeoForge 26.1.2 port work package ③ loading. modId=tacz
WP③ payloads registered (play + configuration), version=1.0.5
Done (0.424s)! For help, type "help"
```

未接触 `tacz-port` jar。

# 工作包① 冒烟记录

目标：能进主菜单，mod 列表里可见。

本沙盒无显示器 / GPU，无法 `runClient` 开主菜单。用 **同一套 FML 元数据** 在 `runServer` 上验证：

- FML 发现 `tacz`
- Mod List 打印显示名 + 版本 + modId
- `@Mod` 构造器跑通
- 专用服务器 `Done`

客户端 Mods 屏幕读的就是 `neoforge.mods.toml` 里同一条 `[[mods]]`。

## 环境

- Minecraft 26.1.2 + NeoForge 26.1.2.97
- JDK 25.0.4 Temurin
- `./gradlew runServer --no-daemon`（run JVM `-Xms256M -Xmx768M`）
- 日志：`run/logs/latest.log`

## 摘录

```
Got mod coordinates tacz%%/home/user/build/classes/java/main:tacz%%/home/user/build/resources/main from env
     Mod List:
		Timeless and Classics Zero 1.1.8+neoforge.26.1.2.r0 (tacz)
 - tacz (composite(folder(.../build/classes/java/main), folder(.../build/resources/main)))
Creating FMLModContainer instance for tacz with entrypoints [com.tacz.guns.GunMod]
[com.tacz.guns.GunMod/]: TaCZ NeoForge 26.1.2 port skeleton loaded (work package ①, common). modId=tacz
NeoForge mod loading, version 26.1.2.97, for MC 26.1.2
Starting minecraft server version 26.1.2
Done (13.801s)! For help, type "help"
```

物理客户端入口 `GunModClient`（`dist = Dist.CLIENT`）未出现在专用服务器 entrypoints，符合预期。

## 产物

`build/libs/tacz-1.1.8+neoforge.26.1.2.r0.jar`（约 5 KB，空骨架，无枪包资源）。

#!/usr/bin/env python3
"""静态守卫：配件内联脚本「加载期编译、运行期受限执行」的接线。

1.2.0 把 `Modifier#function` 从「每次求值都 ScriptEngine.eval」改成「加载期编译一次」。
这条链有三处会静默失效，编译期一个都查不出来：

    Gson(type adapter factory)  ──► Modifier#compileFunction
        └─► Modifier#getCompiledFunction ──► ParameterizedCache / AttachmentPropertyManager
                                             （若这里又回到拿 String 求值，
                                               功能照常工作，只是退化成 1.1.8 的慢路径）

本脚本只做源码文本核对；脚本语义与沙箱由 `attachmentScriptTest` 实际执行。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/tacz/guns"

FAILURES: list[str] = []
CHECKS = 0


def read(rel: str) -> str:
    return (SRC / rel).read_text(encoding="utf-8")


def check(condition: bool, message: str) -> None:
    global CHECKS
    CHECKS += 1
    if not condition:
        FAILURES.append(message)


def main() -> int:
    manager = read("resource/manager/AttachmentScriptManager.java")
    modifier = read("resource/pojo/data/attachment/Modifier.java")
    factory = read("resource/serialize/ModifierTypeAdapterFactory.java")
    assets = read("resource/CommonAssetsManager.java")
    props = read("resource/modifier/AttachmentPropertyManager.java")
    cache = read("api/modifier/ParameterizedCache.java")

    # --- 1. 沙箱：只装白名单库，危险入口必须显式摘掉。
    for lib in ("BaseLib", "Bit32Lib", "TableLib", "StringLib", "MathLib"):
        check(f"new {lib}()" in manager, f"沙箱应装载 {lib}（白名单）")
    for banned in ("JseIoLib", "JseOsLib", "LuajavaLib", "PackageLib", "DebugLib", "JseBaseLib"):
        check(banned not in manager, f"沙箱不允许装载 {banned}")
    for name in ("load", "loadfile", "dofile", "collectgarbage", "print", "getmetatable"):
        check('"' + name + '"' in manager, f"沙箱应把 '{name}' 从 globals 摘掉")
    check("LuaC.install(globals)" in manager, "缺少 LuaC.install（否则 compilePrototype 不可用）")
    check('globals.set("package", packageShim)' in manager,
          "装载库之前必须先垫一个 package 垫片：luaj 3.0.x 的 table/string/bit32/math 库"
          "装载时会写 package.loaded，没有 package 直接抛 LuaError")
    check('globals.set("package", LuaValue.NIL)' in manager,
          "垫片只能在装载期存在，装完必须把 package 摘掉（否则 require 又活了）")
    install_at = manager.find("LuaC.install(globals)")
    strip_at = manager.find("globals.set(name, LuaValue.NIL)")
    check(strip_at > install_at >= 0,
          "摘除危险全局必须发生在 LuaC.install 之后：install 会把 load/loadstring 重新装回 globals，"
          "顺序反了沙箱就是漏的")

    # --- 2. 预编译：加载期编译一次，运行期不再传脚本文本。
    check("compilePrototype(" in manager, "脚本应在编译期解析成 Prototype，而不是每次 eval 文本")
    check("synchronized (GLOBALS)" in manager,
          "共享 Globals 的求值必须加锁（否则并发脚本互相覆盖 x/r/y）")
    check("modifier.compileFunction()" in factory, "Gson 工厂必须在反序列化后编译脚本")
    check('getRawType() != Modifier.class' in factory or "getRawType() != Modifier.class" in factory,
          "Gson 工厂只能接管 Modifier，其它类型要返回 null 交给下一个 factory")
    check("registerTypeAdapterFactory(new ModifierTypeAdapterFactory())" in assets,
          "CommonAssetsManager 的 GSON 未注册 ModifierTypeAdapterFactory"
          "（否则网络同步来的配件 JSON 没有编译结果）")

    # --- 3. 运行期路径必须用编译结果，不能退回字符串。
    check("getCompiledFunction()" in modifier, "Modifier 缺少 getCompiledFunction()")
    check("transient AttachmentScriptManager.CompiledScript" in modifier,
          "编译结果必须是 transient，不能参与 Gson 序列化")
    check("CompiledScript function = mod.getCompiledFunction()" in cache,
          "ParameterizedCache 应直接保存编译结果，而不是脚本字符串")
    check("List<String> scripts" not in cache,
          "ParameterizedCache 仍按脚本字符串缓存（预编译被绕过）")
    check("CompiledScript function = modifier.getCompiledFunction()" in props,
          "AttachmentPropertyManager#eval 应使用预编译结果")
    check("LUAJ_ENGINE" not in props and "LuaScriptEngineFactory" not in props,
          "AttachmentPropertyManager 不应再持有 ScriptEngine（走老路径）")
    check("AttachmentScriptManager.compile(script)" in props,
          "functionEval 的兜底实现应交给 AttachmentScriptManager")

    # --- 4. 脚本失败不能冒出 tick 循环。
    script_api = read("item/ModernKineticGunScriptAPI.java")
    guard = read("entity/shooter/GunScriptGuard.java")
    holder = read("entity/shooter/ShooterDataHolder.java")
    check("catch (LuaError" in guard, "GunScriptGuard 必须捕获 LuaError")
    check("data.cancelGunScriptState()" in guard, "脚本失败后必须清掉中间状态")
    check("data.gunScriptFailed = true" in guard, "脚本失败后必须置位暂停标记")
    check("if (data.gunScriptFailed)" in guard, "暂停期间必须短路")
    check("GunScriptGuard.run(dataHolder, shooter, gunId, \"shoot\"" in script_api,
          "射击循环必须经 GunScriptGuard 兜底")
    check("GunScriptGuard.run(dataHolder, shooter, gunId, \"safe_async_task\"" in script_api,
          "safeAsyncTask 必须经 GunScriptGuard 兜底")
    check("public void setCurrentGunItem(" in holder, "ShooterDataHolder 缺少 setCurrentGunItem")
    check("public void cancelGunScriptState()" in holder, "ShooterDataHolder 缺少 cancelGunScriptState")
    draw = read("entity/shooter/LivingEntityDrawGun.java")
    check("data.setCurrentGunItem(gunItemSupplier)" in draw,
          "切枪必须走 setCurrentGunItem（否则 currentGunId 一直是 null，兜底永远短路）")
    check("data.currentGunItem = gunItemSupplier" not in draw,
          "LivingEntityDrawGun 仍有直接赋值，绕过了 gunId 记录")

    return report()


def report() -> int:
    if FAILURES:
        for failure in FAILURES:
            print(f"::error::{failure}")
        print(f"verify_attachment_scripts: FAILED ({len(FAILURES)}/{CHECKS} checks)")
        return 1
    print(f"verify_attachment_scripts: OK ({CHECKS} checks)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

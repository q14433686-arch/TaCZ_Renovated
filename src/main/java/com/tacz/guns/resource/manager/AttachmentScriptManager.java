package com.tacz.guns.resource.manager;

import com.tacz.guns.GunMod;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

/**
 * 配件内联函数的编译与执行（1.2.0 引入）。
 *
 * <p>1.1.8 的做法是每次求值都走 {@code ScriptEngine.eval(String)}：既重新解析脚本，
 * 又要靠 {@code javax.script} 的绑定表传参，枪包里的 {@code function} 字段越多越慢，
 * 而且脚本环境是全功能的（{@code io}/{@code os}/{@code package} 都在）。
 *
 * <p>这里改成两段式：
 * <ol>
 *   <li><b>加载期</b>编译一次（{@code ModifierTypeAdapterFactory} 在 Gson 反序列化时调用
 *       {@link #compile}），缓存 {@link Prototype}；</li>
 *   <li><b>运行期</b>用共享的受限 {@link Globals} 执行已编译的闭包，只通过全局变量
 *       {@code x} / {@code r} 传参，读 {@code y} 取结果。</li>
 * </ol>
 *
 * <p><b>沙箱</b>：只装载 {@code base}/{@code bit32}/{@code table}/{@code string}/{@code math}，
 * 不装载 {@code package}（无 {@code require}/{@code loadlib}）、{@code io}、{@code os}、
 * {@code debug}、{@code coroutine}，并把 {@code load}/{@code loadfile}/{@code dofile}/
 * {@code collectgarbage}/{@code print}/{@code getmetatable} 置为 {@code nil}。
 *
 * <p><b>与上游 1.2.0 的差异（有意为之）</b>：
 * <ul>
 *   <li>上游用 {@code JseStringLib} 提权（反射访问 Java 侧字符串方法），本仓的
 *       {@code luaj-jse} 里并没有这个类，也不该有 —— 这里用 {@code StringLib}，
 *       脚本拿到的 {@code string.*} 语义不变，但无法从 Lua 触达 Java 反射。</li>
 *   <li>上游按原样先装库、不留 {@code package}，但 luaj 3.0.x 的
 *       {@code TableLib}/{@code StringLib}/{@code Bit32Lib}/{@code MathLib} 装载时都会写
 *       {@code package.loaded}，没有 {@code package} 会直接抛
 *       {@code attempt to index ? (a nil value)}（本仓测试实测）。这里先垫一个只含
 *       {@code loaded} 的 {@code package}，装完库再摘掉。</li>
 * </ul>
 *
 * <p><b>线程安全</b>：所有脚本共用一份 {@code GLOBALS}，求值时必须持有它的监视器，
 * 否则并发执行会互相覆盖 {@code x}/{@code r}/{@code y}。锁的粒度就是「一次求值」。
 */
public final class AttachmentScriptManager {
    /** 需要从受限环境里摘掉的入口：动态加载、GC 干预、调试取元表、控制台输出。 */
    private static final String[] UNSAFE_GLOBALS = {
            "collectgarbage", "dofile", "loadfile", "load", "print", "getmetatable"
    };

    private static final Globals GLOBALS = secureStandardGlobals();

    private AttachmentScriptManager() {
    }

    /**
     * 编译一段配件脚本。空串与 {@code null} 返回 {@code null}（调用方视为「没有脚本」），
     * 语法错误同样返回 {@code null} 并记一条 warn：一条坏脚本不该让整个枪包加载失败。
     *
     * <p>返回 {@code null} 是常态，不是异常；调用方一律按「没有脚本」处理。
     */
    public static CompiledScript compile(String script) {
        if (script == null || script.isEmpty()) {
            return null;
        }
        try {
            // 与 1.1.8 一致：脚本按小写处理（历史行为，枪包里的 Lua 依赖它）。
            Prototype prototype = GLOBALS.compilePrototype(
                    new StringReader(script.toLowerCase(Locale.ENGLISH)), "attachment_modifier");
            return new CompiledScript(prototype);
        } catch (IOException | LuaError e) {
            GunMod.LOGGER.warn("Failed to compile attachment modifier function", e);
            return null;
        }
    }

    private static Globals secureStandardGlobals() {
        Globals globals = new Globals();
        // TableLib / StringLib / Bit32Lib / MathLib 在装载时都会执行
        // `package.loaded[<name>] = <table>`，没有 package 就直接 LuaError。
        // 所以先塞一个只含 loaded 的最小垫片，装完库再把 package 摘掉
        //（上游 1.2.0 的写法在这几个库上会崩，见类注释里的差异说明）。
        LuaTable loaded = new LuaTable();
        LuaTable packageShim = new LuaTable();
        packageShim.set("loaded", loaded);
        globals.set("package", packageShim);

        globals.load(new BaseLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        // 数学库用 luaj 自带的 Lua 语义实现，不引入 java.lang.Math 的反射包装。
        globals.load(new MathLib());

        // 顺序要紧：LuaC.install 会把 load / loadstring 之类重新装进 globals，
        // 所以必须先 install、再摘掉危险入口，否则沙箱是漏的。
        LuaC.install(globals);
        for (String name : UNSAFE_GLOBALS) {
            globals.set(name, LuaValue.NIL);
        }
        // 垫片用完即撤：脚本里 package / require 必须是 nil。
        globals.set("package", LuaValue.NIL);
        return globals;
    }

    /** 一份已编译的配件脚本。只读，可跨线程共享；求值时靠 {@link #GLOBALS} 的锁串行化。 */
    public static final class CompiledScript {
        private final LuaClosure function;

        private CompiledScript(Prototype prototype) {
            this.function = new LuaClosure(prototype, GLOBALS);
        }

        /**
         * 以 {@code x} 为输入、{@code r} 为默认值执行脚本，返回脚本写回的 {@code y}。
         *
         * @return {@code y} 不是数字、脚本报错或没写 {@code y} 时，返回 {@code value}（即 {@code x}）
         */
        public double eval(double value, double defaultValue) {
            synchronized (GLOBALS) {
                GLOBALS.rawset("x", LuaValue.valueOf(value));
                GLOBALS.rawset("r", LuaValue.valueOf(defaultValue));
                GLOBALS.rawset("y", LuaValue.NIL);
                try {
                    function.call();
                    LuaValue result = GLOBALS.rawget("y");
                    return result instanceof LuaNumber number ? number.todouble() : value;
                } catch (LuaError e) {
                    GunMod.LOGGER.warn("Failed to execute attachment modifier function", e);
                    return value;
                } finally {
                    // 不让上一份脚本的返回值泄漏给下一份（x/r 反正会被覆盖）。
                    GLOBALS.rawset("y", LuaValue.NIL);
                }
            }
        }
    }
}

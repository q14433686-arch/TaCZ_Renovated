package attachmentscripttest;

import com.tacz.guns.GunMod;
import com.tacz.guns.resource.manager.AttachmentScriptManager;
import com.tacz.guns.resource.manager.AttachmentScriptManager.CompiledScript;

/**
 * 直接跑 {@code src/main/java} 里的真实现（不是副本），编译期需要 GuMod 替身与 luaj 的
 * 真实 jar（build.gradle 里 {@code compileAttachmentScriptTest} 会带上 libs/luaj-jse）。
 *
 * <p>覆盖 1.2.0 引入的两件事：
 * <ol>
 *   <li>脚本与数值的约定没变：读 {@code x}、读默认值 {@code r}、写 {@code y}；</li>
 *   <li>沙箱真的关得上门：{@code load}/{@code io}/{@code os}/{@code package}/{@code debug}
 *       在脚本里都不可用，而且被摘掉的全局不会被子环境重新长出来。</li>
 * </ol>
 */
public final class AttachmentScriptTest {
    private static int checks;

    public static void main(String[] args) {
        try {
            runAll();
            System.out.println("AttachmentScriptTest: passed (" + checks + " checks)");
        } catch (Throwable failure) {
            // 以 ::error:: 形式输出：GitHub Actions 会把它变成 annotation，
            // 即使 job 日志拿不到也能定位失败点。
            System.out.println("::error::AttachmentScriptTest FAILED after " + checks + " checks: " + failure);
            for (StackTraceElement frame : failure.getStackTrace()) {
                System.out.println("::error::  at " + frame);
            }
            Throwable cause = failure.getCause();
            while (cause != null) {
                System.out.println("::error::  caused by " + cause);
                cause = cause.getCause();
            }
            System.exit(1);
        }
    }

    private static void runAll() {
        basicArithmetic();
        readsDefaultValue();
        missingResultKeepsInput();
        emptyOrNullIsNoScript();
        syntaxErrorDoesNotThrow();
        runtimeErrorDoesNotThrow();
        sandboxBlocksDangerousGlobals();
        sandboxHasMathAndString();
        compiledScriptIsReusable();
        exceptionsInOneScriptDoNotPoisonTheNext();
    }

    private static void basicArithmetic() {
        CompiledScript script = compile("y = x * 2");
        require(near(script.eval(3, 1), 6), "y = x * 2 evaluates x");
    }

    /** 枪包脚本用 r 表达「未改装时的默认值」，必须原样传进去。 */
    private static void readsDefaultValue() {
        CompiledScript script = compile("y = x + r");
        require(near(script.eval(1.5, 2.5), 4), "r is the untouched default value");
    }

    private static void missingResultKeepsInput() {
        CompiledScript noY = compile("local unused = 1");
        require(near(noY.eval(7, 0), 7), "a script that never writes y leaves the input alone");

        CompiledScript notANumber = compile("y = 'abc'");
        require(near(notANumber.eval(7, 0), 7), "a non-numeric y leaves the input alone");
    }

    private static void emptyOrNullIsNoScript() {
        require(AttachmentScriptManager.compile(null) == null, "null script compiles to no script");
        require(AttachmentScriptManager.compile("") == null, "empty script compiles to no script");
    }

    /** 坏脚本是「没有脚本」，不是加载失败：枪包里一个错字不该让整包拒载。 */
    private static void syntaxErrorDoesNotThrow() {
        int logsBefore = GunMod.LOG.size();
        CompiledScript broken = AttachmentScriptManager.compile("y = (((");
        require(broken == null, "a syntax error yields no script");
        require(GunMod.LOG.size() > logsBefore, "a syntax error is logged");
    }

    private static void runtimeErrorDoesNotThrow() {
        CompiledScript script = compile("y = x.nonexistent.field");
        double result = script.eval(5, 0);
        require(near(result, 5), "a runtime error falls back to the input value");
    }

    /**
     * 沙箱红线：这些名字在受限环境里必须是 nil。逐条断言，避免「只关了一半」。
     */
    private static void sandboxBlocksDangerousGlobals() {
        for (String name : new String[]{"load", "loadfile", "dofile", "print", "collectgarbage",
                "package", "require", "io", "os", "debug", "coroutine", "luajava"}) {
            CompiledScript probe = compile("y = (" + name + " == nil) and 1 or 0");
            require(near(probe.eval(0, 0), 1), "'" + name + "' must be nil inside the sandbox");
        }
        // 通过 load 之类动态加载入口绕过沙箱必须不可行。
        CompiledScript escape = compile("y = (type(load) == 'nil' and function() return 1 end or function() return 0 end)()");
        require(near(escape.eval(0, 0), 1), "dynamic loading entry points are removed");
    }

    private static void sandboxHasMathAndString() {
        require(near(compile("y = math.floor(x)").eval(2.7, 0), 2), "math lib is available");
        require(near(compile("y = (string.upper('ab') == 'AB') and 1 or 0").eval(0, 0), 1),
                "string lib is available");
        require(near(compile("y = bit32.band(6, 3)").eval(0, 0), 2), "bit32 lib is available");
        require(near(compile("y = select('#', table.unpack({1,2,3}))").eval(0, 0), 3), "table lib is available");
        // 装载期垫的 package 必须已经被撤掉：否则 require 又能加载任意模块。
        require(near(compile("y = (package == nil) and 1 or 0").eval(0, 0), 1),
                "the package shim must not survive library loading");
    }

    /** 同一份编译结果要能反复求值（每条配件属性都复用同一个对象）。 */
    private static void compiledScriptIsReusable() {
        CompiledScript script = compile("y = x + 1");
        for (int i = 0; i < 100; i++) {
            require(near(script.eval(i, 0), i + 1), "reuse #" + i);
        }
    }

    /** 一份脚本报错不能把下一份的返回值污染成上一份的 y。 */
    private static void exceptionsInOneScriptDoNotPoisonTheNext() {
        CompiledScript writer = compile("y = 42");
        CompiledScript broken = compile("error('boom')");
        require(near(writer.eval(1, 0), 42), "writer writes 42");
        broken.eval(1, 0);
        require(near(compile("y = x").eval(9, 0), 9), "a later script sees its own y, not 42");
    }

    // ------------------------------------------------------------------ tools

    private static CompiledScript compile(String source) {
        CompiledScript script = AttachmentScriptManager.compile(source);
        require(script != null, "script compiles: " + source);
        return script;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private AttachmentScriptTest() {
    }
}

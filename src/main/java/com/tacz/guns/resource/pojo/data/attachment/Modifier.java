package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.manager.AttachmentScriptManager;

import javax.annotation.Nullable;

public class Modifier {
    @SerializedName("addend")
    private double addend = 0;

    @SerializedName("percent")
    private double percent = 0;

    @SerializedName("multiplier")
    private double multiplier = 1;

    @Nullable
    @SerializedName("function")
    private String function = null;

    /**
     * {@link #function} 的预编译结果，加载期一次性编译（见 {@code ModifierTypeAdapterFactory}）。
     * {@code transient}：不参与 Gson 序列化，也不会被比较/复制。
     */
    @Nullable
    private transient AttachmentScriptManager.CompiledScript compiledFunction;

    public double getAddend() {
        return addend;
    }

    public double getPercent() {
        return percent;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Nullable
    public String getFunction() {
        return function;
    }

    /** 编译 {@link #function}；由反序列化钩子调用，正常流程不需要手动调。 */
    public void compileFunction() {
        compiledFunction = AttachmentScriptManager.compile(function);
    }

    /**
     * 已编译的脚本。
     *
     * <p>兜底：若这个对象不是经 Gson 构造的（例如手动 {@code new Modifier()} 后在代码里
     * 通过反射/旧接口塞了 function），这里现场编译一次，避免功能静默失效。
     */
    @Nullable
    public AttachmentScriptManager.CompiledScript getCompiledFunction() {
        if (compiledFunction == null && function != null && !function.isEmpty()) {
            compileFunction();
        }
        return compiledFunction;
    }

    @Deprecated
    public void setAddend(double addend) {
        this.addend = addend;
    }

    @Deprecated
    public void setPercent(double percent) {
        this.percent = percent;
    }
}

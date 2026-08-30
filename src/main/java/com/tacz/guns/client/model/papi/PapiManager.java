package com.tacz.guns.client.model.papi;

import com.google.common.collect.Maps;
import net.minecraft.client.resources.language.Language;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Function;

public final class PapiManager {
    private static final Map<String, Function<ItemStack, String>> PAPI = Maps.newHashMap();

    // 注册，不知道放哪里，先放这
    static {
        addPapi(PlayerNamePapi.NAME, new PlayerNamePapi());
        addPapi(AmmoCountPapi.NAME, new AmmoCountPapi());
    }

    public static void addPapi(String textKey, Function<ItemStack, String> function) {
        textKey = "%" + textKey + "%";
        PAPI.put(textKey, function);
    }

    /**
     * 解析枪包 display json 的 {@code text_show} 文字。
     *
     * <h2>为什么是 {@code Language#getOrDefault} 而不是 {@code I18n#get}</h2>
     * 上游 1.20.1 原文是 {@code I18n.language.getOrDefault(textKey)} —— <b>纯查表</b>：
     * 查不到就原样返回键。26.2 的 {@code I18n.language} 字段没了，移植时误换成了
     * {@code I18n.get(textKey)}，它俩<b>不等价</b>（26.2 {@code I18n.class} 字节码实读）：
     * <pre>
     * String s = Language.getInstance().getOrDefault(key);
     * try { return String.format(Locale.ROOT, s, args); }
     * catch (IllegalFormatException e) { return "Format error: " + s; }
     * </pre>
     * 多出来的这一步 {@code String.format} 对枪包是致命的：枪包经常把显示串<b>直接内联</b>
     * 在 {@code text_key} 里（MK5HD 就是 {@code "%ammo_count%"}），那不是语言键。于是链条变成
     * ① 查表落空 → 原样返回 {@code "%ammo_count%"}；② {@code String.format} 把 {@code %a...}
     * 当格式说明符解析 → {@code IllegalFormatException}；③ 返回 {@code "Format error: %ammo_count%"}；
     * ④ 下面 PAPI 的占位符循环照常把其中的 {@code %ammo_count%} 换成弹药数 ——
     * 于是垃圾串在前、真数字缀在最后。
     *
     * <p>结论：这里要的语义是「查得到就用翻译，查不到就原样返回键」，
     * 正是 {@code Language#getOrDefault}，而不是任何带格式化的包装。</p>
     */
    public static String getTextShow(String textKey, ItemStack stack) {
        String text = Language.getInstance().getOrDefault(textKey);
        for (var entry : PAPI.entrySet()) {
            String placeholder = entry.getKey();
            String data = entry.getValue().apply(stack);
            text = text.replace(placeholder, data);
        }
        return text;
    }
}

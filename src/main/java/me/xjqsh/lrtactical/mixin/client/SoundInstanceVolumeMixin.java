package me.xjqsh.lrtactical.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.xjqsh.lrtactical.client.audio.DeafenState;
import me.xjqsh.lrtactical.client.audio.StunRingingSound;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 闪光弹的「耳鸣消声」—— 在音效<b>自报音量</b>的唯一出口处打折。
 *
 * <h2>为什么从 {@code SoundEngine#calculateVolume(SoundInstance)} 搬到这里</h2>
 *
 * <p><b>旧注入点（本分支此前用的 {@code SoundEngineMixin}）是错的</b>。
 * refab 来源分支对 26.2 jar 字节码逐条核对（两种方法互证：按指令流走一遍 +
 * 直接搜 Methodref 常量池下标的字节串）：</p>
 * <pre>
 * play(SoundInstance):
 *   @154  SoundInstance.getVolume()
 *   @177  SoundInstance.getSource()
 *   @189  SoundEngine.calculateVolume(F, SoundSource)      ← play 直接调【内层】重载
 *
 * calculateVolume(SoundInstance)F 的调用方只有两个：
 *   tickInGameSound()V                          @117     ← 每 tick 更新「可 tick 的」音效
 *   lambda$refreshCategoryVolume$0(...)V        @19      ← 玩家改音量滑条时
 * </pre>
 * ⇒ 26.x 上<b>新播放的音效根本不经过外层重载</b>：耳鸣期间新响起来的声音一点没被压低，
 * 只有「可 tick 的音效」在下一 tick 被压、以及改滑条时重算的那批被压 ——
 * 玩家看到的就是「有时闷有时不闷、毫无规律」。1.21.x 上没有这个毛病，
 * 是因为那条线上 {@code play} 走的是外层重载（该线用户实测有效，
 * <b>所以 1.21.11 分支的消声 mixin 保持不动</b>）。
 *
 * <p><b>换到 {@code AbstractSoundInstance#getVolume()} 后</b>：</p>
 * <ul>
 *   <li><b>覆盖完整</b>：{@code play()} 在 @154 取 {@code getVolume()} 再交给
 *       @189 的 {@code calculateVolume(F,SoundSource)}；
 *       {@code calculateVolume(SoundInstance)} 的实现也就是
 *       {@code calculateVolume(getVolume(), getSource())}。
 *       三条路径（新播放 / tick 更新 / 改滑条）<b>全都</b>读这个方法，
 *       一处即全覆盖，且<b>不会</b>像「内外两层都挂」那样把系数乘两次。</li>
 *   <li><b>拿得到实例</b>：{@code this} 就是音效实例，耳鸣声可以用
 *       {@code instanceof} 干净地豁免 —— 不依赖反射猜名字，
 *       也不依赖耳鸣声用哪个 {@code SoundSource} 类别。</li>
 * </ul>
 *
 * <p><b>26.1.2 的核验状态（如实记录）</b>：本沙箱拿不到 26.1.2 的 jar
 * （piston-meta 与 NeoForge maven 均不可达），上述字节码证据来自 refab 对
 * <b>26.2</b> 的核对。26.1.2 与 26.2 属同一代引擎（26.x），推断行为一致；
 * 且本注入点在两种引擎形态下都成立 —— 若 26.1.2 的 {@code play()} 仍走外层重载，
 * 外层内部也是 {@code calculateVolume(getVolume(), getSource())}，依然经过这里。
 * 属「源码级」结论，待实机确认（见本类类注释的验证清单）。</p>
 *
 * <h2>已知边界</h2>
 * 只覆盖 {@code AbstractSoundInstance} 的子类（原版与绝大多数模组都是）。
 * 若有模组直接实现 {@code SoundInstance} 接口而不继承它，那些音效不会被消声 ——
 * 可接受的降级，且不会影响耳鸣声本身。
 */
@Mixin(AbstractSoundInstance.class)
public class SoundInstanceVolumeMixin {

    /**
     * 耳鸣声豁免 + 其余音效按 {@link DeafenState#getVolumeFactor} 打折。
     *
     * <p>豁免必须排在最前：耳鸣声正是在 {@code DEAFENED} 生效期间播放的，
     * 不豁免就会把自己压到几乎听不见，变成「什么都听不到」而不是「耳朵在响」。</p>
     */
    @ModifyReturnValue(method = "getVolume()F", at = @At("RETURN"))
    private float lrtactical$applyDeafen(float original) {
        if ((Object) this instanceof StunRingingSound) {
            return original;
        }
        return original * DeafenState.getVolumeFactor(((SoundInstance) (Object) this).getSource());
    }
}

package me.xjqsh.lrtactical.client.audio;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 「耳鸣时把所有音量压低」的判定与系数计算。
 *
 * <p>被 {@code SoundInstanceVolumeMixin} 每次读取音效音量时调用，
 * 因此<b>必须极轻量</b> —— 只做一次效果查询与一次乘法。</p>
 *
 * <p>消声的注入点在 {@code AbstractSoundInstance#getVolume()}（见
 * {@code SoundInstanceVolumeMixin} 的类注释，那里有从
 * {@code SoundEngine#calculateVolume(SoundInstance)} 搬过来的完整证据）。
 * 耳鸣声在 {@code instanceof StunRingingSound} 处豁免，不再依赖
 * {@code SoundSource} 类别 —— {@code StunRingingSound} 保持 {@code PLAYERS} 不动
 * （改类别的方案在来源分支实测把耳鸣声压没了）。</p>
 */
public final class DeafenState {
    /** 耳鸣最重时保留的音量比例（0.01 = 几乎全聋）。 */
    private static final float MIN_VOLUME_FACTOR = 0.01f;
    /** 剩余时长超过此值即为「最重」，之后线性恢复。 */
    private static final float FADE_START_TICKS = 100f;

    private DeafenState() {
    }

    /** 当前是否正在播放耳鸣声，避免重复播放。 */
    private static StunRingingSound ringing;
    /** 只在第一次播放失败时告警，之后闭嘴（否则会每个 tick 刷一行）。 */
    private static boolean playFailureWarned;

    /**
     * 取当前应施加的音量系数。
     *
     * @return 1.0 表示不衰减
     */
    public static float getVolumeFactor(SoundSource source) {
        // 界面音效（按钮点击等）不该被战场效果影响，否则玩家会以为游戏卡了
        if (source == SoundSource.MASTER || source == SoundSource.MUSIC || source == SoundSource.UI) {
            return 1.0f;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return 1.0f;
        }
        MobEffectInstance effect = player.getEffect(ModEffects.DEAFENED);
        if (effect == null) {
            return 1.0f;
        }
        // 剩余越久压得越狠；接近结束时线性恢复到正常
        float progress = Math.min(effect.getDuration() / FADE_START_TICKS, 1.0f);
        return MIN_VOLUME_FACTOR + (1.0f - progress) * (1.0f - MIN_VOLUME_FACTOR);
    }

    /**
     * 每 tick 驱动耳鸣声的播放与停止。
     *
     * <p>只在<b>刚获得 {@code DEAFENED} 且当前没在响</b>时启动一次；
     * 停止交给 {@link StunRingingSound#tick()} 自行判断
     * （效果消失时它会 {@code stop()}），这样淡出逻辑集中在一处。</p>
     */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || player.getEffect(ModEffects.DEAFENED) == null) {
            ringing = null;
            return;
        }
        if (ringing == null || !mc.getSoundManager().isActive(ringing)) {
            ringing = new StunRingingSound();
            // SoundManager#play 有返回值，而它的失败路径里有几条【几乎不留痕】的
            // 分支：音量算出 0 时只在 DEBUG 级别打一行；找不到音效定义时那条 WARN
            // 带 Marker，某些日志配置下也看不到。耳鸣声听不见时默认日志里可能
            // 什么都没有 —— 之前的排查因此走了弯路。这里把结果接住，
            // 非 STARTED 就 WARN 一次（只一次，避免每 tick 刷屏），
            // 并把已知的几个坑直接写进消息里。
            var result = mc.getSoundManager().play(ringing);
            if (result != net.minecraft.client.sounds.SoundEngine.PlayResult.STARTED
                    && !playFailureWarned) {
                playFailureWarned = true;
                EquipmentMod.LOGGER.warn(
                        "[LRTactical] Stun ringing sound did not start: result={} id={}. "
                                + "排查顺序：① assets/lrtactical/sounds.json 顶层是否混入了"
                                + "非对象值（例如 _comment 字符串 —— 引擎按 "
                                + "Map<String,SoundEventRegistration> 整体反序列化，"
                                + "一个坏键会让整个文件作废）；② sounds/stun_ringing.ogg 是否存在；"
                                + "③ '{}' 音量滑条是否为 0。可跑 scripts/verify_lr_assets.py 自查。",
                        result, StunRingingSound.RINGING_ID, SoundSource.PLAYERS.getName());
            }
        }
    }
}

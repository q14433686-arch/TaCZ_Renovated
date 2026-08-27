package me.xjqsh.lrtactical.api.animation;

import com.tacz.guns.client.animation.statemachine.ItemAnimationStateContext;
import net.minecraft.world.item.ItemStack;

/**
 * 消耗品动画上下文。方法名是内容包 Lua 的 API 表面，与官方 0.4.3 保持一致。
 */
@SuppressWarnings("unused")
public class ConsumableAnimationStateContext extends ItemAnimationStateContext {
    private ItemStack currentItem = ItemStack.EMPTY;
    private boolean using = false;
    private int usingTick = 0;

    public void setCurrentItem(ItemStack currentItem) {
        this.currentItem = currentItem;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public int getStackCount() {
        return currentItem.getCount();
    }

    public boolean isUsing() {
        return using;
    }

    public void setUsing(boolean using) {
        this.using = using;
    }

    public int getUsingTick() {
        return usingTick;
    }

    public void setUsingTick(int usingTick) {
        this.usingTick = usingTick;
    }
}

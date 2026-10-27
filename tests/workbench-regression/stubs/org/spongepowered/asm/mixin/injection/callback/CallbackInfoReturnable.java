package org.spongepowered.asm.mixin.injection.callback;
public final class CallbackInfoReturnable<T> {
    private boolean cancelled;
    private T value;
    public void setReturnValue(T value) { this.value = value; cancelled = true; }
    public boolean isCancelled() { return cancelled; }
    public T getReturnValue() { return value; }
}

package net.minecraft.server.packs.resources;
import net.minecraft.util.profiling.ProfilerFiller;
public abstract class SimplePreparableReloadListener<T> implements PreparableReloadListener {
    protected abstract T prepare(ResourceManager manager, ProfilerFiller profiler);
    protected abstract void apply(T data, ResourceManager manager, ProfilerFiller profiler);
}

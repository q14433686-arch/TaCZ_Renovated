package com.mojang.serialization;
import java.util.Optional;
public final class DataResult<T> {
    private final T value;
    public DataResult(T value) { this.value = value; }
    public Optional<T> result() { return Optional.ofNullable(value); }
}

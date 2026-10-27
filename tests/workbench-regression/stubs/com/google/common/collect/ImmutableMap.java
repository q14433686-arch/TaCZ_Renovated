package com.google.common.collect;
import java.util.*;
public final class ImmutableMap {
    public static <K,V> Builder<K,V> builder() { return new Builder<>(); }
    public static final class Builder<K,V> {
        private final Map<K,V> data = new LinkedHashMap<>();
        public Builder<K,V> put(K key, V value) { data.put(key, value); return this; }
        public Map<K,V> build() { return Map.copyOf(data); }
    }
}

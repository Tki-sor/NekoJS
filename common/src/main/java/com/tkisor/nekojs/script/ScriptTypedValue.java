package com.tkisor.nekojs.script;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 支持泛型枚举的状态存储器
 * @param <E> 枚举类型
 * @param <T> 存储的值类型
 */
public final class ScriptTypedValue<E extends Enum<E>, T> {

    public static <E extends Enum<E>, T> ScriptTypedValue<E, T> of(E[] allTypes) {
        Objects.requireNonNull(allTypes, "allTypes == null");
        return new ScriptTypedValue<>(allTypes, new Object[allTypes.length + 1], null);
    }

    public static <E extends Enum<E>, T> ScriptTypedValue<E, T> of(
            E[] allTypes,
            Function<? super E, ? extends T> initializer
    ) {
        Objects.requireNonNull(allTypes, "allTypes == null");
        Objects.requireNonNull(initializer, "initializer == null");
        return new ScriptTypedValue<>(allTypes, new Object[allTypes.length + 1], initializer);
    }

    public static <E extends Enum<E>, T> ScriptTypedValue<E, T> ofNullable(E[] allTypes) {
        Objects.requireNonNull(allTypes, "allTypes == null");
        return new ScriptTypedValue<>(allTypes, new Object[allTypes.length + 1], null);
    }

    public static <E extends Enum<E>, T> ScriptTypedValue<E, T> ofNullable(
            E[] allTypes,
            Function<? super E, ? extends T> initializer
    ) {
        Objects.requireNonNull(allTypes, "allTypes == null");
        Objects.requireNonNull(initializer, "initializer == null");
        return new ScriptTypedValue<>(allTypes, new Object[allTypes.length + 1], initializer);
    }

    private final E[] allTypes;
    private final Object[] internal;
    private final Function<? super E, ? extends T> initializer;

    private ScriptTypedValue(
            E[] allTypes,
            Object[] internal,
            Function<? super E, ? extends T> initializer
    ) {
        this.allTypes = allTypes;
        this.internal = internal;
        this.initializer = initializer;
    }

    @SuppressWarnings("unchecked")
    private T getCasted(int index) {
        return (T) internal[index];
    }

    private int index(E type) {
        // 如果 type 为 null 且允许 null（数组长度比枚举长度多1），存放在最后一位
        return type == null ? allTypes.length : type.ordinal();
    }

    public T set(E type, T value) {
        int idx = index(type);
        T old = getCasted(idx);
        internal[idx] = value;
        return old;
    }

    public T at(E type) {
        int idx = index(type);
        T got = getCasted(idx);
        if (got == null && initializer != null) {
            got = initializer.apply(type);
            internal[idx] = got;
        }
        return got;
    }

    public boolean hasInitializer() {
        return initializer != null;
    }

    @SuppressWarnings("unchecked")
    public Stream<T> viewExisted() {
        return (Stream<T>) Arrays.stream(internal).filter(Objects::nonNull);
    }

    @Override
    public String toString() {
        return "ScriptTypedValue" + Arrays.toString(internal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScriptTypedValue<?, ?> that)) return false;
        return Arrays.equals(internal, that.internal)
                && Objects.equals(initializer, that.initializer);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(internal);
    }
}
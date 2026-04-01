package com.tkisor.nekojs.api.data;

import com.tkisor.nekojs.script.ScriptType;
import lombok.Getter;

public class Binding {
    @Getter
    private final String name;

    @Getter
    private final Object object;

    @Getter
    private final Class<?> type;

    @Getter
    private final boolean isStaticClass;

    @Getter
    private final ScriptType targetType;

    private Binding(String name, Object object, boolean isStaticClass, ScriptType targetType) {
        this.name = name;
        this.object = object;
        this.type = isStaticClass ? (Class<?>) object : object.getClass();
        this.isStaticClass = isStaticClass;
        this.targetType = targetType;
    }

    /**
     * 判断该绑定是否允许注入到指定的脚本环境中
     */
    public boolean isValidFor(ScriptType envType) {
        return this.targetType == ScriptType.COMMON || this.targetType == envType;
    }

    // ================= 默认通用环境 (COMMON) =================

    public static Binding of(String name, Object object) {
        return of(ScriptType.COMMON, name, object);
    }

    public static Binding of(String name, Class<?> type) {
        return of(ScriptType.COMMON, name, type);
    }

    // ================= 指定环境 (SERVER, CLIENT, STARTUP 等) =================

    public static Binding of(ScriptType targetType, String name, Object object) {
        if (object instanceof Class<?>) {
            throw new IllegalArgumentException("请使用 Binding.of(targetType, name, Class) 来绑定静态类");
        }
        return new Binding(name, object, false, targetType);
    }

    public static Binding of(ScriptType targetType, String name, Class<?> type) {
        return new Binding(name, type, true, targetType);
    }
}
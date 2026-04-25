package com.tkisor.nekojs.script;

public interface WithScriptType {
    Enum<?> scriptType();

    default boolean canApplyOn(Enum<?> type) {
        var target = scriptType();
        return target == type;
    }
}
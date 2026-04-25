package com.tkisor.nekojs.api.data;

@FunctionalInterface
public interface BindingsRegister {
    void register(Object binding);
}
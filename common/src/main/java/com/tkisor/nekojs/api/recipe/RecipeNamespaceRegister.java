package com.tkisor.nekojs.api.recipe;

@FunctionalInterface
public interface RecipeNamespaceRegister {
    void register(String namespace, Object factory);
}
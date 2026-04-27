package com.tkisor.nekojs.api.recipe;

@FunctionalInterface
public interface RecipeNamespaceRegister {
    /**
     * 注册一个配方命名空间处理器。
     */
    void register(RecipeNamespaceEntry entry);
}

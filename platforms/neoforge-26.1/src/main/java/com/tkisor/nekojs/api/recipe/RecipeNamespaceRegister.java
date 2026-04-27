package com.tkisor.nekojs.api.recipe;

@FunctionalInterface
public interface RecipeNamespaceRegister {
    /**
     * Registers a recipe namespace handler.
     */
    void register(RecipeNamespaceEntry entry);
}

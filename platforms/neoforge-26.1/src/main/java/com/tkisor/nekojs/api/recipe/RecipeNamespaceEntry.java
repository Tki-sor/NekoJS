package com.tkisor.nekojs.api.recipe;

import com.tkisor.nekojs.wrapper.event.server.RecipeEventJS;

import java.util.function.Function;

/**
 * Unified recipe namespace registration entry.
 *
 * @param namespace    the namespace (e.g. "minecraft")
 * @param factory      handler constructor factory (e.g. {@code Handler::new})
 * @param handlerClass handler Java type (e.g. {@code Handler.class}), for external mods to query
 */
public record RecipeNamespaceEntry(
        String namespace,
        Function<RecipeEventJS, Object> factory,
        Class<?> handlerClass
) {
    public static RecipeNamespaceEntry of(String namespace,
                                          Function<RecipeEventJS, Object> factory,
                                          Class<?> handlerClass) {
        return new RecipeNamespaceEntry(namespace, factory, handlerClass);
    }
}

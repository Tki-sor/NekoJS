package com.tkisor.nekojs.bindings.static_access;

import com.tkisor.nekojs.wrapper.item.IngredientWrapper;

public class IngredientJS {
    public IngredientWrapper of(String... ids) {
        return new IngredientWrapper(ids);
    }
}

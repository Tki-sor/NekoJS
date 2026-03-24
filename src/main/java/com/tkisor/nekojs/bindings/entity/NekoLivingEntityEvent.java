package com.tkisor.nekojs.bindings.entity;

import com.tkisor.nekojs.wrapper.entity.LivingEntityJS;

public interface NekoLivingEntityEvent extends NekoEntityEvent {
    @Override
    LivingEntityJS getEntity();
}

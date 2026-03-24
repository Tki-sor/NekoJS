package com.tkisor.nekojs.bindings.player;

import com.tkisor.nekojs.bindings.entity.NekoLivingEntityEvent;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import org.jetbrains.annotations.Nullable;

public interface NekoPlayerEvent extends NekoLivingEntityEvent {
    @Override
    PlayerJS getEntity();

    @Override
    @Nullable
    default PlayerJS getPlayer() {
        return getEntity();
    }
}

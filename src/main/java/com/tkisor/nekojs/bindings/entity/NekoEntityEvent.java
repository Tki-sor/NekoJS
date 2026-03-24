package com.tkisor.nekojs.bindings.entity;

import com.tkisor.nekojs.bindings.level.NekoLevelEvent;
import com.tkisor.nekojs.wrapper.entity.EntityJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface NekoEntityEvent extends NekoLevelEvent {
    EntityJS getEntity();

    @Nullable
    default PlayerJS getPlayer() {
        return getEntity() instanceof PlayerJS p ? p : null;
    }

    @Override
    default Level getLevel() {
        return getEntity().level();
    }
}

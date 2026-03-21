package com.tkisor.nekojs.bindings.event;

import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;
import com.tkisor.nekojs.wrapper.event.level.ExplosionStartEventJS;

public interface LevelEvents {
    EventGroup GROUP = EventGroup.of("LevelEvents");
    EventBusJS<ExplosionStartEventJS, Void> EXPLOSION_START =
            GROUP.server("explosionStart", ExplosionStartEventJS.class);
}

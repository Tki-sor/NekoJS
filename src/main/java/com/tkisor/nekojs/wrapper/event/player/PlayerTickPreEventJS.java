package com.tkisor.nekojs.wrapper.event.player;

import com.tkisor.nekojs.bindings.player.NekoPlayerEvent;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerTickPreEventJS implements NekoPlayerEvent {
    private final PlayerTickEvent.Pre rawEvent;

    public PlayerTickPreEventJS(PlayerTickEvent.Pre rawEvent) {
        this.rawEvent = rawEvent;
    }

    @Override
    public PlayerJS getEntity() {
        return getPlayer();
    }

    @Override
    public PlayerJS getPlayer() {
        return new PlayerJS(rawEvent.getEntity());
    }
}

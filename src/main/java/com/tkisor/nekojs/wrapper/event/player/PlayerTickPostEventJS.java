package com.tkisor.nekojs.wrapper.event.player;

import com.tkisor.nekojs.bindings.player.NekoPlayerEvent;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerTickPostEventJS implements NekoPlayerEvent {
    private final PlayerTickEvent.Post rawEvent;

    public PlayerTickPostEventJS(PlayerTickEvent.Post rawEvent) {
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
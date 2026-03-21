package com.tkisor.nekojs.wrapper.event.player;

import com.tkisor.nekojs.wrapper.entity.PlayerWrapper;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerTickEventJS {
    private final PlayerTickEvent.Post rawEvent;

    public PlayerTickEventJS(PlayerTickEvent.Post rawEvent) {
        this.rawEvent = rawEvent;
    }

    public PlayerWrapper getPlayer() {
        return new PlayerWrapper(rawEvent.getEntity());
    }
}
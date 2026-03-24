package com.tkisor.nekojs.wrapper.event.player;

import com.tkisor.nekojs.bindings.player.NekoPlayerEvent;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import lombok.Getter;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerLoggedInEventJS implements NekoPlayerEvent {

    private final PlayerEvent.PlayerLoggedInEvent rawEvent;

    @Getter
    private final PlayerJS player;

    public PlayerLoggedInEventJS(PlayerEvent.PlayerLoggedInEvent rawEvent) {
        this.rawEvent = rawEvent;
        this.player = new PlayerJS(rawEvent.getEntity());
    }

    @Override
    public PlayerJS getEntity() {
        return player;
    }
}
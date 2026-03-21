package com.tkisor.nekojs.wrapper.event.player;

import com.tkisor.nekojs.wrapper.entity.PlayerWrapper;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerRespawnEventJS {
    private final PlayerEvent.PlayerRespawnEvent rawEvent;

    public PlayerRespawnEventJS(PlayerEvent.PlayerRespawnEvent rawEvent) {
        this.rawEvent = rawEvent;
    }

    public PlayerWrapper getPlayer() {
        return new PlayerWrapper(rawEvent.getEntity());
    }

    /**
     * 判断玩家是不是因为打败了末影龙从末地传送门回来的（而不是死回来的）
     * JS: event.isEndConquered()
     */
    public boolean isEndConquered() {
        return rawEvent.isEndConquered();
    }
}
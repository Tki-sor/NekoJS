package com.tkisor.nekojs.wrapper.event.block;

import com.tkisor.nekojs.api.event.NekoCancellableEvent;
import com.tkisor.nekojs.wrapper.block.BlockJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import lombok.Getter;
import net.neoforged.neoforge.event.level.BlockEvent;

public class BlockBreakEventJS implements NekoCancellableEvent {
    private final BlockEvent.BreakEvent rawEvent;

    @Getter
    private final PlayerJS player;

    @Getter
    private final BlockJS block;

    public BlockBreakEventJS(BlockEvent.BreakEvent rawEvent) {
        this.rawEvent = rawEvent;
        this.player = new PlayerJS(rawEvent.getPlayer());

        this.block = new BlockJS(
                rawEvent.getLevel(),
                rawEvent.getPos(),
                rawEvent.getState()
        );
    }

    public String getBlockId() {
        return this.block.getId();
    }

}
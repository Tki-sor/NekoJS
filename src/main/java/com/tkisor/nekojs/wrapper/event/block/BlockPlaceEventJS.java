package com.tkisor.nekojs.wrapper.event.block;

import com.tkisor.nekojs.api.event.NekoCancellableEvent;
import com.tkisor.nekojs.wrapper.block.BlockJS;
import com.tkisor.nekojs.wrapper.entity.EntityJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockEvent;

public class BlockPlaceEventJS implements NekoCancellableEvent {
    private final BlockEvent.EntityPlaceEvent rawEvent;

    @Getter
    private final EntityJS entity;

    @Getter
    private final BlockJS block;

    public BlockPlaceEventJS(BlockEvent.EntityPlaceEvent rawEvent) {
        this.rawEvent = rawEvent;

        this.entity = EntityJS.of(rawEvent.getEntity());

        this.block = new BlockJS(
                rawEvent.getLevel(),
                rawEvent.getPos(),
                rawEvent.getPlacedBlock()
        );
    }

    public String getBlockId() {
        return this.block.getId();
    }

    public boolean isPlayer() {
        return rawEvent.getEntity() instanceof Player;
    }

    public PlayerJS getPlayer() {
        return entity instanceof PlayerJS pw ? pw : null;
    }

}
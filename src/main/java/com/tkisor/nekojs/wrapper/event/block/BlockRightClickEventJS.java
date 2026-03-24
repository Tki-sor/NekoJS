package com.tkisor.nekojs.wrapper.event.block;

import com.tkisor.nekojs.api.event.NekoCancellableEvent;
import com.tkisor.nekojs.wrapper.block.BlockJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import com.tkisor.nekojs.wrapper.item.ItemStackJS;
import lombok.Getter;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockRightClickEventJS implements NekoCancellableEvent {
    private final PlayerInteractEvent.RightClickBlock rawEvent;

    @Getter
    private final PlayerJS player;

    @Getter
    private final BlockJS block;

    @Getter
    private final ItemStackJS item;

    public BlockRightClickEventJS(PlayerInteractEvent.RightClickBlock rawEvent) {
        this.rawEvent = rawEvent;

        this.player = new PlayerJS(rawEvent.getEntity());

        this.item = new ItemStackJS(rawEvent.getItemStack());

        Level level = rawEvent.getLevel();
        BlockPos pos = rawEvent.getPos();
        BlockState state = level.getBlockState(pos);
        this.block = new BlockJS(level, pos, state);
    }

    public String getBlockId() {
        return this.block.getId();
    }

    public String getFacing() {
        return rawEvent.getFace() != null ? rawEvent.getFace().getSerializedName() : "null";
    }

    public String getHand() {
        return rawEvent.getHand().name();
    }

}
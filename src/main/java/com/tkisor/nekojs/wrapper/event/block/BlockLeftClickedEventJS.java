package com.tkisor.nekojs.wrapper.event.block;

import com.tkisor.nekojs.api.event.NekoCancellableEvent;
import com.tkisor.nekojs.wrapper.block.BlockJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import com.tkisor.nekojs.wrapper.item.ItemStackJS;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BlockLeftClickedEventJS implements NekoCancellableEvent {

    private final PlayerInteractEvent.LeftClickBlock rawEvent;

    @Getter
    private final BlockJS block;

    public BlockLeftClickedEventJS(PlayerInteractEvent.LeftClickBlock rawEvent) {
        this.rawEvent = rawEvent;

        Level level = rawEvent.getLevel();
        BlockPos pos = rawEvent.getPos();
        BlockState state = level.getBlockState(pos);
        this.block = new BlockJS(level, pos, state);
    }

    /**
     * 获取点击的玩家
     * JS 侧: event.player
     */
    public PlayerJS getPlayer() {
        return new PlayerJS(rawEvent.getEntity());
    }

    public String getBlockId() {
        return this.block.getId();
    }

    public ItemStackJS getItemStack() {
        return new ItemStackJS(rawEvent.getItemStack());
    }

    public TriState getUseItem() {
        return rawEvent.getUseItem();
    }

    public PlayerInteractEvent.LeftClickBlock.Action getAction() {
        return rawEvent.getAction();
    }

    public void setUseBlock(TriState triggerBlock) {
        rawEvent.setUseBlock(triggerBlock);
    }

    public void setUseItem(TriState triggerItem) {
        rawEvent.setUseItem(triggerItem);
    }


    /**
     * 获取方块坐标
     * JS 侧: event.pos
     */
    public BlockPos getPos() {
        return rawEvent.getPos();
    }

    /**
     * 获取点击的面 (上、下、东、西等)
     * JS 侧: event.face
     */
    public Direction getFace() {
        return rawEvent.getFace();
    }

}
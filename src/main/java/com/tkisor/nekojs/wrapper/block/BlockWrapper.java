package com.tkisor.nekojs.wrapper.block;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockWrapper implements NekoWrapper<BlockState> {

    @Getter
    private final LevelAccessor level;
    @Getter
    private final BlockPos pos;
    private BlockState state;

    public BlockWrapper(LevelAccessor level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public int getX() {
        return pos.getX();
    }

    public int getY() {
        return pos.getY();
    }

    public int getZ() {
        return pos.getZ();
    }

    public boolean isAir() {
        return state.isAir();
    }

    public boolean isWater() {
        return state.is(Blocks.WATER);
    }

    public boolean isSolid() {
        return state.isSolid();
    }

    public BlockEntity getEntity() {
        if (level == null) return null;
        return level.getBlockEntity(pos);
    }

    public BlockWrapper offset(int x, int y, int z) {
        if (level == null) return this;
        BlockPos newPos = pos.offset(x, y, z);
        return new BlockWrapper(level, newPos, level.getBlockState(newPos));
    }

    public BlockWrapper up() {
        return offset(0, 1, 0);
    }

    public BlockWrapper down() {
        return offset(0, -1, 0);
    }

    public BlockWrapper north() {
        return offset(0, 0, -1);
    }

    public BlockWrapper south() {
        return offset(0, 0, 1);
    }

    public BlockWrapper west() {
        return offset(-1, 0, 0);
    }

    public BlockWrapper east() {
        return offset(1, 0, 0);
    }

    // 获取相邻方块，JS 侧：block.relative("up")
    public BlockWrapper relative(String direction) {
        Direction dir = Direction.byName(direction.toLowerCase());
        if (dir == null) return this;
        return offset(dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }

    public Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        for (Property<?> prop : state.getProperties()) {
            map.put(prop.getName(), getPropertyValue(prop));
        }
        return map;
    }

    public String getProperty(String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals(name)) {
                return getPropertyValue(prop);
            }
        }
        return null;
    }

    /**
     * JS 侧调用：block.with("facing", "north").with("lit", "true")
     * 支持链式调用，修改属性并立即更新到世界中！
     */
    public BlockWrapper with(String propertyName, String value) {
        if (level == null) return this;
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals(propertyName)) {
                parseAndSetProperty(prop, value);
                level.setBlock(pos, this.state, 3);
                return this;
            }
        }
        return this;
    }

    private <T extends Comparable<T>> void parseAndSetProperty(Property<T> prop, String value) {
        prop.getValue(value).ifPresent(v -> this.state = this.state.setValue(prop, v));
    }

    private <T extends Comparable<T>> String getPropertyValue(Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public boolean hasTag(Identifier tagId) {
        if (tagId == null) return false;
        return this.state.is(TagKey.create(Registries.BLOCK, tagId));
    }

    public boolean destroy(boolean drop) {
        if (this.level == null) return false;
        return this.level.destroyBlock(this.pos, drop);
    }

    public void set(Identifier blockId) {
        if (level == null) return;
        if (blockId == null) return;

        Optional<Holder.Reference<Block>> newBlock = BuiltInRegistries.BLOCK.get(blockId);

        newBlock.ifPresent(blockReference -> {
            this.state = blockReference.value().defaultBlockState();
            level.setBlock(pos, this.state, 3);
        });
    }

    @Override
    public BlockState unwrap() {
        return this.state;
    }
}
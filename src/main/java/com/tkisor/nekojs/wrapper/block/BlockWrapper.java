package com.tkisor.nekojs.wrapper.block;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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

    public int getX() { return pos.getX(); }
    public int getY() { return pos.getY(); }
    public int getZ() { return pos.getZ(); }

    @Override
    public BlockState unwrap() {
        return this.state;
    }

    public Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        for (Property<?> prop : state.getValues().keySet()) {
            map.put(prop.getName(), getPropertyValue(prop));
        }
        return map;
    }

    public String getProperty(String name) {
        for (Property<?> prop : state.getValues().keySet()) {
            if (prop.getName().equals(name)) {
                return getPropertyValue(prop);
            }
        }
        return null;
    }

    private <T extends Comparable<T>> String getPropertyValue(Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public boolean hasTag(String tagId) {
        Identifier tagLocation = Identifier.tryParse(tagId);
        if (tagLocation == null) return false;

        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagLocation);
        return this.state.is(tagKey);
    }

    public boolean destroy(boolean drop) {
        if (this.level == null) return false;
        return this.level.destroyBlock(this.pos, drop);
    }

    public void set(String blockId) {
        if (level == null) return;

        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) return;

        Optional<Holder.Reference<Block>> newBlock = BuiltInRegistries.BLOCK.get(identifier);

        newBlock.ifPresent(blockReference -> {
            this.state = blockReference.value().defaultBlockState();
            // 3 是 Minecraft 原版的方块更新 Flag
            level.setBlock(pos, this.state, 3);
        });
    }
}
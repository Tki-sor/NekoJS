package com.tkisor.nekojs.wrapper.level;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class LevelWrapper implements NekoWrapper<Level> {
    private final Level level;

    public LevelWrapper(Level level) {
        this.level = level;
    }

    public String getDimension() {
        return this.level.dimension().registry().toString();
    }

    public boolean setBlock(int x, int y, int z, String blockId) {
        Identifier location = Identifier.tryParse(blockId);
        if (location == null) return false;

        Block block = BuiltInRegistries.BLOCK.get(location)
                .map(Holder.Reference::value)
                .orElse(Blocks.AIR);

        if (block == Blocks.AIR && !blockId.equals("minecraft:air")) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        return this.level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    public String getBlockId(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = this.level.getBlockState(pos);
        Identifier location = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return location != null ? location.toString() : "minecraft:air";
    }

    public void spawnLightning(double x, double y, double z) {
        if (this.level instanceof ServerLevel serverLevel) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
            if (lightning != null) {
                lightning.setPos(x, y, z);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    public void playSound(String soundId, double x, double y, double z, float volume, float pitch) {
        Identifier location = Identifier.tryParse(soundId);
        if (location == null) return;

        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(location);
        this.level.playSound(null, x, y, z, soundEvent, SoundSource.PLAYERS, volume, pitch);
    }

    public long getTime() {
        if (this.level instanceof ServerLevel serverLevel) {
            Optional<Holder<WorldClock>> defaultClock = serverLevel.dimensionType().defaultClock();
            if (defaultClock.isPresent()) {
                return serverLevel.getServer().clockManager().getTotalTicks(defaultClock.get());
            }
        }
        return this.level.getDefaultClockTime();
    }

    public void setTime(long time) {
        if (this.level instanceof ServerLevel serverLevel) {
            Optional<Holder<WorldClock>> defaultClock = serverLevel.dimensionType().defaultClock();
            defaultClock.ifPresent(worldClockHolder -> {
                serverLevel.getServer().clockManager().setTotalTicks(worldClockHolder, time);
            });
        }
    }

    public void setTimeSpeed(float speed) {
        if (this.level instanceof ServerLevel serverLevel) {
            Optional<Holder<WorldClock>> defaultClock = serverLevel.dimensionType().defaultClock();
            defaultClock.ifPresent(worldClockHolder -> {
                serverLevel.getServer().clockManager().setSpeed(worldClockHolder, speed);
            });
        }
    }

    public boolean isDay() {
        long timeOfDay = this.getTime() % 24000L;
        return timeOfDay >= 0 && timeOfDay < 13000;
    }

    public boolean isRaining() {
        return this.level.isRaining();
    }

    public boolean isClientSide() {
        return this.level.isClientSide();
    }

    @Override
    public Level unwrap() {
        return this.level;
    }
}
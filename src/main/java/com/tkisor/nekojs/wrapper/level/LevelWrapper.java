package com.tkisor.nekojs.wrapper.level;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import com.tkisor.nekojs.wrapper.block.BlockWrapper;
import com.tkisor.nekojs.wrapper.entity.EntityWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.Entity;
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
        return this.level.dimension().identifier().toString();
    }

    public BlockWrapper getBlock(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return new BlockWrapper(this.level, pos, this.level.getBlockState(pos));
    }

    public void setBlock(int x, int y, int z, Identifier blockId) {
        getBlock(x, y, z).set(blockId);
    }

    public String getBlockId(int x, int y, int z) {
        return getBlock(x, y, z).getId();
    }

    public EntityWrapper spawnEntity(Identifier entityId, double x, double y, double z) {
        if (!(this.level instanceof ServerLevel serverLevel)) return null;

        if (entityId == null) return null;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) return null;

        Entity entity = type.create(serverLevel, EntitySpawnReason.EVENT);
        if (entity != null) {
            entity.setPos(x, y, z);
            serverLevel.addFreshEntity(entity);
            return EntityWrapper.of(entity);
        }
        return null;
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

    public void explode(double x, double y, double z, float radius, boolean breakBlocks) {
        Level.ExplosionInteraction interaction = breakBlocks ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        this.level.explode(null, x, y, z, radius, interaction);
    }

    public void playSound(Identifier soundId, double x, double y, double z, float volume, float pitch) {
        if (soundId == null) return;

        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
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

    public boolean isBrightOutside() {
        return this.level.isBrightOutside();
    }

    public boolean isDarkOutside() {
        return this.level.isDarkOutside();
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
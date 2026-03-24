package com.tkisor.nekojs.wrapper.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class CommandContextJS {
    private final CommandContext<CommandSourceStack> rawContext;

    public CommandContextJS(CommandContext<CommandSourceStack> rawContext) {
        this.rawContext = rawContext;
    }

    public boolean isPlayer() {
        return rawContext.getSource().getEntity() instanceof ServerPlayer;
    }

    public PlayerJS getPlayer() {
        if (rawContext.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            return new PlayerJS(serverPlayer);
        }
        return null;
    }

    public void reply(Component message) {
        rawContext.getSource().sendSystemMessage(message);
    }

    public void error(Component message) {
        rawContext.getSource().sendFailure(message);
    }

    public double getX() {
        return rawContext.getSource().getPosition().x;
    }

    public double getY() {
        return rawContext.getSource().getPosition().y;
    }

    public double getZ() {
        return rawContext.getSource().getPosition().z;
    }

    public Vec3 getPosition() {
        return rawContext.getSource().getPosition();
    }

    public String getString(String name) {
        return StringArgumentType.getString(rawContext, name);
    }

    public int getInt(String name) {
        return IntegerArgumentType.getInteger(rawContext, name);
    }

    public float getFloat(String name) {
        return FloatArgumentType.getFloat(rawContext, name);
    }

    public double getDouble(String name) {
        return DoubleArgumentType.getDouble(rawContext, name);
    }

    public boolean getBool(String name) {
        return BoolArgumentType.getBool(rawContext, name);
    }

    public CommandContext<CommandSourceStack> unwrap() {
        return rawContext;
    }
}
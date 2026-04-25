package com.tkisor.nekojs.wrapper.network;

import com.tkisor.nekojs.network.NekoScriptPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
// 1.21.1 中统一使用 PacketDistributor
import net.neoforged.neoforge.network.PacketDistributor;

public class NetworkJS {

    /**
     * 【客户端脚本专用】发给服务端
     */
    public static void sendToServer(String channel, CompoundTag data) {
        // 1.21.1: 替换为 PacketDistributor.sendToServer
        PacketDistributor.sendToServer(new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }

    /**
     * 【服务端脚本专用】发给指定玩家
     */
    public static void sendToPlayer(ServerPlayer player, String channel, CompoundTag data) {
        PacketDistributor.sendToPlayer(player, new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }

    /**
     * 【服务端脚本专用】发给所有人
     */
    public static void sendToAll(String channel, CompoundTag data) {
        PacketDistributor.sendToAllPlayers(new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }
}
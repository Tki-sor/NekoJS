package com.tkisor.nekojs.wrapper.network;

import com.tkisor.nekojs.network.NekoScriptPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetworkJS {

    /**
     * 【客户端脚本专用】发给服务端
     */
    public void sendToServer(String channel, CompoundTag data) {
        ClientPacketDistributor.sendToServer(new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }

    /**
     * 【服务端脚本专用】发给指定玩家
     */
    public void sendToPlayer(ServerPlayer player, String channel, CompoundTag data) {
        // 服务端发包依然使用 PacketDistributor
        PacketDistributor.sendToPlayer(player, new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }

    /**
     * 【服务端脚本专用】发给所有人
     */
    public void sendToAll(String channel, CompoundTag data) {
        // 方法名在 1.21 也变成了明确的 sendToAllPlayers
        PacketDistributor.sendToAllPlayers(new NekoScriptPayload(channel, data != null ? data : new CompoundTag()));
    }
}
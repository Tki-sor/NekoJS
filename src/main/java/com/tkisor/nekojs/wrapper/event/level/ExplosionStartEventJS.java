package com.tkisor.nekojs.wrapper.event.level;

import net.neoforged.neoforge.event.level.ExplosionEvent;

public class ExplosionStartEventJS {
    private final ExplosionEvent.Start rawEvent;

    public ExplosionStartEventJS(ExplosionEvent.Start rawEvent) {
        this.rawEvent = rawEvent;
    }

    public double getX() {
        return rawEvent.getExplosion().center().x;
    }

    public double getY() {
        return rawEvent.getExplosion().center().y;
    }

    public double getZ() {
        return rawEvent.getExplosion().center().z;
    }

    /**
     * 取消爆炸
     * JS 侧调用: event.cancel()
     */
    public void cancel() {
        rawEvent.setCanceled(true);
    }
}
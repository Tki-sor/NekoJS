package com.tkisor.nekojs.js.type_adapter;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.api.JSTypeAdapter;
import net.minecraft.resources.ResourceLocation;
import graal.graalvm.polyglot.Value;

public class ResourceLocationAdapter implements JSTypeAdapter<ResourceLocation> {

    private static final String DEFAULT_NAMESPACE = NekoJS.MODID;

    @Override
    public Class<ResourceLocation> getTargetClass() {
        return ResourceLocation.class;
    }

    @Override
    public boolean canConvert(Value value) {
        return value.isString();
    }

    @Override
    public ResourceLocation convert(Value value) {
        String id = value.asString();

        if (id.contains(":")) {
            return ResourceLocation.parse(id);
        } else {
            return ResourceLocation.fromNamespaceAndPath(DEFAULT_NAMESPACE, id);
        }
    }
}
package io.github.kltyton.temple;

import net.fabricmc.api.ModInitializer;

public final class TempleFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TempleCommon.initialize();
    }
}

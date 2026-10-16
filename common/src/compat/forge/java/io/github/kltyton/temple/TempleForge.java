package io.github.kltyton.temple;

import net.minecraftforge.fml.common.Mod;

@Mod(BuildInfo.MOD_ID)
public final class TempleForge {
    public TempleForge() {
        TempleCommon.initialize();
    }
}

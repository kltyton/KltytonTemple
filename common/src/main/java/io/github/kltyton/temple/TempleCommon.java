package io.github.kltyton.temple;

import net.minecraft.SharedConstants;

public final class TempleCommon {
    private static final System.Logger LOGGER = System.getLogger(BuildInfo.MOD_ID);

    private TempleCommon() {
    }

    public static void initialize() {
        LOGGER.log(System.Logger.Level.INFO, "Initializing {0} for Minecraft {1}",
                BuildInfo.MOD_ID, SharedConstants.getCurrentVersion());
    }
}

package com.mrfdev.watersourcemod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/** Optional Mod Menu adapter. The core mod has no runtime dependency on it. */
public final class WaterSourceModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WaterSourceConfigScreen::new;
    }
}

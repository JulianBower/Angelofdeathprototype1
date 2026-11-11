package com.angelofdeath.mod;

import com.angelofdeath.mod.network.ModNetwork;
import com.angelofdeath.mod.registry.ModCreativeTabs;
import com.angelofdeath.mod.registry.ModDataComponents;
import com.angelofdeath.mod.registry.ModEntities;
import com.angelofdeath.mod.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AngelOfDeathMod.MOD_ID)
public class AngelOfDeathMod {
    public static final String MOD_ID = "angelofdeath";

    public AngelOfDeathMod(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

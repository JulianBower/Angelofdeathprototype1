package com.angelofdeath.mod.registry;

import com.angelofdeath.mod.AngelOfDeathMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AngelOfDeathMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.angelofdeath"))
                    .icon(() -> new ItemStack(ModItems.ANGEL_OF_DEATH.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.ANGEL_OF_DEATH.get()))
                    .build());

    private ModCreativeTabs() {
    }
}

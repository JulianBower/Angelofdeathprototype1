package com.angelofdeath.mod.registry;

import com.angelofdeath.mod.AngelOfDeathMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(AngelOfDeathMod.MOD_ID);

    /** false = Black Hole mode (default), true = White Hole mode. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_WHITE_HOLE =
            DATA_COMPONENTS.registerComponentType("is_white_hole",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {
    }
}

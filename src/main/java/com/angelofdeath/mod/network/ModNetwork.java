package com.angelofdeath.mod.network;

import com.angelofdeath.mod.item.AngelOfDeathWeaponItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(PlaceSingularityPayload.TYPE, PlaceSingularityPayload.STREAM_CODEC,
                ModNetwork::handlePlaceSingularity);
    }

    private static void handlePlaceSingularity(PlaceSingularityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AngelOfDeathWeaponItem.placeSingularity(player);
            }
        });
    }
}

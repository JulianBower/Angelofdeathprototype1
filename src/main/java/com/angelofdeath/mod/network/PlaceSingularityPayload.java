package com.angelofdeath.mod.network;

import com.angelofdeath.mod.AngelOfDeathMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: "the player left-clicked while channeling the weapon; place the singularity". */
public record PlaceSingularityPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlaceSingularityPayload> TYPE =
            new CustomPacketPayload.Type<>(AngelOfDeathMod.id("place_singularity"));

    public static final StreamCodec<ByteBuf, PlaceSingularityPayload> STREAM_CODEC =
            StreamCodec.unit(new PlaceSingularityPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

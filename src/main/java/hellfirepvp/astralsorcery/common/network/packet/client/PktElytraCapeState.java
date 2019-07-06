/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.network.packet.client;

import hellfirepvp.astralsorcery.common.network.base.ASPacket;
import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nonnull;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: PktElytraCapeState
 * Created by HellFirePvP
 * Date: 02.06.2019 / 13:44
 */
public class PktElytraCapeState extends ASPacket<PktElytraCapeState> {

    private byte type = 0;

    public PktElytraCapeState() {}

    private PktElytraCapeState(int type) {
        this.type = (byte) type;
    }

    public static PktElytraCapeState resetFallDistance() {
        return new PktElytraCapeState(0);
    }

    public static PktElytraCapeState setFlying() {
        return new PktElytraCapeState(1);
    }

    public static PktElytraCapeState resetFlying() {
        return new PktElytraCapeState(2);
    }

    @Nonnull
    @Override
    public Encoder<PktElytraCapeState> encoder() {
        return (packet, buffer) -> buffer.writeByte(packet.type);
    }

    @Nonnull
    @Override
    public Decoder<PktElytraCapeState> decoder() {
        return buffer -> new PktElytraCapeState(buffer.readByte());
    }

    @Nonnull
    @Override
    public Handler<PktElytraCapeState> handler() {
        return (packet, context, side) -> {
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                switch (packet.type) {
                    case 0: {
                        player.fallDistance = 0F;
                        break;
                    }
                    case 1: {
                        player.setElytraFlying();
                        break;
                    }
                    case 2: {
                        player.clearElytraFlying();
                        break;
                    }
                }
            });
        };
    }
}

package com.myachi.mcdonaldsmod.beacon;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A zero payload marker channel used purely for capability detection.
 *
 * <p>Sending a custom status effect to a client that does not know it disconnects
 * that client. The channel below is declared by every client running this mod, so
 * {@code ServerPlayNetworking.canSend(player, ...)} tells the server whether it is
 * safe to hand out the flight effect to that player.
 */
public final class BeaconNetwork {
	public static final CustomPayload.Id<BeaconPayload> BEACON_CAPABILITY_ID =
			new CustomPayload.Id<>(Identifier.of(McDonaldsMod.MOD_ID, "beacon_capability"));

	public record BeaconPayload() implements CustomPayload {
		public static final PacketCodec<RegistryByteBuf, BeaconPayload> CODEC =
				PacketCodec.unit(new BeaconPayload());

		@Override
		public Id<? extends CustomPayload> getId() {
			return BEACON_CAPABILITY_ID;
		}
	}

	private BeaconNetwork() {
	}

	public static void initialize() {
		PayloadTypeRegistry.playS2C().register(BEACON_CAPABILITY_ID, BeaconPayload.CODEC);
	}
}

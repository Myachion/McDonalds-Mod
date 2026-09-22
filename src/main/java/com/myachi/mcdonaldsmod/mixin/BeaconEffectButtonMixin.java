package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.beacon.BeaconEffectButtonAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the tier and column of an effect button to the layout code, without
 * needing an access widener for the package private inner class.
 */
@Mixin(targets = "net.minecraft.client.gui.screen.ingame.BeaconScreen$EffectButtonWidget")
public class BeaconEffectButtonMixin implements BeaconEffectButtonAccess {
	/** Tier index; the beacon level needed to unlock this row is derived from it. */
	@Shadow
	@Final
	protected int level;

	@Shadow
	@Final
	private boolean primary;

	@Override
	public int mcdonalds$getTier() {
		return this.level;
	}

	@Override
	public boolean mcdonalds$isPrimary() {
		return this.primary;
	}
}

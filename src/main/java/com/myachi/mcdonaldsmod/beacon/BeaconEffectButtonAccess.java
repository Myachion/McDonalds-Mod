package com.myachi.mcdonaldsmod.beacon;

/**
 * Duck interface mixed into {@code BeaconScreen$EffectButtonWidget} so the layout
 * code can read a button's tier without an access widener.
 */
public interface BeaconEffectButtonAccess {
	int mcdonalds$getTier();

	boolean mcdonalds$isPrimary();
}

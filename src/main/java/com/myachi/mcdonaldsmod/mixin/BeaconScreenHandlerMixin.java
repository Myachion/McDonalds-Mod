package com.myachi.mcdonaldsmod.mixin;

import net.minecraft.screen.BeaconScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Removes the beacon activation cost.
 *
 * <p>Big Beacons lets a beacon apply its powers without spending a netherite ingot,
 * so the payment slot is parked far off screen, is never treated as empty (which
 * would let shift clicking drop items into it) and always counts as paid.
 */
@Mixin(BeaconScreenHandler.class)
public class BeaconScreenHandlerMixin {
	/** Horizontal offset applied to the player inventory to match the taller window. */
	private static final int INVENTORY_X = 62;
	/** Vertical offset applied to the player inventory to match the taller window. */
	private static final int INVENTORY_Y = 144;
	/** Far enough away that the payment slot can never be seen or clicked. */
	private static final int OFF_SCREEN = 1000000;

	/** Parks the payment slot off screen. */
	@ModifyArgs(
			method = "<init>(ILnet/minecraft/inventory/Inventory;Lnet/minecraft/screen/PropertyDelegate;Lnet/minecraft/screen/ScreenHandlerContext;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/screen/BeaconScreenHandler$PaymentSlot;<init>(Lnet/minecraft/inventory/Inventory;III)V"
			)
	)
	private void hidePaymentSlot(Args args) {
		args.set(2, OFF_SCREEN);
	}

	/** Slides the player inventory down to make room for the taller window. */
	@ModifyArgs(
			method = "<init>(ILnet/minecraft/inventory/Inventory;Lnet/minecraft/screen/PropertyDelegate;Lnet/minecraft/screen/ScreenHandlerContext;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/screen/BeaconScreenHandler;addPlayerSlots(Lnet/minecraft/inventory/Inventory;II)V"
			)
	)
	private void movePlayerInventory(Args args) {
		args.set(1, INVENTORY_X);
		args.set(2, INVENTORY_Y);
	}

	/** Keeps shift clicking from dropping items into the hidden payment slot. */
	@Redirect(
			method = "quickMove",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/BeaconScreenHandler$PaymentSlot;hasStack()Z")
	)
	private boolean blockQuickMoveIntoPayment(BeaconScreenHandler.PaymentSlot slot) {
		return true;
	}

	/** Lets the chosen powers be applied without a payment item. */
	@Redirect(
			method = "setEffects",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/BeaconScreenHandler$PaymentSlot;hasStack()Z")
	)
	private boolean alwaysAllowApplyingEffects(BeaconScreenHandler.PaymentSlot slot) {
		return true;
	}

	/** Keeps the confirm button enabled. */
	@Inject(method = "hasPayment", at = @At("HEAD"), cancellable = true)
	private void alwaysPaid(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}
}

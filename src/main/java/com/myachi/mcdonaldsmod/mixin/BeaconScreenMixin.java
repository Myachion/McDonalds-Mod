package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.beacon.BeaconEffectButtonAccess;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Big Beacons beacon window.
 *
 * <p>The vanilla window only has room for four effect tiers. This mixin grows it
 * to 233px, swaps in a taller background and lays the effect buttons out as a
 * numbered list on the left plus separate secondary and tertiary panels on the
 * right, matching the original mod. The activation cost is removed separately in
 * {@link BeaconScreenHandlerMixin}.
 */
@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends HandledScreen<BeaconScreenHandler> {
	/** Taller window; vanilla is 219. */
	@Unique
	private static final int WINDOW_HEIGHT = 233;
	/** Left edge of the primary effect list. */
	@Unique
	private static final int PRIMARY_X = 22;
	/** Horizontal step between the two effect columns. */
	@Unique
	private static final int COLUMN_STEP = 24;
	/** Top edge of the first primary effect row. */
	@Unique
	private static final int PRIMARY_Y = 22;
	/** Vertical step between effect rows. */
	@Unique
	private static final int ROW_STEP = 25;
	/** Beacon level shown next to each primary row, in order. */
	@Unique
	private static final int[] PRIMARY_LEVELS = {1, 2, 3, 5, 6, 7, 8, 9};
	/** Horizontal centre of the primary effect list. */
	@Unique
	private static final int PRIMARY_LABEL_X = 16;
	/** Horizontal centre of the secondary level label. */
	@Unique
	private static final int SECONDARY_LABEL_X = 138;
	/** Vertical centre of the secondary level label. */
	@Unique
	private static final int SECONDARY_LABEL_Y = 54;
	/** Horizontal centre of the tertiary level label. */
	@Unique
	private static final int TERTIARY_LABEL_X = 149;
	/** Vertical centre of the tertiary level label. */
	@Unique
	private static final int TERTIARY_LABEL_Y = 108;
	/** Top left corner of the level III button. */
	@Unique
	private static final int TERTIARY_BUTTON_X = 157;
	/** Top edge of the level III button. */
	@Unique
	private static final int TERTIARY_BUTTON_Y = 100;
	/** Beacon level required for the level III power. */
	@Unique
	private static final int TERTIARY_REQUIREMENT = 10;

	@Shadow
	@Nullable
	RegistryEntry<StatusEffect> primaryEffect;

	@Shadow
	private <T extends ClickableWidget> void addButton(T button) {
	}

	protected BeaconScreenMixin(BeaconScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	/** Uses the taller background. */
	@Redirect(
			method = "<clinit>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/Identifier;ofVanilla(Ljava/lang/String;)Lnet/minecraft/util/Identifier;",
					ordinal = 0
			)
	)
	private static Identifier useTallerBackground(String path) {
		return Identifier.of(McDonaldsMod.MOD_ID, "textures/gui/container/beacon_big.png");
	}

	/** Grows the window so every tier fits. */
	@ModifyConstant(method = "<init>", constant = @Constant(intValue = 219))
	private int tallerWindow(int original) {
		return WINDOW_HEIGHT;
	}

	/** Covers all eight effect tiers instead of the vanilla three. */
	@ModifyConstant(method = "init", constant = @Constant(intValue = 2, ordinal = 0))
	private static int showEveryTier(int original) {
		return PRIMARY_LEVELS.length - 1;
	}

	/**
	 * Pushes the tiers above strength up by one level, so level 4 keeps its vanilla
	 * meaning (regeneration as the secondary power) and the new tiers sit at 5 to 9.
	 */
	@ModifyArg(
			method = "init",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screen/ingame/BeaconScreen$EffectButtonWidget;<init>(Lnet/minecraft/client/gui/screen/ingame/BeaconScreen;IILnet/minecraft/registry/entry/RegistryEntry;ZI)V",
					ordinal = 0
			),
			index = 5
	)
	private static int spreadTierLevels(int tier) {
		return tier >= 3 ? tier + 1 : tier;
	}

	/** Moves the confirm button into the small well under the primary list. */
	@ModifyConstant(method = "init", constant = @Constant(intValue = 164))
	private static int moveConfirmButton(int original) {
		return 61;
	}

	/** Moves the cancel button next to the confirm button. */
	@ModifyConstant(method = "init", constant = @Constant(intValue = 190))
	private static int moveCancelButton(int original) {
		return 87;
	}

	/** Lays the buttons out as a numbered list and adds the level III button. */
	@Inject(method = "init", at = @At("TAIL"))
	private void layOutBeaconPowers(CallbackInfo ci) {
		TreeMap<Integer, List<ClickableWidget>> rows = new TreeMap<>();

		for (Element element : this.children()) {
			if (element instanceof BeaconEffectButtonAccess button
					&& button.mcdonalds$isPrimary()
					&& element instanceof ClickableWidget widget) {
				rows.computeIfAbsent(button.mcdonalds$getTier(), key -> new ArrayList<>()).add(widget);
			}
		}

		for (var row : rows.entrySet()) {
			int index = requiredIndex(requiredLevel(row.getKey()));
			List<ClickableWidget> buttons = row.getValue();
			buttons.sort(Comparator.comparingInt(ClickableWidget::getX));

			for (int column = 0; column < buttons.size(); column++) {
				buttons.get(column).setX(this.x + PRIMARY_X + column * COLUMN_STEP);
				buttons.get(column).setY(this.y + PRIMARY_Y + index * ROW_STEP);
			}
		}

		addTertiaryButton();
	}

	/** Hides the payment item icons; activation is free. */
	@Redirect(
			method = "drawBackground",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;II)V")
	)
	private void hidePaymentIcons(DrawContext context, ItemStack stack, int x, int y) {
		// the taller background has no payment row, so the icons are skipped
	}

	/** Draws the tertiary title and the level number beside every row. */
	@Inject(method = "drawForeground", at = @At("TAIL"))
	private void drawLevelLabels(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
		context.drawCenteredTextWithShadow(this.textRenderer, TERTIARY_TITLE, TERTIARY_TITLE_X, TERTIARY_TITLE_Y, Colors.WHITE);

		for (int index = 0; index < PRIMARY_LEVELS.length; index++) {
			drawNumber(context, PRIMARY_LEVELS[index], PRIMARY_LABEL_X, PRIMARY_Y + 7 + index * ROW_STEP);
		}

		drawNumber(context, 4, SECONDARY_LABEL_X, SECONDARY_LABEL_Y);
		drawNumber(context, TERTIARY_REQUIREMENT, TERTIARY_LABEL_X, TERTIARY_LABEL_Y);
	}

	@Unique
	private void drawNumber(DrawContext context, int value, int x, int y) {
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Integer.toString(value)), x, y, Colors.WHITE);
	}

	/**
	 * Beacon level that unlocks a tier. Level 4 keeps its vanilla meaning, which is
	 * regeneration as the secondary power, so later primary tiers start at level 5.
	 */
	@Unique
	private static int requiredLevel(int tier) {
		// `tier` is the button's level field, already shifted by spreadTierLevels().
		return tier + 1;
	}

	/** Row index of a required beacon level. */
	@Unique
	private static int requiredIndex(int requiredLevel) {
		for (int index = 0; index < PRIMARY_LEVELS.length; index++) {
			if (PRIMARY_LEVELS[index] == requiredLevel) {
				return index;
			}
		}

		return 0;
	}

	/** Adds the level III button, which mirrors the primary power once level 10 is reached. */
	@Unique
	private void addTertiaryButton() {
		BeaconScreen screen = (BeaconScreen) (Object) this;
		RegistryEntry<StatusEffect> first = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(0).get(0);
		BeaconScreen.EffectButtonWidget button = screen.new EffectButtonWidget(
				this.x + TERTIARY_BUTTON_X, this.y + TERTIARY_BUTTON_Y, first, false, TERTIARY_REQUIREMENT - 1) {
			@Override
			protected MutableText getEffectName(RegistryEntry<StatusEffect> effect) {
				return Text.translatable(effect.value().getTranslationKey()).append(" III");
			}

			@Override
			public void tick(int level) {
				if (BeaconScreenMixin.this.primaryEffect != null) {
					this.visible = true;
					this.init(BeaconScreenMixin.this.primaryEffect);
					super.tick(level);
				} else {
					this.visible = false;
				}
			}
		};
		button.active = false;
		this.addButton(button);
	}

	/** Horizontal centre of the tertiary panel title. */
	@Unique
	private static final int TERTIARY_TITLE_X = 169;
	/** Vertical centre of the tertiary panel title. */
	@Unique
	private static final int TERTIARY_TITLE_Y = 84;

	/** Window relative title of the tertiary panel. */
	@Unique
	private static final Text TERTIARY_TITLE = Text.translatable("block.minecraft.beacon.tertiary");
}

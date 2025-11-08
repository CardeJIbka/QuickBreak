package cardejibka.quickbreak;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuickBreakClient implements ClientModInitializer {
	public static final String MOD_ID = "quickbreak";
	private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private static final int BREAK_DELAY_TICKS = 0;
	private int tickCounter = 0;
	private boolean isEnabled = true;

	private KeyBinding toggleKeyBinding;

	private static final KeyBinding.Category QUICKBREAK_CATEGORY =
			new KeyBinding.Category(Identifier.of("quickbreak", "quickbreak"));

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing QuickBreak Mod (Client)");

		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.quickbreak.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				QUICKBREAK_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKeyBinding.wasPressed()) {
				isEnabled = !isEnabled;
				LOGGER.info("QuickBreak {} (toggled by V key)", isEnabled ? "enabled" : "disabled");
			}

			if (!isEnabled || client.player == null || client.world == null || client.interactionManager == null) {
				resetTickCounter();
				return;
			}

			ClientPlayerEntity player = client.player;

			if (!player.getAbilities().creativeMode) {
				resetTickCounter();
				return;
			}

			if (client.options.attackKey.isPressed() && tickCounter <= 0) {
				HitResult hitResult = client.crosshairTarget;
				if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHitResult = (BlockHitResult) hitResult;
					BlockPos pos = blockHitResult.getBlockPos();
					Direction direction = blockHitResult.getSide();
					boolean result = client.interactionManager.attackBlock(pos, direction);
					if (result) {
						player.swingHand(Hand.MAIN_HAND);
						tickCounter = BREAK_DELAY_TICKS;

						LOGGER.debug("Broke block at {} from direction {} (cooldown started)", pos, direction);
					}
				}
			}

			if (tickCounter > 0) {
				tickCounter--;
			}
		});
	}

	private void resetTickCounter() {
		tickCounter = 0;
	}
}
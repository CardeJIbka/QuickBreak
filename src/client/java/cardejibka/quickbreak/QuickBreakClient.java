package cardejibka.quickbreak;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickBreakClient implements ClientModInitializer {

	public static final String MOD_ID = "quickbreak";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping toggleKeyBinding;
	private static boolean isEnabled = true;

	private BlockPos lastBreakingPos = null;
	private Direction lastBreakingDir = null;
	private boolean wasAttackingLastTick = false;

	@Override
	public void onInitializeClient() {
		toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.quickbreak.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		LOGGER.info("QuickBreak initialized | Toggle key: V");
	}

	private void onClientTick(Minecraft client) {
		while (toggleKeyBinding.consumeClick()) {
			isEnabled = !isEnabled;

			LOGGER.info("QuickBreak {}", isEnabled ? "enabled" : "disabled");
		}

		if (!isEnabled || client.player == null || client.level == null || client.gameMode == null
				|| !client.gameMode.getPlayerMode().isCreative()) {
			stopBreakingIfNeeded(client);
			return;
		}

		boolean isAttackingNow = client.options.keyAttack.isDown();
		HitResult hitResult = client.hitResult;

		boolean lookingAtBlock = hitResult != null && hitResult.getType() == HitResult.Type.BLOCK;

		if (isAttackingNow && lookingAtBlock) {
			BlockHitResult blockHitResult = (BlockHitResult) hitResult;
			BlockPos pos = blockHitResult.getBlockPos();
			Direction direction = blockHitResult.getDirection();

			LocalPlayer player = client.player;

			if (!pos.equals(lastBreakingPos)) {
				if (lastBreakingPos != null) {
					client.gameMode.stopDestroyBlock();
				}
				client.gameMode.startDestroyBlock(pos, direction);
				lastBreakingPos = pos;
				lastBreakingDir = direction;
			}

			boolean broken = client.gameMode.continueDestroyBlock(pos, direction);

			if (broken) {
				player.swing(InteractionHand.MAIN_HAND);

				client.gameMode.destroyBlock(pos);

				lastBreakingPos = null;
				lastBreakingDir = null;

				if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
					BlockHitResult nextHit = (BlockHitResult) client.hitResult;
					client.gameMode.startDestroyBlock(nextHit.getBlockPos(), nextHit.getDirection());
					lastBreakingPos = nextHit.getBlockPos();
					lastBreakingDir = nextHit.getDirection();
				}
			}

		} else {
			stopBreakingIfNeeded(client);
		}

		wasAttackingLastTick = isAttackingNow;
	}

	private void stopBreakingIfNeeded(Minecraft client) {
		if (lastBreakingPos != null && client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
			lastBreakingPos = null;
			lastBreakingDir = null;
		}
	}
}
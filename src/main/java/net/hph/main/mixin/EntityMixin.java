package net.hph.main.mixin;

import net.hph.main.HPH;
import net.hph.main.WhitelistManager;
import net.hph.main.config.HPHConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

	@Unique final HPHConfig config = HPHConfig.INSTANCE;

	@SuppressWarnings("DataFlowIssue") //complains about casting
    @Inject(method = "getTeamColorValue", at = @At(value = "HEAD"), cancellable = true)
	private void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
		if (HPHConfig.INSTANCE.enableGlow && isPlayer()) {

			PlayerEntity target = WhitelistManager.targeted;
			PlayerEntity current = (PlayerEntity) (Object) this;

			if (HPH.selectionKey.isPressed()) {
				if (target != null && getId() == target.getId())
					cir.setReturnValue(WhitelistManager.isWhitelisted(target) ? config.whitelistedTargetColour : config.targetColour);
				else
					cir.setReturnValue(WhitelistManager.isWhitelisted(current) ? config.hp100Colour : config.hp33Colour);
			}
			else if (config.enableWhitelistGlow) {
				if (WhitelistManager.isWhitelisted(current)) cir.setReturnValue(config.getColour(current));
			}
			else cir.setReturnValue(config.getColour(current));
		}
	}

	@Shadow public abstract boolean isPlayer();
	@Shadow public abstract int getId();
}

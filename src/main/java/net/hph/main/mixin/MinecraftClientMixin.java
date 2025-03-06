package net.hph.main.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.hph.main.HPH;
import net.hph.main.WhitelistManager;
import net.hph.main.config.HPHConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "doAttack", at = @At(value = "HEAD"))
	private void doAttack(CallbackInfoReturnable<Boolean> cir) {
		if (HPH.selectionKey.isPressed()) WhitelistManager.onLeftClick();
	}

	@Inject(method = "hasOutline", at = @At(value = "HEAD"), cancellable = true)
	private void hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (HPHConfig.INSTANCE.enableGlow && entity.isPlayer()) {
			if (!FabricLoader.getInstance().isModLoaded("grosshacks")) {
				if (WhitelistManager.shouldForceGlow(entity)) cir.setReturnValue(true);
			}
		}
	}
}

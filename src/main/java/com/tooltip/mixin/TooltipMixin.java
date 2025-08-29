package com.tooltip.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class TooltipMixin {

	/**
	 * Отменяем рендеринг ванильного тултипа для предмета в руке
	 */
	@Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
	private void cancelVanillaTooltip(DrawContext context, CallbackInfo ci) {
		ci.cancel();
	}
}
package com.tooltip.mixin;

import com.tooltip.render.TooltipRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

// 1.21.1: render(DrawContext, int tickDelta, int mouseX, int mouseY, boolean focused)
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("HEAD"))
    private void tooltip$renderUnderChat(DrawContext context, int tickDelta, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        TooltipRenderer inst = TooltipRenderer.INSTANCE;
        if (inst != null) {
            inst.renderUnderChat(context); // тултип рендерится ПОД чатом
        }
    }
}
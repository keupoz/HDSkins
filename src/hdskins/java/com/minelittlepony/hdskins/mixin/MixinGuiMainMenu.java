package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.common.client.gui.IconicButton;
import com.minelittlepony.hdskins.HDSkinManager;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu extends GuiScreen {

    @Inject(method = "initGui()V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        addButton(new IconicButton(width - 50, height - 50, sender -> {
            mc.displayGuiScreen(HDSkinManager.INSTANCE.createSkinsGui());
        }).setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb));
    }
}

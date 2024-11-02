package dev.wolfieboy09.qstorage.block.storage_matrix;

import dev.wolfieboy09.qstorage.api.util.ResourceHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class StorageMatrixScreen extends AbstractContainerScreen<StorageMatrixMenu> {
    private static final ResourceLocation BACKGROUND_LOCATION = ResourceHelper.asResource("textures/gui/storage_matrix.png");

    public StorageMatrixScreen(StorageMatrixMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

    }
}

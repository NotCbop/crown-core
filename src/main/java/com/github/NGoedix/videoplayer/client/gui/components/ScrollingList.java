package com.github.NGoedix.videoplayer.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ScrollingList<E extends AbstractSelectionList.Entry<E>> extends AbstractSelectionList<E> {

    public ScrollingList(int x, int y, int width, int height, int slotHeightIn) {
        super(Minecraft.getInstance(), width, height, y - (height / 2), slotHeightIn);
        this.setX(x - (width / 2));
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.disableScissor();
    }

    @Override
    protected int scrollBarX() {
        return (this.getX() + this.width) - 6;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}

package com.github.NGoedix.videoplayer.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButtonHoverable extends Button {

    private final Identifier resourceLocation;
    private final Identifier hoverResourceLocation;
    private final int xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;

    public ImageButtonHoverable(int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, Identifier pResourceLocation, Identifier pHoverLocation, int pTextureWidth, int pTextureHeight, OnPress pOnPress) {
        this(pX, pY, pWidth, pHeight, pXTexStart, pYTexStart, pYDiffTex, pResourceLocation, pHoverLocation, pTextureWidth, pTextureHeight, pOnPress, Component.literal(""));
    }

    public ImageButtonHoverable(int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, Identifier pResourceLocation, Identifier pHoverLocation, int pTextureWidth, int pTextureHeight, OnPress pOnPress, Component pMessage) {
        this(pX, pY, pWidth, pHeight, pXTexStart, pYTexStart, pYDiffTex, pResourceLocation, pHoverLocation, pTextureWidth, pTextureHeight, pOnPress, Button.DEFAULT_NARRATION, pMessage);
    }

    public ImageButtonHoverable(int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, Identifier pResourceLocation, Identifier pHoverLocation, int pTextureWidth, int pTextureHeight, OnPress pOnPress, CreateNarration pOnTooltip, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
        this.textureWidth = pTextureWidth;
        this.textureHeight = pTextureHeight;
        this.xTexStart = pXTexStart;
        this.yTexStart = pYTexStart;
        this.yDiffTex = pYDiffTex;
        this.resourceLocation = pResourceLocation;
        this.hoverResourceLocation = pHoverLocation;
    }

    @Override
    protected void renderContents(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Identifier texture = getTextureLocation();
        if (texture != null) {
            int i = this.yTexStart;
            if (this.isHovered()) {
                i += this.yDiffTex;
            }

            pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.getX(), this.getY(), (float) this.xTexStart, (float) i, this.width, this.height, this.textureWidth, this.textureHeight);
        }
    }

    public Identifier getTextureLocation() {
        return isHovered() ? this.hoverResourceLocation : this.resourceLocation;
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}

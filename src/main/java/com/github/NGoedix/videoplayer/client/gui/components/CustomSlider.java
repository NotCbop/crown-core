package com.github.NGoedix.videoplayer.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CustomSlider extends AbstractSliderButton {

    private static final Identifier SLIDER_SPRITE = Identifier.withDefaultNamespace("widget/slider");
    private static final Identifier SLIDER_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_highlighted");
    private static final Identifier SLIDER_HANDLE_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle");
    private static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");

    public interface OnSlide {
        void onSlide(double value);
    }

    private final Component text;
    private final boolean progressBar;
    private OnSlide onSlideListener;

    private boolean active;

    public CustomSlider(int x, int y, int width, int height, Component text, double defaultValue, boolean progressBar) {
        super(x, y, width, height, text == null ? Component.literal("") : text, defaultValue);
        this.text = text;
        this.progressBar = progressBar;
        this.active = true;
        updateMessage();
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setOnSlideListener(OnSlide onSlide) {
        this.onSlideListener = onSlide;
    }

    @Override
    protected void updateMessage() {
        if (text != null) {
            String formattedValue = String.format("%d", (int) (value * 100f));
            setMessage(Component.translatable("customslider.videoplayer.value", this.text, formattedValue));
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font fontrenderer = minecraft.font;

        boolean highlighted = this.isHovered() || this.isFocused();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, highlighted ? SLIDER_HIGHLIGHTED_SPRITE : SLIDER_SPRITE, this.getX(), this.getY(), this.width, this.height);

        if (progressBar) {
            int progressBarWidth = (int) (this.width * this.value);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + progressBarWidth, this.getY() + this.height, 0x3300FF00);
        }

        Identifier handle = highlighted ? SLIDER_HANDLE_HIGHLIGHTED_SPRITE : SLIDER_HANDLE_SPRITE;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, handle, this.getX() + (int) (this.value * (double) (this.width - 8)), this.getY(), 8, this.height);

        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        guiGraphics.drawCenteredString(fontrenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, j);
    }

    @Override
    protected void applyValue() {
        if (onSlideListener != null)
            onSlideListener.onSlide(value * 100f);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo pButton) {
        if (!active) return false;

        return super.isValidClickButton(pButton);
    }

    public double getValue() {
        return value * 100f;
    }
}

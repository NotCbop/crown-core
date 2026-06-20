package com.github.NGoedix.videoplayer.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.util.List;

public class ScrollingStringList extends ScrollingList<ScrollingStringList.PlayerSlot> {
    private static final int SLOT_HEIGHT = 30;

    public interface PlayerSlotClickListener {
        void onClick(String text);
    }

    private PlayerSlotClickListener playerSlotClickListener;

    public ScrollingStringList(int x, int y, int width, int height, List<String> text) {
        super(x, y, width, height, SLOT_HEIGHT);
        this.updateEntries(text);
    }

    public void setPlayerSlotClickListener(PlayerSlotClickListener playerSlotClickListener) {
        this.playerSlotClickListener = playerSlotClickListener;
    }

    public String getSelectedText() {
        return this.getSelected().getText();
    }

    public void setSelected(String entry) {
        for (int i = 0; i < this.children().size(); i++) {
            PlayerSlot slot = this.children().get(i);
            if (slot.getText().equals(entry)) {
                this.setSelected(slot);
                break;
            }
        }
    }

    @Override
    public void setSelected(PlayerSlot entry) {
        super.setSelected(entry);
        if (entry != null && this.playerSlotClickListener != null) {
            this.playerSlotClickListener.onClick(entry.getText());
        }
    }

    public void updateEntries(List<String> texts) {
        this.clearEntries();
        texts.forEach(text -> this.addEntry(new PlayerSlot(text, this)));
    }

    public class PlayerSlot extends Entry<PlayerSlot> {

        private final String text;
        private final ScrollingStringList parent;

        PlayerSlot(String text, ScrollingStringList parent) {
            this.text = text;
            this.parent = parent;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
            this.parent.setSelected(this);
            return false;
        }

        @Override
        public void renderContent(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, boolean hovered, float pPartialTick) {
            Font font = Minecraft.getInstance().font;

            int left = getContentX();
            int top = getContentY();
            int width = getContentWidth();
            int height = getContentHeight();

            pGuiGraphics.fillGradient(left, top, left + width, top + height, -435154928, -435154928);

            if (hovered) {
                pGuiGraphics.fillGradient(left, top, left + width, top + height, -1072689136, -804253680);
            }

            pGuiGraphics.drawString(font, this.text, left + 65, top + 10, Color.WHITE.getRGB());
        }
    }
}

package cn.ussshenzhou.tellmewhere.gui;

import cn.ussshenzhou.t88.gui.event.ClearEditBoxFocusEvent;
import cn.ussshenzhou.t88.gui.widegt.TEditBox;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import cn.ussshenzhou.t88.mixin.EditBoxAccessor;
import cn.ussshenzhou.tellmewhere.SignText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;

/**
 * @author USS_Shenzhou
 */
public class SignContentPanel extends TPanel {
    private ArrayList<SignText.BakedText> bakedTextList;
    private int totalLength = 0;
    private String rawText;

    protected TEditBox editBox = new TEditBox() {
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (checkNeedToKeepFocused(event.x(), event.y())) {
                return false;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        protected void onClearEditBoxFocusEvent(ClearEditBoxFocusEvent event) {
            if (checkNeedToKeepFocused(event.mouseX, event.mouseY)) {
                return;
            }
            super.onClearEditBoxFocusEvent(event);
        }

        private boolean checkNeedToKeepFocused(double pMouseX, double pMouseY) {
            if (!isInRange(pMouseX, pMouseY) && this.isFocused()) {
                //prevent losing focus when click images
                var screen = (SignEditScreen) SignContentPanel.this.getTopParentScreen();
                //noinspection RedundantIfStatement
                if (screen.imageSelector.isInRange(pMouseX, pMouseY)) {
                    return true;
                }
            }
            return false;
        }
    };

    public SignContentPanel(String rawText) {
        this.setRawText(rawText);
        this.add(editBox);
        editBox.setValue(rawText);
        ((EditBoxAccessor) editBox).setDisplayPos(0);
        editBox.addResponder(this::setRawText);
    }

    public boolean tryInsertImage(String raw) {
        if (editBox.isFocused()) {
            editBox.insertText(raw);
            return true;
        }
        return false;
    }

    @Override
    public void layout() {
        editBox.setBounds((int) (width * 0.2), height - 20 - 4, (int) (width * 0.6), 20);
        super.layout();
    }

    private void setRawText(String rawText) {
        this.rawText = rawText;
        bakeText();
    }

    private void bakeText() {
        //"左侧乘车&@04test"
        bakedTextList = SignText.bakeTexts(rawText);
        totalLength = 0;
        bakedTextList.forEach(bakedText -> totalLength += bakedText.getLength());
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
        var screen = (SignEditScreen) this.getTopParentScreen();
        var metaPanel = (MetaPanel) screen.multiLanguageContainer.getTabs().stream().filter(t -> t.getContent() instanceof MetaPanel).findFirst().get().getContent();
        int fore = metaPanel.getForegroundColor();
        int back = metaPanel.getBackgroundColor();
        var pose = graphics.pose();
        pose.pushMatrix();
        float scale = 2f;
        pose.translate(this.getXT() + (this.width - totalLength * scale) / 2f, this.getYT() + (this.height - 24) / 2f);
        pose.scale(scale, scale);
        graphics.fill(0, -7, totalLength, 7, back);
        for (SignText.BakedText text : bakedTextList) {
            text.renderInGui(graphics, fore);
            pose.translate(text.getLength(), 0);
        }
        pose.popMatrix();
    }

}

package net.archasmiel.thaumcraft.screen.arcane_workbench;

import com.mojang.blaze3d.systems.RenderSystem;
import net.archasmiel.thaumcraft.Thaumcraft;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ArcaneWorkbenchScreen extends HandledScreen<ArcaneWorkbenchScreenHandler> {

    private static final Identifier BACK =
            new Identifier(Thaumcraft.MOD_ID, "textures/gui/background.png");
    private static final Identifier BACKGROUND =
            new Identifier(Thaumcraft.MOD_ID, "textures/gui/arcane_workbench.png");
    private static final int sizeX = 200, sizeY = 240;


    public ArcaneWorkbenchScreen(ArcaneWorkbenchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, new LiteralText(""));

        // hitbox of gui
        backgroundWidth = sizeX;
        backgroundHeight = sizeY;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, BACK);
        drawTexture(matrices, 0, 0, 0, 0, width, height);

        RenderSystem.setShaderTexture(0, BACKGROUND);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // renders unneeded titles
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }
}

package net.hph.main;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import net.hph.main.config.HPHConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.awt.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextDisplay extends HudElement {

    public static final TextDisplay INSTANCE = new TextDisplay();

    private static final List<TextLine> lines = new ArrayList<>();
    private static final HPHConfig config = HPHConfig.INSTANCE;
    private static final StringBuilder sb = new StringBuilder();
    private static final NumberFormat formatter = NumberFormat.getInstance();

    private static int width = 150;
    private static int height = 12;

    private double dragXRelative;
    private double dragYRelative;
    private double dragXAbsolute;
    private double dragYAbsolute;

    public static Style saturationStyle = Style.EMPTY.withColor(config.saturationColour);

    public static void init() {
        formatter.setMaximumFractionDigits(1);
        formatter.setMinimumFractionDigits(1);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override
    protected void render(DrawContext context, RenderTickCounter tickCounter) {
        if (lines.isEmpty()) return;

        int y = 0;
        float scale = config.scale;

        context.getMatrices().scale(scale, scale, scale);
        for (TextLine line : lines) {
            context.drawText(MinecraftClient.getInstance().textRenderer, line.text, line.pos, y, 0, config.shadow);
            y += 12;
        }
    }

    @Override
    public void renderTooltip(Screen screen, DrawContext drawContext, int mouseX, int mouseY) {
        if (!dragging) drawContext.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.of("Hold ctrl to move"), mouseX, mouseY);
    }

    public static void updateTexts() {
        lines.clear();
        MinecraftClient client = MinecraftClient.getInstance();

        for (PlayerEntity player : getPlayers(client)) {
            float current = player.getHealth();
            float max = player.getMaxHealth();
            float absorption = player.getAbsorptionAmount();

            Style currentStyle = Style.EMPTY.withColor(config.getColour(current / max));

            sb.setLength(0);
            sb.append(player.getName().getString()).append(" ").append(numDisplay(current)).append("/").append(numDisplay(max));
            if (absorption > 0) {
                MutableText text = Text.literal(sb.toString()).setStyle(currentStyle);
                sb.setLength(0);
                sb.append(" +").append(numDisplay(absorption));
                text.append(Text.literal(sb.toString()).setStyle(saturationStyle)).append(Text.of(" ❤"));
                lines.add(new TextLine(text, 0));
            }
            else {
                sb.append(" ❤");
                lines.add(new TextLine(Text.literal(sb.toString()).setStyle(Style.EMPTY.withColor(config.getColour(current / max))), 0));
            }
        }
        width = (int) (150 * config.scale);
        height = (int) (lines.size() * 12 * config.scale);
    }

    public static void updateTextsAligned() {
        lines.clear();
        MinecraftClient client = MinecraftClient.getInstance();

        int maxLength = 0;
        for (PlayerEntity player : getPlayers(client)) {
            float current = player.getHealth();
            float max = player.getMaxHealth();
            float absorption = player.getAbsorptionAmount();

            Style currentStyle = Style.EMPTY.withColor(config.getColour(current / max));

            sb.setLength(0);
            sb.append(player.getName().getString()).append(" ").append(numDisplay(current)).append("/").append(numDisplay(max));
            MutableText text;
            if (absorption > 0) {
                text = Text.literal(sb.toString()).setStyle(currentStyle);
                sb.setLength(0);
                sb.append(" +").append(numDisplay(absorption));
                text.append(Text.literal(sb.toString()).setStyle(saturationStyle)).append(Text.of(" ❤"));
            }
            else {
                sb.append(" ❤");
                text = Text.literal(sb.toString()).setStyle(Style.EMPTY.withColor(config.getColour(current / max)));
            }
            int length = client.textRenderer.getWidth(text);
            if (length > maxLength) maxLength = length;
            lines.add(new TextLine(text, length));
        }
        width = (int) (maxLength * config.scale);
        height = (int) (lines.size() * 12 * config.scale);
        for (TextLine line : lines) line.pos = maxLength - line.pos;
    }

    public static List<PlayerEntity> getPlayers(MinecraftClient client) {
        if (client.world == null || client.player == null) return List.of();

        int clientID = client.player.getId();
        List<PlayerEntity> players = new ArrayList<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (players.size() == config.maxLineCount && !config.sort) break;
            if (shouldDisplay(player, clientID)) players.add(player);
        }
        if (config.sort) players.sort((p1, p2) -> {
            float p1Current = p1.getHealth();
            float p1Max = p1.getMaxHealth();
            float p2Current = p2.getHealth();
            float p2Max = p2.getMaxHealth();

            if (p1Current == p1Max && p2Current == p2Max) return Float.compare(p1Max, p2Max);
            return Float.compare(p1Current / p1Max, p2Current / p2Max);
        });
        if (players.size() > config.maxLineCount) return players.subList(0, config.maxLineCount);
        return players;
    }

    private static boolean shouldDisplay(PlayerEntity player, int clientID) {
        if (player.getId() == clientID) return false;
        if (config.enableWhitelistText && !WhitelistManager.isWhitelisted(player)) return false;
        if (!config.displayTextOnFullHP && player.getHealth() >= player.getMaxHealth()) return false;
        return true;
    }

    private static String numDisplay(float f) {
        return config.showFraction ? formatter.format(f) : String.valueOf((int) f);
    }

    @Override
    protected boolean isEnabled() {
        return config.enableText;
    }

    @Override
    protected boolean isVisible() {
        return true;
    }

    @Override
    protected int getWidth() {
        return width;
    }

    @Override
    protected int getHeight() {
        return height;
    }

    @Override
    protected ElementPosition getPosition() {
        return config.textPosition;
    }

    @Override
    protected int getZOffset() {
        return 1;
    }

    @Override
    public Rectangle getDimension() {
        ElementPosition position = getPosition();
        int width = getWidth();
        int height = getHeight();
        int x = Math.round((float) client.getWindow().getScaledWidth() * position.offsetXRelative + (float) position.offsetXAbsolute - position.alignX * (float) width);
        int y = Math.round((float) client.getWindow().getScaledHeight() * position.offsetYRelative + (float) position.offsetYAbsolute);
        if (config.effectPadding && !(client.currentScreen instanceof ChatScreen)) {
            boolean usePadding = false;
            boolean hasNegative = false;
            for (StatusEffectInstance effect : Objects.requireNonNull(client.player).getStatusEffects()) {
                if (effect.shouldShowIcon()) {
                    usePadding = true;
                    if (!effect.getEffectType().value().isBeneficial()) hasNegative = true;
                }
            }
            if (usePadding) y += hasNegative ? config.effectPaddingSize * 2 : config.effectPaddingSize;
        }
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging) return false;

        Rectangle dimension = getDimension();
        ElementPosition position = getPosition();
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        double newX = mouseX + (double) dimension.x - dragXRelative;
        double newY = mouseY + (double) dimension.y - dragYRelative;

        double horizontalMiddle = Math.abs(newX + (double) dimension.width / (double) 2.0F - (double) scaledWidth / (double) 2.0F);
        double right = (double) scaledWidth - (newX + (double) dimension.width);
        double verticalMiddle = Math.abs(newY + (double) dimension.height / (double) 2.0F - (double) scaledHeight / (double) 2.0F);
        double bottom = (double) scaledHeight - (newY + (double) dimension.height);
        position.offsetXRelative = newX < horizontalMiddle && newX < right ? 0.0F : (horizontalMiddle < right ? 0.5F : 1.0F);
        position.offsetYRelative = newY < verticalMiddle && newY < bottom ? 0.0F : (verticalMiddle < bottom ? 0.5F : 1.0F);
        position.alignX = position.offsetXRelative;
        position.alignY = 0;
        position.offsetXAbsolute = (int) Math.round(newX - (double) ((float) scaledWidth * position.offsetXRelative) + (double) (position.alignX * (float) dimension.width));
        position.offsetYAbsolute = (int) Math.round(newY - (double) ((float) scaledHeight * position.offsetYRelative));
        if (!Screen.hasAltDown()) {
            if (position.offsetXRelative == 0.5F && Math.abs(position.offsetXAbsolute) < 10) {
                position.offsetXAbsolute = 0;
            }

            if (position.offsetYRelative == 0.5F && Math.abs(position.offsetYAbsolute) < 10) {
                position.offsetYAbsolute = 0;
            }
        }
        return true;
    }

    static class TextLine {
        Text text;
        int pos;

        TextLine(Text text, int pos) {
            this.text = text;
            this.pos = pos;
        }
    }
}

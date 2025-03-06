package net.hph.main;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import net.hph.main.config.HPHConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextDisplay extends HudElement {

    public static final TextDisplay INSTANCE = new TextDisplay();

    private static final List<MutableText> lines = new ArrayList<>();
    private static final HPHConfig config = HPHConfig.INSTANCE;
    private static final StringBuilder sb = new StringBuilder();

    public static Style saturationStyle = Style.EMPTY.withColor(config.saturationColour);

    @Override
    protected void render(DrawContext context, float delta) {
        if (lines.isEmpty()) return;

        int y = 0;
        float scale = config.scale;

        context.getMatrices().scale(scale, scale, scale);
        for (Text text : lines) {
            context.drawText(MinecraftClient.getInstance().textRenderer, text, 0, y, 0, config.shadow);
            y += 12;
        }
    }

    @Override
    public void renderTooltip(Screen screen, DrawContext drawContext, int mouseX, int mouseY) {
        drawContext.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.of("Hold ctrl to move"), mouseX, mouseY);
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
            sb.append(player.getName().getString()).append(" ").append((int) current).append("/").append((int) max);
            if (absorption > 0) {
                MutableText text = Text.literal(sb.toString()).setStyle(currentStyle);
                sb.setLength(0);
                sb.append(" +").append((int) absorption);
                text.append(Text.literal(sb.toString()).setStyle(saturationStyle)).append(Text.of(" ❤"));
                lines.add(text);
            }
            else {
                sb.append(" ❤");
                lines.add(Text.literal(sb.toString()).setStyle(Style.EMPTY.withColor(config.getColour(current / max))));
            }
        }
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
        return (int) (150 * config.scale);
    }

    @Override
    protected int getHeight() {
        return (int) (lines.size() * 12 * config.scale);
    }

    @Override
    protected ElementPosition getPosition() {
        return config.textPosition;
    }

    @Override
    protected int getZOffset() {
        return 1;
    }
}

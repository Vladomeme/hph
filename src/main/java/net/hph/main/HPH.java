package net.hph.main;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.hph.main.config.HPHConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HPH implements ClientModInitializer {

    List<MutableText> texts = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (HPHConfig.INSTANCE.enabled) render(drawContext);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (HPHConfig.INSTANCE.enabled) updateTexts();
        });
    }

    StringBuilder sb = new StringBuilder();

    private void updateTexts() {
        texts.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getUuid() == client.player.getUuid()) continue;
            float current = player.getHealth();
            float max = player.getMaxHealth();

            sb.setLength(0);
            sb.append(player.getName().getString()).append(" ").append((int) current).append("/").append((int) max);
            texts.add(Text.literal(sb.toString()).setStyle(Style.EMPTY.withColor(getColor(current / max))));
            if (player.getAbsorptionAmount() > 0) {
                sb.setLength(0);
                sb.append(" +").append((int) player.getAbsorptionAmount()).append(" ❤");
                texts.get(texts.size() - 1).append(Text.literal(sb.toString()).setStyle(Style.EMPTY.withColor(HPHConfig.INSTANCE.saturationColor)));
            }
        }
    }

    private void render(DrawContext context) {
        int x = (int) (HPHConfig.INSTANCE.x * context.getScaledWindowWidth());
        int y = (int) (HPHConfig.INSTANCE.y * context.getScaledWindowHeight());
        float scale = HPHConfig.INSTANCE.scale;

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, scale);
        for (Text text : texts) {
            context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, 0, HPHConfig.INSTANCE.shadow);
            y += 12;
        }
        context.getMatrices().pop();
    }

    int getColor(float percent) {
        if (percent <= 0.33) return HPHConfig.INSTANCE.hp33Color;
        else if (percent <= 0.66) return HPHConfig.INSTANCE.hp66Color;
        else return HPHConfig.INSTANCE.hp100Color;
    }
}

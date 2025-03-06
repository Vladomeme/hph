package net.hph.main;

import ch.njol.minecraft.uiframework.hud.Hud;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.hph.main.config.HPHConfig;
import net.hph.main.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HPH implements ClientModInitializer {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final HPHConfig config = HPHConfig.INSTANCE;

    public static KeyBinding toggleTextKey;
    public static KeyBinding toggleGlowKey;
    public static KeyBinding selectionKey;
    public static KeyBinding toggleTextWhitelistKey;
    public static KeyBinding toggleGlowWhitelistKey;

    @SuppressWarnings("NoTranslation")
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (config.enableText) TextDisplay.INSTANCE.renderAbsolute(context, tickDelta);
        });
        Hud.INSTANCE.addElement(TextDisplay.INSTANCE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (config.enableText) TextDisplay.updateTexts();
            tickKeybinds();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.write());

        toggleTextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle text display", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        toggleGlowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle health glowing", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        selectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Whitelist edit activator", GLFW.GLFW_KEY_G, "HPH"));
        toggleTextWhitelistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle text whitelist", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        toggleGlowWhitelistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle glow whitelist", GLFW.GLFW_KEY_UNKNOWN, "HPH"));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("hph")
                        .then(literal("add")
                                .then(argument("Player", StringArgumentType.string())
                                        .suggests(WhitelistManager::getSuggestionsOnAdd)
                                        .executes(WhitelistManager::add)))
                        .then(literal("remove")
                                .then(argument("Player", StringArgumentType.string())
                                        .suggests(WhitelistManager::getSuggestionsOnRemove)
                                        .executes(WhitelistManager::remove)))
                        .then(literal("clear")
                                        .executes((ctx) -> WhitelistManager.clear()))
                )
        );
    }

    private void tickKeybinds() {
        if (client.player != null && selectionKey.isPressed()) WhitelistManager.updateTargeted();
        else WhitelistManager.targeted = null;

        if (toggleTextKey.wasPressed()) {
            ((KeyBindingAccessor) toggleTextKey).reset();
            onToggle(config.enableText ? "§eDisabled text health display." : "§eEnabled text health display.");
            config.enableText = !config.enableText;
        }
        if (toggleGlowKey.wasPressed()) {
            ((KeyBindingAccessor) toggleGlowKey).reset();
            onToggle(config.enableGlow ? "§eDisabled health glowing." : "§eEnabled health glowing.");
            config.enableGlow = !config.enableGlow;
        }
        if (toggleTextWhitelistKey.wasPressed()) {
            ((KeyBindingAccessor) toggleTextWhitelistKey).reset();
            onToggle(config.enableWhitelistText ? "§eDisabled text whitelist." : "§eEnabled text whitelist.");
            config.enableWhitelistText = !config.enableWhitelistText;
        }
        if (toggleGlowWhitelistKey.wasPressed()) {
            ((KeyBindingAccessor) toggleGlowWhitelistKey).reset();
            onToggle(config.enableWhitelistGlow ? "§eDisabled glow whitelist." : "§eEnabled glow whitelist.");
            config.enableWhitelistGlow = !config.enableWhitelistGlow;
        }
    }

    @SuppressWarnings("DataFlowIssue") //complains about world & player
    public void onToggle(String message) {
        client.inGameHud.setOverlayMessage(Text.of(message), false);
        PlayerEntity player = client.player;
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundCategory.PLAYERS, 0.5f, 1.0f, false);
    }
}

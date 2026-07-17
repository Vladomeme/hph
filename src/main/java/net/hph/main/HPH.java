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
        TextDisplay.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (config.enableText) {
                if (config.rightAlignment) TextDisplay.updateTextsAligned();
                else TextDisplay.updateTexts();
            }
            tickKeybinds();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.write());

        toggleTextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("hph.keybind.toggleText", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        toggleGlowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("hph.keybind.toggleGlow", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        selectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("hph.keybind.selection", GLFW.GLFW_KEY_G, "HPH"));
        toggleTextWhitelistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("hph.keybind.toggleTextWhitelist", GLFW.GLFW_KEY_UNKNOWN, "HPH"));
        toggleGlowWhitelistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("hph.keybind.toggleGlowWhitelist", GLFW.GLFW_KEY_UNKNOWN, "HPH"));

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
            ((KeyBindingAccessor) toggleTextKey).callReset();
            onToggle(config.enableText ? Text.translatable("hph.toggle.text.disabled") : Text.translatable("hph.toggle.text.enabled"));
            config.enableText = !config.enableText;
        }
        if (toggleGlowKey.wasPressed()) {
            ((KeyBindingAccessor) toggleGlowKey).callReset();
            onToggle(config.enableGlow ? Text.translatable("hph.toggle.glow.disabled") : Text.translatable("hph.toggle.glow.enabled"));
            config.enableGlow = !config.enableGlow;
        }
        if (toggleTextWhitelistKey.wasPressed()) {
            ((KeyBindingAccessor) toggleTextWhitelistKey).callReset();
            onToggle(config.enableWhitelistText ? Text.translatable("hph.toggle.textWhitelist.disabled") : Text.translatable("hph.toggle.textWhitelist.enabled"));
            config.enableWhitelistText = !config.enableWhitelistText;
        }
        if (toggleGlowWhitelistKey.wasPressed()) {
            ((KeyBindingAccessor) toggleGlowWhitelistKey).callReset();
            onToggle(config.enableWhitelistGlow ? Text.translatable("hph.toggle.glowWhitelist.disabled") : Text.translatable("hph.toggle.glowWhitelist.enabled"));
            config.enableWhitelistGlow = !config.enableWhitelistGlow;
        }
    }

    @SuppressWarnings("DataFlowIssue") //complains about world & player
    public void onToggle(Text message) {
        client.inGameHud.setOverlayMessage(message, false);
        PlayerEntity player = client.player;
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundCategory.PLAYERS, 0.5f, 1.0f, false);
    }
}

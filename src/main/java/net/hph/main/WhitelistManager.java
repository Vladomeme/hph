package net.hph.main;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.hph.main.config.HPHConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WhitelistManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    public static final Set<String> whitelist = new HashSet<>();
    public static PlayerEntity targeted = null;

    public static void updateTargeted() {
        ClientPlayerEntity player = Objects.requireNonNull(client.player);

        Vec3d pos = player.getEyePos();
        Optional<AbstractClientPlayerEntity> target = player.clientWorld.getPlayers().stream()
                .filter(p -> p != player)
                .filter(p -> Math.abs(getAngleRadians(pos, p, player.getPitch(), player.getYaw())) <= Math.toRadians(20))
                .min((p1, p2) -> {
                    double angle1 = getAngleRadians(pos, p1, player.getPitch(), player.getYaw());
                    double angle2 = getAngleRadians(pos, p2, player.getPitch(), player.getYaw());
                    if (Math.abs(MathHelper.wrapDegrees(angle1 - angle2)) < 0.1) {
                        double distance1 = pos.squaredDistanceTo(p1.getX() + 0.5, p1.getY() + 1, p1.getZ() + 0.5);
                        double distance2 = pos.squaredDistanceTo(p2.getX() + 0.5, p2.getY() + 1, p2.getZ() + 0.5);
                        if (Math.min(distance1, distance2) < p1.getPos().squaredDistanceTo(p2.getPos()) * 4) {
                            return Double.compare(distance1, distance2);
                        }
                    }
                    return Double.compare(Math.abs(angle1), Math.abs(angle2));
                });
        targeted = target.orElse(null);
    }

    private static double getAngleRadians(Vec3d pos, PlayerEntity player, float pitch, float yaw) {
        Vec3d blockVec = new Vec3d(player.getX() + 0.5 - pos.x, player.getY() + 1.0 - pos.y, player.getZ() + 0.5 - pos.z).normalize();
        Vec3d lookVec = Vec3d.fromPolar(pitch, yaw).normalize();
        return Math.acos(lookVec.dotProduct(blockVec));
    }

    public static boolean shouldForceGlow(Entity entity) {
        return HPH.selectionKey.isPressed() || (HPHConfig.INSTANCE.enableWhitelistGlow && WhitelistManager.isWhitelisted(entity));
    }

    public static boolean isWhitelisted(Entity entity) {
        return whitelist.contains(((PlayerEntity) entity).getGameProfile().getName());
    }

    @SuppressWarnings("SameReturnValue") //return int because used in a command
    public static int add(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "Player");
        whitelist.add(name);
        client.inGameHud.getChatHud().addMessage(Text.of("Added " + name + " to HPH whitelist."));
        return 1;
    }

    @SuppressWarnings("SameReturnValue") //return int because used in a command
    public static int remove(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "Player");
        whitelist.remove(name);
        client.inGameHud.getChatHud().addMessage(Text.of("Removed " + name + " from HPH whitelist."));
        return 1;
    }

    @SuppressWarnings("SameReturnValue") //return int because used in a command
    public static int clear() {
        whitelist.clear();
        client.inGameHud.getChatHud().addMessage(Text.of("Cleared HPH whitelist."));
        return 1;
    }

    @SuppressWarnings({"DataFlowIssue", "unused"}) //complains about world & player
    public static CompletableFuture<Suggestions> getSuggestionsOnAdd(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int clientID = client.player.getId();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getId() == clientID) continue;

            String name = player.getGameProfile().getName();
            if (name.toLowerCase().contains(builder.getInput().toLowerCase()
                    .replace("/hph add ", ""))) builder.suggest(name);
        }
        return builder.buildFuture();
    }

    @SuppressWarnings("unused") //unused context
    public static CompletableFuture<Suggestions> getSuggestionsOnRemove(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (String name : whitelist) {
            if (name.toLowerCase().contains(builder.getInput().toLowerCase()
                    .replace("/hph remove ", ""))) builder.suggest(name);
        }
        return builder.buildFuture();
    }

    @SuppressWarnings("DataFlowIssue") //complains about world & player
    public static void onLeftClick() {
        if (targeted == null) return;

        String name = targeted.getGameProfile().getName();
        boolean whitelisted = whitelist.contains(name);

        if (whitelisted) whitelist.remove(name);
        else whitelist.add(name);

        client.inGameHud.setOverlayMessage(Text.of("§e" + (whitelisted ? "Removed " : "Added ")
                + name + (whitelisted ? " from whitelist." : " to whitelist.")), false);

        PlayerEntity player = client.player;
        client.world.playSound(player.getX(), player.getY(), player.getZ(),
                whitelisted ? SoundEvents.BLOCK_BEACON_DEACTIVATE : SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.5f, 1.0f, false);
    }
}

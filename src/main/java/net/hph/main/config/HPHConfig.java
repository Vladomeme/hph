package net.hph.main.config;

import ch.njol.minecraft.uiframework.ElementPosition;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.hph.main.TextDisplay;
import net.hph.main.WhitelistManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Arrays;

public class HPHConfig {

    public String whitelistString = "";

    public boolean enableText = true;
    public boolean rightAlignment = false;
    public boolean displayTextOnFullHP = true;
    public boolean enableWhitelistText = false;
    public boolean sort = true;
    public int maxLineCount = 30;
    public boolean showFraction = true;
    public final ElementPosition textPosition = new ElementPosition(0.015f, 0, 0.3f, 0, 0.0f, 0);
    public float scale = 0.75f;
    public boolean shadow = true;

    public boolean enableGlow = true;
    public boolean glowOnFullHP = true;
    public boolean enableWhitelistGlow = false;
    public boolean overrideGrossHacksGlowing = true;

    public int hp100Colour = -11141291;
    public int hp66Colour = -171;
    public int hp33Colour = -43691;
    public int saturationColour = -12779521;
    public int targetColour = -6250241;
    public int whitelistedTargetColour = -12829441;

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "hph.json");

    public static final HPHConfig INSTANCE = onRead();

    private static HPHConfig onRead() {
        HPHConfig instance = read();
        if (!instance.whitelistString.isEmpty())
            WhitelistManager.whitelist.addAll(Arrays.asList(instance.whitelistString.replace(" ", "").split(",")));
        return instance;
    }

    public static HPHConfig read() {
        if (!FILE.exists())
            return new HPHConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), HPHConfig.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private void onWrite() {
        String textNew = WhitelistManager.whitelist.toString();
        whitelistString = textNew.substring(1, textNew.length() - 1);
        TextDisplay.saturationStyle = Style.EMPTY.withColor(saturationColour);
        write();
    }

    public HPHConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, HPHConfig.class), writer);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }

    public int getColour(PlayerEntity player) {
        return getColour(player.getHealth() / player.getMaxHealth());
    }

    public int getColour(float percent) {
        if (percent <= 0.33) return hp33Colour;
        else if (percent <= 0.66) return hp66Colour;
        else return hp100Colour;
    }

    public Screen create(Screen parent) {
        String textNew = WhitelistManager.whitelist.toString();
        whitelistString = textNew.substring(1, textNew.length() - 1);
        return YetAnotherConfigLib.createBuilder()
                .save(this::onWrite)
                .title(Text.literal("HPH Display"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))

                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Whitelist"))
                                .description(OptionDescription.of(Text.of("Names of whitelisted players, comma separated. " +
                                        "Spaces are ignored.\nRecommend to be only edit it in-game using selection keybind and `/hph`.")))
                                .binding("", () -> whitelistString, newVal -> whitelistString = newVal)
                                .controller(StringControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Text display"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .description(OptionDescription.of(Text.of("Enables text health display.")))
                                .binding(true, () -> enableText, newVal -> enableText = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Use right alignment"))
                                .binding(false, () -> rightAlignment, newVal -> rightAlignment = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Display on full health"))
                                .binding(true, () -> displayTextOnFullHP, newVal -> displayTextOnFullHP = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Use whitelist"))
                                .binding(false, () -> enableWhitelistText, newVal -> enableWhitelistText = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Sort by health"))
                                .description(OptionDescription.of(Text.of("If enabled, lines will be sorted by % health " +
                                        "in ascending order.")))
                                .binding(false, () -> sort, newVal -> sort = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Max line count"))
                                .description(OptionDescription.of(Text.literal("Maximum number of lines to display.")))
                                .binding(30, () -> maxLineCount, newVal -> maxLineCount = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show fraction"))
                                .binding(true, () -> showFraction, newVal -> showFraction = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("Text scale"))
                                .description(OptionDescription.of(Text.literal(
                                        "Size of display, default - 0.75. Recommend to only change value by multiples of 0.25.")))
                                .binding(1f, () -> scale, newVal -> scale = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Draw text with shadow"))
                                .binding(true, () -> shadow, newVal -> shadow = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Glowing"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .description(OptionDescription.of(Text.of("Enables health coloured glowing.")))
                                .binding(true, () -> enableGlow, newVal -> enableGlow = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Glow on full health"))
                                .binding(true, () -> glowOnFullHP, newVal -> glowOnFullHP = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Use whitelist"))
                                .binding(false, () -> enableWhitelistGlow, newVal -> enableWhitelistGlow = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Bypass Gross Hacks glowing toggle"))
                                .binding(true, () -> overrideGrossHacksGlowing, newVal -> overrideGrossHacksGlowing = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Colours"))

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Full health"))
                                .binding(new Color(-11141291, true),
                                        () -> new Color(hp100Colour, true), newVal -> hp100Colour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Damaged"))
                                .binding(new Color(-171, true),
                                        () -> new Color(hp66Colour, true), newVal -> hp66Colour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Low health"))
                                .binding(new Color(-43691, true),
                                        () -> new Color(hp33Colour, true), newVal -> hp33Colour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Saturation"))
                                .binding(new Color(-12779521, true),
                                        () -> new Color(saturationColour, true), newVal -> saturationColour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Selection - targeted"))
                                .binding(new Color(-6250241, true),
                                        () -> new Color(targetColour, true), newVal -> targetColour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Selection - whitelisted & targeted"))
                                .binding(new Color(-12829441, true),
                                        () -> new Color(whitelistedTargetColour, true), newVal -> whitelistedTargetColour = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}

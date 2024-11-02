package net.hph.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

public class HPHConfig {

    public boolean enabled = true;
    public float x = 0.015f;
    public float y = 0.1f;
    public float scale = 1f;
    public boolean shadow = true;

    public int hp100Color = -11141291;
    public int hp66Color = -171;
    public int hp33Color = -43691;
    public int saturationColor = -171;

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "hph.json");

    public static final HPHConfig INSTANCE = read();

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

    public Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .save(this::write)
                .title(Text.literal("HPH Display"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .binding(true, () -> enabled, newVal -> enabled = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("x"))
                                .description(OptionDescription.of(Text.literal(
                                        "Left side of the screen - 0, right - 1")))
                                .binding(0.015f, () -> x, newVal -> x = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("y"))
                                .description(OptionDescription.of(Text.literal(
                                        "Top of the screen - 0, bottom - 1")))
                                .binding(0.1f, () -> y, newVal -> y = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("Scale"))
                                .description(OptionDescription.of(Text.literal(
                                        "Size of display, default - 1")))
                                .binding(1f, () -> scale, newVal -> scale = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Draw text shadow"))
                                .binding(true, () -> shadow, newVal -> shadow = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Colours"))

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Full health"))
                                .binding(new Color(-11141291, true),
                                        () -> new Color(hp100Color, true), newVal -> hp100Color = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Damaged"))
                                .binding(new Color(-171, true),
                                        () -> new Color(hp66Color, true), newVal -> hp66Color = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Low health"))
                                .binding(new Color(-43691, true),
                                        () -> new Color(hp33Color, true), newVal -> hp33Color = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Saturation"))
                                .binding(new Color(-171, true),
                                        () -> new Color(saturationColor, true), newVal -> saturationColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}

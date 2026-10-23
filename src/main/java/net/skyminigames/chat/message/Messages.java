package net.skyminigames.chat.message;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads messages.yml (always as UTF-8, so Portuguese accents work on any OS) and formats messages.
 */
@RequiredArgsConstructor
public final class Messages {

    private static final String FILE_NAME = "messages.yml";

    private final JavaPlugin plugin;
    private YamlConfiguration yaml = new YamlConfiguration();
    private String prefix = "";

    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration loaded = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            loaded.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + FILE_NAME + ", using the default messages.", e);
        }

        // Missing keys fall back to the messages bundled in the jar
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read the default " + FILE_NAME, e);
        }

        yaml = loaded;
        prefix = color(yaml.getString("prefix", ""));
    }

    /**
     * A chat message with the prefix. Placeholders are given as pairs: {@code "online", 5}.
     */
    public String get(String path, Object... placeholders) {
        return prefix + raw(path, placeholders);
    }

    /**
     * A message without the prefix.
     */
    public String raw(String path, Object... placeholders) {
        return format(yaml.getString(path, path), placeholders);
    }

    /**
     * A multi-line message (a YAML list, or a single string).
     */
    public List<String> lines(String path, Object... placeholders) {
        List<String> source = yaml.isList(path) ? yaml.getStringList(path) : Collections.singletonList(yaml.getString(path, path));
        List<String> lines = new ArrayList<>(source.size());
        for (String line : source) {
            lines.add(format(line, placeholders));
        }
        return lines;
    }

    private static String format(String message, Object... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return color(message);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

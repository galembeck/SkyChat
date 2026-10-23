package net.skyminigames.chat.channel;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads every channel from plugins/SkyChat/channels. The default channels (local, global, staff) are
 * only created when the folder doesn't exist yet, so deleted channels stay deleted.
 */
@RequiredArgsConstructor
public final class ChannelManager {

    private static final String FOLDER = "channels";
    private static final String[] DEFAULT_CHANNELS = {"local.yml", "global.yml", "staff.yml"};

    private final JavaPlugin plugin;
    private volatile Map<String, Channel> channels = Collections.emptyMap();

    public void reload() {
        File folder = new File(plugin.getDataFolder(), FOLDER);
        if (!folder.exists()) {
            for (String file : DEFAULT_CHANNELS) {
                plugin.saveResource(FOLDER + "/" + file, false);
            }
        }

        Map<String, Channel> loaded = new LinkedHashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                YamlConfiguration yaml = new YamlConfiguration();
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    yaml.load(reader);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not read channel file " + file.getName(), e);
                    continue;
                }
                Channel channel = Channel.fromConfig(yaml);
                if (channel.getName().isEmpty()) {
                    plugin.getLogger().warning("Channel file " + file.getName() + " has no name, skipped.");
                    continue;
                }
                if (find(loaded.values(), channel.getName()) != null || find(loaded.values(), channel.getNickname()) != null) {
                    plugin.getLogger().warning("Channel '" + channel.getName() + "' uses a name or nickname already taken, skipped.");
                    continue;
                }
                loaded.put(channel.getName(), channel);
            }
        }
        channels = Collections.unmodifiableMap(loaded);
    }

    public Collection<Channel> getChannels() {
        return channels.values();
    }

    /** By name or nickname, case-insensitive. */
    public Channel get(String nameOrNickname) {
        return nameOrNickname == null ? null : find(channels.values(), nameOrNickname);
    }

    /** The channel whose shortcut command is {@code label} (e.g. "g" for /g). */
    public Channel byShortcut(String label) {
        Channel channel = get(label);
        return channel != null && channel.isShortcutAllowed() ? channel : null;
    }

    public List<String> names() {
        return new ArrayList<>(channels.keySet());
    }

    private static Channel find(Collection<Channel> channels, String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (Channel channel : channels) {
            if (channel.getName().equals(lower) || channel.getNickname().equals(lower)) {
                return channel;
            }
        }
        return null;
    }
}

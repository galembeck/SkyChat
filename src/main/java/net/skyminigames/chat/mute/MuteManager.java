package net.skyminigames.chat.mute;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.logging.Level;

/**
 * Muted players (permanent or for some minutes) and the "mute everyone" switch. Saved in mutes.yml.
 */
public final class MuteManager {

    /** Expiry value for a mute without end. */
    public static final long PERMANENT = Long.MAX_VALUE;
    private static final String FILE_NAME = "mutes.yml";

    private final JavaPlugin plugin;
    private final LongSupplier clock;
    private final Map<UUID, Long> mutes = new ConcurrentHashMap<>();
    private volatile boolean allMuted;

    public MuteManager(JavaPlugin plugin) {
        this(plugin, System::currentTimeMillis);
    }

    /** For tests: a custom clock. */
    public MuteManager(JavaPlugin plugin, LongSupplier clock) {
        this.plugin = plugin;
        this.clock = clock;
    }

    public void mute(UUID player, int minutes) {
        mutes.put(player, minutes <= 0 ? PERMANENT : clock.getAsLong() + TimeUnit.MINUTES.toMillis(minutes));
        save();
    }

    /** @return whether the player was muted */
    public boolean unmute(UUID player) {
        boolean removed = mutes.remove(player) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean isMuted(UUID player) {
        Long expiry = mutes.get(player);
        if (expiry == null) {
            return false;
        }
        if (expiry != PERMANENT && clock.getAsLong() >= expiry) {
            mutes.remove(player);
            return false;
        }
        return true;
    }

    /** Minutes left (rounded up), or -1 for a permanent mute, or 0 if not muted. */
    public long minutesLeft(UUID player) {
        if (!isMuted(player)) {
            return 0;
        }
        long expiry = mutes.get(player);
        if (expiry == PERMANENT) {
            return -1;
        }
        long millis = expiry - clock.getAsLong();
        return Math.max(1, (millis + TimeUnit.MINUTES.toMillis(1) - 1) / TimeUnit.MINUTES.toMillis(1));
    }

    public boolean isAllMuted() {
        return allMuted;
    }

    public void setAllMuted(boolean allMuted) {
        this.allMuted = allMuted;
        save();
    }

    public void load() {
        if (plugin == null) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file());
        allMuted = yaml.getBoolean("all-muted", false);
        mutes.clear();
        if (yaml.isConfigurationSection("players")) {
            for (String key : yaml.getConfigurationSection("players").getKeys(false)) {
                try {
                    mutes.put(UUID.fromString(key), yaml.getLong("players." + key));
                } catch (IllegalArgumentException e) {
                    // skip broken entries
                }
            }
        }
    }

    private void save() {
        if (plugin == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("all-muted", allMuted);
        for (Map.Entry<UUID, Long> mute : mutes.entrySet()) {
            yaml.set("players." + mute.getKey(), mute.getValue());
        }
        try {
            yaml.save(file());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save " + FILE_NAME, e);
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }
}

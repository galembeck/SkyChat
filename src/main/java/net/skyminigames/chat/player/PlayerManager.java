package net.skyminigames.chat.player;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Online players' chat state. Ignored players / channels and the focused channel are saved in
 * players.yml so they survive reconnects and restarts.
 */
@RequiredArgsConstructor
public final class PlayerManager {

    private static final String FILE_NAME = "players.yml";

    private final JavaPlugin plugin;
    private final Map<UUID, ChatPlayer> players = new ConcurrentHashMap<>();
    private YamlConfiguration data;

    public void load() {
        data = YamlConfiguration.loadConfiguration(file());
    }

    /** Never null for an online player; creates the state on first access. */
    public ChatPlayer get(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), id -> loadPlayer(id, player.getName()));
    }

    public ChatPlayer getIfOnline(UUID uniqueId) {
        return players.get(uniqueId);
    }

    public void unload(Player player) {
        ChatPlayer chatPlayer = players.remove(player.getUniqueId());
        if (chatPlayer != null) {
            store(chatPlayer);
            save();
        }
    }

    public void saveAll() {
        for (ChatPlayer chatPlayer : players.values()) {
            store(chatPlayer);
        }
        save();
    }

    private ChatPlayer loadPlayer(UUID uniqueId, String name) {
        ChatPlayer chatPlayer = new ChatPlayer(uniqueId, name);
        ConfigurationSection section = data.getConfigurationSection(uniqueId.toString());
        if (section != null) {
            chatPlayer.setFocusChannel(section.getString("focus"));
            for (String ignored : section.getStringList("ignored-players")) {
                try {
                    chatPlayer.getIgnoredPlayers().add(UUID.fromString(ignored));
                } catch (IllegalArgumentException ignoredInvalid) {
                    // skip broken entries
                }
            }
            chatPlayer.getIgnoredChannels().addAll(section.getStringList("ignored-channels"));
        }
        return chatPlayer;
    }

    private void store(ChatPlayer chatPlayer) {
        String path = chatPlayer.getUniqueId().toString();
        boolean empty = chatPlayer.getFocusChannel() == null
                && chatPlayer.getIgnoredPlayers().isEmpty()
                && chatPlayer.getIgnoredChannels().isEmpty();
        if (empty) {
            data.set(path, null);
            return;
        }
        List<String> ignored = new ArrayList<>();
        for (UUID id : chatPlayer.getIgnoredPlayers()) {
            ignored.add(id.toString());
        }
        data.set(path + ".name", chatPlayer.getName());
        data.set(path + ".focus", chatPlayer.getFocusChannel());
        data.set(path + ".ignored-players", ignored);
        data.set(path + ".ignored-channels", new ArrayList<>(chatPlayer.getIgnoredChannels()));
    }

    private void save() {
        try {
            data.save(file());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save " + FILE_NAME, e);
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }
}

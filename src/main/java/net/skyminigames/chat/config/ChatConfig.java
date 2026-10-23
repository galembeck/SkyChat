package net.skyminigames.chat.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed, read-only view of config.yml.
 */
@Getter
public final class ChatConfig {

    private final String defaultChannel;
    private final boolean logToConsole;
    private final boolean showNoOneHearsYou;
    private final boolean spacing;
    private final boolean blockPrivateWhenMuted;
    private final boolean censorEnabled;
    private final List<String> censoredWords;

    public ChatConfig(FileConfiguration config) {
        defaultChannel = config.getString("default-channel", "local");
        logToConsole = config.getBoolean("log-to-console", true);
        showNoOneHearsYou = config.getBoolean("show-no-one-hears-you", true);
        spacing = config.getBoolean("spacing", true);
        blockPrivateWhenMuted = config.getBoolean("private-messages.block-when-muted", true);
        censorEnabled = config.getBoolean("censor.enabled", false);
        censoredWords = Collections.unmodifiableList(new ArrayList<>(config.getStringList("censor.words")));
    }
}

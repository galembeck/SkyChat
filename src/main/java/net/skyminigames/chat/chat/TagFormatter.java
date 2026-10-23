package net.skyminigames.chat.chat;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

import java.util.Map;

/**
 * Builds the final channel line from the format and its {tags}.
 * <p>
 * The format and tag values may use "&amp;" colours; the player's message ({msg}) is inserted last and
 * untouched, so it can't inject tags or colours it isn't allowed to use.
 */
@UtilityClass
public class TagFormatter {

    public static final String MESSAGE_TAG = "{msg}";

    public String format(String format, Map<String, String> tags, String message) {
        String line = format;
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            line = line.replace("{" + tag.getKey() + "}", tag.getValue() == null ? "" : tag.getValue());
        }
        line = ChatColor.translateAlternateColorCodes('&', line);
        return line.replace(MESSAGE_TAG, message);
    }
}

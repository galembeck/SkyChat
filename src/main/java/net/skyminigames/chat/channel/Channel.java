package net.skyminigames.chat.channel;

import lombok.Builder;
import lombok.Value;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * A chat channel, loaded from plugins/SkyChat/channels/&lt;name&gt;.yml (same keys as Legendchat).
 */
@Value
@Builder
public class Channel {

    String name;
    String nickname;
    String format;
    ChatColor color;
    boolean shortcutAllowed;
    /** Only players focused on the channel (/ch) see its messages. */
    boolean needFocus;
    /** Blocks; 0 = whole world (or every world, with crossworlds). */
    double distance;
    boolean crossworlds;
    int delayPerMessage;
    double costPerMessage;
    boolean showCostMessage;

    public static Channel fromConfig(ConfigurationSection config) {
        String name = config.getString("name", "").toLowerCase(Locale.ROOT);
        return Channel.builder()
                .name(name)
                .nickname(config.getString("nickname", name).toLowerCase(Locale.ROOT))
                .format(config.getString("format", "{color}[{nick}] {prefix}{plainsender}{color}: {msg}"))
                .color(parseColor(config.getString("color", "WHITE")))
                .shortcutAllowed(config.getBoolean("shortcutAllowed", true))
                .needFocus(config.getBoolean("needFocus", false))
                .distance(Math.max(0, config.getDouble("distance", 0)))
                .crossworlds(config.getBoolean("crossworlds", true))
                .delayPerMessage(Math.max(0, config.getInt("delayPerMessage", 0)))
                .costPerMessage(Math.max(0, config.getDouble("costPerMessage", 0)))
                .showCostMessage(config.getBoolean("showCostMessage", true))
                .build();
    }

    /**
     * Accepts Bukkit names (DARK_BLUE), Legendchat names (DARKBLUE, and its typo DARGREEN) or a code (&amp;1).
     */
    public static ChatColor parseColor(String value) {
        if (value == null) {
            return ChatColor.WHITE;
        }
        String clean = value.trim().toUpperCase(Locale.ROOT).replace("_", "").replace(" ", "");
        if (clean.length() == 2 && (clean.charAt(0) == '&' || clean.charAt(0) == ChatColor.COLOR_CHAR)) {
            ChatColor byCode = ChatColor.getByChar(Character.toLowerCase(clean.charAt(1)));
            return byCode != null && byCode.isColor() ? byCode : ChatColor.WHITE;
        }
        if (clean.equals("DARGREEN")) {
            return ChatColor.DARK_GREEN;
        }
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor() && color.name().replace("_", "").equals(clean)) {
                return color;
            }
        }
        return ChatColor.WHITE;
    }

    public String chatPermission() {
        return "skychat.channel." + name + ".chat";
    }

    public String focusPermission() {
        return "skychat.channel." + name + ".focus";
    }

    public String noDelayPermission() {
        return "skychat.channel." + name + ".nodelay";
    }

    public String freePermission() {
        return "skychat.channel." + name + ".free";
    }

    public boolean isLocal() {
        return distance > 0;
    }
}

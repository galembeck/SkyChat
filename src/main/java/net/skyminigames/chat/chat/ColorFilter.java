package net.skyminigames.chat.chat;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * Turns "&amp;x" codes typed by a player into colours, but only the ones they have permission for:
 * skychat.color.allcolors (0-9, a-f), skychat.color.allformats (k-o, r) or skychat.color.&lt;name&gt;
 * (e.g. skychat.color.red, skychat.color.bold). Codes without permission stay as plain text.
 */
@UtilityClass
public class ColorFilter {

    public String apply(String message, Predicate<String> hasPermission) {
        StringBuilder out = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '&' && i + 1 < message.length()) {
                ChatColor code = ChatColor.getByChar(Character.toLowerCase(message.charAt(i + 1)));
                if (code != null && allowed(code, hasPermission)) {
                    out.append(ChatColor.COLOR_CHAR).append(code.getChar());
                    i++;
                    continue;
                }
            }
            // A raw colour symbol typed by the player is never allowed through
            out.append(c == ChatColor.COLOR_CHAR ? '&' : c);
        }
        return out.toString();
    }

    /** Permission name of a code: "red", "darkblue", "bold", "magic"... */
    public String permissionName(ChatColor code) {
        return code.name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    private boolean allowed(ChatColor code, Predicate<String> hasPermission) {
        String group = code.isColor() ? "skychat.color.allcolors" : "skychat.color.allformats";
        return hasPermission.test(group) || hasPermission.test("skychat.color." + permissionName(code));
    }
}

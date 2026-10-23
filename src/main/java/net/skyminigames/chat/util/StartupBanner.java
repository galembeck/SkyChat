package net.skyminigames.chat.util;

import lombok.experimental.UtilityClass;
import net.skyminigames.chat.SkyChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Prints the SkyChat logo and startup information to the console.
 */
@UtilityClass
public class StartupBanner {

    private static final String[] LOGO = {
            "  _____ _            _____ _           _",
            " / ____| |          / ____| |         | |",
            "| (___ | | __ _   _| |    | |__   __ _| |_",
            " \\___ \\| |/ /| | | | |    | '_ \\ / _` | __|",
            " ____) |   < | |_| | |____| | | | (_| | |_",
            "|_____/|_|\\_\\ \\__, |\\_____|_| |_|\\__,_|\\__|",
            "               __/ |",
            "              |___/",
    };

    /** Column where "Sky" ends and "Chat" begins in {@link #LOGO}. */
    private static final int LOGO_SPLIT = 20;
    private static final String INDENT = "  ";

    public void printEnable(SkyChat plugin, long elapsedMillis) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage("");
        for (String line : LOGO) {
            console.sendMessage(INDENT + colorLogoLine(line));
        }
        console.sendMessage("");
        console.sendMessage(INDENT + ChatColor.AQUA + ChatColor.BOLD + plugin.getName()
                + ChatColor.GRAY + " v" + plugin.getDescription().getVersion()
                + ChatColor.DARK_GRAY + " by " + ChatColor.WHITE + String.join(", ", plugin.getDescription().getAuthors()));
        info(console, "Server", Bukkit.getName() + " " + Bukkit.getBukkitVersion().split("-")[0]
                + ChatColor.DARK_GRAY + " (Java " + System.getProperty("java.version") + ")");
        info(console, "Channels", String.join(", ", plugin.getChannels().names())
                + ChatColor.DARK_GRAY + " (default: " + plugin.getChatConfig().getDefaultChannel() + ")");
        info(console, "Prefixes", plugin.getChat() != null ? plugin.getChat().getName() + ChatColor.DARK_GRAY + " (via Vault)" : ChatColor.RED + "none");
        info(console, "Economy", plugin.getEconomy() != null ? plugin.getEconomy().getName() : ChatColor.GRAY + "none (channel costs disabled)");
        info(console, "Status", ChatColor.GREEN + "Enabled in " + elapsedMillis + " ms");
        console.sendMessage("");
    }

    public void printDisable(JavaPlugin plugin) {
        Bukkit.getConsoleSender().sendMessage(INDENT + ChatColor.AQUA + ChatColor.BOLD + plugin.getName()
                + ChatColor.GRAY + " v" + plugin.getDescription().getVersion()
                + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Disabled.");
    }

    private String colorLogoLine(String line) {
        if (line.length() <= LOGO_SPLIT) {
            return ChatColor.AQUA + line;
        }
        return ChatColor.AQUA + line.substring(0, LOGO_SPLIT) + ChatColor.WHITE + line.substring(LOGO_SPLIT);
    }

    private void info(ConsoleCommandSender console, String label, String value) {
        console.sendMessage(INDENT + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + label + ": " + ChatColor.WHITE + value);
    }
}

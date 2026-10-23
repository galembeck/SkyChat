package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.mute.MuteManager;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * /skychat reload | spy | hide | mute &lt;jogador&gt; [minutos] | unmute &lt;jogador&gt; | muteall | unmuteall
 * <p>
 * /mute and /unmute are aliases of the mute sub-commands (set in the server's commands.yml).
 */
@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sender.hasPermission("skychat.admin." + sub) && !sender.hasPermission("skychat.admin")) {
            sender.sendMessage(messages.get("general.no-permission"));
            return true;
        }
        switch (sub) {
            case "reload":
                plugin.reload();
                sender.sendMessage(messages.get("general.reloaded"));
                return true;
            case "spy":
            case "hide":
                toggle(sender, sub);
                return true;
            case "mute":
                mute(sender, args);
                return true;
            case "unmute":
                unmute(sender, args);
                return true;
            case "muteall":
            case "unmuteall":
                muteAll(sender, sub.equals("muteall"));
                return true;
            default:
                help(sender);
                return true;
        }
    }

    private void help(CommandSender sender) {
        for (String line : plugin.getMessages().lines("general.admin-help")) {
            sender.sendMessage(line);
        }
    }

    private void toggle(CommandSender sender, String what) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return;
        }
        ChatPlayer chatPlayer = plugin.getPlayers().get((Player) sender);
        if (what.equals("spy")) {
            chatPlayer.setSpy(!chatPlayer.isSpy());
            sender.sendMessage(messages.get(chatPlayer.isSpy() ? "spy.on" : "spy.off"));
        } else {
            chatPlayer.setHidden(!chatPlayer.isHidden());
            sender.sendMessage(messages.get(chatPlayer.isHidden() ? "spy.hide-on" : "spy.hide-off"));
        }
    }

    @SuppressWarnings("deprecation") // name lookup for offline players is what /mute <name> needs
    private void mute(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(messages.get("mute.usage"));
            return;
        }
        int minutes = 0;
        if (args.length == 3) {
            try {
                minutes = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                minutes = -1;
            }
            if (minutes < 0) {
                sender.sendMessage(messages.get("mute.invalid-time"));
                return;
            }
        }

        Player online = Bukkit.getPlayer(args[1]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
        String name = target.getName() != null ? target.getName() : args[1];
        if (online != null && online.hasPermission("skychat.block.mute")) {
            sender.sendMessage(messages.get("mute.cannot"));
            return;
        }
        MuteManager mutes = plugin.getMutes();
        if (mutes.isMuted(target.getUniqueId())) {
            sender.sendMessage(messages.get("mute.already"));
            return;
        }

        mutes.mute(target.getUniqueId(), minutes);
        if (minutes == 0) {
            sender.sendMessage(messages.get("mute.done", "player", name));
            if (online != null) {
                online.sendMessage(messages.get("mute.notify", "staff", sender.getName()));
            }
        } else {
            sender.sendMessage(messages.get("mute.done-time", "player", name, "minutes", minutes));
            if (online != null) {
                online.sendMessage(messages.get("mute.notify-time", "staff", sender.getName(), "minutes", minutes));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void unmute(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length != 2) {
            sender.sendMessage(messages.get("mute.unmute-usage"));
            return;
        }
        Player online = Bukkit.getPlayer(args[1]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
        String name = target.getName() != null ? target.getName() : args[1];
        if (!plugin.getMutes().unmute(target.getUniqueId())) {
            sender.sendMessage(messages.get("mute.not-muted"));
            return;
        }
        sender.sendMessage(messages.get("mute.unmuted", "player", name));
        if (online != null) {
            online.sendMessage(messages.get("mute.unmuted-notify", "staff", sender.getName()));
        }
    }

    private void muteAll(CommandSender sender, boolean mute) {
        Messages messages = plugin.getMessages();
        MuteManager mutes = plugin.getMutes();
        if (mutes.isAllMuted() == mute) {
            sender.sendMessage(messages.get(mute ? "mute.all-already-on" : "mute.all-already-off"));
            return;
        }
        mutes.setAllMuted(mute);
        Bukkit.broadcastMessage(messages.get(mute ? "mute.all-on" : "mute.all-off", "staff", sender.getName()));
    }
}

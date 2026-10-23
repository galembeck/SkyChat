package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /afk [motivo] - toggles AFK. Players who send you a private message are told you're AFK (and why).
 */
@RequiredArgsConstructor
public final class AfkCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skychat.afk")) {
            player.sendMessage(messages.get("general.no-permission"));
            return true;
        }
        ChatPlayer chatPlayer = plugin.getPlayers().get(player);
        if (chatPlayer.isAfk() && args.length == 0) {
            plugin.getChatService().clearAfk(player, chatPlayer);
            return true;
        }
        // The motive is shown to others: no colour codes
        chatPlayer.setAfkMotive(ChatColor.stripColor(TellCommand.join(args, 0).replace('&', ' ')).trim());
        player.sendMessage(messages.get("afk.on", "motive", plugin.getChatService().motive(chatPlayer)));
        return true;
    }
}

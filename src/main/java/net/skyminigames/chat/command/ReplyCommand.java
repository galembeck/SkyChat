package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /r &lt;mensagem&gt;
 */
@RequiredArgsConstructor
public final class ReplyCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skychat.tell")) {
            player.sendMessage(messages.get("general.no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(messages.get("private.reply-usage"));
            return true;
        }
        Player target = plugin.getChatService().replyTarget(player);
        if (target == null) {
            player.sendMessage(messages.get("private.no-reply"));
            return true;
        }
        plugin.getChatService().sendPrivate(player, target, TellCommand.join(args, 0));
        return true;
    }
}

package net.skyminigames.chat.api;

import lombok.Getter;
import lombok.Setter;
import net.skyminigames.chat.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Set;

/**
 * Fired (on the main thread) before a channel message is delivered. Other plugins can change the
 * message, add or remove recipients, or cancel it.
 */
@Getter
public class SkyChatMessageEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Channel channel;
    /** Already colour-filtered and censored. */
    @Setter
    private String message;
    /** Who will receive the message (the sender included, when in range). Modifiable. */
    private final Set<Player> recipients;
    @Setter
    private boolean cancelled;

    public SkyChatMessageEvent(Player sender, Channel channel, String message, Set<Player> recipients) {
        super(sender);
        this.channel = channel;
        this.message = message;
        this.recipients = recipients;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

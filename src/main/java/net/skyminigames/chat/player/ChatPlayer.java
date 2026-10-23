package net.skyminigames.chat.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat state of one online player.
 */
@Getter
@Setter
@RequiredArgsConstructor
public final class ChatPlayer {

    private final UUID uniqueId;
    private final String name;

    /** Channel used when typing without a shortcut. {@code null} = the default channel. */
    private volatile String focusChannel;
    private final Set<UUID> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> ignoredChannels = ConcurrentHashMap.newKeySet();

    private volatile boolean spy;
    private volatile boolean hidden;
    /** {@code null} = not AFK; empty = AFK without a reason. */
    private volatile String afkMotive;
    /** Who /r answers. */
    private volatile UUID replyTarget;
    /** Set by "/tell &lt;player&gt;" without a message: normal chat goes to that player. */
    private volatile UUID tellLock;

    /** Last message time per channel, for delayPerMessage. */
    private final Map<String, Long> lastMessage = new ConcurrentHashMap<>();

    public boolean isAfk() {
        return afkMotive != null;
    }
}

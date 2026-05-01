package com.nova.common.renatusFriends.service;

import com.nova.common.renatusFriends.database.repository.*;
import com.nova.common.renatusFriends.util.MessageUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class FriendManager
{
    private static final int                    FRIENDS_PER_PAGE = 10;

    private final ProxyServer                   m_proxyServer;
    private final PlayerRepository              m_playerRepository;
    private final FriendRepository              m_friendRepository;
    private final FriendRequestRepository       m_friendRequestRepository;
    private final BlockRepository               m_blockRepository;
    private final FriendSettingsRepository      m_friendSettingsRepository;
    private final FriendNotificationService     m_friendNotificationService;

    //----------------------------------------------------------------------------------------------------------------------
    public FriendManager(ProxyServer proxyServer, PlayerRepository playerRepository, FriendRepository friendRepository, FriendRequestRepository friendRequestRepository, BlockRepository blockRepository, FriendSettingsRepository friendSettingsRepository, FriendNotificationService friendNotificationService)
    {
        m_proxyServer                   = proxyServer;
        m_playerRepository              = playerRepository;
        m_friendRepository              = friendRepository;
        m_friendRequestRepository       = friendRequestRepository;
        m_blockRepository               = blockRepository;
        m_friendSettingsRepository      = friendSettingsRepository;
        m_friendNotificationService     = friendNotificationService;
    }

    //------------------------------------------------------------------------------------------------------------------
    public void onPlayerJoin(Player player)
    {
        trackPlayer(player);

        List<UUID> friendUUIDs = m_friendRepository.getFriendUUIDs(player.getUniqueId());
        String serverName = getCurrentServerName(player);

        for (UUID friendUUID : friendUUIDs)
        {
            if (m_friendSettingsRepository.areNotificationsEnabled(friendUUID))
            {
                m_friendNotificationService.sendFriendJoinMessage(friendUUID, player.getUsername(), serverName);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public void onPlayerLeave(Player player)
    {
        List<UUID> friendUUIDs = m_friendRepository.getFriendUUIDs(player.getUniqueId());

        for (UUID friendUUID : friendUUIDs)
        {
            if (m_friendSettingsRepository.areNotificationsEnabled(friendUUID))
            {
                m_friendNotificationService.sendFriendLeaveMessage(friendUUID, player.getUsername());
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public void trackPlayer(Player player)
    {
        if (player == null) return;

        m_playerRepository.insertPlayer(player.getUniqueId(), player.getUsername());
    }

    //------------------------------------------------------------------------------------------------------------------
    public void showCommandUsage(Player player)
    {
        player.sendMessage(MessageUtils.success("/friend add <username>"));
        player.sendMessage(MessageUtils.success("/friend accept <username>"));
        player.sendMessage(MessageUtils.success("/friend reject <username>"));
        player.sendMessage(MessageUtils.success("/friend remove <username>"));
        player.sendMessage(MessageUtils.success("/friend block <username>"));
        player.sendMessage(MessageUtils.success("/friend unblock <username>"));
        player.sendMessage(MessageUtils.success("/friend list [page]"));
        player.sendMessage(MessageUtils.success("/friend notifications"));
    }

    //------------------------------------------------------------------------------------------------------------------
    public void showFriendsList(Player player, int page)
    {
        List<UUID> friendUUIDs = new ArrayList<>(m_friendRepository.getFriendUUIDs(player.getUniqueId()));

        if (friendUUIDs.isEmpty())
        {
            player.sendMessage(MessageUtils.error("You have no friends."));
            return;
        }

        friendUUIDs.sort(Comparator.comparing(m_playerRepository::findUsernameByUuid, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int)Math.ceil(friendUUIDs.size() / (double)FRIENDS_PER_PAGE));
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int startIndex = (currentPage - 1) * FRIENDS_PER_PAGE;
        int endIndex = Math.min(startIndex + FRIENDS_PER_PAGE, friendUUIDs.size());

        player.sendMessage(MessageUtils.success("Your Friends (Page " + currentPage + "/" + totalPages + ")"));

        for (int index = startIndex; index < endIndex; index++)
        {
            UUID friendUUID = friendUUIDs.get(index);
            String friendName = m_playerRepository.findUsernameByUuid(friendUUID);

            Optional<Player> onlineFriendOptional = m_proxyServer.getPlayer(friendUUID);
            if (onlineFriendOptional.isPresent())
            {
                Player onlineFriend = onlineFriendOptional.get();
                player.sendMessage(MessageUtils.success("- " + friendName + " (Online - " + getCurrentServerName(onlineFriend) + ")"));
            }
            else
            {
                player.sendMessage(MessageUtils.info("- " + friendName + " (Offline)"));
            }
        }

        if (totalPages > 1)
        {
            player.sendMessage(MessageUtils.info("Use /friend list <page> to change pages."));
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public void toggleNotifications(Player player)
    {
        boolean enabled = m_friendSettingsRepository.toggleNotifications(player.getUniqueId());

        if (enabled)
        {
            player.sendMessage(MessageUtils.success("Friend notifications enabled."));
        }
        else
        {
            player.sendMessage(MessageUtils.error("Friend notifications disabled."));
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public void addFriend(Player player, String targetName)
    {
        trackPlayer(player);

        UUID playerUUID = player.getUniqueId();
        UUID targetUUID = findTargetUUID(player, targetName);

        if (targetUUID == null) return;

        if (playerUUID.equals(targetUUID))
        {
            player.sendMessage(MessageUtils.error("You cannot add yourself."));
            return;
        }

        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        if (m_friendRepository.areFriends(playerUUID, targetUUID))      // Already friends
        {
            player.sendMessage(MessageUtils.error("You are already friends with " + resolvedTargetName + "."));
            return;
        }

        if (m_blockRepository.isBlocked(playerUUID, targetUUID))
        {
            player.sendMessage(MessageUtils.error("You have blocked " + resolvedTargetName + "."));
            return;
        }

        if (m_blockRepository.isBlocked(targetUUID, playerUUID))
        {
            player.sendMessage(MessageUtils.error("You cannot send a friend request to " + resolvedTargetName + "."));
            return;
        }

        if (m_friendRequestRepository.hasValidFriendRequest(targetUUID, playerUUID))
        {
            acceptFriend(player, resolvedTargetName);
            return;
        }

        if (m_friendRequestRepository.hasValidFriendRequest(playerUUID, targetUUID))
        {
            player.sendMessage(MessageUtils.error("You already sent a friend request to " + resolvedTargetName + "."));
            return;
        }

        boolean wasRequestSent = m_friendRequestRepository.sendFriendRequest(playerUUID, targetUUID);
        if (!wasRequestSent)
        {
            player.sendMessage(MessageUtils.error("Failed to send friend request."));
            return;
        }

        player.sendMessage(MessageUtils.success("Friend request sent to " + resolvedTargetName + "."));
        m_friendNotificationService.sendIncomingFriendRequestMessages(targetUUID, player.getUsername());
    }

    //------------------------------------------------------------------------------------------------------------------
    public void acceptFriend(Player player, String targetName)
    {
        UUID playerUUID = player.getUniqueId();
        UUID targetUUID = findTargetUUID(player, targetName);
        if (targetUUID == null)
        {
            return;
        }

        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        if (m_blockRepository.isBlocked(playerUUID, targetUUID) || m_blockRepository.isBlocked(targetUUID, playerUUID))
        {
            player.sendMessage(MessageUtils.error("You cannot accept this friend request because one player has blocked the other."));
            return;
        }

        boolean wasAccepted = m_friendRequestRepository.acceptFriendRequest(playerUUID, targetUUID, m_friendRepository);
        if (!wasAccepted)
        {
            player.sendMessage(MessageUtils.error("No valid friend request from " + resolvedTargetName + "."));
            return;
        }

        player.sendMessage(MessageUtils.success("You accepted " + resolvedTargetName + "'s friend request."));
        m_friendNotificationService.sendFriendRequestAcceptedMessage(targetUUID, player.getUsername());
    }

    //------------------------------------------------------------------------------------------------------------------
    public void rejectFriend(Player player, String targetName)
    {
        UUID targetUUID = findTargetUUID(player, targetName);
        if (targetUUID == null)
        {
            return;
        }

        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        boolean wasRejected = m_friendRequestRepository.rejectFriendRequest(player.getUniqueId(), targetUUID);
        if (!wasRejected)
        {
            player.sendMessage(MessageUtils.error("No pending friend request from " + resolvedTargetName + "."));
            return;
        }

        player.sendMessage(MessageUtils.success("You rejected " + resolvedTargetName + "'s friend request."));
        m_friendNotificationService.sendFriendRequestRejectedMessage(targetUUID, player.getUsername());
    }

    //------------------------------------------------------------------------------------------------------------------
    public void removeFriend(Player player, String targetName)
    {
        UUID targetUUID = findTargetUUID(player, targetName);
        if (targetUUID == null)
        {
            return;
        }

        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        boolean wasRemoved = m_friendRepository.removeFriend(player.getUniqueId(), targetUUID);
        if (!wasRemoved)
        {
            player.sendMessage(MessageUtils.error("You are not friends with " + resolvedTargetName + "."));
            return;
        }

        player.sendMessage(MessageUtils.success("You removed " + resolvedTargetName + " from your friends list."));
    }

    //------------------------------------------------------------------------------------------------------------------
    public void blockPlayer(Player player, String targetName)
    {
        UUID targetUUID = findTargetUUID(player, targetName);
        if (targetUUID == null)
        {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        if (playerUUID.equals(targetUUID))
        {
            player.sendMessage(MessageUtils.error("You cannot block yourself."));
            return;
        }

        boolean wasBlocked = m_blockRepository.blockPlayer(playerUUID, targetUUID);
        if (!wasBlocked)
        {
            player.sendMessage(MessageUtils.error("Failed to block " + resolvedTargetName + "."));
            return;
        }

        m_friendRepository.removeFriend(playerUUID, targetUUID);
        m_friendRequestRepository.deleteRequestsBetween(playerUUID, targetUUID);

        player.sendMessage(MessageUtils.success("You blocked " + resolvedTargetName + "."));
    }

    //------------------------------------------------------------------------------------------------------------------
    public void unblockPlayer(Player player, String targetName)
    {
        UUID targetUUID = findTargetUUID(player, targetName);
        if (targetUUID == null)
        {
            return;
        }

        String resolvedTargetName = m_playerRepository.findUsernameByUuid(targetUUID);

        boolean wasUnblocked = m_blockRepository.unblockPlayer(player.getUniqueId(), targetUUID);
        if (!wasUnblocked)
        {
            player.sendMessage(MessageUtils.error("Failed to unblock " + resolvedTargetName + "."));
            return;
        }

        player.sendMessage(MessageUtils.success("You unblocked " + resolvedTargetName + "."));
    }

    //------------------------------------------------------------------------------------------------------------------
    private UUID findTargetUUID(Player player, String targetName)
    {
        Optional<Player> onlineTargetOptional = m_proxyServer.getPlayer(targetName);
        if (onlineTargetOptional.isPresent())
        {
            Player onlineTarget = onlineTargetOptional.get();
            trackPlayer(onlineTarget);
            return onlineTarget.getUniqueId();
        }

        UUID targetUUID = m_playerRepository.findUuidByUsername(targetName);
        if (targetUUID == null)
        {
            player.sendMessage(MessageUtils.error("That player could not be found. They may need to join the network once first."));
        }

        return targetUUID;
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCurrentServerName(Player player)
    {
        Optional<ServerConnection> currentServer = player.getCurrentServer();

        if (currentServer.isEmpty())
        {
            return "unknown";
        }

        ServerConnection serverConnection = currentServer.get();
        return serverConnection.getServerInfo().getName();
    }
}
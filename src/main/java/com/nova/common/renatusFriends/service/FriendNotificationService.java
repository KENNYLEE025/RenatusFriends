package com.nova.common.renatusFriends.service;

import com.nova.common.renatusFriends.util.MessageUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class FriendNotificationService
{
    private final ProxyServer m_proxyServer;

    //------------------------------------------------------------------------------------------------------------------
    public FriendNotificationService(ProxyServer proxyServer)
    {
        m_proxyServer = proxyServer;
    }

    //------------------------------------------------------------------------------------------------------------------
    public void sendIncomingFriendRequestMessages(UUID targetUUID, String senderName)
    {
        Optional<Player> targetPlayerCheck = m_proxyServer.getPlayer(targetUUID);
        if (targetPlayerCheck.isEmpty())    return;

        Player targetPlayer = targetPlayerCheck.get();

        targetPlayer.sendMessage(MessageUtils.success(senderName + " sent you a friend request."));

        Component acceptButton = Component.text("[Accept] ", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/friend accept " + senderName));

        Component rejectButton = Component.text("[Reject]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/friend reject " + senderName));

        targetPlayer.sendMessage(acceptButton.append(rejectButton));
    }

    //------------------------------------------------------------------------------------------------------------------
    public void sendFriendRequestAcceptedMessage(UUID senderUUID, String accepterName)
    {
        m_proxyServer.getPlayer(senderUUID).ifPresent(sender ->
                sender.sendMessage(MessageUtils.success(accepterName + " accepted your friend request."))
        );
    }

    //------------------------------------------------------------------------------------------------------------------
    public void sendFriendRequestRejectedMessage(UUID senderUUID, String rejecterName)
    {
        m_proxyServer.getPlayer(senderUUID).ifPresent(sender ->
                sender.sendMessage(MessageUtils.error(rejecterName + " rejected your friend request."))
        );
    }

    //------------------------------------------------------------------------------------------------------------------
    public void sendFriendJoinMessage(UUID friendUUID, String joiningPlayerName, String serverName)
    {
        m_proxyServer.getPlayer(friendUUID).ifPresent(friend ->
                friend.sendMessage(MessageUtils.success("Your friend, " + joiningPlayerName + ", has joined the server!"))
        );
    }

    //------------------------------------------------------------------------------------------------------------------
    public void sendFriendLeaveMessage(UUID friendUUID, String leavingPlayerName)
    {
        m_proxyServer.getPlayer(friendUUID).ifPresent(friend ->
                friend.sendMessage(MessageUtils.info("Your friend, " + leavingPlayerName + ", has left the server."))
        );
    }
}
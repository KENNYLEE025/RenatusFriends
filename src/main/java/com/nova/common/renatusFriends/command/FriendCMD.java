package com.nova.common.renatusFriends.command;

import com.nova.common.renatusFriends.service.FriendManager;
import com.nova.common.renatusFriends.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

//----------------------------------------------------------------------------------------------------------------------
public class FriendCMD implements SimpleCommand
{
    private final FriendManager m_friendManager;

    //------------------------------------------------------------------------------------------------------------------
    public FriendCMD(FriendManager friendManager)
    {
        m_friendManager = friendManager;
    }

    //------------------------------------------------------------------------------------------------------------------
    @Override
    public void execute(Invocation invocation)
    {
        if (!(invocation.source() instanceof Player player))
        {
            invocation.source().sendMessage(MessageUtils.error("Only players can use this command."));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            m_friendManager.showCommandUsage(player);
            return;
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            int page = 1;

            if (args.length >= 2)
            {
                try
                {
                    page = Integer.parseInt(args[1]);
                }
                catch (NumberFormatException ignored)
                {
                }
            }

            m_friendManager.showFriendsList(player, page);
            return;
        }

        if (args[0].equalsIgnoreCase("notifications") || args[0].equalsIgnoreCase("sounds"))
        {
            m_friendManager.toggleNotifications(player);
            return;
        }

        if (args.length < 2)
        {
            m_friendManager.showCommandUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        switch (subCommand)
        {
            case "add"      -> m_friendManager.addFriend(player, targetName);
            case "accept"   -> m_friendManager.acceptFriend(player, targetName);
            case "reject"   -> m_friendManager.rejectFriend(player, targetName);
            case "remove"   -> m_friendManager.removeFriend(player, targetName);
            case "block"    -> m_friendManager.blockPlayer(player, targetName);
            case "unblock"  -> m_friendManager.unblockPlayer(player, targetName);
            default         -> m_friendManager.showCommandUsage(player);
        }
    }
}
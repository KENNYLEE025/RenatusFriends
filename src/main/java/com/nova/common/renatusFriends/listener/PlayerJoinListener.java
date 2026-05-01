package com.nova.common.renatusFriends.listener;

import com.nova.common.renatusFriends.service.FriendManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

//----------------------------------------------------------------------------------------------------------------------
public class PlayerJoinListener
{
    private final FriendManager m_friendManager;

    //------------------------------------------------------------------------------------------------------------------
    public PlayerJoinListener(FriendManager friendManager)
    {
        m_friendManager = friendManager;
    }

    //------------------------------------------------------------------------------------------------------------------
    @Subscribe
    public void onPostLogin(PostLoginEvent event)
    {
        m_friendManager.onPlayerJoin(event.getPlayer());
    }

    //------------------------------------------------------------------------------------------------------------------
    @Subscribe
    public void onDisconnect(DisconnectEvent event)
    {
        m_friendManager.onPlayerLeave(event.getPlayer());
    }
}
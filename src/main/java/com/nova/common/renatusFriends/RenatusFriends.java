package com.nova.common.renatusFriends;

import com.google.inject.Inject;
import com.nova.common.renatusFriends.command.FriendCMD;
import com.nova.common.renatusFriends.database.DatabaseManager;
import com.nova.common.renatusFriends.database.FriendDatabase;
import com.nova.common.renatusFriends.database.repository.*;
import com.nova.common.renatusFriends.listener.PlayerJoinListener;
import com.nova.common.renatusFriends.service.FriendManager;
import com.nova.common.renatusFriends.service.FriendNotificationService;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

//----------------------------------------------------------------------------------------------------------------------
@Plugin(id = "rn-friends", name = "RN-Friends", description = "Renatus Network Friends Plugin", authors = {"xxBen, U3J"}, version = "2.0.0")
public class RenatusFriends
{
    //------------------------------------------------------------------------------------------------------------------
    private final ProxyServer   m_proxyServer;
    private final Logger        m_logger;
    private final Path          m_dataDirectory;

    private DatabaseManager     m_databaseManager;
    private FriendDatabase      m_friendRepository;
    private FriendManager       m_friendManager;

    //-------------------------------------------------------------------------------------------------------------------
    @Inject
    public RenatusFriends(ProxyServer proxyServer, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory)
    {
        m_proxyServer = proxyServer;
        m_logger = logger;
        m_dataDirectory = dataDirectory;
    }

    //-------------------------------------------------------------------------------------------------------------------
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        m_databaseManager = new DatabaseManager(m_dataDirectory, m_logger);
        m_databaseManager.initialize();

        m_friendRepository = new FriendDatabase(m_databaseManager, m_logger);
        m_friendRepository.createTables();

        PlayerRepository            playerRepository            = new PlayerRepository(m_databaseManager, m_logger);
        FriendRepository            friendRepository            = new FriendRepository(m_databaseManager, m_logger);
        FriendRequestRepository     friendRequestRepository     = new FriendRequestRepository(m_databaseManager, m_logger);
        BlockRepository             blockRepository             = new BlockRepository(m_databaseManager, m_logger);
        FriendSettingsRepository    friendSettingsRepository    = new FriendSettingsRepository(m_databaseManager, m_logger);
        FriendNotificationService   friendNotificationService   = new FriendNotificationService(m_proxyServer);

        m_friendManager = new FriendManager(
                m_proxyServer,
                playerRepository,
                friendRepository,
                friendRequestRepository,
                blockRepository,
                friendSettingsRepository,
                friendNotificationService
        );

        m_proxyServer.getEventManager().register(this, new PlayerJoinListener(m_friendManager));

        CommandManager commandManager = m_proxyServer.getCommandManager();

        commandManager.register(
                commandManager.metaBuilder("friend")
                        .aliases("friends")
                        .build(),
                new FriendCMD(m_friendManager)
        );

        m_logger.info("Registered /friend command.");
    }
}

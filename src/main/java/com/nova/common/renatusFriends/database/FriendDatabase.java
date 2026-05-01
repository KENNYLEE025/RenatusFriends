package com.nova.common.renatusFriends.database;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

//----------------------------------------------------------------------------------------------------------------------
public class FriendDatabase
{
    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public FriendDatabase(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public void createTables()
    {
        executeStatements(
                getCreatePlayersTableQuery(),
                getCreateFriendsTableQuery(),
                getCreateFriendRequestsTableQuery(),
                getCreateBlockedPlayersTableQuery(),
                getCreateFriendSettingsTableQuery()
        );
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCreatePlayersTableQuery()
    {
        return """
                CREATE TABLE IF NOT EXISTS Players (
                    UUID TEXT PRIMARY KEY NOT NULL,
                    Username VARCHAR(16) NOT NULL UNIQUE
                );
                """;
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCreateFriendsTableQuery()
    {
        return """
                CREATE TABLE IF NOT EXISTS Friends (
                    PlayerUUID TEXT NOT NULL,
                    FriendUUID TEXT NOT NULL,
                    PRIMARY KEY (PlayerUUID, FriendUUID)
                );
                """;
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCreateFriendRequestsTableQuery()
    {
        return """
                CREATE TABLE IF NOT EXISTS FriendRequests (
                    SenderUUID TEXT NOT NULL,
                    TargetUUID TEXT NOT NULL,
                    RequestCreatedMilliseconds INTEGER NOT NULL,
                    RequestExpiredMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY (SenderUUID, TargetUUID)
                );
                """;
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCreateBlockedPlayersTableQuery()
    {
        return """
                CREATE TABLE IF NOT EXISTS BlockedPlayers (
                    PlayerUUID TEXT NOT NULL,
                    BlockedUUID TEXT NOT NULL,
                    PRIMARY KEY (PlayerUUID, BlockedUUID)
                );
                """;
    }

    //------------------------------------------------------------------------------------------------------------------
    private String getCreateFriendSettingsTableQuery()
    {
        return """
                CREATE TABLE IF NOT EXISTS FriendSettings (
                    PlayerUUID TEXT PRIMARY KEY NOT NULL,
                    NotificationsEnabled INTEGER NOT NULL DEFAULT 1
                );
                """;
    }

    //------------------------------------------------------------------------------------------------------------------
    private void executeStatements(String... sqlStatements)
    {
        try (Connection connection = m_databaseManager.getConnection();
             Statement statement = connection.createStatement())
        {
            for (String sqlStatement : sqlStatements)
            {
                statement.execute(sqlStatement);
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to create RenatusFriends tables.", exception);
        }
    }
}
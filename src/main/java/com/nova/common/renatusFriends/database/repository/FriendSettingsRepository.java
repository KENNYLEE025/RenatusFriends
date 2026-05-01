package com.nova.common.renatusFriends.database.repository;

import com.nova.common.renatusFriends.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FriendSettingsRepository
{
    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public FriendSettingsRepository(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean areNotificationsEnabled(UUID playerUUID)
    {
        if (playerUUID == null)
        {
            return true;
        }

        String sql = """
                SELECT NotificationsEnabled
                FROM FriendSettings
                WHERE PlayerUUID = ?
                LIMIT 1
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());

            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return resultSet.getInt("NotificationsEnabled") == 1;
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to read friend notification settings.", exception);
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean setNotificationsEnabled(UUID playerUUID, boolean enabled)
    {
        if (playerUUID == null)
        {
            return false;
        }

        String sql = """
                INSERT INTO FriendSettings (PlayerUUID, NotificationsEnabled)
                VALUES (?, ?)
                ON CONFLICT(PlayerUUID) DO UPDATE SET NotificationsEnabled = excluded.NotificationsEnabled
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setInt(2, enabled ? 1 : 0);

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to set friend notification settings.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean toggleNotifications(UUID playerUUID)
    {
        boolean currentSetting = areNotificationsEnabled(playerUUID);
        boolean updatedSetting = !currentSetting;

        boolean wasUpdateSuccessful = setNotificationsEnabled(playerUUID, updatedSetting);

        return wasUpdateSuccessful ? updatedSetting : currentSetting;
    }
}
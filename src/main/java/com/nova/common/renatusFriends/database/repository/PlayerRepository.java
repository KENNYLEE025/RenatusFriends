package com.nova.common.renatusFriends.database.repository;

import com.nova.common.renatusFriends.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class PlayerRepository
{
    //------------------------------------------------------------------------------------------------------------------
    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public PlayerRepository(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public void insertPlayer(UUID playerUUID, String username)
    {
        if (playerUUID == null || username == null || username.isBlank())
        {
            return;
        }

        String sql = """
                INSERT INTO Players (UUID, Username)
                VALUES (?, ?)
                ON CONFLICT(UUID) DO UPDATE SET Username = excluded.Username
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, username);
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to upsert player.", exception);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public UUID findUuidByUsername(String username)
    {
        if (username == null || username.isBlank())
        {
            return null;
        }

        String sql = """
                SELECT UUID
                FROM Players
                WHERE LOWER(Username) = LOWER(?)
                LIMIT 1
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return UUID.fromString(resultSet.getString("UUID"));
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to find UUID by username.", exception);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------------
    public String findUsernameByUuid(UUID playerUUID)
    {
        if (playerUUID == null)
        {
            return "Unknown";
        }

        String sql = """
                SELECT Username
                FROM Players
                WHERE UUID = ?
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
                    return resultSet.getString("Username");
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to find username by UUID.", exception);
        }

        return playerUUID.toString();
    }
}
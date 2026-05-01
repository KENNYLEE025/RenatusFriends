package com.nova.common.renatusFriends.database.repository;

import com.nova.common.renatusFriends.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class BlockRepository
{
    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public BlockRepository(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean isBlocked(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                SELECT 1
                FROM BlockedPlayers
                WHERE PlayerUUID = ? AND BlockedUUID = ?
                LIMIT 1
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, targetUUID.toString());

            try (ResultSet resultSet = statement.executeQuery())
            {
                return resultSet.next();
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to check blocked player.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean blockPlayer(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null || playerUUID.equals(targetUUID))
        {
            return false;
        }

        String sql = """
                INSERT OR IGNORE INTO BlockedPlayers (PlayerUUID, BlockedUUID)
                VALUES (?, ?)
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, targetUUID.toString());

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to block player.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean unblockPlayer(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                DELETE FROM BlockedPlayers
                WHERE PlayerUUID = ? AND BlockedUUID = ?
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, targetUUID.toString());

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to unblock player.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public List<UUID> getBlockedUUIDs(UUID playerUUID)
    {
        List<UUID> blockedUUIDs = new ArrayList<>();

        if (playerUUID == null)
        {
            return blockedUUIDs;
        }

        String sql = """
                SELECT BlockedUUID
                FROM BlockedPlayers
                WHERE PlayerUUID = ?
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());

            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    blockedUUIDs.add(UUID.fromString(resultSet.getString("BlockedUUID")));
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to get blocked UUIDs.", exception);
        }

        return blockedUUIDs;
    }
}
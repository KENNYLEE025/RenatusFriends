package com.nova.common.renatusFriends.database.repository;

import com.nova.common.renatusFriends.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class FriendRepository
{
    //------------------------------------------------------------------------------------------------------------------
    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public FriendRepository(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean areFriends(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                SELECT 1
                FROM Friends
                WHERE PlayerUUID = ? AND FriendUUID = ?
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
            m_logger.error("Failed to check friendship.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean addFriendPair(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null || playerUUID.equals(targetUUID))
        {
            return false;
        }

        Connection connection = null;

        try
        {
            connection = m_databaseManager.getConnection();
            connection.setAutoCommit(false);

            String sql = """
                    INSERT OR IGNORE INTO Friends (PlayerUUID, FriendUUID)
                    VALUES (?, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, targetUUID.toString());
                statement.executeUpdate();

                statement.setString(1, targetUUID.toString());
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();

                connection.commit();
                return true;
            }
            catch (SQLException exception)
            {
                connection.rollback();
                m_logger.error("Failed to add friend pair.", exception);
                return false;
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to add friend pair.", exception);
            return false;
        }
        finally
        {
            closeTransactionConnection(connection);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean removeFriend(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                DELETE FROM Friends
                WHERE (PlayerUUID = ? AND FriendUUID = ?)
                   OR (PlayerUUID = ? AND FriendUUID = ?)
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, targetUUID.toString());
            statement.setString(3, targetUUID.toString());
            statement.setString(4, playerUUID.toString());

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to remove friend.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public List<UUID> getFriendUUIDs(UUID playerUUID)
    {
        List<UUID> friendUUIDs = new ArrayList<>();

        if (playerUUID == null)
        {
            return friendUUIDs;
        }

        String sql = """
                SELECT FriendUUID
                FROM Friends
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
                    friendUUIDs.add(UUID.fromString(resultSet.getString("FriendUUID")));
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to get friend UUIDs.", exception);
        }

        return friendUUIDs;
    }

    //------------------------------------------------------------------------------------------------------------------
    private void closeTransactionConnection(Connection connection)
    {
        if (connection == null)
        {
            return;
        }

        try
        {
            connection.setAutoCommit(true);
            connection.close();
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to close transaction connection.", exception);
        }
    }
}
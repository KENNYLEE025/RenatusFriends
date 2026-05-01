package com.nova.common.renatusFriends.database.repository;

import com.nova.common.renatusFriends.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------------------------
public class FriendRequestRepository
{
    private static final long FRIEND_REQUEST_DURATION_MILLISECONDS = 60_000L;

    private final DatabaseManager m_databaseManager;
    private final Logger m_logger;

    //------------------------------------------------------------------------------------------------------------------
    public FriendRequestRepository(DatabaseManager databaseManager, Logger logger)
    {
        m_databaseManager = databaseManager;
        m_logger = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean sendFriendRequest(UUID senderUUID, UUID targetUUID)
    {
        if (senderUUID == null || targetUUID == null || senderUUID.equals(targetUUID))
        {
            return false;
        }

        String sql = """
                INSERT INTO FriendRequests
                (SenderUUID, TargetUUID, RequestCreatedMilliseconds, RequestExpiredMilliseconds)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(SenderUUID, TargetUUID) DO UPDATE SET
                    RequestCreatedMilliseconds = excluded.RequestCreatedMilliseconds,
                    RequestExpiredMilliseconds = excluded.RequestExpiredMilliseconds
                """;

        long currentTimeMilliseconds = System.currentTimeMillis();
        long expiresAtMilliseconds = currentTimeMilliseconds + FRIEND_REQUEST_DURATION_MILLISECONDS;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, senderUUID.toString());
            statement.setString(2, targetUUID.toString());
            statement.setLong(3, currentTimeMilliseconds);
            statement.setLong(4, expiresAtMilliseconds);

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to send friend request.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean hasValidFriendRequest(UUID senderUUID, UUID targetUUID)
    {
        if (senderUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                SELECT 1
                FROM FriendRequests
                WHERE SenderUUID = ? AND TargetUUID = ? AND RequestExpiredMilliseconds > ?
                LIMIT 1
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, senderUUID.toString());
            statement.setString(2, targetUUID.toString());
            statement.setLong(3, System.currentTimeMillis());

            try (ResultSet resultSet = statement.executeQuery())
            {
                return resultSet.next();
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to check valid friend request.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean acceptFriendRequest(UUID playerUUID, UUID targetUUID, FriendRepository friendRepository)
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

            String deleteSQLStatement = """
                    DELETE FROM FriendRequests
                    WHERE SenderUUID = ? AND TargetUUID = ? AND RequestExpiredMilliseconds > ?
                    """;

            String addFriendSQLStatement = """
                    INSERT OR IGNORE INTO Friends (PlayerUUID, FriendUUID)
                    VALUES (?, ?)
                    """;

            try (PreparedStatement deleteRequestStatement = connection.prepareStatement(deleteSQLStatement);
                 PreparedStatement addFriendStatement = connection.prepareStatement(addFriendSQLStatement))
            {
                long currentTimeMilliseconds = System.currentTimeMillis();

                deleteRequestStatement.setString(1, targetUUID.toString());
                deleteRequestStatement.setString(2, playerUUID.toString());
                deleteRequestStatement.setLong(3, currentTimeMilliseconds);

                int deletedRows = deleteRequestStatement.executeUpdate();
                if (deletedRows == 0)
                {
                    connection.rollback();
                    return false;
                }

                addFriendStatement.setString(1, playerUUID.toString());
                addFriendStatement.setString(2, targetUUID.toString());
                addFriendStatement.executeUpdate();

                addFriendStatement.setString(1, targetUUID.toString());
                addFriendStatement.setString(2, playerUUID.toString());
                addFriendStatement.executeUpdate();

                connection.commit();
                return true;
            }
            catch (SQLException exception)
            {
                connection.rollback();
                m_logger.error("Failed to accept friend request.", exception);
                return false;
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to accept friend request.", exception);
            return false;
        }
        finally
        {
            closeTransactionConnection(connection);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean rejectFriendRequest(UUID playerUUID, UUID targetUUID)
    {
        if (playerUUID == null || targetUUID == null)
        {
            return false;
        }

        String sql = """
                DELETE FROM FriendRequests
                WHERE SenderUUID = ? AND TargetUUID = ?
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, targetUUID.toString());
            statement.setString(2, playerUUID.toString());

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to reject friend request.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public boolean deleteRequestsBetween(UUID firstUUID, UUID secondUUID)
    {
        if (firstUUID == null || secondUUID == null)
        {
            return false;
        }

        String sql = """
                DELETE FROM FriendRequests
                WHERE (SenderUUID = ? AND TargetUUID = ?)
                   OR (SenderUUID = ? AND TargetUUID = ?)
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, firstUUID.toString());
            statement.setString(2, secondUUID.toString());
            statement.setString(3, secondUUID.toString());
            statement.setString(4, firstUUID.toString());

            return statement.executeUpdate() > 0;
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to delete friend requests between players.", exception);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public int cleanupExpiredFriendRequests()
    {
        String sql = """
                DELETE FROM FriendRequests
                WHERE RequestExpiredMilliseconds <= ?
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, System.currentTimeMillis());
            return statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to cleanup expired friend requests.", exception);
            return 0;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public List<UUID> getIncomingFriendRequestUUIDs(UUID playerUUID)
    {
        List<UUID> requestSenderUUIDs = new ArrayList<>();

        if (playerUUID == null)
        {
            return requestSenderUUIDs;
        }

        String sql = """
                SELECT SenderUUID
                FROM FriendRequests
                WHERE TargetUUID = ? AND RequestExpiredMilliseconds > ?
                ORDER BY RequestCreatedMilliseconds DESC
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setLong(2, System.currentTimeMillis());

            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    requestSenderUUIDs.add(UUID.fromString(resultSet.getString("SenderUUID")));
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to get incoming friend requests.", exception);
        }

        return requestSenderUUIDs;
    }

    //------------------------------------------------------------------------------------------------------------------
    public List<UUID> getOutgoingFriendRequestUUIDs(UUID playerUUID)
    {
        List<UUID> requestTargetUUIDs = new ArrayList<>();

        if (playerUUID == null)
        {
            return requestTargetUUIDs;
        }

        String sql = """
                SELECT TargetUUID
                FROM FriendRequests
                WHERE SenderUUID = ? AND RequestExpiredMilliseconds > ?
                ORDER BY RequestCreatedMilliseconds DESC
                """;

        try (Connection connection = m_databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUUID.toString());
            statement.setLong(2, System.currentTimeMillis());

            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    requestTargetUUIDs.add(UUID.fromString(resultSet.getString("TargetUUID")));
                }
            }
        }
        catch (SQLException exception)
        {
            m_logger.error("Failed to get outgoing friend requests.", exception);
        }

        return requestTargetUUIDs;
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
package com.nova.common.renatusFriends.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

//----------------------------------------------------------------------------------------------------------------------
public class DatabaseManager
{
    private final Path          m_dataDirectory;
    private final Logger        m_logger;

    private HikariDataSource    m_dataSource;

    //------------------------------------------------------------------------------------------------------------------
    public DatabaseManager(Path dataDirectory, Logger logger)
    {
        m_dataDirectory = dataDirectory;
        m_logger        = logger;
    }

    //------------------------------------------------------------------------------------------------------------------
    public void initialize()
    {
        try
        {
            Files.createDirectories(m_dataDirectory);
            Path databasePath = m_dataDirectory.resolve("renatusfriends.db");

            Class.forName("org.sqlite.JDBC");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
            hikariConfig.setPoolName("RenatusFriends-Hikari");
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(60000);
            hikariConfig.setMaxLifetime(0);

            m_dataSource = new HikariDataSource(hikariConfig);

            m_logger.info("RenatusFriends database initialized at {}", databasePath.toAbsolutePath());
        }
        catch (Exception exception)
        {
            throw new RuntimeException("Failed to initialize RenatusFriends database.", exception);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public Connection getConnection() throws SQLException
    {
        if (m_dataSource == null)
        {
            throw new IllegalStateException("Database has not been initialized.");
        }

        return m_dataSource.getConnection();
    }

    //------------------------------------------------------------------------------------------------------------------
    public void close()
    {
        if (m_dataSource != null && !m_dataSource.isClosed())
        {
            m_dataSource.close();
        }
    }
}
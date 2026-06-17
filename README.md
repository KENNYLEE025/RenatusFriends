# RenatusFriends

A Velocity plugin that provides a Friend Command system for the Renatus Network.

## Overview

RenatusFriends is a Java-based plugin designed for the **Velocity proxy server**. It adds friend list management capabilities, allowing players on the Renatus Network to manage their friends and interact with friend-related commands.

This plugin uses:
- **Velocity API 3.4.0** for proxy-level functionality
- **HikariCP** for efficient database connection pooling
- **SQLite** for persistent friend data storage

## Who Is This For?

This plugin is intended for:
- Server administrators running a **Velocity-based proxy** network
- Networks that want to provide friend management features across multiple backend servers
- Communities like the **Renatus Network** that need cross-server social features

## Prerequisites

- **Java 17+** (as specified in the project configuration)
- **Velocity Proxy Server** (3.4.0 compatible)
- **Maven 3.6+** (for building from source)

## Installation & Setup

### Option 1: Download Pre-Built JAR

If a pre-built JAR is available, download it and place it in your Velocity server's `plugins` directory:
```bash
plugins/
├── RenatusFriends.jar
└── ...
```

Then restart your Velocity server:
```bash
./velocity
```

### Option 2: Build from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/KENNYLEE025/RenatusFriends.git
   cd RenatusFriends
   ```

2. **Build the project using Maven:**
   ```bash
   mvn clean package
   ```

3. **Locate the built JAR:**
   ```bash
   target/renatusfriends-1.0-SNAPSHOT.jar
   ```

4. **Place the JAR in your Velocity server's `plugins` directory:**
   ```bash
   cp target/renatusfriends-1.0-SNAPSHOT.jar /path/to/velocity/plugins/
   ```

5. **Restart your Velocity server:**
   ```bash
   ./velocity
   ```

## Features

- **Friend Management**: Add, remove, and manage friends
- **Cross-Server Support**: Friend lists persist across your network
- **Database Persistence**: Uses SQLite for reliable data storage
- **Connection Pooling**: HikariCP ensures efficient database operations

## Configuration

After the plugin loads, configuration files will be created in your Velocity server's `plugins/renatusfriends/` directory. Customize these settings to match your network's needs.

## Usage

Once installed and running, players can use friend-related commands to manage their social interactions. Refer to the in-game help or plugin documentation for specific commands and usage.

## Building & Development

This project is built with **Maven** and uses standard Java tooling.

### Build Commands

- **Clean and package:**
  ```bash
  mvn clean package
  ```

- **Compile only:**
  ```bash
  mvn compile
  ```

- **Run tests:**
  ```bash
  mvn test
  ```

### Project Structure

```
RenatusFriends/
├── pom.xml                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/          # Plugin source code
│   │   └── resources/     # Configuration templates
│   └── test/              # Unit tests
└── target/                # Build output
```

## Dependencies

- **Velocity API** (3.4.0-SNAPSHOT): Core Velocity proxy server API
- **HikariCP** (5.1.0): Database connection pooling
- **SQLite JDBC** (3.45.1.0): SQLite database driver

## Support & Issues

For bugs, feature requests, or questions, please open an issue on the [GitHub repository](https://github.com/KENNYLEE025/RenatusFriends/issues).

## License

See the repository for license information.

---

**Made for the Renatus Network**

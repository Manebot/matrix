package io.manebot.plugin.matrix.database.model;

import io.manebot.chat.Chat;
import io.manebot.chat.Community;
import io.manebot.database.Database;
import io.manebot.database.model.TimedRow;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformConnection;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Entity
@Table(
        indexes = {
                @Index(columnList = "endpoint", unique = true),
                @Index(columnList = "enabled")
        },
        uniqueConstraints = {@UniqueConstraint(columnNames ={"endpoint"})}
)
public class MatrixHomeserver extends TimedRow implements Community {
    @Transient
    private final Database database;

    @Transient
    private MatrixHomeserverConnection connection;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column()
    private int matrixHomeserverId;

    @Column()
    private String id;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false, length = 1024)
    private String username;

    @Column(nullable = false, length = 1024)
    private String password;

    @Column(nullable = true, length = 1024)
    private String accessToken;

    @Column(nullable = true)
    private String deviceId;

    @Column(nullable = true)
    private String displayName;

    @Column
    private boolean enabled;

    public MatrixHomeserver(Database database, String id, String endpoint, String username, String password) {
        this.database = database;
        this.id = id;
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
    }

    public MatrixHomeserver(Database database) {
        this.database = database;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Platform getPlatform() {
        return database.getDatabaseManager().getBot().getPlatformById("ts3");
    }

    @Override
    public Collection<String> getChatIds() {
        MatrixHomeserverConnection connection = this.connection;
        if (connection == null || !connection.isConnected()) return Collections.emptyList();

        return connection.getChatIds();
    }

    @Override
    public Collection<Chat> getChats() {
        MatrixHomeserverConnection connection = this.connection;
        if (connection == null || !connection.isConnected())
            return Collections.emptyList();

        return connection.getChatIds().stream()
                .map(connection::getChatById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        MatrixHomeserverConnection connection = this.connection;
        if (connection == null || !connection.isConnected()) return Collections.emptyList();

        return connection.getUserIds();
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        MatrixHomeserverConnection connection = this.connection;
        if (connection == null || !connection.isConnected())
            return Collections.emptyList();

        return connection.getUserIds().stream()
                .map(connection::getUserById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Chat getDefaultChat() {
        throw new UnsupportedOperationException();
    }

    public void remove() {
        try {
            database.executeTransaction(s -> { s.remove(MatrixHomeserver.this); });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (this.displayName == null || !this.displayName.equals(displayName)) {
            try {
                this.displayName = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.displayName = displayName;
                    model.setUpdated(System.currentTimeMillis());
                    return displayName;
                });

                MatrixHomeserverConnection serverConnection = getConnection();
                if (serverConnection != null && serverConnection.isConnected()) {
                    serverConnection.onNicknameChanged(displayName);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean isConnected() {
        MatrixHomeserverConnection connection = getConnection();
        return connection != null && connection.isConnected();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!this.enabled == enabled) {
            try {
                this.enabled = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.enabled = enabled;
                    model.setUpdated(System.currentTimeMillis());
                    return enabled;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            if (!enabled && isConnected())
                getConnection().disconnectAsync();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (this.username == null || !this.username.equals(username)) {
            try {
                this.username = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.username = username;
                    model.setUpdated(System.currentTimeMillis());
                    return username;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.password == null || !this.password.equals(password)) {
            try {
                this.password = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.password = password;
                    model.setUpdated(System.currentTimeMillis());
                    return password;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        if (this.accessToken == null || !this.accessToken.equals(accessToken)) {
            try {
                this.accessToken = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.accessToken = accessToken;
                    model.setUpdated(System.currentTimeMillis());
                    return accessToken;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (this.deviceId == null || !this.deviceId.equals(deviceId)) {
            try {
                this.deviceId = database.executeTransaction(s -> {
                    MatrixHomeserver model = s.find(MatrixHomeserver.class, matrixHomeserverId);
                    model.deviceId = deviceId;
                    model.setUpdated(System.currentTimeMillis());
                    return deviceId;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public MatrixHomeserverConnection getConnection() {
        return connection;
    }

    public void setConnection(MatrixHomeserverConnection connection) {
        this.connection = connection;
    }

    public MatrixHomeserverConnection connectAsync() {
        MatrixHomeserverConnection serverConnection = connection;
        if (serverConnection == null) throw new NullPointerException();

        if (!isEnabled()) throw new IllegalStateException();

        serverConnection.connectAsync().exceptionally((e) -> {
            Logger.getGlobal().log(Level.WARNING, "Problem connecting to Matrix homeserver \"" + id + "\"", e);

            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException ex) {
                return serverConnection;
            }

            if (isEnabled()) return connectAsync();
            else return serverConnection;
        });

        return serverConnection;
    }
}

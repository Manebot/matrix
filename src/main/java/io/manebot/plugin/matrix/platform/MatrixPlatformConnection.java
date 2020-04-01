package io.manebot.plugin.matrix.platform;

import io.manebot.chat.*;
import io.manebot.platform.*;
import io.manebot.plugin.*;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.chat.MatrixChat;
import io.manebot.plugin.matrix.platform.homeserver.HomeserverManager;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;
import io.manebot.plugin.matrix.platform.user.MatrixPlatformUser;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class MatrixPlatformConnection extends AbstractPlatformConnection {
    private final Platform platform;
    private final Plugin plugin;
    private final HomeserverManager serverManager;
    private final List<MatrixHomeserverConnection> connections = new LinkedList<>();

    public MatrixPlatformConnection(Platform platform, Plugin plugin) {
        this.platform = platform;
        this.plugin = plugin;
        this.serverManager = plugin.getInstance(HomeserverManager.class);
    }

    @Override
    protected PlatformUser loadUserById(String userId) {
        Matcher matcher = MatrixPlatformUser.PATTERN.matcher(userId);
        if (!matcher.find()) throw new IllegalArgumentException("user ID is not in acceptable Matrix user ID format");
        String host = matcher.group(2);

        MatrixHomeserverConnection connection = connections.stream()
                .filter(x -> x.getHost().equalsIgnoreCase(host)).findFirst().orElse(null);

        if (connection == null || !connection.isConnected())
            return null;

        return connection.getUserById(userId);
    }

    @Override
    protected Chat loadChatById(String chatId) {
        Matcher matcher = MatrixChat.PATTERN.matcher(chatId);
        if (!matcher.find()) {
            throw new IllegalArgumentException("chat ID is not in acceptable Matrix chat ID format");
        }

        String host = matcher.group(2);

        // attempt direct connection resolution first
        MatrixHomeserverConnection connection = connections.stream()
                .filter(x -> x.getHost().equalsIgnoreCase(host))
                .max(Comparator.comparing(MatrixHomeserverConnection::isConnected))
                .orElse(null);

        // attempt indirect resolution
        if (connection == null) {
            connection = connections.stream()
                    .filter(x -> x.getChatIds().contains(chatId))
                    .max(Comparator.comparing(MatrixHomeserverConnection::isConnected))
                    .orElse(null);
        }

        if (connection != null) {
            return connection.getChatById(chatId);
        } else {
            return null;
        }
    }

    @Override
    protected Community loadCommunityById(String communityId) {
        return serverManager.getServer(communityId);
    }

    @Override
    public MatrixPlatformUser getPlatformUser(String id) {
        return (MatrixPlatformUser) super.getPlatformUser(id);
    }

    @Override
    public MatrixChat getChat(String id) {
        return (MatrixChat) super.getChat(id);
    }

    public MatrixHomeserverConnection connectToServer(MatrixHomeserver homeserver) {
        MatrixHomeserverConnection serverConnection = new MatrixHomeserverConnection(this, homeserver);
        connections.add(serverConnection);
        homeserver.setConnection(serverConnection);
        return homeserver.connectAsync();
    }

    @Override
    public void connect() throws PluginException {
        for (MatrixHomeserver server : serverManager.getServers()) {
            if (server.isEnabled() && !server.isConnected()) {
                try {
                    connectToServer(server);
                } catch (Exception e) {
                    getPlugin().getLogger().log(Level.WARNING, "Problem connecting to server " + server.getId(), e);
                }
            }
        }
    }

    @Override
    public PlatformUser getSelf() {
        return null;
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        return connections.stream().filter(MatrixHomeserverConnection::isConnected)
                .flatMap(connection -> connection.getUserIds().stream())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getChatIds() {
        return connections.stream().filter(MatrixHomeserverConnection::isConnected)
                .flatMap(connection -> connection.getChatIds().stream())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getCommunityIds() {
        return serverManager.getServers().stream().map(MatrixHomeserver::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Community> getCommunities() {
        return serverManager.getServers().stream().map(homeserver -> (Community) homeserver)
                .collect(Collectors.toList());
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Platform getPlatform() {
        return platform;
    }
}

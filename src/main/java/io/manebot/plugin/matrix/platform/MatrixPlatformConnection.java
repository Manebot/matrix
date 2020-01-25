package io.manebot.plugin.matrix.platform;

import io.manebot.chat.*;
import io.manebot.platform.*;
import io.manebot.plugin.*;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.homeserver.HomeserverManager;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
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
    protected PlatformUser loadUserById(String s) {
        return null;
    }

    @Override
    protected Chat loadChatById(String s) {
        return null;
    }

    @Override
    protected Community loadCommunityById(String id) {
        return null;
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
        return null;
    }

    @Override
    public Collection<String> getChatIds() {
        return null;
    }

    @Override
    public Collection<String> getCommunityIds() {
        return getCommunities().stream().map(Community::getId).collect(Collectors.toList());
    }

    @Override
    public Collection<Community> getCommunities() {
        return connections.stream().map(connection -> (Community) connection).collect(Collectors.toList());
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Platform getPlatform() {
        return platform;
    }
}

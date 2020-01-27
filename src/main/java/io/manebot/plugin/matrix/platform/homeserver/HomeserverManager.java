package io.manebot.plugin.matrix.platform.homeserver;

import io.manebot.database.Database;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginReference;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;

import java.sql.SQLException;
import java.util.Collection;

public class HomeserverManager implements PluginReference {
    private final Database database;

    public HomeserverManager(Database database) {
        this.database = database;
    }

    public MatrixHomeserver getServer(String id) {
        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + MatrixHomeserver.class.getName() + " x WHERE x.id=:id",
                    MatrixHomeserver.class
            ).setParameter("id", id)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);
        });
    }

    public MatrixHomeserver addServer(String id, String endpoint, String username, String password) {
        try {
            return database.executeTransaction(s -> {
                MatrixHomeserver homeserver = new MatrixHomeserver(database, id, endpoint, username, password);
                s.persist(homeserver);
                return homeserver;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public MatrixHomeserver removeServer(String id) {
        MatrixHomeserver server = getServer(id);
        if (server != null) server.remove();
        return server;
    }

    public Collection<MatrixHomeserver> getServers() {
        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + MatrixHomeserver.class.getName() + " x",
                    MatrixHomeserver.class
            ).getResultList();
        });
    }

    @Override
    public void load(Plugin.Future future) {}

    @Override
    public void unload(Plugin.Future future) {}
}

package io.manebot.plugin.matrix.platform.user;

import io.manebot.chat.Chat;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;

import java.util.Collection;
import java.util.regex.Pattern;

public class MatrixPlatformUser implements PlatformUser {
    public static final Pattern PATTERN = Pattern.compile("\\@(.+):(.+)");

    private final MatrixHomeserverConnection connection;
    private final String id;
    private final String nickname;

    public MatrixPlatformUser(MatrixHomeserverConnection connection, String id, String nickname) {
        this.connection = connection;
        this.id = id;
        this.nickname = nickname;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public Platform getPlatform() {
        return connection.getPlatformConnection().getPlatform();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Status getStatus() {
        return connection.getUserStatus(getId());
    }

    @Override
    public boolean isSelf() {
        if (id == null) return false;
        String selfId = connection.getSelfId();
        return selfId != null && selfId.equals(id);
    }

    @Override
    public Collection<Chat> getChats() {
        throw new UnsupportedOperationException();
    }
}

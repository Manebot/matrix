package io.manebot.plugin.matrix.platform.user;

import io.manebot.chat.Chat;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;

import java.util.Collection;

public class MatrixPlatformUser implements PlatformUser {
    @Override
    public Platform getPlatform() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isSelf() {
        return false;
    }

    @Override
    public Collection<Chat> getChats() {
        return null;
    }
}

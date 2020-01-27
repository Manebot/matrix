package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.chat.Community;
import io.manebot.chat.TextFormat;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MatrixChat implements Chat {
    public static final Pattern PATTERN = Pattern.compile("\\@(\\w+):(\\w+)");

    private final MatrixHomeserverConnection connection;
    private final String id;

    public MatrixChat(MatrixHomeserverConnection connection, String id) {
        this.id = id;
        this.connection = connection;
    }

    @Override
    public boolean isBuffered() {
        return true;
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
    public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void removeMember(String platformId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMember(String platformId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Community getCommunity() {
        return connection.getHomeserver();
    }

    @Override
    public Collection<ChatMessage> getLastMessages(int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        return null;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean canChangeTypingStatus() {
        return true;
    }

    @Override
    public void setTyping(boolean typing) {

    }

    @Override
    public boolean isTyping() {
        return false;
    }

    @Override
    public TextFormat getFormat() {
        return MatrixTextFormat.INSTANCE;
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        MatrixChatMessage.Builder builder = new MatrixChatMessage.Builder(
                (MatrixPlatformConnection) getPlatformConnection(),
                connection.getHomeserver(),
                connection.getSelfUser(),
                this
        );

        function.accept(builder);

        MatrixChatMessage chatMessage = builder.build();
        connection.sendRawMessage(getId(), chatMessage);
        return Collections.singletonList(chatMessage);
    }

    @Override
    public boolean canSendEmbeds() {
        return false;
    }
}

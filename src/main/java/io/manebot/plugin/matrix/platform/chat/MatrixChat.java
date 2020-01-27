package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.*;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.plugin.matrix.platform.homeserver.MatrixHomeserverConnection;
import io.manebot.plugin.matrix.platform.homeserver.model.Room;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MatrixChat implements Chat {
    public static final Pattern PATTERN = Pattern.compile("\\!(.+):(.+)");

    private final MatrixHomeserverConnection connection;
    private final String id;
    private final Room room;

    public MatrixChat(MatrixHomeserverConnection connection, String id, Room room) {
        this.id = id;
        this.connection = connection;
        this.room = room;
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
    public String getTopic() {
        return room.getTopic();
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
        return room.getUserIds().stream()
                .map(connection::getUserById)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPrivate() {
        Collection<String> userIds = room.getUserIds();
        return userIds.size() == 2 && userIds.contains(connection.getSelfId());
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
    public boolean canSendEmoji() {
        return true;
    }

    @Override
    public TextBuilder text() {
        return new MatrixTextBuilder(this);
    }

    @Override
    public TextFormat getFormat() {
        return MatrixTextFormat.INSTANCE;
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        if (!room.isJoined())
            throw new IllegalStateException("not a member of room: " + getId());

        MatrixChatMessage.Builder builder = new MatrixChatMessage.Builder(
                (MatrixPlatformConnection) getPlatformConnection(),
                connection.getHomeserver(),
                connection.getSelfUser(),
                this
        );

        function.accept(builder);

        MatrixChatMessage chatMessage = builder.build();
        connection.sendMessage(getId(), chatMessage);
        return Collections.singletonList(chatMessage);
    }

    @Override
    public boolean canSendEmbeds() {
        return false;
    }
}

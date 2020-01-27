package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.DefaultChatSender;
import io.manebot.plugin.matrix.platform.user.MatrixPlatformUser;

public class MatrixChatSender extends DefaultChatSender {
    private final MatrixPlatformUser user;
    private final MatrixChat chat;

    public MatrixChatSender(MatrixPlatformUser user, MatrixChat chat) {
        super(user, chat);

        this.user = user;
        this.chat = chat;
    }

    @Override
    public MatrixPlatformUser getPlatformUser() {
        return user;
    }

    @Override
    public MatrixChat getChat() {
        return chat;
    }
}
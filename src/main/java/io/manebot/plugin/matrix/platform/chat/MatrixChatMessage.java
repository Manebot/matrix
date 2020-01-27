package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.*;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.plugin.matrix.platform.user.MatrixPlatformUser;
import org.jsoup.Jsoup;

import java.util.Calendar;
import java.util.Date;

public class MatrixChatMessage extends BasicTextChatMessage {
    private final Date date;
    private final String rawMessage;

    public MatrixChatMessage(Date date, MatrixChatSender sender, String message, String rawMessage) {
        super(sender, message);

        this.date = date;
        this.rawMessage = rawMessage;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }

    public static class Builder extends BasicTextChatMessage.Builder {
        private final MatrixPlatformConnection platformConnection;
        private final MatrixHomeserver server;
        private final MatrixPlatformUser user;
        private final MatrixChat chat;

        public Builder(MatrixPlatformConnection platformConnection,
                       MatrixHomeserver server,
                       MatrixPlatformUser user,
                       MatrixChat chat) {
            super(user, chat);
            this.platformConnection = platformConnection;
            this.server = server;

            this.user = user;
            this.chat = chat;
        }

        @Override
        public MatrixPlatformUser getUser() {
            return user;
        }

        @Override
        public MatrixChat getChat() {
            return chat;
        }

        @Override
        public Builder rawMessage(String message) {
            super.rawMessage(message);
            return this;
        }

        public MatrixChatMessage build() {
            return new MatrixChatMessage(
                    Calendar.getInstance().getTime(),
                    new MatrixChatSender(user, chat),
                    Jsoup.parse(getMessage()).text(),
                    getMessage()
            );
        }
    }
}

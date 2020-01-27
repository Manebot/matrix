package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.DefaultTextBuilder;
import io.manebot.chat.TextBuilder;

public class MatrixTextBuilder extends DefaultTextBuilder {
    public MatrixTextBuilder(Chat chat) {
        super(chat, MatrixTextFormat.INSTANCE);
    }

    @Override
    public TextBuilder newLine() {
        return appendRaw("<br/>");
    }

    @Override
    public TextBuilder appendUrl(String url) {
        return appendRaw("<a href=\"" + url + "\">" + getFormat().escape(url) + "</a>");
    }
}

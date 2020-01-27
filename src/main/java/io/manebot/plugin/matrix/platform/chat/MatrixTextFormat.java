package io.manebot.plugin.matrix.platform.chat;

import io.manebot.chat.TextFormat;
import io.manebot.chat.TextStyle;
import io.manebot.platform.PlatformUser;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class MatrixTextFormat implements TextFormat {
    private static final String mentionFormat = "<a href=\"https://matrix.to/#/%s\">%s</a>";

    public static final MatrixTextFormat INSTANCE = new MatrixTextFormat();

    @Override
    public boolean shouldMention(PlatformUser user) {
        return user.isConnected();
    }

    @Override
    public String mention(PlatformUser user) {
        return String.format(mentionFormat, user.getId(), user.getNickname());
    }

    @Override
    public String format(String string, EnumSet<TextStyle> styles) {
        if (string.trim().length() <= 0) return string;

        List<TextStyle> list = new ArrayList<>(styles);

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i ++) {
            TextStyle style = list.get(i);
            String key = styleToKey(style);
            if (key != null) builder.append("<").append(key).append(">");
        }

        builder.append(escape(string));

        for (int i = list.size()-1; i >= 0; i --) {
            TextStyle style = list.get(i);
            String key = styleToKey(style);
            if (key != null) builder.append("</").append(key).append(">");
        }

        return builder.toString();
    }

    private static String styleToKey(TextStyle style) {
        switch (style) {
            case BOLD:
                return "strong";
            case ITALICS:
                return "em";
            case STRIKE_THROUGH:
                return "del";
            case UNDERLINE:
                return "u";
            default:
                return null;
        }
    }

    @Override
    public String escape(String string) {
        return StringEscapeUtils.escapeHtml4(string);
    }

}

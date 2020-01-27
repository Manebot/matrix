package io.manebot.plugin.matrix.platform.homeserver.model;

import java.util.*;

public class Room {
    private final String id;
    private final Set<String> userIds = new HashSet<>();
    private boolean joined = false;
    private boolean direct = false;
    private String name;
    private String topic;

    public Room(String id) {
        this.id = id;
        this.name = id;
    }

    public String getId() {
        return id;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public void addUserId(String userId) {
        userIds.add(userId);
    }

    public void removeUserId(String userId) {
        userIds.remove(userId);
    }

    public Collection<String> getUserIds() {
        return Collections.unmodifiableCollection(userIds);
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}

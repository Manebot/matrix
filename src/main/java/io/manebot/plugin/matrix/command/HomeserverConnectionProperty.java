package io.manebot.plugin.matrix.command;

import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.tuple.Pair;

import java.util.function.Consumer;

public enum HomeserverConnectionProperty {
    NICKNAME("nickname", (pair) -> pair.getLeft().setDisplayName(pair.getRight())),
    USERNAME("username", (pair) -> pair.getLeft().setUsername(pair.getRight())),
    PASSWORD("token", (pair) -> pair.getLeft().setToken(pair.getRight()));

    private final Consumer<Pair<MatrixHomeserver, String>> setter;
    private final String name;

    HomeserverConnectionProperty(String name, Consumer<Pair<MatrixHomeserver, String>> setter) {
        this.name = name;
        this.setter = setter;
    }

    public String getName() {
        return name;
    }

    public Consumer<Pair<MatrixHomeserver, String>> getSetter() {
        return setter;
    }

    public static HomeserverConnectionProperty fromName(String propertyKey) {
        for (HomeserverConnectionProperty property : values())
             if (property.getName().equalsIgnoreCase(propertyKey)) return property;

        throw new IllegalArgumentException("Property not recognized.");
    }
}
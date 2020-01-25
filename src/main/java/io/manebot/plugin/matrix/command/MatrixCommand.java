package io.manebot.plugin.matrix.command;

import io.manebot.chat.TextStyle;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.*;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.plugin.matrix.platform.homeserver.HomeserverManager;
import io.manebot.tuple.Pair;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MatrixCommand extends AnnotatedCommandExecutor {
    private final HomeserverManager serverManager;
    private final MatrixPlatformConnection platformConnection;

    public MatrixCommand(Plugin plugin, HomeserverManager serverManager) {
        this.serverManager = serverManager;
        this.platformConnection = (MatrixPlatformConnection) plugin.getPlatformById("matrix").getConnection();
    }

    @Command(description = "Lists Matrix homeservers", permission = "matrix.server.list")
    public void list(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "homeserver") String server,
                     @CommandArgumentLabel.Argument(label = "list") String list,
                     @CommandArgumentPage.Argument int page)
            throws CommandExecutionException {
        sender.sendList(
                MatrixHomeserver.class,
                builder -> builder.direct(
                        serverManager.getServers()
                                .stream()
                                .sorted(Comparator.comparing(MatrixHomeserver::getId))
                                .collect(Collectors.toList()))
                        .page(page)
                        .responder((textBuilder, s) ->
                                textBuilder.append(s.getId().toLowerCase(), EnumSet.of(TextStyle.BOLD))
                                        .append(s.isConnected() ? " (connected)" : ""))
                        .build()
        );
    }

    @Command(description = "Gets Matrix homeserver information", permission = "matrix.server.info")
    public void info(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                     @CommandArgumentLabel.Argument(label = "info") String info,
                     @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver not found.");

        sender.sendDetails(builder -> builder.name("homeserver").key(server.getId().toLowerCase())
                .item("Enabled", Boolean.toString(server.isEnabled()))
                .item("Connected", Boolean.toString(server.isConnected()))
                .item("Address", server.getEndpoint())
                .item("Username", server.getUsername())
                .item("Display Name", server.getDisplayName())
        );
    }

    @Command(description = "Adds a Matrix homeserver", permission = "matrix.server.add")
    public void add(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                    @CommandArgumentLabel.Argument(label = "add") String add,
                    @CommandArgumentString.Argument(label = "id") String id,
                    @CommandArgumentString.Argument(label = "endpoint") String endpoint,
                    @CommandArgumentString.Argument(label = "username") String username,
                    @CommandArgumentString.Argument(label = "token") String token)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server != null) throw new CommandArgumentException("Homeserver \"" + id + "\" already exists.");

        server = serverManager.addServer(id, endpoint, username, token);

        sender.sendMessage("Homeserver \"" + server.getId() + "\" created.");
    }

    @Command(description = "Removes a Matrix homeserver from the list", permission = "matrix.server.remove")
    public void remove(CommandSender sender,
                       @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                       @CommandArgumentLabel.Argument(label = "remove") String remove,
                       @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver does not exist.");

        server.setEnabled(false);

        server = serverManager.removeServer(id);

        sender.sendMessage("Homeserver \"" + server.getId() + "\" removed.");
    }

    @Command(description = "Enables a homeserver", permission = "matrix.server.enable")
    public void enable(CommandSender sender,
                       @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                       @CommandArgumentLabel.Argument(label = "enable") String enable,
                       @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver does not exist.");

        if (server.isEnabled()) throw new CommandArgumentException("Homeserver is already enabled.");

        server.setEnabled(true);

        try {
            platformConnection.connectToServer(server);
        } catch (Exception e) {
            throw new CommandExecutionException("Problem connecting to Matrix homeserver", e);
        }

        sender.sendMessage("Homeserver \"" + server.getId() + "\" enabled.");
    }

    @Command(description = "Disables a homeserver", permission = "matrix.server.disable")
    public void disable(CommandSender sender,
                        @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                        @CommandArgumentLabel.Argument(label = "disable") String disable,
                        @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver does not exist.");

        if (!server.isEnabled()) throw new CommandArgumentException("Homeserver is already disabled.");

        server.setEnabled(false);
        sender.sendMessage("Homeserver \"" + server.getId() + "\" disabled.");
    }

    @Command(description = "Sets a homeserver property", permission = "matrix.server.property.set")
    public void set(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                    @CommandArgumentLabel.Argument(label = "set") String enable,
                    @CommandArgumentString.Argument(label = "id") String id,
                    @CommandArgumentSwitch.Argument(labels = {"username","token","nickname"}) String propertyKey,
                    @CommandArgumentFollowing.Argument() String value)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver does not exist.");

        HomeserverConnectionProperty property = HomeserverConnectionProperty.fromName(propertyKey);
        property.getSetter().accept(new Pair<>(server, value));

        sender.sendMessage("Homeserver \"" + server.getId() + "\" property \"" +
                property.getName() + "\" set to \"" + value + "\".");
    }

    @Command(description = "Unsets a homeserver property", permission = "matrix.server.property.unset")
    public void unset(CommandSender sender,
                      @CommandArgumentLabel.Argument(label = "homeserver") String serverLabel,
                      @CommandArgumentLabel.Argument(label = "unset") String disable,
                      @CommandArgumentString.Argument(label = "id") String id,
                      @CommandArgumentSwitch.Argument(labels = {"username","token","nickname"}) String propertyKey)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        MatrixHomeserver server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Homeserver does not exist.");

        HomeserverConnectionProperty property = HomeserverConnectionProperty.valueOf(propertyKey);
        property.getSetter().accept(new Pair<>(server, null));

        sender.sendMessage("Homeserver \"" + server.getId() + "\" property \"" + property.getName() + "\" unset.");
    }
}

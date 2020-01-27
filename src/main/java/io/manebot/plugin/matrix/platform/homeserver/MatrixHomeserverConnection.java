package io.manebot.plugin.matrix.platform.homeserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.ma1uta.matrix.client.MatrixClient;

import io.github.ma1uta.matrix.client.factory.jaxrs.JaxRsRequestFactory;
import io.github.ma1uta.matrix.client.model.auth.LoginRequest;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import io.github.ma1uta.matrix.client.model.profile.Profile;
import io.github.ma1uta.matrix.client.model.room.JoinRequest;
import io.github.ma1uta.matrix.client.model.sync.*;
import io.github.ma1uta.matrix.client.sync.SyncLoop;
import io.github.ma1uta.matrix.client.sync.SyncParams;


import io.github.ma1uta.matrix.event.Event;
import io.github.ma1uta.matrix.event.Presence;

import io.github.ma1uta.matrix.event.RoomMessage;
import io.github.ma1uta.matrix.event.content.EventContent;
import io.github.ma1uta.matrix.event.message.Text;
import io.github.ma1uta.matrix.support.jackson.JacksonContextResolver;
import io.manebot.chat.ChatMessage;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.plugin.matrix.platform.chat.MatrixChat;
import io.manebot.plugin.matrix.platform.chat.MatrixChatMessage;
import io.manebot.plugin.matrix.platform.chat.MatrixChatSender;
import io.manebot.plugin.matrix.platform.user.MatrixPlatformUser;
import io.manebot.virtual.Virtual;
import org.jsoup.Jsoup;

import javax.annotation.Nonnull;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class MatrixHomeserverConnection {
    private static final String userIdFormat = "@%s:%s";

    private final LoadingCache<String, MatrixPlatformUser> users;
    private final LoadingCache<String, MatrixChat> chats;
    private final Map<String, PlatformUser.Status> statuses = new LinkedHashMap<>();

    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(1, Virtual.getInstance());
    private final MatrixPlatformConnection platformConnection;
    private final MatrixHomeserver homeserver;

    private String selfId;
    private MatrixClient client;
    private SyncLoop syncLoop;

    public MatrixHomeserverConnection(MatrixPlatformConnection platformConnection, MatrixHomeserver homeserver) {
        this.platformConnection = platformConnection;
        this.homeserver = homeserver;

        this.users = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .maximumSize(1024)
                .build(new CacheLoader<>() {
                    @Override
                    public MatrixPlatformUser load(@Nonnull String userId) {
                        return loadUserById(userId);
                    }
                });

        this.chats = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .maximumSize(1024)
                .build(new CacheLoader<>() {
                    @Override
                    public MatrixChat load(@Nonnull String userId) {
                        return loadChatById(userId);
                    }
                });
    }

    private MatrixPlatformUser loadUserById(@Nonnull String userId) {
        Profile profile = client.profile().profile(userId).join();
        String displayName = profile.getDisplayName();
        if (displayName == null) {
            Matcher matcher = MatrixPlatformUser.PATTERN.matcher(userId);
            if (!matcher.find()) throw new IllegalArgumentException("userId is not in acceptable Matrix user ID format");
            String username = matcher.group(1);
            displayName = username;
        }
        return new MatrixPlatformUser(this, userId, displayName);
    }

    public MatrixPlatformUser getUserById(@Nonnull String userId) {
        try {
            return users.get(userId);
        } catch (ExecutionException e) {
            getPlugin().getLogger().log(Level.WARNING, "Problem loading user " + userId, e);
            return null;
        }
    }

    public MatrixPlatformUser getSelfUser() {
        return getUserById(getSelfId());
    }

    public Collection<String> getUserIds() {
        return Collections.unmodifiableCollection(users.asMap().keySet());
    }

    private MatrixChat loadChatById(@Nonnull String chatId) {
        return new MatrixChat(this, chatId);
    }

    public MatrixChat getChatById(@Nonnull String chatId) {
        try {
            return chats.get(chatId);
        } catch (ExecutionException e) {
            getPlugin().getLogger().log(Level.WARNING, "Problem loading chat " + chatId, e);
            return null;
        }
    }

    public Collection<String> getChatIds() {
        return Collections.unmodifiableCollection(chats.asMap().keySet());
    }

    public void sendRawMessage(String roomId, MatrixChatMessage chatMessage) {
        if (!isConnected())
            throw new IllegalStateException("not connected");

        client.event().sendFormattedMessage(roomId, chatMessage.getMessage(), chatMessage.getRawMessage()).join();
    }

    public MatrixHomeserver getHomeserver() {
        return homeserver;
    }

    public PlatformUser.Status getUserStatus(String userId) {
        return Objects.requireNonNullElse(statuses.get(userId), PlatformUser.Status.UNKNOWN);
    }

    public String getHost() {
        try {
            return URI.create(getHomeserver().getEndpoint()).toURL().getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public MatrixPlatformConnection getPlatformConnection() {
        return platformConnection;
    }

    private Plugin getPlugin() {
        return platformConnection.getPlugin();
    }

    public MatrixHomeserverConnection connect() throws IOException, InterruptedException {
        try {
            return connectAsync().get();
        } catch (ExecutionException e) {
            throw new IOException("Problem connecting to Matrix homeserver" + homeserver.getId(), e);
        }
    }

    public CompletableFuture<MatrixHomeserverConnection> connectAsync() {
        getPlugin().getLogger().log(Level.FINE, "Connecting to Matrix homeserver " + homeserver.getId() + "...");

        LoginRequest request = new LoginRequest();
        request.setUser(homeserver.getUsername());

        if (homeserver.getAccessToken() != null && homeserver.getAccessToken().length() > 0) {
            request.setType("m.login.token");
            request.setToken(homeserver.getAccessToken());
        } else if (homeserver.getPassword() != null && homeserver.getPassword().length() > 0) {
            request.setType("m.login.password");
            request.setPassword(homeserver.getPassword().toCharArray());
            request.setInitialDeviceDisplayName("io.manebot.plugin:matrix");
        } else {
            throw new IllegalArgumentException("No valid login options available");
        }

        return CompletableFuture.supplyAsync(() -> {
            MatrixClient client = createMatrixClient(homeserver.getEndpoint());

            LoginResponse loginResponse;

            if (homeserver.getAccessToken() == null || homeserver.getAccessToken().length() <= 0) {
                getPlugin().getLogger().log(Level.FINE, "Logging in to Matrix homeserver " +
                        homeserver.getId() + "...");

                // Login and request a brand new device ID
                loginResponse = client.auth().login(request).join();
            } else {
                getPlugin().getLogger().log(Level.FINE, "Restoring state for Matrix homeserver " +
                        homeserver.getId() + "...");

                // Load from saved state
                loginResponse = new LoginResponse();
                loginResponse.setUserId(homeserver.getUsername());
                loginResponse.setAccessToken(homeserver.getAccessToken());
                loginResponse.setDeviceId(homeserver.getDeviceId());
                loginResponse.setHomeServer(URI.create(client.getHomeserverUrl()).getHost());
            }

            client.afterLogin(loginResponse);

            if (loginResponse.getAccessToken() != null &&
                    (homeserver.getAccessToken() == null || !loginResponse.getAccessToken().equals(
                            homeserver.getAccessToken()))) {
                homeserver.setAccessToken(loginResponse.getAccessToken());
                getPlugin().getLogger().log(Level.FINE, "Set access token for homeserver " + homeserver.getId());
            }

            if (loginResponse.getDeviceId() != null &&
                    (homeserver.getDeviceId() == null || !loginResponse.getDeviceId().equals(
                            homeserver.getDeviceId()))) {
                homeserver.setDeviceId(loginResponse.getDeviceId());
                getPlugin().getLogger().log(Level.FINE, "Set device ID for homeserver " + homeserver.getId());
            }

            String myUserId;
            if (loginResponse.getUserId().startsWith("@")) {
                myUserId = loginResponse.getUserId();
            } else {
                myUserId = String.format(userIdFormat, loginResponse.getUserId(), loginResponse.getHomeServer());
            }

            // Set display name
            client.profile().setDisplayName(homeserver.getDisplayName());

            // Synchronize with events
            SyncLoop sync = new SyncLoop(client.sync());
            sync.setInboundListener(new BiFunction<>() {
                private boolean initial = true;

                @Override
                public SyncParams apply(SyncResponse syncResponse, SyncParams params) {
                    try {
                        MatrixHomeserverConnection.this.handleEvent(syncResponse, initial);
                    } catch (Throwable ex) {
                        getPlugin().getLogger().log(
                                Level.WARNING,
                                "Problem handling sync event from homeserver " + homeserver.getId(),
                                ex
                        );
                    }

                    initial = false;

                    params.setNextBatch(syncResponse.getNextBatch());
                    params.setFullState(false);
                    return params;
                }
            });

            SyncParams params = new SyncParams();
            params.setTimeout(10_000L);
            params.setFullState(false);
            sync.setInit(params);

            syncExecutor.submit(sync);

            getPlugin().getLogger().log(Level.INFO, "Connected to Matrix homeserver " +
                    homeserver.getId() + " as " + myUserId + ".");

            MatrixHomeserverConnection.this.client = client;
            MatrixHomeserverConnection.this.syncLoop = sync;
            MatrixHomeserverConnection.this.selfId = myUserId;

            return MatrixHomeserverConnection.this;
        });
    }

    public MatrixHomeserverConnection disconnect() {
        if (syncLoop != null) {
            syncLoop.setInboundListener(null);
            syncLoop = null;
        }

        if (client != null) {
            client.close();
            client = null;
        }

        return this;
    }

    private void handleEvent(SyncResponse syncResponse, boolean initial) {
        // Handle all accounts
        syncResponse.getAccountData().getEvents().forEach(this::handleAccountEvent);

        // Handle all presence events
        syncResponse.getPresence().getEvents().forEach((event) -> handlePresenceEvent((Presence) event));

        // Handle all rooms
        syncResponse.getRooms().getJoin().forEach((roomId, room) -> handleRoom(roomId, room, initial));
        syncResponse.getRooms().getInvite().forEach(this::handleInvitedRoom);
        syncResponse.getRooms().getLeave().forEach(this::handleLeftRoom);
    }

    private void handleAccountEvent(Event event) {

    }

    private void handlePresenceEvent(Presence event) {
        String userId = event.getSender();
        String presence = event.getContent().getPresence();

        PlatformUser.Status status;
        switch (presence) {
            case "online":
                status = PlatformUser.Status.ONLINE;
                break;
            default:
                status = PlatformUser.Status.UNKNOWN;
                break;
        }

        if (status != PlatformUser.Status.UNKNOWN)
            statuses.put(userId, status);
        else
            statuses.remove(userId);
    }

    private void handleRoom(String roomId, JoinedRoom room, boolean initial) {
        room.getTimeline().getEvents().forEach((event) -> handleRoomEvent(roomId, event, initial));
    }

    private void handleInvitedRoom(String roomId, InvitedRoom room) {
        client.room().joinById(roomId, new JoinRequest());
    }

    private void handleLeftRoom(String roomId, LeftRoom room) {
        chats.invalidate(roomId);
    }

    private void handleRoomEvent(String roomId, Event event, boolean initial) {
        if (initial) {
            getChatById(roomId); // pre-cache
            return;
        }

        if (event instanceof RoomMessage) {
            handleMessage(roomId, (RoomMessage) event);
        }
    }

    private void handleMessage(String roomId, RoomMessage message) {
        EventContent eventContent = message.getContent();
        String canonicalMessage, textMessage;
        String userId = message.getSender();
        Date date = new Date(message.getOriginServerTs());

        if (eventContent instanceof Text) {
            Text text = (Text) eventContent;
            canonicalMessage = text.getBody();

            if (text.getFormattedBody() != null) {
                switch (text.getFormat()) {
                    case "org.matrix.custom.html":
                        textMessage = Jsoup.parse(text.getFormattedBody()).text();
                        break;
                    default:
                        return;
                }
            } else {
                textMessage = canonicalMessage;
            }
        } else {
            return;
        }

        // Get chat instance
        MatrixChat chat = getChatById(roomId);

        // Get user instance
        MatrixPlatformUser platformUser = getUserById(userId);

        // Handle message
        MatrixChatSender sender = new MatrixChatSender(platformUser, chat);
        ChatMessage chatMessage = new MatrixChatMessage(date, sender, textMessage, canonicalMessage);
        platformConnection.getPlugin().getBot().getChatDispatcher().executeAsync(chatMessage);
    }

    public void onNicknameChanged(String displayName) {
        client.profile().setDisplayName(displayName);
    }

    public CompletionStage<MatrixHomeserverConnection> disconnectAsync() {
        return CompletableFuture.supplyAsync(() -> {
            disconnect();
            return this;
        });
    }

    private static MatrixClient createMatrixClient(String endpoint) {
        return new MatrixClient(new JaxRsRequestFactory(
                ClientBuilder.newBuilder().register(new JacksonContextResolver()).build(),
                endpoint,
                Executors.newScheduledThreadPool(4, Virtual.getInstance())
        ));
    }

    public boolean isConnected() {
        return client != null && client.getAccessToken() != null;
    }

    public String getSelfId() {
        return selfId;
    }
}

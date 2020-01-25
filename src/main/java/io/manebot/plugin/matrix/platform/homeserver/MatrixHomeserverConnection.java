package io.manebot.plugin.matrix.platform.homeserver;

import io.github.ma1uta.matrix.client.MatrixClient;

import io.github.ma1uta.matrix.client.factory.RequestFactory;
import io.github.ma1uta.matrix.client.factory.jaxrs.JaxRsRequestFactory;
import io.github.ma1uta.matrix.client.model.auth.LoginRequest;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import io.github.ma1uta.matrix.client.model.auth.UserIdentifier;
import io.github.ma1uta.matrix.client.model.sync.SyncResponse;
import io.github.ma1uta.matrix.client.sync.SyncLoop;
import io.github.ma1uta.matrix.client.sync.SyncParams;

import io.github.ma1uta.matrix.support.jackson.JacksonContextResolver;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.MatrixPlatformConnection;
import io.manebot.virtual.Virtual;

import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.util.concurrent.*;

import java.util.logging.Level;

public class MatrixHomeserverConnection {
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(1, Virtual.getInstance());
    private final MatrixPlatformConnection platformConnection;
    private final MatrixHomeserver homeserver;

    private MatrixClient client;
    private SyncLoop syncLoop;

    private boolean disconnecting = false;

    public MatrixHomeserverConnection(MatrixPlatformConnection platformConnection, MatrixHomeserver homeserver) {
        this.platformConnection = platformConnection;
        this.homeserver = homeserver;
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
        request.setType("m.login.token");
        request.setToken(homeserver.getToken());
        request.setInitialDeviceDisplayName("io.manebot.plugin:matrix");

        return CompletableFuture.supplyAsync(() -> {
            MatrixClient client = createMatrixClient(homeserver.getEndpoint());
            LoginResponse loginResponse = client.auth().login(request).join();

            // Set display name
            client.profile().setDisplayName(homeserver.getDisplayName()).join();

            // Synchronize with events
            SyncLoop sync = new SyncLoop(client.sync());
            sync.setInboundListener((syncResponse, params) -> {
                try {
                    MatrixHomeserverConnection.this.handleEvent(syncResponse);
                } catch (Throwable ex) {
                    getPlugin().getLogger().log(
                            Level.WARNING,
                            "Problem handling sync event from homeserver " + homeserver.getId(),
                            ex
                    );
                }

                params.setNextBatch(syncResponse.getNextBatch());
                return params;
            });

            SyncParams params = new SyncParams();
            params.setTimeout(10_000L);
            sync.setInit(params);

            syncExecutor.submit(sync);

            getPlugin().getLogger().log(Level.INFO, "Connected to Matrix homeserver " +
                    homeserver.getId() + " as " + loginResponse.getUserId() + ".");

            MatrixHomeserverConnection.this.client = client;
            return MatrixHomeserverConnection.this;
        });
    }

    public MatrixHomeserverConnection disconnect() {
        disconnecting = true;

        if (client != null) {
            client.close();
            client = null;
        }

        return this;
    }

    private void handleEvent(SyncResponse syncResponse) {

    }

    public void onNicknameChanged(String displayName) {
        client.profile().setDisplayName(displayName);
    }

    public CompletionStage<MatrixHomeserverConnection> disconnectAsync() {
        return CompletableFuture.supplyAsync(() -> {
            disconnecting = true;
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
}

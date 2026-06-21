package com.tuiperacer.network;

import com.tuiperacer.exception.ConnectionException;
import javafx.application.Platform;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RaceServer {

    private final int port;
    private final String text;
    private ServerSocket serverSocket;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<Integer, String> playerNames = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger idCounter = new AtomicInteger(1); // 0 = host
    private volatile boolean running;

    // Callbacks (invoked on FX thread)
    private Consumer<String> onPlayerJoined;       // name of player that joined
    private BiConsumer<Integer, Character> onCharReceived; // (slotId, char)

    public RaceServer(int port, String text) {
        this.port = port;
        this.text = text;
    }

    public void start() throws ConnectionException {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            executor.submit(this::acceptLoop);
        } catch (IOException e) {
            throw new ConnectionException("localhost", port, "Failed to bind: " + e.getMessage());
        }
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket sock = serverSocket.accept();
                int id = idCounter.getAndIncrement();
                ClientHandler handler = new ClientHandler(sock, id, this, text);
                clients.put(id, handler);
                executor.submit(handler);
            } catch (IOException e) {
                if (running) System.err.println("Server accept error: " + e.getMessage());
            }
        }
    }

    // ── Called by ClientHandler ──────────────────────────────────────────────

    /** Sends existing lobby players to a newly connected client. */
    public void sendExistingPlayers(ClientHandler recipient) {
        // slot 0 = host (always present)
        String hostName = playerNames.getOrDefault(0, "Host");
        recipient.sendLine("JOIN:0:" + hostName);
        // other already-connected clients
        for (Map.Entry<Integer, String> e : playerNames.entrySet()) {
            if (e.getKey() != 0 && e.getKey() != recipient.getId()) {
                recipient.sendLine("JOIN:" + e.getKey() + ":" + e.getValue());
            }
        }
    }

    /** Stores the name for a connected slot. */
    public void registerName(int id, String name) {
        playerNames.put(id, name);
    }

    /** Broadcasts JOIN:<id>:<name> to all connected clients. */
    public void broadcastJoin(int id, String name) {
        String msg = "JOIN:" + id + ":" + name;
        for (ClientHandler c : clients.values()) c.sendLine(msg);
        if (onPlayerJoined != null) {
            Platform.runLater(() -> onPlayerJoined.accept(name));
        }
    }

    /** Broadcasts C:<fromId>:<codepoint> to everyone except the sender. */
    public void broadcastChar(int fromId, int codepoint) {
        String msg = "C:" + fromId + ":" + codepoint;
        for (ClientHandler c : clients.values()) {
            if (c.getId() != fromId) c.sendLine(msg);
        }
        // Notify RaceController about chars from clients (not host's own chars)
        if (fromId != 0 && onCharReceived != null) {
            char ch = (char) codepoint;
            Platform.runLater(() -> onCharReceived.accept(fromId, ch));
        }
    }

    /**
     * Sends host's char to all clients AND notifies RaceController
     * so other local PlayerTcp objects are updated.
     * Called from RaceController after the local player successfully types a char.
     */
    public void broadcastHostChar(char c) {
        broadcastChar(0, (int) c);
    }

    /** Sends START to all connected clients. */
    public void broadcastStart() {
        for (ClientHandler c : clients.values()) c.sendLine("START");
    }

    /** Registers the host (slot 0) name in the player map. */
    public void setHostName(String name) {
        playerNames.put(0, name);
    }

    public void removeClient(int id) {
        clients.remove(id);
    }

    public void stop() {
        running = false;
        for (ClientHandler c : clients.values()) c.close();
        clients.clear();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    // ── Getters / setters ────────────────────────────────────────────────────

    public int getClientCount() { return clients.size(); }
    public Map<Integer, String> getPlayerNames() { return playerNames; }
    public boolean isRunning() { return running; }
    public int getPort() { return port; }
    public String getText() { return text; }

    public void setOnPlayerJoined(Consumer<String> cb) { this.onPlayerJoined = cb; }
    public void setOnCharReceived(BiConsumer<Integer, Character> cb) { this.onCharReceived = cb; }
}

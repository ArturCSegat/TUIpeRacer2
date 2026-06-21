package com.tuiperacer.network;

import com.tuiperacer.exception.ConnectionException;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class RaceClient {

    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean connected;

    // Callbacks (invoked on FX thread via Platform.runLater)
    private Consumer<String> onTextReceived;       // race text
    private IntConsumer onMyIdReceived;            // my slot number
    private BiConsumer<Integer, String> onPlayerJoined; // (id, name)
    private Runnable onStartReceived;              // host started the race
    private BiConsumer<Integer, Character> onCharReceived; // (slotId, char)
    private Consumer<String> onError;

    public RaceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws ConnectionException {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            connected = true;
            executor.submit(this::readLoop);
        } catch (IOException e) {
            throw new ConnectionException(host, port, e.getMessage());
        }
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line;
                if (msg.startsWith("TEXT:")) {
                    String text = msg.substring(5);
                    if (onTextReceived != null)
                        Platform.runLater(() -> onTextReceived.accept(text));

                } else if (msg.startsWith("MYID:")) {
                    try {
                        int id = Integer.parseInt(msg.substring(5));
                        if (onMyIdReceived != null)
                            Platform.runLater(() -> onMyIdReceived.accept(id));
                    } catch (NumberFormatException ignored) {}

                } else if (msg.startsWith("JOIN:")) {
                    String[] parts = msg.substring(5).split(":", 2);
                    if (parts.length == 2) {
                        try {
                            int id = Integer.parseInt(parts[0]);
                            String name = parts[1];
                            if (onPlayerJoined != null)
                                Platform.runLater(() -> onPlayerJoined.accept(id, name));
                        } catch (NumberFormatException ignored) {}
                    }

                } else if (msg.equals("START")) {
                    if (onStartReceived != null)
                        Platform.runLater(onStartReceived);

                } else if (msg.startsWith("C:")) {
                    String[] parts = msg.substring(2).split(":", 2);
                    if (parts.length == 2) {
                        try {
                            int id = Integer.parseInt(parts[0]);
                            char c = (char) Integer.parseInt(parts[1]);
                            if (onCharReceived != null)
                                Platform.runLater(() -> onCharReceived.accept(id, c));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            if (connected && onError != null)
                Platform.runLater(() -> onError.accept("Conexão perdida: " + e.getMessage()));
        } finally {
            connected = false;
        }
    }

    /** Sends our display name to the server. Call immediately after connect(). */
    public void sendName(String name) {
        if (out != null) out.println("NAME:" + name);
    }

    /** Sends a typed character to the server. */
    public void sendChar(char c) {
        if (out != null && connected) out.println("C:" + (int) c);
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    public boolean isConnected() { return connected; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    public void setOnTextReceived(Consumer<String> cb) { this.onTextReceived = cb; }
    public void setOnMyIdReceived(IntConsumer cb) { this.onMyIdReceived = cb; }
    public void setOnPlayerJoined(BiConsumer<Integer, String> cb) { this.onPlayerJoined = cb; }
    public void setOnStartReceived(Runnable cb) { this.onStartReceived = cb; }
    public void setOnCharReceived(BiConsumer<Integer, Character> cb) { this.onCharReceived = cb; }
    public void setOnError(Consumer<String> cb) { this.onError = cb; }
}

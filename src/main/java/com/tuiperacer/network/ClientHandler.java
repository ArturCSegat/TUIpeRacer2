package com.tuiperacer.network;

import java.io.*;
import java.net.Socket;

/**
 * Handles one connected client using the line-based race protocol.
 *
 * Server → client:
 *   TEXT:<text>        race text
 *   MYID:<n>           this client's player slot
 *   JOIN:<n>:<name>    a player joined the lobby
 *   START              race is starting now
 *   C:<n>:<codepoint>  player n typed a character
 *
 * Client → server:
 *   NAME:<name>        first message (client's display name)
 *   C:<codepoint>      I typed this character
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int id;
    private final RaceServer server;
    private final String raceText;
    private PrintWriter out;
    private String playerName = "Player";

    public ClientHandler(Socket socket, int id, RaceServer server, String raceText) {
        this.socket = socket;
        this.id = id;
        this.server = server;
        this.raceText = raceText;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);

            // Send race text and this client's slot number
            out.println("TEXT:" + raceText);
            out.println("MYID:" + id);

            // Let the new client know who is already in the lobby
            server.sendExistingPlayers(this);

            // Wait for client's name
            String nameLine = in.readLine();
            if (nameLine != null && nameLine.startsWith("NAME:")) {
                playerName = nameLine.substring(5).trim();
                if (playerName.isEmpty()) playerName = "Player" + id;
            }

            // Announce this player to all (including themselves)
            server.broadcastJoin(id, playerName);
            server.registerName(id, playerName);

            // Relay loop
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("C:")) {
                    try {
                        int codepoint = Integer.parseInt(line.substring(2));
                        server.broadcastChar(id, codepoint);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            // client disconnected
        } finally {
            close();
            server.removeClient(id);
        }
    }

    public void sendLine(String line) {
        if (out != null) out.println(line);
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public int getId() { return id; }
    public String getPlayerName() { return playerName; }
}

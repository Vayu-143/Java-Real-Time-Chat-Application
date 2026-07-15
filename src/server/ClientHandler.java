package server;

import common.ChatLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private final Socket socket;

    private BufferedReader reader;

    private PrintWriter writer;

    private String username;

    private volatile boolean authenticated;

    private volatile boolean connected = true;

    public ClientHandler(Socket socket) {

        this.socket = socket;
    }

    @Override
    public void run() {

        try {

            initializeStreams();

            authenticateUser();

            if (!authenticated) {
                return;
            }

            sendWelcomeMessage();

            ChatServer.sendSystemMessage(
                    username
                            + " joined the chat. "
                            + "("
                            + ChatServer.getOnlineUserCount()
                            + " users online)"
            );

            String message;

            while (connected
                    && (message = reader.readLine())
                    != null) {

                processMessage(
                        message.trim()
                );
            }

        } catch (SocketException e) {

            System.out.println(
                    "[DISCONNECTED] "
                            + username
            );

        } catch (IOException e) {

            System.err.println(
                    "[CLIENT ERROR] "
                            + e.getMessage()
            );

        } finally {

            disconnect();
        }
    }

    private void initializeStreams()
            throws IOException {

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );

        writer =
                new PrintWriter(
                        socket.getOutputStream(),
                        true
                );
    }

    private void authenticateUser()
            throws IOException {

        while (connected) {

            writer.println(
                    "ENTER_USERNAME"
            );

            String requestedUsername =
                    reader.readLine();

            if (requestedUsername == null) {
                return;
            }

            requestedUsername =
                    requestedUsername.trim();

            if (!isValidUsername(
                    requestedUsername)) {

                writer.println(
                        "INVALID_USERNAME"
                );

                continue;
            }

            synchronized (ChatServer.class) {

                if (ChatServer.isUsernameTaken(
                        requestedUsername)) {

                    writer.println(
                            "USERNAME_TAKEN"
                    );

                    continue;
                }

                username =
                        requestedUsername;

                authenticated = true;
            }

            writer.println(
                    "USERNAME_ACCEPTED"
            );

            ChatLogger.log(
                    "LOGIN | User: "
                            + username
                            + " | Address: "
                            + socket
                            .getRemoteSocketAddress()
            );

            break;
        }
    }

    private boolean isValidUsername(
            String username) {

        return username != null
                && username.matches(
                "[A-Za-z0-9_]{3,20}"
        );
    }

    private void sendWelcomeMessage() {

        sendMessage(
                "========================================"
        );

        sendMessage(
                " Welcome to Java Real-Time Chat"
        );

        sendMessage(
                "========================================"
        );

        sendMessage(
                "[INFO] Logged in as: "
                        + username
        );

        sendMessage(
                "[INFO] Available commands:"
        );

        sendMessage(
                "  /users"
                        + "                 Show online users"
        );

        sendMessage(
                "  /msg <user> <text>"
                        + "     Send private message"
        );

        sendMessage(
                "  /help"
                        + "                  Show commands"
        );

        sendMessage(
                "  /quit"
                        + "                  Disconnect safely"
        );

        sendMessage(
                "========================================"
        );
    }

    private void processMessage(
            String message) {

        if (message.isBlank()) {

            sendMessage(
                    "[ERROR] Empty messages are not allowed."
            );

            return;
        }

        if (message.length() > 1000) {

            sendMessage(
                    "[ERROR] Message exceeds 1000 characters."
            );

            return;
        }

        if (message.equalsIgnoreCase(
                "/quit")) {

            connected = false;

            sendMessage(
                    "[SYSTEM] Disconnecting..."
            );

            return;
        }

        if (message.equalsIgnoreCase(
                "/users")) {

            ChatServer.sendActiveUsers(this);

            return;
        }

        if (message.equalsIgnoreCase(
                "/help")) {

            sendHelp();

            return;
        }

        if (message.toLowerCase()
                .startsWith("/msg ")) {

            processPrivateMessage(
                    message
            );

            return;
        }

        if (message.startsWith("/")) {

            sendMessage(
                    "[ERROR] Unknown command. Type /help."
            );

            return;
        }

        ChatServer.broadcast(
                "[" + username + "] "
                        + message,
                this
        );
    }

    private void processPrivateMessage(
            String message) {

        String[] parts =
                message.split(
                        "\\s+",
                        3
                );

        if (parts.length < 3
                || parts[2].isBlank()) {

            sendMessage(
                    "[ERROR] Usage: "
                            + "/msg <username> <message>"
            );

            return;
        }

        ChatServer.sendPrivateMessage(
                parts[1],
                parts[2],
                this
        );
    }

    private void sendHelp() {

        sendMessage(
                "========== COMMANDS =========="
        );

        sendMessage(
                "/users - View online users"
        );

        sendMessage(
                "/msg <username> <message>"
                        + " - Private message"
        );

        sendMessage(
                "/help - Display this menu"
        );

        sendMessage(
                "/quit - Leave the chat"
        );

        sendMessage(
                "=============================="
        );
    }

    public synchronized void sendMessage(
            String message) {

        if (writer != null
                && connected) {

            writer.println(message);
        }
    }

    public String getUsername() {

        return username;
    }

    public boolean isAuthenticated() {

        return authenticated;
    }

    private void disconnect() {

        if (!connected
                && !authenticated) {

            closeConnection();

            return;
        }

        connected = false;

        boolean wasAuthenticated =
                authenticated;

        authenticated = false;

        ChatServer.removeClient(this);

        if (wasAuthenticated
                && username != null) {

            ChatLogger.log(
                    "LOGOUT | User: "
                            + username
            );

            ChatServer.sendSystemMessage(
                    username
                            + " left the chat. "
                            + "("
                            + ChatServer
                            .getOnlineUserCount()
                            + " users online)"
            );
        }

        closeConnection();
    }

    public void closeConnection() {

        connected = false;

        try {

            if (reader != null) {
                reader.close();
            }

        } catch (IOException ignored) {
        }

        if (writer != null) {
            writer.close();
        }

        try {

            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException ignored) {
        }
    }
}
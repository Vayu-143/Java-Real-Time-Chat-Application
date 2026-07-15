package server;

import common.ChatLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    public static final int DEFAULT_PORT = 5000;

    private static final Set<ClientHandler> clients =
            ConcurrentHashMap.newKeySet();

    private static final ExecutorService threadPool =
            Executors.newCachedThreadPool();

    private static volatile boolean running = true;

    private static ServerSocket serverSocket;

    public static void main(String[] args) {

        int port = DEFAULT_PORT;

        if (args.length > 0) {

            try {
                port = Integer.parseInt(args[0]);

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid port. Using default port "
                                + DEFAULT_PORT
                );

                port = DEFAULT_PORT;
            }
        }

        startServer(port);
    }

    private static void startServer(int port) {

        printBanner();

        try {

            serverSocket = new ServerSocket(port);

            ChatLogger.log(
                    "SERVER STARTED | Port: " + port
            );

            System.out.println(
                    "[SERVER] Server started successfully."
            );

            System.out.println(
                    "[SERVER] Listening on port: " + port
            );

            System.out.println(
                    "[SERVER] Waiting for client connections..."
            );

            System.out.println();

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(ChatServer::shutdown)
                    );

            while (running) {

                try {

                    Socket clientSocket =
                            serverSocket.accept();

                    clientSocket.setKeepAlive(true);

                    System.out.println(
                            "[CONNECTION] New client: "
                                    + clientSocket
                                    .getRemoteSocketAddress()
                    );

                    ClientHandler handler =
                            new ClientHandler(clientSocket);

                    clients.add(handler);

                    threadPool.execute(handler);

                } catch (SocketException e) {

                    if (running) {
                        System.err.println(
                                "[SERVER ERROR] "
                                        + e.getMessage()
                        );
                    }
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "[SERVER ERROR] Unable to start server."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

        } finally {

            shutdown();
        }
    }

    public static void broadcast(
            String message,
            ClientHandler sender) {

        String formattedMessage =
                "[" + ChatLogger.getCurrentTime() + "] "
                        + message;

        for (ClientHandler client : clients) {

            if (client.isAuthenticated()) {
                client.sendMessage(formattedMessage);
            }
        }

        System.out.println(formattedMessage);

        ChatLogger.log(message);
    }

    public static void sendSystemMessage(
            String message) {

        broadcast(
                "[SYSTEM] " + message,
                null
        );
    }

    public static void sendPrivateMessage(
            String targetUsername,
            String message,
            ClientHandler sender) {

        ClientHandler target =
                findClient(targetUsername);

        if (target == null) {

            sender.sendMessage(
                    "[ERROR] User '"
                            + targetUsername
                            + "' is not online."
            );

            return;
        }

        if (target == sender) {

            sender.sendMessage(
                    "[ERROR] You cannot send a private message to yourself."
            );

            return;
        }

        String time =
                ChatLogger.getCurrentTime();

        target.sendMessage(
                "[" + time + "] [PRIVATE] "
                        + sender.getUsername()
                        + " -> You: "
                        + message
        );

        sender.sendMessage(
                "[" + time + "] [PRIVATE] You -> "
                        + target.getUsername()
                        + ": "
                        + message
        );

        ChatLogger.log(
                "PRIVATE | "
                        + sender.getUsername()
                        + " -> "
                        + target.getUsername()
                        + " | "
                        + message
        );
    }

    public static void sendActiveUsers(
            ClientHandler requester) {

        List<String> usernames =
                getActiveUsernames();

        requester.sendMessage(
                "[USERS] Online (" +
                        usernames.size() +
                        "): " +
                        String.join(", ", usernames)
        );
    }

    public static List<String> getActiveUsernames() {

        List<String> usernames =
                new ArrayList<>();

        for (ClientHandler client : clients) {

            if (client.isAuthenticated()) {

                usernames.add(
                        client.getUsername()
                );
            }
        }

        usernames.sort(
                String.CASE_INSENSITIVE_ORDER
        );

        return usernames;
    }

    public static boolean isUsernameTaken(
            String username) {

        return findClient(username) != null;
    }

    private static ClientHandler findClient(
            String username) {

        if (username == null) {
            return null;
        }

        for (ClientHandler client : clients) {

            if (client.getUsername() != null
                    && client.getUsername()
                    .equalsIgnoreCase(username)) {

                return client;
            }
        }

        return null;
    }

    public static void removeClient(
            ClientHandler client) {

        clients.remove(client);
    }

    public static int getOnlineUserCount() {

        int count = 0;

        for (ClientHandler client : clients) {

            if (client.isAuthenticated()) {
                count++;
            }
        }

        return count;
    }

    public static synchronized void shutdown() {

        if (!running) {
            return;
        }

        running = false;

        System.out.println();
        System.out.println(
                "[SERVER] Shutting down..."
        );

        ChatLogger.log(
                "SERVER SHUTDOWN"
        );

        for (ClientHandler client : clients) {

            client.sendMessage(
                    "[SYSTEM] Server is shutting down."
            );

            client.closeConnection();
        }

        clients.clear();

        threadPool.shutdownNow();

        if (serverSocket != null
                && !serverSocket.isClosed()) {

            try {
                serverSocket.close();

            } catch (IOException ignored) {
            }
        }

        System.out.println(
                "[SERVER] Shutdown complete."
        );
    }

    private static void printBanner() {

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "       JAVA REAL-TIME CHAT SERVER"
        );

        System.out.println(
                "=========================================="
        );
    }
}
package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ChatClient {

    private static final String DEFAULT_HOST =
            "localhost";

    private static final int DEFAULT_PORT =
            5000;

    public static void main(String[] args) {

        String host =
                args.length > 0
                        ? args[0]
                        : DEFAULT_HOST;

        int port =
                DEFAULT_PORT;

        if (args.length > 1) {

            try {

                port =
                        Integer.parseInt(
                                args[1]
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid port. Using "
                                + DEFAULT_PORT
                );
            }
        }

        startClient(
                host,
                port
        );
    }

    private static void startClient(
            String host,
            int port) {

        printBanner();

        System.out.println(
                "Connecting to "
                        + host
                        + ":"
                        + port
                        + "..."
        );

        try (
                Socket socket =
                        new Socket(
                                host,
                                port
                        );

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket
                                                .getInputStream()
                                )
                        );

                PrintWriter writer =
                        new PrintWriter(
                                socket
                                        .getOutputStream(),
                                true
                        );

                Scanner scanner =
                        new Scanner(
                                System.in
                        )
        ) {

            System.out.println(
                    "Connected successfully."
            );

            if (!authenticate(
                    reader,
                    writer,
                    scanner)) {

                return;
            }

            Thread receiverThread =
                    createReceiverThread(
                            reader
                    );

            receiverThread.start();

            while (!socket.isClosed()
                    && scanner.hasNextLine()) {

                String message =
                        scanner.nextLine();

                writer.println(message);

                if (message.trim()
                        .equalsIgnoreCase(
                                "/quit"
                        )) {

                    break;
                }
            }

            System.out.println(
                    "Client closed."
            );

        } catch (IOException e) {

            System.err.println(
                    "Unable to connect to server."
            );

            System.err.println(
                    "Make sure ChatServer is running."
            );

            System.err.println(
                    "Reason: "
                            + e.getMessage()
            );
        }
    }

    private static boolean authenticate(
            BufferedReader reader,
            PrintWriter writer,
            Scanner scanner)
            throws IOException {

        while (true) {

            String response =
                    reader.readLine();

            if (response == null) {

                System.out.println(
                        "Server closed connection."
                );

                return false;
            }

            switch (response) {

                case "ENTER_USERNAME" -> {

                    System.out.print(
                            "Enter username: "
                    );

                    writer.println(
                            scanner.nextLine()
                    );
                }

                case "INVALID_USERNAME" ->

                        System.out.println(
                                "Invalid username. "
                                        + "Use 3-20 letters, "
                                        + "numbers or underscores."
                        );

                case "USERNAME_TAKEN" ->

                        System.out.println(
                                "Username already in use. "
                                        + "Choose another."
                        );

                case "USERNAME_ACCEPTED" -> {

                    System.out.println(
                            "Login successful."
                    );

                    return true;
                }

                default ->

                        System.out.println(
                                response
                        );
            }
        }
    }

    private static Thread createReceiverThread(
            BufferedReader reader) {

        Thread thread =
                new Thread(() -> {

                    try {

                        String message;

                        while (
                                (message =
                                        reader.readLine())
                                        != null) {

                            System.out.println(
                                    message
                            );
                        }

                    } catch (SocketException e) {

                        System.out.println(
                                "Connection closed."
                        );

                    } catch (IOException e) {

                        System.out.println(
                                "Lost connection to server."
                        );
                    }

                }, "message-receiver");

        thread.setDaemon(true);

        return thread;
    }

    private static void printBanner() {

        System.out.println(
                "========================================"
        );

        System.out.println(
                "       JAVA REAL-TIME CHAT CLIENT"
        );

        System.out.println(
                "========================================"
        );
    }
}
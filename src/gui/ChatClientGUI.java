package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientGUI extends JFrame {

    private static final String HOST =
            "localhost";

    private static final int PORT =
            5000;

    private final JTextArea chatArea =
            new JTextArea();

    private final JTextField messageField =
            new JTextField();

    private final JButton sendButton =
            new JButton("Send");

    private final JButton usersButton =
            new JButton("Online Users");

    private final JLabel statusLabel =
            new JLabel("Disconnected");

    private Socket socket;

    private BufferedReader reader;

    private PrintWriter writer;

    private volatile boolean connected;

    public ChatClientGUI() {

        initializeWindow();

        initializeComponents();

        connectToServer();
    }

    private void initializeWindow() {

        setTitle(
                "Java Real-Time Chat"
        );

        setSize(
                750,
                550
        );

        setMinimumSize(
                new Dimension(
                        600,
                        450
                )
        );

        setLocationRelativeTo(
                null
        );

        setDefaultCloseOperation(
                DO_NOTHING_ON_CLOSE
        );

        addWindowListener(
                new WindowAdapter() {

                    @Override
                    public void windowClosing(
                            WindowEvent e) {

                        disconnect();

                        dispose();
                    }
                }
        );
    }

    private void initializeComponents() {

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        mainPanel.setBorder(
                new EmptyBorder(
                        15,
                        15,
                        15,
                        15
                )
        );

        JPanel headerPanel =
                new JPanel(
                        new BorderLayout()
                );

        JLabel titleLabel =
                new JLabel(
                        "Java Real-Time Chat"
                );

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        statusLabel.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        headerPanel.add(
                titleLabel,
                BorderLayout.WEST
        );

        headerPanel.add(
                statusLabel,
                BorderLayout.EAST
        );

        chatArea.setEditable(
                false
        );

        chatArea.setLineWrap(
                true
        );

        chatArea.setWrapStyleWord(
                true
        );

        chatArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        chatArea
                );

        JPanel bottomPanel =
                new JPanel(
                        new BorderLayout(
                                8,
                                8
                        )
                );

        bottomPanel.add(
                messageField,
                BorderLayout.CENTER
        );

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                5,
                                0
                        )
                );

        buttonPanel.add(
                usersButton
        );

        buttonPanel.add(
                sendButton
        );

        bottomPanel.add(
                buttonPanel,
                BorderLayout.EAST
        );

        mainPanel.add(
                headerPanel,
                BorderLayout.NORTH
        );

        mainPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        mainPanel.add(
                bottomPanel,
                BorderLayout.SOUTH
        );

        add(mainPanel);

        sendButton.addActionListener(
                e -> sendMessage()
        );

        usersButton.addActionListener(
                e -> sendCommand(
                        "/users"
                )
        );

        messageField.addActionListener(
                e -> sendMessage()
        );

        setInputEnabled(
                false
        );
    }

    private void connectToServer() {

        Thread connectionThread =
                new Thread(() -> {

                    try {

                        socket =
                                new Socket(
                                        HOST,
                                        PORT
                                );

                        reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                socket
                                                        .getInputStream()
                                        )
                                );

                        writer =
                                new PrintWriter(
                                        socket
                                                .getOutputStream(),
                                        true
                                );

                        connected =
                                true;

                        updateStatus(
                                "Connected"
                        );

                        authenticate();

                    } catch (IOException e) {

                        updateStatus(
                                "Disconnected"
                        );

                        showError(
                                "Unable to connect to server.\n"
                                        + "Start ChatServer first."
                        );
                    }
                });

        connectionThread.start();
    }

    private void authenticate()
            throws IOException {

        while (connected) {

            String response =
                    reader.readLine();

            if (response == null) {
                return;
            }

            if (response.equals(
                    "ENTER_USERNAME")) {

                String username =
                        requestUsername();

                if (username == null) {

                    disconnect();

                    SwingUtilities.invokeLater(
                            this::dispose
                    );

                    return;
                }

                writer.println(
                        username
                );

            } else if (
                    response.equals(
                            "INVALID_USERNAME")) {

                showError(
                        "Username must contain "
                                + "3-20 letters, "
                                + "numbers or underscores."
                );

            } else if (
                    response.equals(
                            "USERNAME_TAKEN")) {

                showError(
                        "That username is already online."
                );

            } else if (
                    response.equals(
                            "USERNAME_ACCEPTED")) {

                appendMessage(
                        "[CLIENT] Login successful."
                );

                setInputEnabled(
                        true
                );

                startMessageReceiver();

                return;

            } else {

                appendMessage(
                        response
                );
            }
        }
    }

    private String requestUsername() {

        final String[] result =
                new String[1];

        try {

            SwingUtilities.invokeAndWait(
                    () ->
                            result[0] =
                                    JOptionPane
                                            .showInputDialog(
                                                    this,
                                                    "Enter your username:",
                                                    "Connect to Chat",
                                                    JOptionPane
                                                            .QUESTION_MESSAGE
                                            )
            );

        } catch (Exception e) {

            return null;
        }

        return result[0];
    }

    private void startMessageReceiver() {

        Thread receiverThread =
                new Thread(() -> {

                    try {

                        String message;

                        while (connected
                                && (message =
                                reader.readLine())
                                != null) {

                            appendMessage(
                                    message
                            );
                        }

                    } catch (IOException e) {

                        if (connected) {

                            appendMessage(
                                    "[CLIENT] Connection lost."
                            );
                        }

                    } finally {

                        connected =
                                false;

                        updateStatus(
                                "Disconnected"
                        );

                        setInputEnabled(
                                false
                        );
                    }

                }, "gui-message-receiver");

        receiverThread.setDaemon(
                true
        );

        receiverThread.start();
    }

    private void sendMessage() {

        String message =
                messageField
                        .getText()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        sendCommand(
                message
        );

        messageField.setText(
                ""
        );

        messageField.requestFocusInWindow();
    }

    private void sendCommand(
            String command) {

        if (connected
                && writer != null) {

            writer.println(
                    command
            );
        }
    }

    private void appendMessage(
            String message) {

        SwingUtilities.invokeLater(
                () -> {

                    chatArea.append(
                            message
                                    + System.lineSeparator()
                    );

                    chatArea.setCaretPosition(
                            chatArea
                                    .getDocument()
                                    .getLength()
                    );
                }
        );
    }

    private void updateStatus(
            String status) {

        SwingUtilities.invokeLater(
                () ->
                        statusLabel.setText(
                                "Status: "
                                        + status
                        )
        );
    }

    private void setInputEnabled(
            boolean enabled) {

        SwingUtilities.invokeLater(
                () -> {

                    messageField.setEnabled(
                            enabled
                    );

                    sendButton.setEnabled(
                            enabled
                    );

                    usersButton.setEnabled(
                            enabled
                    );

                    if (enabled) {

                        messageField
                                .requestFocusInWindow();
                    }
                }
        );
    }

    private void showError(
            String message) {

        SwingUtilities.invokeLater(
                () ->
                        JOptionPane
                                .showMessageDialog(
                                        this,
                                        message,
                                        "Chat Application",
                                        JOptionPane
                                                .ERROR_MESSAGE
                                )
        );
    }

    private void disconnect() {

        connected =
                false;

        if (writer != null) {

            writer.println(
                    "/quit"
            );
        }

        try {

            if (socket != null
                    && !socket.isClosed()) {

                socket.close();
            }

        } catch (IOException ignored) {
        }
    }

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    ChatClientGUI gui =
                            new ChatClientGUI();

                    gui.setVisible(
                            true
                    );
                }
        );
    }
}
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MeshNetworkClient extends JFrame {
    private static final int BROADCAST_PORT = 8888;
    private static final int MAX_HOP_COUNT = 5;
    private static final long ACKNOWLEDGMENT_TIMEOUT = 5000; // 5 seconds
    private static final long DEVICE_CLEANUP_INTERVAL = 30000; // 30 seconds

    private JTextArea messageArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel deviceCountLabel;

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean isReceiving = true;

    // Track seen message IDs to prevent duplicates
    private Set<String> seenMessageIds = ConcurrentHashMap.newKeySet();

    // Track connected devices
    private Set<String> connectedDevices = ConcurrentHashMap.newKeySet();

    // Track message delivery status
    private ConcurrentHashMap<String, Boolean> messageDeliveryStatus = new ConcurrentHashMap<>();

    // Unique device ID and username
    private final String DEVICE_ID = UUID.randomUUID().toString();
    private String localUsername = "Anonymous";

    private DatagramSocket receiveSocket;

    public MeshNetworkClient() {
        super("Mesh Network Client");
        promptForUsername();
        initializeComponents();
        setupUI();
        startMessageReceiver();
        startDeviceCleanup();
    }

    private void promptForUsername() {
        String input = JOptionPane.showInputDialog(
                this,
                "Enter your username:",
                "Username",
                JOptionPane.PLAIN_MESSAGE
        );

        localUsername = (input == null || input.trim().isEmpty())
                ? "User-" + DEVICE_ID.substring(0, 8)
                : input.trim();
    }

    private void initializeComponents() {
        executorService = Executors.newCachedThreadPool();
        scheduledExecutor = Executors.newScheduledThreadPool(1);

        try {
            receiveSocket = new DatagramSocket(BROADCAST_PORT);
            receiveSocket.setBroadcast(true);
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to initialize socket: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Status panel at the top
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Network Status: Initializing...");
        deviceCountLabel = new JLabel("Devices: 0");
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(deviceCountLabel);
        add(statusPanel, BorderLayout.NORTH);

        // Message display area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageInput = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Action listeners
        sendButton.addActionListener(e -> sendMessage());
        messageInput.addActionListener(e -> sendMessage());

        // Update status
        updateConnectionStatus();
    }

    private void broadcastMessage(final String message) {
        executorService.execute(() -> {
            try {
                String messageId = UUID.randomUUID().toString();

                // Add to delivery tracking
                messageDeliveryStatus.put(messageId, false);

                // Construct mesh message
                String meshMessage = constructMeshMessage(messageId, message);

                // Update UI immediately for sent message
                SwingUtilities.invokeLater(() -> {
                    messageArea.append(String.format("\n[%s] Me: %s\n",
                            localUsername, message));
                    scrollToBottom();
                });

                // Create UDP broadcast socket
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                // Broadcast to all interfaces
                byte[] sendData = meshMessage.getBytes();
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData,
                                sendData.length,
                                broadcastAddress,
                                BROADCAST_PORT
                        );
                        socket.send(sendPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                socket.close();

                // Schedule acknowledgment timeout
                scheduleAcknowledgmentTimeout(messageId);

            } catch (SocketException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Failed to broadcast message",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void scheduleAcknowledgmentTimeout(String messageId) {
        scheduledExecutor.schedule(() -> {
            Boolean delivered = messageDeliveryStatus.get(messageId);
            if (delivered == null || !delivered) {
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("Message delivery timeout: " + messageId + "\n");
                    scrollToBottom();
                });
            }
        }, ACKNOWLEDGMENT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private String constructMeshMessage(String messageId, String message) {
        // Format: messageId|originDeviceId|hopCount|senderUsername|message
        return String.format("%s|%s|0|%s|%s",
                messageId,
                DEVICE_ID,
                localUsername,
                message
        );
    }

    private void processReceivedMessage(String receivedMessage, String senderIP) {
        String[] parts = receivedMessage.split("\\|");
        if (parts.length != 5) {
            if (parts.length == 2 && parts[0].equals("ACK")) {
                handleAcknowledgment(parts[1]);
                return;
            }
            return; // Malformed message
        }

        String messageId = parts[0];
        String originDeviceId = parts[1];
        int hopCount = Integer.parseInt(parts[2]);
        String senderUsername = parts[3];
        String message = parts[4];

        // Track connected device
        connectedDevices.add(originDeviceId);
        updateDeviceCount();

        // Check for duplicates
        if (seenMessageIds.contains(messageId)) {
            return;
        }

        seenMessageIds.add(messageId);

        // Check hop count
        if (hopCount >= MAX_HOP_COUNT) {
            return;
        }

        // Process message if not from this device
        if (!originDeviceId.equals(DEVICE_ID)) {
            // Send acknowledgment
            sendAcknowledgment(messageId, originDeviceId);

            // Display received message
            SwingUtilities.invokeLater(() -> {
                messageArea.append(String.format("\n[%s] %s: %s\n",
                        senderUsername, senderIP, message));
                scrollToBottom();
            });

            // Rebroadcast message
            rebroadcastMessage(messageId, originDeviceId, hopCount, senderUsername, message);
        }
    }

    private void sendAcknowledgment(String messageId, String targetDeviceId) {
        executorService.execute(() -> {
            try {
                String ackMessage = "ACK|" + messageId;
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] sendData = ackMessage.getBytes();
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData,
                            sendData.length,
                            broadcastAddress,
                            BROADCAST_PORT
                    );
                    socket.send(sendPacket);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleAcknowledgment(String messageId) {
        messageDeliveryStatus.put(messageId, true);
        SwingUtilities.invokeLater(() -> {
            messageArea.append("Message delivered: " + messageId + "\n");
            scrollToBottom();
        });
    }

    private void rebroadcastMessage(String messageId, String originDeviceId,
                                    int hopCount, String senderUsername, String message) {
        executorService.execute(() -> {
            try {
                int newHopCount = hopCount + 1;
                String forwardMessage = String.format("%s|%s|%d|%s|%s",
                        messageId, originDeviceId, newHopCount, senderUsername, message);

                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] sendData = forwardMessage.getBytes();
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData,
                            sendData.length,
                            broadcastAddress,
                            BROADCAST_PORT
                    );
                    socket.send(sendPacket);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void startDeviceCleanup() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            // Clear old devices (implementation depends on how you want to track device activity)
            updateDeviceCount();
        }, DEVICE_CLEANUP_INTERVAL, DEVICE_CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void updateDeviceCount() {
        SwingUtilities.invokeLater(() -> {
            deviceCountLabel.setText("Devices: " + connectedDevices.size());
        });
    }

    // Helper methods from original implementation remain the same:
    // - sendMessage()
    // - getBroadcastAddresses()
    // - startMessageReceiver()
    // - scrollToBottom()
    // - getLocalIpAddress()
    // - cleanup()
    // - main()

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            broadcastMessage(message);
            messageInput.setText("");
        }
    }

    private void startMessageReceiver() {
        executorService.execute(() -> {
            byte[] receiveBuffer = new byte[1024];

            while (isReceiving) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );

                    receiveSocket.receive(receivePacket);

                    String receivedMessage = new String(
                            receivePacket.getData(),
                            0,
                            receivePacket.getLength()
                    );

                    processReceivedMessage(
                            receivedMessage,
                            receivePacket.getAddress().getHostAddress()
                    );

                } catch (IOException e) {
                    if (isReceiving) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void updateConnectionStatus() {
        try {
            String localIP = getLocalIpAddress();
            String status = "Network Status: " +
                    (localIP != null ? "Connected (IP: " + localIP + ")" : "Not Connected");
            statusLabel.setText(status);
        } catch (Exception e) {
            statusLabel.setText("Network Status: Error detecting network");
        }
    }

    private List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        broadcastAddresses.add(broadcast);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (broadcastAddresses.isEmpty()) {
            try {
                broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return broadcastAddresses;
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            inetAddress.getHostAddress().contains(".")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void scrollToBottom() {
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private void cleanup() {
        isReceiving = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdownNow();
        }
    }
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MeshNetworkClient client = new MeshNetworkClient();
            client.setLocationRelativeTo(null);
            client.setVisible(true);
        });
    }
}
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

public class MeshNetworkClient extends JFrame {
    private static final int BROADCAST_PORT = 8888;
    private static final int MAX_HOP_COUNT = 5;

    private JTextArea messageArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JLabel statusLabel;

    private ExecutorService executorService;
    private volatile boolean isReceiving = true;

    // Track seen message IDs to prevent duplicates
    private Set<String> seenMessageIds = ConcurrentHashMap.newKeySet();

    // Unique device ID
    private final String DEVICE_ID = UUID.randomUUID().toString();

    private DatagramSocket receiveSocket;

    public MeshNetworkClient() {
        super("Mesh Network Client");
        initializeComponents();
        setupUI();
        startMessageReceiver();
    }

    private void initializeComponents() {
        executorService = Executors.newCachedThreadPool();
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
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Status panel at the top
        statusLabel = new JLabel("Network Status: Initializing...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        // Message display area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
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

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            broadcastMessage(message);
            messageInput.setText("");
        }
    }

    private void broadcastMessage(final String message) {
        executorService.execute(() -> {
            try {
                // Create a unique message ID to prevent routing loops
                String messageId = UUID.randomUUID().toString();

                // Construct mesh network message
                String meshMessage = constructMeshMessage(messageId, message);

                // Create UDP broadcast socket
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                // Prepare message data
                byte[] sendData = meshMessage.getBytes();

                // Get network interface details
                List<InetAddress> broadcastAddresses = getBroadcastAddresses();

                // Send to all potential broadcast addresses
                for (InetAddress broadcastAddress : broadcastAddresses) {
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

                // Update UI with sent message
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("\nMe: " + message + "\n");
                    scrollToBottom();
                });

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

    private List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
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

        // Add fallback broadcast address if no addresses found
        if (broadcastAddresses.isEmpty()) {
            try {
                broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return broadcastAddresses;
    }

    private void startMessageReceiver() {
        executorService.execute(() -> {
            byte[] receiveBuffer = new byte[1024];

            while (isReceiving) {
                try {
                    // Prepare packet to receive data
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );

                    // Receive incoming packet
                    receiveSocket.receive(receivePacket);

                    // Convert received data to message
                    String receivedMessage = new String(
                            receivePacket.getData(),
                            0,
                            receivePacket.getLength()
                    );

                    // Process the received mesh message
                    processReceivedMessage(
                            receivedMessage,
                            receivePacket.getAddress().getHostAddress()
                    );

                } catch (IOException e) {
                    if (isReceiving) {
                        e.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                    "Failed to receive messages",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }
            }
        });
    }

    private String constructMeshMessage(String messageId, String message) {
        // Format: messageId|originDeviceId|hopCount|message
        return String.format("%s|%s|0|%s",
                messageId,
                DEVICE_ID,
                message
        );
    }

    private void processReceivedMessage(String receivedMessage, String senderIP) {
        // Split the mesh message
        String[] parts = receivedMessage.split("\\|");
        if (parts.length != 4) {
            return; // Malformed message
        }

        String messageId = parts[0];
        String originDeviceId = parts[1];
        int hopCount = Integer.parseInt(parts[2]);
        String message = parts[3];

        // Check if we've already seen this message
        if (seenMessageIds.contains(messageId)) {
            return; // Prevent duplicate processing
        }

        // Add message to seen list
        seenMessageIds.add(messageId);

        // Check hop count to prevent infinite routing
        if (hopCount >= MAX_HOP_COUNT) {
            return;
        }

        // Process and potentially rebroadcast if not from this device
        if (!originDeviceId.equals(DEVICE_ID)) {
            // Rebroadcast the message with incremented hop count
            String forwardMessage = String.format("%s|%s|%d|%s",
                    messageId,
                    originDeviceId,
                    hopCount + 1,
                    message
            );

            // Broadcast to all interfaces
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                List<InetAddress> broadcastAddresses = getBroadcastAddresses();

                for (InetAddress broadcastAddress : broadcastAddresses) {
                    byte[] sendData = forwardMessage.getBytes();
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

            // Display the received message
            SwingUtilities.invokeLater(() -> {
                messageArea.append(senderIP + " (Forwarded): " + message + "\n");
                scrollToBottom();
            });
        }
    }

    private void scrollToBottom() {
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
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

    private void cleanup() {
        isReceiving = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
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
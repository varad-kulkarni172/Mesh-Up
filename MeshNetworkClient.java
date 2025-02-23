import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class MeshNetworkClient extends JFrame {
    private static final int BROADCAST_PORT = 8888;
    private static final int MAX_HOP_COUNT = 5;
    private static final long ACKNOWLEDGMENT_TIMEOUT = 555000;
    private static final long DEVICE_CLEANUP_INTERVAL = 30000;

    // UI Colors
    private static final Color DARK_BACKGROUND = new Color(30, 30, 30);
    private static final Color DARKER_BACKGROUND = new Color(20, 20, 20);
    private static final Color ACCENT_COLOR = new Color(86, 156, 214);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color INPUT_BACKGROUND = new Color(45, 45, 45);
    private static final Color BUTTON_HOVER = new Color(106, 176, 234);

    private JTextArea messageArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel deviceCountLabel;

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean isReceiving = true;

    private Set<String> seenMessageIds = ConcurrentHashMap.newKeySet();
    private Set<String> connectedDevices = ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap<String, Boolean> messageDeliveryStatus = new ConcurrentHashMap<>();

    private final String DEVICE_ID = UUID.randomUUID().toString();
    private String localUsername = "Anonymous";
    private DatagramSocket receiveSocket;

    public MeshNetworkClient() {
        super("Mesh Network Client");
        setDarkLookAndFeel();
        promptForUsername();
        initializeComponents();
        setupUI();
        startMessageReceiver();
        startDeviceCleanup();
    }

    private void setDarkLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // General background and text color settings
            UIManager.put("Panel.background", DARK_BACKGROUND);
            UIManager.put("OptionPane.background", DARK_BACKGROUND);
            UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
            UIManager.put("Label.foreground", TEXT_COLOR);
            UIManager.put("TextField.background", INPUT_BACKGROUND);
            UIManager.put("TextField.foreground", TEXT_COLOR);
            UIManager.put("TextArea.background", INPUT_BACKGROUND);
            UIManager.put("TextArea.foreground", TEXT_COLOR);
            UIManager.put("Button.background", ACCENT_COLOR);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("ScrollPane.background", DARK_BACKGROUND);
            UIManager.put("Viewport.background", DARK_BACKGROUND);

            // Force all components to be non-transparent
            UIManager.put("Button.opaque", true);
            UIManager.put("Panel.opaque", true);
            UIManager.put("Label.opaque", true);
            UIManager.put("TextField.opaque", true);
            UIManager.put("TextArea.opaque", true);
            UIManager.put("ScrollPane.opaque", true);
            UIManager.put("Viewport.opaque", true);

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private JButton sosButton; // Declare SOS button

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(DARK_BACKGROUND);

        // Status Panel
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);

        // Message Area
        JPanel messagePanel = createMessagePanel();
        add(messagePanel, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        // Add padding around the main content
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Action listeners
        sendButton.addActionListener(e -> sendMessage());
        messageInput.addActionListener(e -> sendMessage());

        // SOS Button Setup
        sosButton = createStyledButton("SOS", Color.RED);
        inputPanel.add(sosButton, BorderLayout.WEST);
        sosButton.setBackground(Color.RED);
        sosButton.addActionListener(e -> sendSOSMessage());
        inputPanel.add(sosButton, BorderLayout.WEST); // Add to input panel

        // Update status
        updateConnectionStatus();
    }

    private void sendSOSMessage() {
        String batteryPercentage = getBatteryPercentage() + "%";
        String gpsCoordinates = getGPSCoordinates();
        String ipAddress = getLocalIpAddress() != null ? getLocalIpAddress() : "Unknown";

        String sosMessage = String.format(
                "🚨 EMERGENCY SOS 🚨\nDevice Info:\n- Manufacturer: %s\n- Model: %s\n- OS: %s\n- Battery: %s\n- Location: %s\n- IP: %s",
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("os.version"),
                batteryPercentage,
                gpsCoordinates,
                ipAddress
        );

        broadcastMessage(sosMessage);
        JOptionPane.showMessageDialog(this, "SOS message sent!", "SOS", JOptionPane.WARNING_MESSAGE);
    }

    private int getBatteryPercentage() {
        return (int) (Math.random() * 100); // Placeholder since Java cannot get battery info directly
    }

    private String getGPSCoordinates() {
        return "GPS Unavailable (Java Desktop)"; // Placeholder since Java Swing cannot access GPS
    }


    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        statusPanel.setBackground(DARKER_BACKGROUND);
        statusPanel.setBorder(createRoundedBorder());

        statusLabel = new JLabel("Network Status: Initializing...");
        deviceCountLabel = new JLabel("Devices: 0");

        styleLabel(statusLabel);
        styleLabel(deviceCountLabel);

        statusPanel.add(statusLabel);
        statusPanel.add(new JSeparator(JSeparator.VERTICAL));
        statusPanel.add(deviceCountLabel);

        return statusPanel;
    }

    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(DARK_BACKGROUND);

        messageArea = new JTextArea();
        messageArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        messageArea.setBackground(INPUT_BACKGROUND);
        messageArea.setForeground(TEXT_COLOR);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setMargin(new Insets(10, 10, 10, 10));
        messageArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(createRoundedBorder());
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        messagePanel.add(scrollPane, BorderLayout.CENTER);
        return messagePanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(DARK_BACKGROUND);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        messageInput = new JTextField();
        messageInput.setBackground(INPUT_BACKGROUND);
        messageInput.setForeground(TEXT_COLOR);
        messageInput.setCaretColor(TEXT_COLOR);
        messageInput.setBorder(createRoundedBorder());
        messageInput.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));

        sendButton = createStyledButton("Send", ACCENT_COLOR);

        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        return inputPanel;
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
                String messageId = UUID.randomUUID().toString();
                messageDeliveryStatus.put(messageId, false);
                String meshMessage = constructMeshMessage(messageId, message);

                SwingUtilities.invokeLater(() -> {
                    messageArea.append(String.format("\n[%s] Me: %s\n",
                            localUsername, message));
                    scrollToBottom();
                });

                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

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
        String messageId = UUID.randomUUID().toString();
        messageDeliveryStatus.put(messageId, false);
        String meshMessage = constructMeshMessage(messageId, message);
    }

    private String constructMeshMessage(String messageId, String message) {
        return String.format("%s|%s|0|%s|%s",
                messageId,
                DEVICE_ID,
                localUsername,
                message
        );
    }

    private void processReceivedMessage(String receivedMessage, String senderIP) {
        String[] parts = receivedMessage.split("\\|");

        String messageId = parts[0];
        String originDeviceId = parts[1];
        int hopCount = Integer.parseInt(parts[2]);
        String senderUsername = parts[3];
        String message = parts[4];

        connectedDevices.add(originDeviceId);
        updateDeviceCount();

        if (seenMessageIds.contains(messageId)) {
            return;
        }

        seenMessageIds.add(messageId);

        if (hopCount >= MAX_HOP_COUNT) {
            return;
        }

        if (!originDeviceId.equals(DEVICE_ID)) {
            sendAcknowledgment(messageId, originDeviceId);

            SwingUtilities.invokeLater(() -> {
                messageArea.append(String.format("\n[%s] %s: %s\n",
                        senderUsername, senderIP, message));
                scrollToBottom();
            });

            if (!originDeviceId.equals(DEVICE_ID)) {
                sendAcknowledgment(messageId, originDeviceId);

                SwingUtilities.invokeLater(() -> {
                    messageArea.append(String.format("\n[%s] %s: %s\n",
                            senderUsername, senderIP, message));
                    scrollToBottom();
                });

                rebroadcastMessage(messageId, originDeviceId, hopCount, senderUsername, message);
            }

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

    private void startDeviceCleanup() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            updateDeviceCount();
        }, DEVICE_CLEANUP_INTERVAL, DEVICE_CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
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

    private void updateDeviceCount() {
        SwingUtilities.invokeLater(() -> {
            deviceCountLabel.setText("Devices: " + connectedDevices.size());
        });
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

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setOpaque(true); // Ensure background is applied
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(bgColor); // Set the background color dynamically
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker()); // Darker color on hover
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }


    private void styleLabel(JLabel label) {
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    private Border createRoundedBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(8),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );
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

    // Custom rounded border class
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ACCENT_COLOR);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 8, 4, 8);
        }
    }

    // Custom ScrollBar UI
    private static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = ACCENT_COLOR;
            trackColor = INPUT_BACKGROUND;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            MeshNetworkClient client = new MeshNetworkClient();
            client.setLocationRelativeTo(null);
            client.setVisible(true);
        });
    }
}
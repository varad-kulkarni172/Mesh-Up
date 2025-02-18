#include <ESP8266WiFi.h>
#include <WiFiUdP.h>

// Mesh Network Configuration
const int UDP_PORT = 8888;
const int MAX_HOP_COUNT = 5;  // Prevent infinite routing
WiFiUDP udp;

// Device Identification
String DEVICE_ID = String(ESP.getChipId(), HEX);

// Message Management
const int MESSAGE_HISTORY_SIZE = 50;
String messageHistory[MESSAGE_HISTORY_SIZE];
int historyIndex = 0;

// Network State
bool isStationConnected = false;

void setup() {
    Serial.begin(115200);
    WiFi.mode(WIFI_AP_STA);  // Enable both Access Point and Station modes

    // Start open SoftAP (Hotspot without password)
    WiFi.softAP("MeshNet-" + DEVICE_ID);
    Serial.println("Open SoftAP started: MeshNet-" + DEVICE_ID);

    // Start UDP server
    udp.begin(UDP_PORT);
    Serial.println("UDP server started on port: " + String(UDP_PORT));

    // Scan and connect to open networks
    connectToOpenNetwork();
}

void loop() {
    // Maintain network connectivity
    if (WiFi.status() != WL_CONNECTED) {
        isStationConnected = false;
        connectToOpenNetwork();
    } else {
        isStationConnected = true;
    }

    // Receive and process incoming messages
    receiveMessage();
}

void connectToOpenNetwork() {
    Serial.println("Scanning for open networks...");
    int numNetworks = WiFi.scanNetworks();

    for (int i = 0; i < numNetworks; i++) {
        if (WiFi.encryptionType(i) == ENC_TYPE_NONE) {
            String ssid = WiFi.SSID(i);
            Serial.println("Attempting to connect to: " + ssid);

            WiFi.begin(ssid.c_str());
            unsigned long startTime = millis();
            
            while (WiFi.status() != WL_CONNECTED && millis() - startTime < 10000) {
                delay(500);
                Serial.print(".");
            }

            if (WiFi.status() == WL_CONNECTED) {
                Serial.println("\nConnected to open network: " + ssid);
                return;
            }
        }
    }
    Serial.println("No open networks found.");
}

void sendMessage(String message, int hopCount = 0) {
    String messageId = generateMessageId();
    // Format: messageId|originDeviceId|hopCount|message
    String fullMessage = messageId + "|" + DEVICE_ID + "|" + 
                         String(hopCount) + "|" + message;

    // Broadcast via UDP
    udp.beginPacket("255.255.255.255", UDP_PORT);
    udp.print(fullMessage);
    udp.endPacket();

    Serial.println("Sent: " + fullMessage);
}

void receiveMessage() {
    char incomingPacket[255];
    int packetSize = udp.parsePacket();
    
    if (packetSize > 0) {
        int len = udp.read(incomingPacket, 255);
        if (len > 0) incomingPacket[len] = '\0';
        
        String receivedMessage = String(incomingPacket);
        processReceivedMessage(receivedMessage);
    }
}

void processReceivedMessage(String receivedMessage) {
    // Split message components
    int firstDelim = receivedMessage.indexOf('|');
    int secondDelim = receivedMessage.indexOf('|', firstDelim + 1);
    int thirdDelim = receivedMessage.indexOf('|', secondDelim + 1);

    String messageId = receivedMessage.substring(0, firstDelim);
    String originId = receivedMessage.substring(firstDelim + 1, secondDelim);
    int hopCount = receivedMessage.substring(secondDelim + 1, thirdDelim).toInt();
    String message = receivedMessage.substring(thirdDelim + 1);

    // Prevent duplicate and excessive routing
    if (isMessageSeen(messageId) || hopCount >= MAX_HOP_COUNT) {
        return;
    }

    // Add to message history
    addMessageToHistory(messageId);

    // Print received message
    Serial.println("Received from " + originId + ": " + message);

    // Send acknowledgment
    sendAck(messageId, originId);

    // Rebroadcast if not from this device
    if (originId != DEVICE_ID) {
        rebroadcastMessage(messageId, originId, hopCount, message);
    }
}

void rebroadcastMessage(String messageId, String originId, int hopCount, String message) {
    int newHopCount = hopCount + 1;
    String forwardMessage = messageId + "|" + originId + "|" + 
                            String(newHopCount) + "|" + message;

    udp.beginPacket("255.255.255.255", UDP_PORT);
    udp.print(forwardMessage);
    udp.endPacket();

    Serial.println("Rebroadcast: " + forwardMessage);
}

void sendAck(String messageId, String targetDeviceId) {
    String ackMessage = "ACK|" + messageId + "|" + DEVICE_ID;
    
    udp.beginPacket("255.255.255.255", UDP_PORT);
    udp.print(ackMessage);
    udp.endPacket();

    Serial.println("Acknowledgment sent for: " + messageId);
}

String generateMessageId() {
    return DEVICE_ID + "-" + String(random(1000, 9999));
}

bool isMessageSeen(String messageId) {
    for (int i = 0; i < MESSAGE_HISTORY_SIZE; i++) {
        if (messageHistory[i] == messageId) return true;
    }
    return false;
}

void addMessageToHistory(String messageId) {
    messageHistory[historyIndex] = messageId;
    historyIndex = (historyIndex + 1) % MESSAGE_HISTORY_SIZE;
}

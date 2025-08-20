package shared;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UDP Communication Handler for fast message broadcasting
 * Complements TCP for optimization progress updates and quick notifications
 */
public class UDPCommunicationHandler {
    
    public static final int UDP_PORT = 12346; // Different from TCP port
    private DatagramSocket socket;
    private boolean isRunning = false;
    private Thread listenerThread;
    private ConcurrentHashMap<String, InetAddress> knownClients;
    
    public interface UDPMessageListener {
        void onUDPMessage(String message, InetAddress sender);
        void onOptimizationBroadcast(OptimizationProtocol.OptimizationProgress progress, InetAddress sender);
    }
    
    private UDPMessageListener messageListener;
    
    public UDPCommunicationHandler(UDPMessageListener listener) {
        this.messageListener = listener;
        this.knownClients = new ConcurrentHashMap<>();
    }
    
    /**
     * Start UDP listener (for server)
     */
    public void startServer() throws SocketException {
        socket = new DatagramSocket(UDP_PORT);
        isRunning = true;
        
        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Start UDP communication (for client)
     */
    public void startClient() throws SocketException {
        socket = new DatagramSocket(); // Random port for client
        isRunning = true;
        
        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    private void listenForMessages() {
        byte[] buffer = new byte[8192]; // 8KB buffer
        
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                InetAddress sender = packet.getAddress();
                
                // Register sender for future broadcasts
                String senderKey = sender.getHostAddress() + ":" + packet.getPort();
                knownClients.put(senderKey, sender);
                
                if (messageListener != null) {
                    // Try to parse as optimization progress
                    if (message.startsWith("OPTIMIZATION_PROGRESS:")) {
                        try {
                            String progressData = message.substring("OPTIMIZATION_PROGRESS:".length());
                            OptimizationProtocol.OptimizationProgress progress = 
                                deserializeProgress(progressData);
                            messageListener.onOptimizationBroadcast(progress, sender);
                        } catch (Exception e) {
                            messageListener.onUDPMessage(message, sender);
                        }
                    } else {
                        messageListener.onUDPMessage(message, sender);
                    }
                }
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("UDP receive error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Send UDP message to specific address
     */
    public void sendMessage(String message, InetAddress target, int port) throws IOException {
        byte[] data = message.getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(data, data.length, target, port);
        socket.send(packet);
    }
    
    /**
     * Broadcast message to all known clients
     */
    public void broadcastMessage(String message) {
        for (InetAddress client : knownClients.values()) {
            try {
                sendMessage(message, client, UDP_PORT);
            } catch (IOException e) {
                System.err.println("Failed to send UDP broadcast to " + client + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Broadcast optimization progress to all clients
     */
    public void broadcastOptimizationProgress(OptimizationProtocol.OptimizationProgress progress) {
        try {
            String progressMessage = "OPTIMIZATION_PROGRESS:" + serializeProgress(progress);
            broadcastMessage(progressMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast optimization progress: " + e.getMessage());
        }
    }
    
    /**
     * Send discovery message to find server
     */
    public void sendDiscovery(InetAddress serverAddress) throws IOException {
        String discoveryMessage = "CLIENT_DISCOVERY:" + socket.getLocalPort();
        sendMessage(discoveryMessage, serverAddress, UDP_PORT);
    }
    
    /**
     * Register a client for future broadcasts
     */
    public void registerClient(InetAddress clientAddress, int port) {
        String key = clientAddress.getHostAddress() + ":" + port;
        knownClients.put(key, clientAddress);
    }
    
    private String serializeProgress(OptimizationProtocol.OptimizationProgress progress) {
        return String.format("%d|%.6f|%.6f|%d|%s",
            progress.currentIteration,
            progress.bestFitness,
            progress.convergenceRate,
            progress.elapsedTime,
            progress.status);
    }
    
    private OptimizationProtocol.OptimizationProgress deserializeProgress(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 5) {
            return new OptimizationProtocol.OptimizationProgress(
                Integer.parseInt(parts[0]),
                Double.parseDouble(parts[1]),
                new double[0], // bestSolution not transmitted via UDP for efficiency
                Double.parseDouble(parts[2]),
                Long.parseLong(parts[3]),
                parts[4]
            );
        }
        throw new IllegalArgumentException("Invalid progress data format");
    }
    
    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }
}
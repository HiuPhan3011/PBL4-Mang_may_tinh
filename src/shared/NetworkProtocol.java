package shared;

import java.io.Serializable;

/**
 * Enhanced Network Protocol supporting both TCP and UDP communication
 * TCP for reliable data transfer (chat, files, optimization requests/results)
 * UDP for fast broadcasts (progress updates, notifications)
 */
public class NetworkProtocol {
    
    // Communication Types
    public static final String COMM_TCP = "TCP";
    public static final String COMM_UDP = "UDP";
    
    // Message Priorities
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
    
    // Network Message Types
    public static final String NET_HEARTBEAT = "HEARTBEAT";
    public static final String NET_CLIENT_DISCOVERY = "CLIENT_DISCOVERY";
    public static final String NET_SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
    public static final String NET_OPTIMIZATION_BROADCAST = "OPT_BROADCAST";
    
    /**
     * Enhanced network message with communication type and priority
     */
    public static class NetworkMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String communicationType; // TCP or UDP
        public int priority;
        public String messageId;
        public OptimizationProtocol.Message payload;
        public long timestamp;
        public String sourceAddress;
        public int sourcePort;
        
        public NetworkMessage(String commType, int priority, OptimizationProtocol.Message payload) {
            this.communicationType = commType;
            this.priority = priority;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
            this.messageId = generateMessageId();
        }
        
        private String generateMessageId() {
            return "MSG_" + System.currentTimeMillis() + "_" + 
                   Integer.toHexString(hashCode());
        }
        
        @Override
        public String toString() {
            return "NetworkMessage{" +
                   "type=" + communicationType +
                   ", priority=" + priority +
                   ", id=" + messageId +
                   ", payload=" + payload.type +
                   '}';
        }
    }
    
    /**
     * Network statistics for monitoring
     */
    public static class NetworkStats implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public long tcpMessagesSent = 0;
        public long tcpMessagesReceived = 0;
        public long udpMessagesSent = 0;
        public long udpMessagesReceived = 0;
        public long bytesTransferred = 0;
        public long optimizationsCompleted = 0;
        public double averageResponseTime = 0.0;
        
        public void recordTCPSent() { tcpMessagesSent++; }
        public void recordTCPReceived() { tcpMessagesReceived++; }
        public void recordUDPSent() { udpMessagesSent++; }
        public void recordUDPReceived() { udpMessagesReceived++; }
        public void recordBytesTransferred(long bytes) { bytesTransferred += bytes; }
        public void recordOptimizationCompleted() { optimizationsCompleted++; }
        
        @Override
        public String toString() {
            return String.format(
                "NetworkStats{TCP: %d/%d, UDP: %d/%d, Bytes: %d, Optimizations: %d, AvgResponse: %.2fms}",
                tcpMessagesSent, tcpMessagesReceived, udpMessagesSent, udpMessagesReceived,
                bytesTransferred, optimizationsCompleted, averageResponseTime
            );
        }
    }
    
    /**
     * Connection information for clients/server
     */
    public static class ConnectionInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String clientId;
        public String ipAddress;
        public int tcpPort;
        public int udpPort;
        public long connectionTime;
        public boolean tcpConnected;
        public boolean udpConnected;
        public NetworkStats stats;
        
        public ConnectionInfo(String clientId, String ipAddress, int tcpPort, int udpPort) {
            this.clientId = clientId;
            this.ipAddress = ipAddress;
            this.tcpPort = tcpPort;
            this.udpPort = udpPort;
            this.connectionTime = System.currentTimeMillis();
            this.stats = new NetworkStats();
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s:%d/%d] - TCP:%s UDP:%s",
                clientId, ipAddress, tcpPort, udpPort, 
                tcpConnected ? "✓" : "✗", udpConnected ? "✓" : "✗"
            );
        }
    }
    
    /**
     * Utility methods for message routing
     */
    public static class MessageRouter {
        
        /**
         * Determine if message should use TCP or UDP based on type and priority
         */
        public static String determineProtocol(OptimizationProtocol.Message message, int priority) {
            // High priority and critical messages always use TCP
            if (priority >= PRIORITY_HIGH) {
                return COMM_TCP;
            }
            
            // Determine based on message type
            switch (message.type) {
                case OptimizationProtocol.MSG_CHAT:
                case OptimizationProtocol.MSG_FILE:
                case OptimizationProtocol.MSG_OPTIMIZATION_REQUEST:
                case OptimizationProtocol.MSG_OPTIMIZATION_RESULT:
                    return COMM_TCP; // Reliable delivery required
                    
                case OptimizationProtocol.MSG_OPTIMIZATION_PROGRESS:
                    return COMM_UDP; // Fast delivery, some loss acceptable
                    
                case NET_HEARTBEAT:
                case NET_OPTIMIZATION_BROADCAST:
                    return COMM_UDP; // Broadcast messages
                    
                default:
                    return COMM_TCP; // Default to reliable
            }
        }
        
        /**
         * Determine message priority based on type
         */
        public static int determinePriority(OptimizationProtocol.Message message) {
            switch (message.type) {
                case OptimizationProtocol.MSG_OPTIMIZATION_STOP:
                    return PRIORITY_URGENT;
                    
                case OptimizationProtocol.MSG_OPTIMIZATION_REQUEST:
                case OptimizationProtocol.MSG_OPTIMIZATION_RESULT:
                    return PRIORITY_HIGH;
                    
                case OptimizationProtocol.MSG_CHAT:
                case OptimizationProtocol.MSG_FILE:
                    return PRIORITY_NORMAL;
                    
                case OptimizationProtocol.MSG_OPTIMIZATION_PROGRESS:
                case NET_HEARTBEAT:
                    return PRIORITY_LOW;
                    
                default:
                    return PRIORITY_NORMAL;
            }
        }
        
        /**
         * Check if message requires acknowledgment
         */
        public static boolean requiresAck(OptimizationProtocol.Message message) {
            switch (message.type) {
                case OptimizationProtocol.MSG_OPTIMIZATION_REQUEST:
                case OptimizationProtocol.MSG_OPTIMIZATION_STOP:
                case OptimizationProtocol.MSG_FILE:
                    return true;
                default:
                    return false;
            }
        }
    }
    
    /**
     * Network configuration constants
     */
    public static class Config {
        public static final int DEFAULT_TCP_PORT = 12345;
        public static final int DEFAULT_UDP_PORT = 12346;
        public static final int MAX_UDP_PACKET_SIZE = 8192; // 8KB
        public static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
        public static final int HEARTBEAT_INTERVAL = 5000; // 5 seconds
        public static final int MAX_RETRIES = 3;
        public static final int RETRY_DELAY = 1000; // 1 second
        
        // Buffer sizes
        public static final int TCP_BUFFER_SIZE = 65536; // 64KB
        public static final int UDP_BUFFER_SIZE = 8192;  // 8KB
        
        // Thread pool sizes
        public static final int SERVER_THREAD_POOL = 10;
        public static final int OPTIMIZATION_THREAD_POOL = 8;
        public static final int NETWORK_THREAD_POOL = 4;
    }
}
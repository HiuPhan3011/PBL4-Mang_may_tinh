package server;

import java.io.*;
import java.net.*;
import shared.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientAddress;
    private String clientId;
    private boolean isRunning = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.clientAddress = socket.getInetAddress().toString();
        this.clientId = "Unknown";
        
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            server.logMessage("Error setting up client streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                Object received = input.readObject();
                
                if (received instanceof OptimizationProtocol.Message) {
                    OptimizationProtocol.Message msg = (OptimizationProtocol.Message) received;
                    handleProtocolMessage(msg);
                } else if (received instanceof String) {
                    // Legacy string messages - for backward compatibility
                    String message = (String) received;
                    server.logMessage("Legacy message from " + clientAddress + ": " + message);
                    
                    // Convert to protocol message and broadcast
                    OptimizationProtocol.Message protocolMsg = new OptimizationProtocol.Message(
                        OptimizationProtocol.MSG_CHAT, clientId, 
                        new OptimizationProtocol.ChatMessage(message)
                    );
                    server.broadcastMessage(protocolMsg, this);
                    
                } else if (received instanceof FileData) {
                    // Legacy file data - for backward compatibility
                    FileData fileData = (FileData) received;
                    server.logMessage("Legacy file received from " + clientAddress + ": " + fileData.fileName);
                    
                    // Convert to protocol message and broadcast
                    OptimizationProtocol.Message protocolMsg = new OptimizationProtocol.Message(
                        OptimizationProtocol.MSG_FILE, clientId, fileData
                    );
                    server.broadcastMessage(protocolMsg, this);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                server.logMessage("Client " + clientAddress + " (" + clientId + ") disconnected: " + e.getMessage());
            }
        } finally {
            server.removeClient(this);
            closeConnection();
        }
    }

    private void handleProtocolMessage(OptimizationProtocol.Message msg) {
        // Update client ID if it's the first message from this client
        if ("Unknown".equals(clientId)) {
            clientId = msg.sender;
            server.logMessage("Client " + clientAddress + " identified as: " + clientId);
        }

        switch (msg.type) {
            case OptimizationProtocol.MSG_CHAT:
                server.logMessage("Chat from " + clientId + ": " + 
                    ((OptimizationProtocol.ChatMessage) msg.payload).content);
                server.broadcastMessage(msg, this);
                break;

            case OptimizationProtocol.MSG_FILE:
                FileData fileData = (FileData) msg.payload;
                server.logMessage("File from " + clientId + ": " + fileData.fileName + 
                    " (" + fileData.getFileSizeKB() + " KB)");
                server.broadcastMessage(msg, this);
                break;

            case OptimizationProtocol.MSG_OPTIMIZATION_REQUEST:
                OptimizationProtocol.OptimizationRequest request = 
                    (OptimizationProtocol.OptimizationRequest) msg.payload;
                server.logMessage("Optimization request from " + clientId + ": " + 
                    request.algorithm + " (Pop: " + request.populationSize + 
                    ", Iter: " + request.maxIterations + ")");
                server.handleOptimizationRequest(request, this);
                break;

            case OptimizationProtocol.MSG_OPTIMIZATION_STOP:
                server.logMessage("Optimization stop request from " + clientId);
                server.stopOptimization(clientId);
                break;

            default:
                server.logMessage("Unknown message type from " + clientId + ": " + msg.type);
                break;
        }
    }

    public void sendMessage(OptimizationProtocol.Message message) {
        try {
            if (output != null && isRunning) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            server.logMessage("Error sending message to " + clientAddress + " (" + clientId + "): " + e.getMessage());
            closeConnection();
        }
    }

    // Legacy methods for backward compatibility
    public void sendMessage(String message) {
        try {
            if (output != null && isRunning) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            server.logMessage("Error sending legacy message to " + clientAddress + " (" + clientId + "): " + e.getMessage());
            closeConnection();
        }
    }

    public void sendFile(FileData fileData) {
        try {
            if (output != null && isRunning) {
                output.writeObject(fileData);
                output.flush();
            }
        } catch (IOException e) {
            server.logMessage("Error sending legacy file to " + clientAddress + " (" + clientId + "): " + e.getMessage());
            closeConnection();
        }
    }

    public void closeConnection() {
        isRunning = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            server.logMessage("Error closing connection: " + e.getMessage());
        }
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public String getClientId() {
        return clientId;
    }
}
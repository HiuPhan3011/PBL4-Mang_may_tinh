package server;

import shared.FileData;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientAddress;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.clientAddress = socket.getInetAddress().toString();
        
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
            while (true) {
                Object received = input.readObject();
                
                if (received instanceof String) {
                    String message = (String) received;
                    server.logMessage("Message from " + clientAddress + ": " + message);
                    server.broadcastMessage(message, this);
                } else if (received instanceof FileData) {
                    FileData fileData = (FileData) received;
                    server.logMessage("File received from " + clientAddress + ": " + fileData.fileName);
                    server.broadcastFile(fileData, this);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            server.logMessage("Client " + clientAddress + " disconnected: " + e.getMessage());
        } finally {
            server.removeClient(this);
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            server.logMessage("Error sending message to " + clientAddress + ": " + e.getMessage());
        }
    }

    public void sendFile(FileData fileData) {
        try {
            output.writeObject(fileData);
            output.flush();
        } catch (IOException e) {
            server.logMessage("Error sending file to " + clientAddress + ": " + e.getMessage());
        }
    }

    public void closeConnection() {
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
}
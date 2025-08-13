package server;

import shared.FileData;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ChatServer extends JFrame {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private JTextArea serverLog;
    private JTextArea clientList;
    private JButton startButton, stopButton;
    private JLabel statusLabel, clientCountLabel;
    private boolean isRunning = false;

    public ChatServer() {
        clients = new ArrayList<>();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Chat Server - Heuristic Optimization System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel - Controls
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(52, 73, 94));
        
        startButton = new JButton("Start Server");
        startButton.setBackground(new Color(39, 174, 96));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        
        stopButton = new JButton("Stop Server");
        stopButton.setBackground(new Color(231, 76, 60));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font("Arial", Font.BOLD, 12));
        stopButton.setEnabled(false);

        statusLabel = new JLabel("Server Status: Stopped");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        clientCountLabel = new JLabel("Connected Clients: 0");
        clientCountLabel.setForeground(Color.WHITE);
        clientCountLabel.setFont(new Font("Arial", Font.BOLD, 14));

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(statusLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(clientCountLabel);

        // Center Panel - Logs and Client List
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // Server Log
        serverLog = new JTextArea(20, 60);
        serverLog.setEditable(false);
        serverLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        serverLog.setBackground(new Color(44, 62, 80));
        serverLog.setForeground(Color.WHITE);
        JScrollPane logScroll = new JScrollPane(serverLog);
        logScroll.setBorder(new TitledBorder("Server Log"));

        // Client List
        clientList = new JTextArea(20, 20);
        clientList.setEditable(false);
        clientList.setFont(new Font("Arial", Font.PLAIN, 12));
        clientList.setBackground(new Color(236, 240, 241));
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(new TitledBorder("Connected Clients"));

        centerPanel.add(logScroll, BorderLayout.CENTER);
        centerPanel.add(clientScroll, BorderLayout.EAST);

        add(controlPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Event Listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("Server Status: Running on Port " + PORT);
            
            logMessage("Server started on port " + PORT);
            
            // Accept clients in a separate thread
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler client = new ClientHandler(clientSocket, this);
                        clients.add(client);
                        new Thread(client).start();
                        
                        logMessage("New client connected: " + clientSocket.getInetAddress());
                        updateClientCount();
                        updateClientList();
                    } catch (IOException e) {
                        if (isRunning) {
                            logMessage("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            }).start();
            
        } catch (IOException e) {
            logMessage("Failed to start server: " + e.getMessage());
        }
    }

    private void stopServer() {
        isRunning = false;
        
        try {
            // Close all client connections
            for (ClientHandler client : clients) {
                client.closeConnection();
            }
            clients.clear();
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Server Status: Stopped");
            
            logMessage("Server stopped");
            updateClientCount();
            updateClientList();
            
        } catch (IOException e) {
            logMessage("Error stopping server: " + e.getMessage());
        }
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            serverLog.append("[" + timestamp + "] " + message + "\n");
            serverLog.setCaretPosition(serverLog.getDocument().getLength());
        });
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        updateClientCount();
        updateClientList();
        logMessage("Client disconnected");
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastFile(FileData fileData, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(fileData);
            }
        }
    }

    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            clientCountLabel.setText("Connected Clients: " + clients.size());
        });
    }

    private void updateClientList() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clients.size(); i++) {
                ClientHandler client = clients.get(i);
                sb.append("Client ").append(i + 1).append(": ")
                  .append(client.getClientAddress()).append("\n");
            }
            clientList.setText(sb.toString());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ChatServer();
        });
    }
}
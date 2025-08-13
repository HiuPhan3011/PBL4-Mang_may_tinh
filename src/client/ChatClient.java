package client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import shared.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    private JTextField serverAddressField, portField, messageField;
    private JTextArea chatArea;
    private JButton connectButton, disconnectButton, sendButton, sendFileButton;
    private JLabel statusLabel, fileStatusLabel;
    private JProgressBar fileProgressBar;
    
    private boolean isConnected = false;

    public ChatClient() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Chat Client - Heuristic Optimization System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel - Connection Controls
        JPanel connectionPanel = new JPanel(new FlowLayout());
        connectionPanel.setBackground(new Color(52, 73, 94));
        connectionPanel.setBorder(new TitledBorder("Server Connection"));

        JLabel serverLabel = new JLabel("Server Address:");
        serverLabel.setForeground(Color.WHITE);
        serverAddressField = new JTextField("localhost", 10);
        
        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(Color.WHITE);
        portField = new JTextField("12345", 5);

        connectButton = new JButton("Connect");
        connectButton.setBackground(new Color(39, 174, 96));
        connectButton.setForeground(Color.WHITE);
        
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setBackground(new Color(231, 76, 60));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setEnabled(false);

        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        connectionPanel.add(serverLabel);
        connectionPanel.add(serverAddressField);
        connectionPanel.add(portLabel);
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(Box.createHorizontalStrut(20));
        connectionPanel.add(statusLabel);

        // Center Panel - Chat Area
        chatArea = new JTextArea(20, 50);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(new Color(236, 240, 241));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new TitledBorder("Chat Messages"));

        // Bottom Panel - Message Input
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(new TitledBorder("Send Message"));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 12));
        messageField.setEnabled(false);
        
        sendButton = new JButton("Send Message");
        sendButton.setBackground(new Color(52, 152, 219));
        sendButton.setForeground(Color.WHITE);
        sendButton.setEnabled(false);
        
        sendFileButton = new JButton("Send File");
        sendFileButton.setBackground(new Color(155, 89, 182));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.EAST);

        // File Transfer Panel
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.setBorder(new TitledBorder("File Transfer"));
        
        fileStatusLabel = new JLabel("No file transfer in progress");
        fileProgressBar = new JProgressBar(0, 100);
        fileProgressBar.setStringPainted(true);
        fileProgressBar.setVisible(false);

        filePanel.add(fileStatusLabel);
        filePanel.add(fileProgressBar);

        // Layout
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messagePanel, BorderLayout.CENTER);
        bottomPanel.add(filePanel, BorderLayout.SOUTH);

        add(connectionPanel, BorderLayout.NORTH);
        add(chatScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Event Listeners
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        sendButton.addActionListener(e -> sendMessage());
        sendFileButton.addActionListener(e -> sendFile());
        
        messageField.addActionListener(e -> sendMessage());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToServer() {
        String serverAddress = serverAddressField.getText().trim();
        int port;
        
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number!");
            return;
        }

        try {
            socket = new Socket(serverAddress, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            
            isConnected = true;
            updateConnectionStatus();
            
            // Start listening for incoming messages
            new Thread(this::listenForMessages).start();
            
            appendMessage("Connected to server: " + serverAddress + ":" + port);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        try {
            isConnected = false;
            
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            
            updateConnectionStatus();
            appendMessage("Disconnected from server");
            
        } catch (IOException e) {
            appendMessage("Error during disconnect: " + e.getMessage());
        }
    }

    private void sendMessage() {
        if (!isConnected) return;
        
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        
        try {
            String fullMessage = "Message: " + message;
            output.writeObject(fullMessage);
            output.flush();
            
            appendMessage("You: " + message);
            messageField.setText("");
            
        } catch (IOException e) {
            appendMessage("Error sending message: " + e.getMessage());
        }
    }

    private void sendFile() {
        if (!isConnected) return;
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select file to send");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Show progress
            fileStatusLabel.setText("Sending: " + selectedFile.getName());
            fileProgressBar.setVisible(true);
            fileProgressBar.setValue(0);
            
            new Thread(() -> {
                try {
                    FileData fileData = new FileData(selectedFile);
                    output.writeObject(fileData);
                    output.flush();
                    
                    SwingUtilities.invokeLater(() -> {
                        fileProgressBar.setValue(100);
                        fileStatusLabel.setText("File sent: " + selectedFile.getName());
                        appendMessage("You sent file: " + selectedFile.getName() + 
                                    " (" + (selectedFile.length() / 1024) + " KB)");
                        
                        Timer timer = new Timer(3000, e -> {
                            fileStatusLabel.setText("No file transfer in progress");
                            fileProgressBar.setVisible(false);
                        });
                        timer.setRepeats(false);
                        timer.start();
                    });
                    
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        fileStatusLabel.setText("File send failed");
                        fileProgressBar.setVisible(false);
                        appendMessage("Error sending file: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void listenForMessages() {
        try {
            while (isConnected) {
                Object received = input.readObject();
                
                if (received instanceof String) {
                    String message = (String) received;
                    SwingUtilities.invokeLater(() -> appendMessage("Other: " + message.substring(9))); // Remove "Message: " prefix
                } else if (received instanceof FileData) {
                    FileData fileData = (FileData) received;
                    SwingUtilities.invokeLater(() -> handleReceivedFile(fileData));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Connection lost: " + e.getMessage());
                    disconnectFromServer();
                });
            }
        }
    }

    private void handleReceivedFile(FileData fileData) {
        int option = JOptionPane.showConfirmDialog(this, 
            "Received file: " + fileData.fileName + " (" + (fileData.fileContent.length / 1024) + " KB)\n" +
            "Do you want to save it?", 
            "File Received", 
            JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setSelectedFile(new File(fileData.fileName));
            
            if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(saveChooser.getSelectedFile())) {
                    fos.write(fileData.fileContent);
                    appendMessage("File saved: " + saveChooser.getSelectedFile().getName());
                } catch (IOException e) {
                    appendMessage("Error saving file: " + e.getMessage());
                }
            }
        }
        
        appendMessage("Received file: " + fileData.fileName + 
                     " (" + (fileData.fileContent.length / 1024) + " KB)");
    }

    private void updateConnectionStatus() {
        connectButton.setEnabled(!isConnected);
        disconnectButton.setEnabled(isConnected);
        serverAddressField.setEnabled(!isConnected);
        portField.setEnabled(!isConnected);
        messageField.setEnabled(isConnected);
        sendButton.setEnabled(isConnected);
        sendFileButton.setEnabled(isConnected);
        
        statusLabel.setText(isConnected ? "Status: Connected" : "Status: Disconnected");
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            chatArea.append("[" + timestamp + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ChatClient();
        });
    }
}
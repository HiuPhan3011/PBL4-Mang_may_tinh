package client;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import shared.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    private JTextField serverAddressField, portField, messageField;
    private JTextArea chatArea, optimizationResultArea;
    private JButton connectButton, disconnectButton, sendButton, sendFileButton;
    private JButton psoButton, acoButton, gaButton, stopOptimizationButton;
    private JLabel statusLabel, fileStatusLabel, optimizationStatusLabel;
    private JProgressBar fileProgressBar, optimizationProgressBar;
    
    private boolean isConnected = false;
    private String clientId;

    public ChatClient() {
        this.clientId = "Client_" + System.currentTimeMillis();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Chat Client - Heuristic Optimization System [" + clientId + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel - Connection Controls
        JPanel connectionPanel = createConnectionPanel();
        
        // Center Panel - Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Chat Tab
        JPanel chatPanel = createChatPanel();
        tabbedPane.addTab("Chat", chatPanel);
        
        // Optimization Tab
        JPanel optimizationPanel = createOptimizationPanel();
        tabbedPane.addTab("Optimization", optimizationPanel);

        add(connectionPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Event Listeners
        setupEventListeners();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createConnectionPanel() {
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

        return connectionPanel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        // Chat Area
        chatArea = new JTextArea(20, 50);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(new Color(236, 240, 241));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new TitledBorder("Chat Messages"));

        // Message Input Panel
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

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messagePanel, BorderLayout.CENTER);
        bottomPanel.add(filePanel, BorderLayout.SOUTH);

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        return chatPanel;
    }

    private JPanel createOptimizationPanel() {
        JPanel optimizationPanel = new JPanel(new BorderLayout());
        
        // Optimization Controls
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(new TitledBorder("Optimization Algorithms"));
        
        psoButton = new JButton("Run PSO");
        psoButton.setBackground(new Color(230, 126, 34));
        psoButton.setForeground(Color.WHITE);
        psoButton.setEnabled(false);
        
        acoButton = new JButton("Run ACO");
        acoButton.setBackground(new Color(142, 68, 173));
        acoButton.setForeground(Color.WHITE);
        acoButton.setEnabled(false);
        
        gaButton = new JButton("Run GA");
        gaButton.setBackground(new Color(46, 204, 113));
        gaButton.setForeground(Color.WHITE);
        gaButton.setEnabled(false);
        
        stopOptimizationButton = new JButton("Stop Optimization");
        stopOptimizationButton.setBackground(new Color(231, 76, 60));
        stopOptimizationButton.setForeground(Color.WHITE);
        stopOptimizationButton.setEnabled(false);

        controlPanel.add(psoButton);
        controlPanel.add(acoButton);
        controlPanel.add(gaButton);
        controlPanel.add(stopOptimizationButton);

        // Optimization Status
        JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.setBorder(new TitledBorder("Optimization Status"));
        
        optimizationStatusLabel = new JLabel("No optimization running");
        optimizationProgressBar = new JProgressBar(0, 100);
        optimizationProgressBar.setStringPainted(true);
        optimizationProgressBar.setVisible(false);

        statusPanel.add(optimizationStatusLabel);
        statusPanel.add(optimizationProgressBar);

        // Results Area
        optimizationResultArea = new JTextArea(15, 50);
        optimizationResultArea.setEditable(false);
        optimizationResultArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        optimizationResultArea.setBackground(new Color(44, 62, 80));
        optimizationResultArea.setForeground(Color.CYAN);
        JScrollPane resultScroll = new JScrollPane(optimizationResultArea);
        resultScroll.setBorder(new TitledBorder("Optimization Results"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.CENTER);
        topPanel.add(statusPanel, BorderLayout.SOUTH);

        optimizationPanel.add(topPanel, BorderLayout.NORTH);
        optimizationPanel.add(resultScroll, BorderLayout.CENTER);

        return optimizationPanel;
    }

    private void setupEventListeners() {
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        sendButton.addActionListener(e -> sendMessage());
        sendFileButton.addActionListener(e -> sendFile());
        messageField.addActionListener(e -> sendMessage());
        
        psoButton.addActionListener(e -> runOptimization("PSO"));
        acoButton.addActionListener(e -> runOptimization("ACO"));
        gaButton.addActionListener(e -> runOptimization("GA"));
        stopOptimizationButton.addActionListener(e -> stopOptimization());
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
            
            appendChatMessage("Connected to server: " + serverAddress + ":" + port);
            
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
            appendChatMessage("Disconnected from server");
            
        } catch (IOException e) {
            appendChatMessage("Error during disconnect: " + e.getMessage());
        }
    }

    private void sendMessage() {
        if (!isConnected) return;
        
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        
        try {
            OptimizationProtocol.Message msg = new OptimizationProtocol.Message(
                OptimizationProtocol.MSG_CHAT, clientId, 
                new OptimizationProtocol.ChatMessage(message)
            );
            
            output.writeObject(msg);
            output.flush();
            
            appendChatMessage("You: " + message);
            messageField.setText("");
            
        } catch (IOException e) {
            appendChatMessage("Error sending message: " + e.getMessage());
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
                    OptimizationProtocol.Message msg = new OptimizationProtocol.Message(
                        OptimizationProtocol.MSG_FILE, clientId, fileData
                    );
                    
                    output.writeObject(msg);
                    output.flush();
                    
                    SwingUtilities.invokeLater(() -> {
                        fileProgressBar.setValue(100);
                        fileStatusLabel.setText("File sent: " + selectedFile.getName());
                        appendChatMessage("You sent file: " + selectedFile.getName() + 
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
                        appendChatMessage("Error sending file: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void runOptimization(String algorithm) {
        if (!isConnected) return;
        
        try {
            // Create optimization parameters
            OptimizationProtocol.OptimizationParameters params = new OptimizationProtocol.OptimizationParameters();
            
            // Generate test problem data (10-dimensional sphere function)
            double[] problemData = new double[10];
            for (int i = 0; i < problemData.length; i++) {
                problemData[i] = Math.random() * 200 - 100; // Random initialization
            }
            
            OptimizationProtocol.OptimizationRequest request = new OptimizationProtocol.OptimizationRequest(
                algorithm,
                OptimizationProtocol.OBJECTIVE_MINIMIZE,
                50,    // Population size
                1000,  // Max iterations
                problemData,
                params
            );
            
            OptimizationProtocol.Message msg = new OptimizationProtocol.Message(
                OptimizationProtocol.MSG_OPTIMIZATION_REQUEST, clientId, request
            );
            
            output.writeObject(msg);
            output.flush();
            
            optimizationStatusLabel.setText("Starting " + algorithm + " optimization...");
            optimizationProgressBar.setVisible(true);
            optimizationProgressBar.setValue(0);
            stopOptimizationButton.setEnabled(true);
            psoButton.setEnabled(false);
            acoButton.setEnabled(false);
            gaButton.setEnabled(false);
            
            appendOptimizationResult("=== " + algorithm + " Optimization Started ===");
            
        } catch (IOException e) {
            appendOptimizationResult("Error starting optimization: " + e.getMessage());
        }
    }

    private void stopOptimization() {
        if (!isConnected) return;
        
        try {
            OptimizationProtocol.Message msg = new OptimizationProtocol.Message(
                OptimizationProtocol.MSG_OPTIMIZATION_STOP, clientId, null
            );
            
            output.writeObject(msg);
            output.flush();
            
        } catch (IOException e) {
            appendOptimizationResult("Error stopping optimization: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            while (isConnected) {
                Object received = input.readObject();
                
                if (received instanceof OptimizationProtocol.Message) {
                    OptimizationProtocol.Message msg = (OptimizationProtocol.Message) received;
                    handleProtocolMessage(msg);
                } else if (received instanceof String) {
                    // Legacy string messages
                    String message = (String) received;
                    SwingUtilities.invokeLater(() -> appendChatMessage("Other: " + message));
                } else if (received instanceof FileData) {
                    // Legacy file data
                    FileData fileData = (FileData) received;
                    SwingUtilities.invokeLater(() -> handleReceivedFile(fileData));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> {
                    appendChatMessage("Connection lost: " + e.getMessage());
                    disconnectFromServer();
                });
            }
        }
    }

    private void handleProtocolMessage(OptimizationProtocol.Message msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.type) {
                case OptimizationProtocol.MSG_CHAT:
                    if (msg.payload instanceof OptimizationProtocol.ChatMessage) {
                        OptimizationProtocol.ChatMessage chatMsg = (OptimizationProtocol.ChatMessage) msg.payload;
                        appendChatMessage(msg.sender + ": " + chatMsg.content);
                    }
                    break;
                    
                case OptimizationProtocol.MSG_FILE:
                    if (msg.payload instanceof FileData) {
                        handleReceivedFile((FileData) msg.payload);
                    }
                    break;
                    
                case OptimizationProtocol.MSG_OPTIMIZATION_PROGRESS:
                    if (msg.payload instanceof OptimizationProtocol.OptimizationProgress) {
                        handleOptimizationProgress((OptimizationProtocol.OptimizationProgress) msg.payload);
                    }
                    break;
                    
                case OptimizationProtocol.MSG_OPTIMIZATION_RESULT:
                    if (msg.payload instanceof OptimizationProtocol.OptimizationResult) {
                        handleOptimizationResult((OptimizationProtocol.OptimizationResult) msg.payload);
                    }
                    break;
            }
        });
    }

    private void handleOptimizationProgress(OptimizationProtocol.OptimizationProgress progress) {
        optimizationStatusLabel.setText(String.format(
            "%s - Iteration: %d, Best Fitness: %.6f",
            progress.status, progress.currentIteration, progress.bestFitness
        ));
        
        int progressPercent = (int) ((progress.currentIteration * 100.0) / 1000); // Assuming max 1000 iterations
        optimizationProgressBar.setValue(Math.min(progressPercent, 100));
        
        appendOptimizationResult(String.format(
            "Iter %d: Fitness=%.6f, Time=%dms, Convergence=%.6f",
            progress.currentIteration, progress.bestFitness, 
            progress.elapsedTime, progress.convergenceRate
        ));
    }

    private void handleOptimizationResult(OptimizationProtocol.OptimizationResult result) {
        optimizationStatusLabel.setText("Optimization completed");
        optimizationProgressBar.setValue(100);
        optimizationProgressBar.setVisible(false);
        
        stopOptimizationButton.setEnabled(false);
        psoButton.setEnabled(true);
        acoButton.setEnabled(true);
        gaButton.setEnabled(true);
        
        appendOptimizationResult("=== " + result.algorithm + " Results ===");
        appendOptimizationResult("Success: " + result.success);
        appendOptimizationResult("Best Fitness: " + result.bestFitness);
        appendOptimizationResult("Total Iterations: " + result.totalIterations);
        appendOptimizationResult("Total Time: " + result.totalTime + "ms");
        
        if (result.bestSolution != null && result.bestSolution.length > 0) {
            StringBuilder sb = new StringBuilder("Best Solution: [");
            for (int i = 0; i < Math.min(result.bestSolution.length, 5); i++) {
                sb.append(String.format("%.3f", result.bestSolution[i]));
                if (i < Math.min(result.bestSolution.length, 5) - 1) sb.append(", ");
            }
            if (result.bestSolution.length > 5) sb.append("...");
            sb.append("]");
            appendOptimizationResult(sb.toString());
        }
        
        if (!result.success && result.errorMessage != null) {
            appendOptimizationResult("Error: " + result.errorMessage);
        }
        
        appendOptimizationResult("===============================");
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
                    appendChatMessage("File saved: " + saveChooser.getSelectedFile().getName());
                } catch (IOException e) {
                    appendChatMessage("Error saving file: " + e.getMessage());
                }
            }
        }
        
        appendChatMessage("Received file: " + fileData.fileName + 
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
        psoButton.setEnabled(isConnected);
        acoButton.setEnabled(isConnected);
        gaButton.setEnabled(isConnected);
        
        statusLabel.setText(isConnected ? "Status: Connected" : "Status: Disconnected");
    }

    private void appendChatMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            chatArea.append("[" + timestamp + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void appendOptimizationResult(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            optimizationResultArea.append("[" + timestamp + "] " + message + "\n");
            optimizationResultArea.setCaretPosition(optimizationResultArea.getDocument().getLength());
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
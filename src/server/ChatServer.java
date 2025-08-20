package server;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import shared.*;

public class ChatServer extends JFrame {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private ExecutorService optimizationExecutor;
    
    // GUI Components
    private JTextArea serverLog;
    private JTextArea clientList;
    private JTextArea optimizationLog;
    private JButton startButton, stopButton;
    private JLabel statusLabel, clientCountLabel, activeOptimizationsLabel;
    
    private boolean isRunning = false;
    private Map<String, OptimizationTask> activeOptimizations;

    public ChatServer() {
        clients = Collections.synchronizedList(new ArrayList<>());
        activeOptimizations = Collections.synchronizedMap(new HashMap<>());
        optimizationExecutor = Executors.newFixedThreadPool(8); // Increased thread pool
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Optimization Server - PSO/ACO/GA System");
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
        
        activeOptimizationsLabel = new JLabel("Active Optimizations: 0");
        activeOptimizationsLabel.setForeground(Color.WHITE);
        activeOptimizationsLabel.setFont(new Font("Arial", Font.BOLD, 14));

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(statusLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(clientCountLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(activeOptimizationsLabel);

        // Center Panel - Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Server Log Tab
        serverLog = new JTextArea(15, 50);
        serverLog.setEditable(false);
        serverLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        serverLog.setBackground(new Color(44, 62, 80));
        serverLog.setForeground(Color.WHITE);
        JScrollPane serverLogScroll = new JScrollPane(serverLog);
        
        // Optimization Log Tab
        optimizationLog = new JTextArea(15, 50);
        optimizationLog.setEditable(false);
        optimizationLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        optimizationLog.setBackground(new Color(44, 62, 80));
        optimizationLog.setForeground(Color.CYAN);
        JScrollPane optimizationLogScroll = new JScrollPane(optimizationLog);
        
        // Client List Tab
        clientList = new JTextArea(15, 30);
        clientList.setEditable(false);
        clientList.setFont(new Font("Arial", Font.PLAIN, 12));
        clientList.setBackground(new Color(236, 240, 241));
        JScrollPane clientScroll = new JScrollPane(clientList);
        
        tabbedPane.addTab("Server Log", serverLogScroll);
        tabbedPane.addTab("Optimization Log", optimizationLogScroll);
        tabbedPane.addTab("Connected Clients", clientScroll);

        add(controlPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

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
            logMessage("Optimization engine initialized with 8 worker threads");
            logOptimization("PSO, ACO, GA algorithms ready for execution");
            
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
            // Stop all active optimizations
            synchronized (activeOptimizations) {
                for (OptimizationTask task : activeOptimizations.values()) {
                    task.stop();
                }
                activeOptimizations.clear();
            }
            
            // Close all client connections
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.closeConnection();
                }
                clients.clear();
            }
            
            // Shutdown optimization executor
            optimizationExecutor.shutdownNow();
            
            // Close server socket
            if (serverSocket != null) {
                serverSocket.close();
            }
            
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Server Status: Stopped");
            
            logMessage("Server stopped");
            updateClientCount();
            updateClientList();
            updateOptimizationCount();
            
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

    public void logOptimization(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            optimizationLog.append("[" + timestamp + "] " + message + "\n");
            optimizationLog.setCaretPosition(optimizationLog.getDocument().getLength());
        });
    }

    public void broadcastMessage(OptimizationProtocol.Message message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        updateClientCount();
        updateClientList();
    }

    public void handleOptimizationRequest(OptimizationProtocol.OptimizationRequest request, 
                                        ClientHandler requester) {
        String taskId = requester.getClientId() + "_" + System.currentTimeMillis();
        
        logOptimization("Starting " + request.algorithm + " optimization for client: " + 
                       requester.getClientId());
        
        OptimizationTask task = new OptimizationTask(taskId, request, requester, this);
        
        synchronized (activeOptimizations) {
            activeOptimizations.put(taskId, task);
        }
        
        optimizationExecutor.submit(task);
        updateOptimizationCount();
    }

    public void stopOptimization(String clientId) {
        synchronized (activeOptimizations) {
            OptimizationTask taskToStop = null;
            for (OptimizationTask task : activeOptimizations.values()) {
                if (task.getRequester().getClientId().equals(clientId)) {
                    taskToStop = task;
                    break;
                }
            }
            
            if (taskToStop != null) {
                taskToStop.stop();
                activeOptimizations.remove(taskToStop.getTaskId());
                logOptimization("Stopped optimization for client: " + clientId);
                updateOptimizationCount();
            }
        }
    }

    public void optimizationCompleted(String taskId) {
        synchronized (activeOptimizations) {
            activeOptimizations.remove(taskId);
        }
        updateOptimizationCount();
    }

    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            clientCountLabel.setText("Connected Clients: " + clients.size());
        });
    }

    private void updateOptimizationCount() {
        SwingUtilities.invokeLater(() -> {
            activeOptimizationsLabel.setText("Active Optimizations: " + activeOptimizations.size());
        });
    }

    private void updateClientList() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            synchronized (clients) {
                for (int i = 0; i < clients.size(); i++) {
                    ClientHandler client = clients.get(i);
                    sb.append("Client ").append(i + 1).append(": ")
                      .append(client.getClientAddress())
                      .append(" [").append(client.getClientId()).append("]\n");
                }
            }
            clientList.setText(sb.toString());
        });
    }

    // Inner class for handling optimization tasks
    public static class OptimizationTask implements Runnable {
        private final String taskId;
        private final OptimizationProtocol.OptimizationRequest request;
        private final ClientHandler requester;
        private final ChatServer server;
        private volatile boolean stopped = false;

        public OptimizationTask(String taskId, OptimizationProtocol.OptimizationRequest request, 
                              ClientHandler requester, ChatServer server) {
            this.taskId = taskId;
            this.request = request;
            this.requester = requester;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                server.logOptimization("Executing " + request.algorithm + " optimization [" + taskId + "]");
                
                // Progress callback
                OptimizationAlgorithms.ProgressCallback progressCallback = progress -> {
                    if (!stopped) {
                        OptimizationProtocol.Message progressMsg = new OptimizationProtocol.Message(
                            OptimizationProtocol.MSG_OPTIMIZATION_PROGRESS, "Server", progress
                        );
                        requester.sendMessage(progressMsg);
                        
                        server.logOptimization(String.format(
                            "[%s] Iteration %d: Fitness=%.6f", 
                            request.algorithm, progress.currentIteration, progress.bestFitness
                        ));
                    }
                };
                
                // Select fitness function based on problem type
                OptimizationAlgorithms.FitnessFunction fitnessFunction = 
                    OptimizationAlgorithms.SPHERE_FUNCTION;
                
                OptimizationProtocol.OptimizationResult result = null;
                
                // Execute the appropriate algorithm
                switch (request.algorithm) {
                    case OptimizationProtocol.ALGORITHM_PSO:
                        OptimizationAlgorithms.PSO pso = new OptimizationAlgorithms.PSO();
                        result = pso.optimize(request, fitnessFunction, progressCallback);
                        break;
                        
                    case OptimizationProtocol.ALGORITHM_ACO:
                        OptimizationAlgorithms.ACO aco = new OptimizationAlgorithms.ACO();
                        result = aco.optimize(request, fitnessFunction, progressCallback);
                        break;
                        
                    case OptimizationProtocol.ALGORITHM_GA:
                        OptimizationAlgorithms.GA ga = new OptimizationAlgorithms.GA();
                        result = ga.optimize(request, fitnessFunction, progressCallback);
                        break;
                        
                    default:
                        result = new OptimizationProtocol.OptimizationResult(
                            request.algorithm, false, "Unknown algorithm: " + request.algorithm
                        );
                }
                
                if (!stopped) {
                    // Send result back to client
                    OptimizationProtocol.Message resultMsg = new OptimizationProtocol.Message(
                        OptimizationProtocol.MSG_OPTIMIZATION_RESULT, "Server", result
                    );
                    requester.sendMessage(resultMsg);
                    
                    server.logOptimization(String.format(
                        "[%s] Completed - Best Fitness: %.6f, Time: %dms", 
                        request.algorithm, result.bestFitness, result.totalTime
                    ));
                }
                
            } catch (Exception e) {
                server.logOptimization("Error in optimization [" + taskId + "]: " + e.getMessage());
                
                if (!stopped) {
                    OptimizationProtocol.OptimizationResult errorResult = 
                        new OptimizationProtocol.OptimizationResult(
                            request.algorithm, false, "Optimization error: " + e.getMessage()
                        );
                    
                    OptimizationProtocol.Message errorMsg = new OptimizationProtocol.Message(
                        OptimizationProtocol.MSG_OPTIMIZATION_RESULT, "Server", errorResult
                    );
                    requester.sendMessage(errorMsg);
                }
            } finally {
                server.optimizationCompleted(taskId);
            }
        }

        public void stop() {
            stopped = true;
        }

        public String getTaskId() { 
            return taskId; 
        }
        
        public ClientHandler getRequester() { 
            return requester; 
        }
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
// src/shared/OptimizationProtocol.java
package shared;

import java.io.Serializable;

/**
 * Protocol definitions for optimization communication
 */
public class OptimizationProtocol {
    
    // Message Types
    public static final String MSG_CHAT = "CHAT";
    public static final String MSG_FILE = "FILE";
    public static final String MSG_OPTIMIZATION_REQUEST = "OPT_REQUEST";
    public static final String MSG_OPTIMIZATION_RESULT = "OPT_RESULT";
    public static final String MSG_OPTIMIZATION_PROGRESS = "OPT_PROGRESS";
    public static final String MSG_OPTIMIZATION_STOP = "OPT_STOP";
    
    // Algorithm Types
    public static final String ALGORITHM_PSO = "PSO";
    public static final String ALGORITHM_ACO = "ACO";
    public static final String ALGORITHM_GA = "GA";
    
    // Optimization Objectives
    public static final String OBJECTIVE_MINIMIZE = "MINIMIZE";
    public static final String OBJECTIVE_MAXIMIZE = "MAXIMIZE";
    
    /**
     * Generic message wrapper for TCP communication
     */
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String type;
        public String sender;
        public long timestamp;
        public Object payload;
        
        public Message(String type, String sender, Object payload) {
            this.type = type;
            this.sender = sender;
            this.timestamp = System.currentTimeMillis();
            this.payload = payload;
        }
        
        @Override
        public String toString() {
            return "Message{type='" + type + "', sender='" + sender + "', timestamp=" + timestamp + "}";
        }
    }
    
    /**
     * Chat message payload
     */
    public static class ChatMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String content;
        
        public ChatMessage(String content) {
            this.content = content;
        }
    }
    
    /**
     * Optimization request payload
     */
    public static class OptimizationRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String algorithm;
        public String objective;
        public int populationSize;
        public int maxIterations;
        public double[] problemData;
        public OptimizationParameters parameters;
        
        public OptimizationRequest(String algorithm, String objective, 
                                 int populationSize, int maxIterations, 
                                 double[] problemData, OptimizationParameters parameters) {
            this.algorithm = algorithm;
            this.objective = objective;
            this.populationSize = populationSize;
            this.maxIterations = maxIterations;
            this.problemData = problemData;
            this.parameters = parameters;
        }
    }
    
    /**
     * Algorithm-specific parameters
     */
    public static class OptimizationParameters implements Serializable {
        private static final long serialVersionUID = 1L;
        
        // PSO parameters
        public double inertiaWeight = 0.5;
        public double cognitiveCoeff = 2.0;
        public double socialCoeff = 2.0;
        
        // ACO parameters
        public double alpha = 1.0;
        public double beta = 5.0;
        public double evaporationRate = 0.1;
        public double pheromoneConstant = 100.0;
        
        // GA parameters
        public double mutationRate = 0.01;
        public double crossoverRate = 0.8;
        public String selectionMethod = "TOURNAMENT";
        
        public OptimizationParameters() {}
    }
    
    /**
     * Optimization progress update
     */
    public static class OptimizationProgress implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public int currentIteration;
        public double bestFitness;
        public double[] bestSolution;
        public double convergenceRate;
        public long elapsedTime;
        public String status;
        
        public OptimizationProgress(int currentIteration, double bestFitness, 
                                  double[] bestSolution, double convergenceRate, 
                                  long elapsedTime, String status) {
            this.currentIteration = currentIteration;
            this.bestFitness = bestFitness;
            this.bestSolution = bestSolution;
            this.convergenceRate = convergenceRate;
            this.elapsedTime = elapsedTime;
            this.status = status;
        }
    }
    
    /**
     * Final optimization result
     */
    public static class OptimizationResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String algorithm;
        public boolean success;
        public double bestFitness;
        public double[] bestSolution;
        public int totalIterations;
        public long totalTime;
        public double[] fitnessHistory;
        public String errorMessage;
        
        public OptimizationResult(String algorithm, boolean success, double bestFitness,
                                double[] bestSolution, int totalIterations, long totalTime,
                                double[] fitnessHistory) {
            this.algorithm = algorithm;
            this.success = success;
            this.bestFitness = bestFitness;
            this.bestSolution = bestSolution;
            this.totalIterations = totalIterations;
            this.totalTime = totalTime;
            this.fitnessHistory = fitnessHistory;
        }
        
        public OptimizationResult(String algorithm, boolean success, String errorMessage) {
            this.algorithm = algorithm;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
}
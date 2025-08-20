// src/shared/OptimizationAlgorithms.java
package shared;

import java.util.*;

/**
 * Implementation of PSO, ACO, and GA algorithms
 */
public class OptimizationAlgorithms {
    
    public interface ProgressCallback {
        void onProgress(OptimizationProtocol.OptimizationProgress progress);
    }
    
    public interface FitnessFunction {
        double evaluate(double[] solution);
    }
    
    // Default test function (Sphere function for minimization)
    public static final FitnessFunction SPHERE_FUNCTION = solution -> {
        double sum = 0.0;
        for (double x : solution) {
            sum += x * x;
        }
        return sum;
    };
    
    // Rastrigin function for testing
    public static final FitnessFunction RASTRIGIN_FUNCTION = solution -> {
        double sum = 0.0;
        for (double x : solution) {
            sum += x * x - 10 * Math.cos(2 * Math.PI * x) + 10;
        }
        return sum;
    };
    
    /**
     * Particle Swarm Optimization (PSO)
     */
    public static class PSO {
        private final Random random = new Random();
        
        public static class Particle {
            public double[] position;
            public double[] velocity;
            public double[] bestPosition;
            public double bestFitness;
            public double currentFitness;
            
            public Particle(int dimensions, double[] bounds) {
                position = new double[dimensions];
                velocity = new double[dimensions];
                bestPosition = new double[dimensions];
                
                // Initialize random position within bounds
                for (int i = 0; i < dimensions; i++) {
                    position[i] = bounds[0] + Math.random() * (bounds[1] - bounds[0]);
                    velocity[i] = 0.0;
                    bestPosition[i] = position[i];
                }
                
                bestFitness = Double.MAX_VALUE;
                currentFitness = Double.MAX_VALUE;
            }
        }
        
        public OptimizationProtocol.OptimizationResult optimize(
                OptimizationProtocol.OptimizationRequest request,
                FitnessFunction fitnessFunction,
                ProgressCallback callback) {
            
            long startTime = System.currentTimeMillis();
            int dimensions = request.problemData.length;
            double[] bounds = {-100, 100}; // Default bounds
            
            // Initialize swarm
            Particle[] swarm = new Particle[request.populationSize];
            for (int i = 0; i < request.populationSize; i++) {
                swarm[i] = new Particle(dimensions, bounds);
            }
            
            double[] globalBest = new double[dimensions];
            double globalBestFitness = Double.MAX_VALUE;
            List<Double> fitnessHistory = new ArrayList<>();
            
            for (int iteration = 0; iteration < request.maxIterations; iteration++) {
                // Evaluate particles
                for (Particle particle : swarm) {
                    particle.currentFitness = fitnessFunction.evaluate(particle.position);
                    
                    // Update personal best
                    if (particle.currentFitness < particle.bestFitness) {
                        particle.bestFitness = particle.currentFitness;
                        System.arraycopy(particle.position, 0, particle.bestPosition, 0, dimensions);
                    }
                    
                    // Update global best
                    if (particle.currentFitness < globalBestFitness) {
                        globalBestFitness = particle.currentFitness;
                        System.arraycopy(particle.position, 0, globalBest, 0, dimensions);
                    }
                }
                
                // Update velocities and positions
                for (Particle particle : swarm) {
                    for (int d = 0; d < dimensions; d++) {
                        double r1 = random.nextDouble();
                        double r2 = random.nextDouble();
                        
                        // PSO velocity update equation
                        particle.velocity[d] = request.parameters.inertiaWeight * particle.velocity[d] +
                                             request.parameters.cognitiveCoeff * r1 * (particle.bestPosition[d] - particle.position[d]) +
                                             request.parameters.socialCoeff * r2 * (globalBest[d] - particle.position[d]);
                        
                        // Update position
                        particle.position[d] += particle.velocity[d];
                        
                        // Boundary constraints
                        if (particle.position[d] < bounds[0]) particle.position[d] = bounds[0];
                        if (particle.position[d] > bounds[1]) particle.position[d] = bounds[1];
                    }
                }
                
                fitnessHistory.add(globalBestFitness);
                
                // Send progress update every 10 iterations
                if (callback != null && iteration % 10 == 0) {
                    OptimizationProtocol.OptimizationProgress progress = new OptimizationProtocol.OptimizationProgress(
                        iteration, globalBestFitness, globalBest.clone(), 
                        calculateConvergenceRate(fitnessHistory), 
                        System.currentTimeMillis() - startTime, "RUNNING");
                    callback.onProgress(progress);
                }
            }
            
            double[] fitnessArray = fitnessHistory.stream().mapToDouble(Double::doubleValue).toArray();
            return new OptimizationProtocol.OptimizationResult(
                "PSO", true, globalBestFitness, globalBest, request.maxIterations,
                System.currentTimeMillis() - startTime, fitnessArray);
        }
    }
    
    /**
     * Ant Colony Optimization (ACO) for TSP-like problems
     */
    public static class ACO {
        private final Random random = new Random();
        
        public OptimizationProtocol.OptimizationResult optimize(
                OptimizationProtocol.OptimizationRequest request,
                FitnessFunction fitnessFunction,
                ProgressCallback callback) {
            
            long startTime = System.currentTimeMillis();
            int numCities = request.problemData.length;
            
            // Initialize pheromone matrix
            double[][] pheromones = new double[numCities][numCities];
            for (int i = 0; i < numCities; i++) {
                for (int j = 0; j < numCities; j++) {
                    pheromones[i][j] = 1.0;
                }
            }
            
            double[] globalBest = null;
            double globalBestFitness = Double.MAX_VALUE;
            List<Double> fitnessHistory = new ArrayList<>();
            
            for (int iteration = 0; iteration < request.maxIterations; iteration++) {
                double[][] deltaPheromones = new double[numCities][numCities];
                
                // Generate solutions for each ant
                for (int ant = 0; ant < request.populationSize; ant++) {
                    double[] solution = generateAntSolution(numCities, pheromones, request.parameters);
                    double fitness = fitnessFunction.evaluate(solution);
                    
                    if (fitness < globalBestFitness) {
                        globalBestFitness = fitness;
                        globalBest = solution.clone();
                    }
                    
                    // Update pheromone deposits
                    updatePheromoneDeposits(solution, fitness, deltaPheromones, request.parameters);
                }
                
                // Evaporate and update pheromones
                for (int i = 0; i < numCities; i++) {
                    for (int j = 0; j < numCities; j++) {
                        pheromones[i][j] *= (1.0 - request.parameters.evaporationRate);
                        pheromones[i][j] += deltaPheromones[i][j];
                        
                        // Ensure minimum pheromone level
                        if (pheromones[i][j] < 0.01) {
                            pheromones[i][j] = 0.01;
                        }
                    }
                }
                
                fitnessHistory.add(globalBestFitness);
                
                if (callback != null && iteration % 10 == 0) {
                    OptimizationProtocol.OptimizationProgress progress = new OptimizationProtocol.OptimizationProgress(
                        iteration, globalBestFitness, globalBest != null ? globalBest.clone() : new double[0], 
                        calculateConvergenceRate(fitnessHistory), 
                        System.currentTimeMillis() - startTime, "RUNNING");
                    callback.onProgress(progress);
                }
            }
            
            double[] fitnessArray = fitnessHistory.stream().mapToDouble(Double::doubleValue).toArray();
            return new OptimizationProtocol.OptimizationResult(
                "ACO", true, globalBestFitness, globalBest != null ? globalBest : new double[0], 
                request.maxIterations, System.currentTimeMillis() - startTime, fitnessArray);
        }
        
        private double[] generateAntSolution(int numCities, double[][] pheromones, OptimizationProtocol.OptimizationParameters params) {
            double[] solution = new double[numCities];
            boolean[] visited = new boolean[numCities];
            int currentCity = random.nextInt(numCities);
            solution[0] = currentCity;
            visited[currentCity] = true;
            
            for (int step = 1; step < numCities; step++) {
                int nextCity = selectNextCity(currentCity, visited, pheromones, params);
                if (nextCity == -1) {
                    // If no valid city found, select randomly from remaining
                    List<Integer> remaining = new ArrayList<>();
                    for (int i = 0; i < numCities; i++) {
                        if (!visited[i]) remaining.add(i);
                    }
                    if (!remaining.isEmpty()) {
                        nextCity = remaining.get(random.nextInt(remaining.size()));
                    } else {
                        break;
                    }
                }
                solution[step] = nextCity;
                visited[nextCity] = true;
                currentCity = nextCity;
            }
            
            return solution;
        }
        
        private int selectNextCity(int currentCity, boolean[] visited, double[][] pheromones, OptimizationProtocol.OptimizationParameters params) {
            List<Integer> availableCities = new ArrayList<>();
            List<Double> probabilities = new ArrayList<>();
            double totalProb = 0.0;
            
            for (int city = 0; city < visited.length; city++) {
                if (!visited[city]) {
                    double pheromone = Math.pow(pheromones[currentCity][city], params.alpha);
                    double heuristic = Math.pow(1.0 / (1.0 + Math.abs(currentCity - city)), params.beta);
                    double prob = pheromone * heuristic;
                    
                    availableCities.add(city);
                    probabilities.add(prob);
                    totalProb += prob;
                }
            }
            
            if (availableCities.isEmpty() || totalProb == 0.0) {
                return -1;
            }
            
            double randomValue = random.nextDouble() * totalProb;
            double cumulative = 0.0;
            
            for (int i = 0; i < availableCities.size(); i++) {
                cumulative += probabilities.get(i);
                if (randomValue <= cumulative) {
                    return availableCities.get(i);
                }
            }
            
            return availableCities.get(availableCities.size() - 1);
        }
        
        private void updatePheromoneDeposits(double[] solution, double fitness, double[][] deltaPheromones, OptimizationProtocol.OptimizationParameters params) {
            double deposit = params.pheromoneConstant / (1.0 + fitness);
            for (int i = 0; i < solution.length - 1; i++) {
                int from = (int) solution[i];
                int to = (int) solution[i + 1];
                if (from >= 0 && from < deltaPheromones.length && to >= 0 && to < deltaPheromones.length) {
                    deltaPheromones[from][to] += deposit;
                    deltaPheromones[to][from] += deposit;
                }
            }
        }
    }
    
    /**
     * Genetic Algorithm (GA)
     */
    public static class GA {
        private final static Random random = new Random();
        
        public static class Individual {
            public double[] genes;
            public double fitness;
            
            public Individual(int length, double[] bounds) {
                genes = new double[length];
                for (int i = 0; i < length; i++) {
                    genes[i] = bounds[0] + random.nextDouble() * (bounds[1] - bounds[0]);
                }
                fitness = Double.MAX_VALUE;
            }
            
            public Individual(double[] genes) {
                this.genes = genes.clone();
                this.fitness = Double.MAX_VALUE;
            }
        }
        
        public OptimizationProtocol.OptimizationResult optimize(
                OptimizationProtocol.OptimizationRequest request,
                FitnessFunction fitnessFunction,
                ProgressCallback callback) {
            
            long startTime = System.currentTimeMillis();
            int dimensions = request.problemData.length;
            double[] bounds = {-100, 100};
            
            // Initialize population
            List<Individual> population = new ArrayList<>();
            for (int i = 0; i < request.populationSize; i++) {
                population.add(new Individual(dimensions, bounds));
            }
            
            Individual bestIndividual = null;
            List<Double> fitnessHistory = new ArrayList<>();
            
            for (int generation = 0; generation < request.maxIterations; generation++) {
                // Evaluate fitness
                for (Individual individual : population) {
                    individual.fitness = fitnessFunction.evaluate(individual.genes);
                }
                
                // Find best individual
                Individual currentBest = Collections.min(population, Comparator.comparingDouble(i -> i.fitness));
                if (bestIndividual == null || currentBest.fitness < bestIndividual.fitness) {
                    bestIndividual = new Individual(currentBest.genes);
                    bestIndividual.fitness = currentBest.fitness;
                }
                
                fitnessHistory.add(bestIndividual.fitness);
                
                // Create new population
                List<Individual> newPopulation = new ArrayList<>();
                
                // Elitism - keep best individuals
                population.sort(Comparator.comparingDouble(i -> i.fitness));
                int eliteCount = Math.max(1, request.populationSize / 10);
                for (int i = 0; i < eliteCount; i++) {
                    newPopulation.add(new Individual(population.get(i).genes));
                }
                
                // Generate offspring
                while (newPopulation.size() < request.populationSize) {
                    Individual parent1 = tournamentSelection(population, 3);
                    Individual parent2 = tournamentSelection(population, 3);
                    
                    if (random.nextDouble() < request.parameters.crossoverRate) {
                        Individual[] offspring = crossover(parent1, parent2);
                        mutate(offspring[0], request.parameters.mutationRate, bounds);
                        newPopulation.add(offspring[0]);
                        if (newPopulation.size() < request.populationSize) {
                            mutate(offspring[1], request.parameters.mutationRate, bounds);
                            newPopulation.add(offspring[1]);
                        }
                    } else {
                        newPopulation.add(new Individual(parent1.genes));
                        if (newPopulation.size() < request.populationSize) {
                            newPopulation.add(new Individual(parent2.genes));
                        }
                    }
                }
                
                population = newPopulation;
                
                if (callback != null && generation % 10 == 0) {
                    OptimizationProtocol.OptimizationProgress progress = new OptimizationProtocol.OptimizationProgress(
                        generation, bestIndividual.fitness, bestIndividual.genes.clone(), 
                        calculateConvergenceRate(fitnessHistory), 
                        System.currentTimeMillis() - startTime, "RUNNING");
                    callback.onProgress(progress);
                }
            }
            
            double[] fitnessArray = fitnessHistory.stream().mapToDouble(Double::doubleValue).toArray();
            return new OptimizationProtocol.OptimizationResult(
                "GA", true, bestIndividual.fitness, bestIndividual.genes, request.maxIterations,
                System.currentTimeMillis() - startTime, fitnessArray);
        }
        
        private Individual tournamentSelection(List<Individual> population, int tournamentSize) {
            Individual best = null;
            for (int i = 0; i < tournamentSize; i++) {
                Individual candidate = population.get(random.nextInt(population.size()));
                if (best == null || candidate.fitness < best.fitness) {
                    best = candidate;
                }
            }
            return best;
        }
        
        private Individual[] crossover(Individual parent1, Individual parent2) {
            int length = parent1.genes.length;
            double[] offspring1 = new double[length];
            double[] offspring2 = new double[length];
            
            // Uniform crossover with 50% probability
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    offspring1[i] = parent1.genes[i];
                    offspring2[i] = parent2.genes[i];
                } else {
                    offspring1[i] = parent2.genes[i];
                    offspring2[i] = parent1.genes[i];
                }
            }
            
            return new Individual[]{new Individual(offspring1), new Individual(offspring2)};
        }
        
        private void mutate(Individual individual, double mutationRate, double[] bounds) {
            for (int i = 0; i < individual.genes.length; i++) {
                if (random.nextDouble() < mutationRate) {
                    // Gaussian mutation
                    double mutation = random.nextGaussian() * 0.1 * (bounds[1] - bounds[0]);
                    individual.genes[i] += mutation;
                    
                    // Ensure bounds
                    if (individual.genes[i] < bounds[0]) individual.genes[i] = bounds[0];
                    if (individual.genes[i] > bounds[1]) individual.genes[i] = bounds[1];
                }
            }
        }
    }
    
    private static double calculateConvergenceRate(List<Double> fitnessHistory) {
        if (fitnessHistory.size() < 2) return 0.0;
        
        int size = fitnessHistory.size();
        double recent = fitnessHistory.get(size - 1);
        double previous = fitnessHistory.get(Math.max(0, size - 11));
        
        return Math.abs(recent - previous) / Math.max(Math.abs(previous), 1.0);
    }
}
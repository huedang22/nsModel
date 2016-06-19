package nsSimulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class TrafficSimulation {

    // GLOBAL CONSTANTS ////////////////////////////////////////////////////////
    
    // SIMULATION DETAILS
    public static final int SIMULATION_STEP_COOLDOWN = 0;
    public static final int CAR_WIDTH = 10;
    
    // BROKEN CAR CONSTANTS 
    public static double BREAKING_DOWN_PROBABILITY = 0.3;
    public static final double GETTING_REPAIRED_PROBABILITY = 0;
    
    // MISCELLANEOUS
    public static final int GLOBAL_MAXIMUM_DECELERATION = 2;
    public static final int GLOBAL_MINIMUM_DECELERATION = 1;
    public static final int GLOBAL_MAXIMUM_ACCELERATION = 2;
    ////////////////////////////////////////////////////////////////////////////

    // CONFIGURATIONS //////////////////////////////////////////////////////////
    public static double DENSITY;
    public static double FAST_CAR_RATIO; // fast/total cars
    public static int NUMBER_OF_ITERATIONS;
    public static boolean GLOBAL_SPEED_RULE;
    public static int GLOBAL_MAX_SPEED;

    
    // parameter of NS model
    public static int MAX_ACCELERATION = 1;               // default is 1 in NS
    public static int MAX_SPEED_FAST_CAR = 5;
    public static int MAX_SPEED_SLOW_CAR = 3;    
    public static double PROBABILITY_FLUCTUATION = .25;
    public static int DISTANCE_TO_LOOK_AHEAD = 7;
    public static int SLACK = 3;
    public static boolean APPLY_SYMMETRIC_RULE = true; 

    // 
    public static int ROAD_SIZE = 0;                      // number of cells
    public static int NUM_FAST_CARS = 0;
    public static int NUM_SLOW_CARS = 0;
    public static boolean HAS_BROKEN_CAR = false;

   
    public static boolean DEBUG = false;
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, IOException {

        //Don't make this value zero, or it'll crash
        NUMBER_OF_ITERATIONS = 3600;
        
        
        // parameters related to road segment
        double cellLength = 7.5;            // in meter
        double roadLength = 7.5;                // in km, should be a multiple of cellLength 
        ROAD_SIZE = (int)(roadLength*1000/cellLength);   // number of cells
        System.out.println("ROAD_SIZE = " + ROAD_SIZE);
        
        // to use in the same scale with our model, set cellLength = 3, 
        // consider changing the MAX_ACCELERATION, masSpeedsFast, maxSpeedsSlow as well
        
        
        // parameters for NS model        
        APPLY_SYMMETRIC_RULE = true;                // paper section VIII-B
        MAX_ACCELERATION = 1;                       // consider changing this value if cell length is modified
                                // to use the basic model in paper section VI-C, set slack=0 and APPLY_SYMMETRIC_RULE = false
        int[] arrDistanceLookAhead = {7};   // {7, 16}
        int[] slacks = {3};                 // {3, 9}
        
//        double[] trafficDensities = {40, 80, 120, 160, 200};
        double[] trafficDensities = {0.05,0.1,0.15,.3,.4};
        double[] fastCarRatios = {0,0.25,0.50,0.75,1.0};
        int[] maxSpeedsSlow = {3,6,9};
        int[] maxSpeedsFast = {4,8,11};
        boolean[] globalRules = {false};
        boolean[] brokenCar = {true, false};
        int numRepetitions = 5;                     // repeat each model xxx times

        int totalCars;
        long startTime;

        AnimatedSimulation simulation = new AnimatedSimulation();
        PrintWriter writer = new PrintWriter("simulations.csv","UTF-8");
        writer.println("model,ith run, road_block, max_speed_slow, max_speed_fast, fast_car_ratio, density, total_all_cars_distance, total_slow_cars_distance, total_fast_cars_distance, worst_case_distance_slow_cars, worst_cast_distance_fast_cars, best_case_distance_slow_car, best_case_distance_fast_car,num_slow_cars,num_fast_cars,global_speed_rule,slack,distance_look_ahead");
        
        for (double density : trafficDensities) {
            for (double ratio : fastCarRatios) {
                for (int slow : maxSpeedsSlow) {
                    for (int fast : maxSpeedsFast) {
                        for (boolean global : globalRules) {
                            for (boolean broken : brokenCar) {
                                for (int distance : arrDistanceLookAhead) {
                                    for (int slack : slacks) {
                                        if (slow <= fast) {
                                            DENSITY = density;
                                            totalCars = (int) (ROAD_SIZE  * density);
                                            NUM_FAST_CARS = (int) (ratio * totalCars);
                                            NUM_SLOW_CARS = totalCars - NUM_FAST_CARS;

                                            DISTANCE_TO_LOOK_AHEAD = distance;
                                            SLACK = slack;

                                            HAS_BROKEN_CAR = broken;
                                            if (HAS_BROKEN_CAR){
                                                NUM_SLOW_CARS ++;   // broken car is counted in number of slow cars
                                            }
                                            
                                            FAST_CAR_RATIO = ratio;
                                            MAX_SPEED_SLOW_CAR = slow;
                                            MAX_SPEED_FAST_CAR = fast;
                                            BREAKING_DOWN_PROBABILITY = broken ? 0.3 : 0.0;
                                            GLOBAL_SPEED_RULE = global;
                                            
                                            for (int rep=0; rep < numRepetitions; rep++) {
//                                                startTime = System.nanoTime();
                                                simulation.initialiseSimulation(NUMBER_OF_ITERATIONS);
                                                writer.println(simulation.runSimulation(rep));    
//                                                System.out.println("Running one simulation: " + (System.nanoTime()-startTime)/Math.pow(10, 9) + "seconds");
                                            }      
                                        }
                                        writer.flush();      
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }               
        writer.close();
    }    
}

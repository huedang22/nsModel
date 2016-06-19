package nsSimulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


/*==============================================================================
Implement NS model based on paper Two-lane traffic rules for cellular Automata
*==============================================================================*/

public class RoadNS {

    public static int NUM_LANES = 2;
    public static int RIGHT_LANE = 1;
    public static int LEFT_LANE = 2;
    
    public static int NUM_TYPE_CAR = 2;
    public static int TYPE_CAR_SLOW = 1;
    public static int TYPE_CAR_FAST = 2;
    
    private int numCarsPassingEnd = 0;                // number of cars passing the end of the segment to verify with result in the paper
            
    private ArrayList<CarNS> cars;                // contains cars on the road

    private int[] rightLane;                    // values: current speed of car (or -1 if no car)
    private int[] leftLane;

    private int[] helperRight;
    private int[] helperLeft;
    
    private String outputFile;
    
    
    public RoadNS(){
        
        outputFile = "debugFlow.txt";
       
        // Initialise variables
        cars = new ArrayList<>();
        rightLane = new int[TrafficSimulation.ROAD_SIZE];
        leftLane = new int[TrafficSimulation.ROAD_SIZE];

        helperRight = new int[TrafficSimulation.ROAD_SIZE];
        helperLeft = new int[TrafficSimulation.ROAD_SIZE];

        for (int i = 0; i < rightLane.length; i++) {
            rightLane[i] = -1;
            leftLane[i] = -1;
        }
        
        generateCars();
        if (TrafficSimulation.DEBUG) printTrafficSituation();
    }

    
    /*==============================================================================
    Generate cars for the model
    - position of car, lane of car, speed of car are randomly generated
    - distance between a car and the car in front of it (same lane) is 2 x speed of the car (2-seconds rule)
    - due to road situation, speed of cars may be limitted:
        road length (both lanes) = 2 x L
        num car = N
        mean gap between cars = 2L/N
        therefore v should be <= L/N (to satisfy 2-seconds rule )
    *==============================================================================*/
    public void generateCars(){
        // randomly generate all the cars
        
        int rightLane_dummyPosition = 0, leftLane_dummyPosition = 0, dummyPosition = 0, slow_generated = 0, fast_generated = 0, lane, type_of_car;
        CarNS tmpC;
        Random r = new Random();
        int limitSpeed = (int) (TrafficSimulation.ROAD_SIZE / (TrafficSimulation.NUM_FAST_CARS + TrafficSimulation.NUM_SLOW_CARS));
        boolean createdBrokenCar = ! TrafficSimulation.HAS_BROKEN_CAR;
        
        
        for (int i = 0; i < TrafficSimulation.NUM_FAST_CARS + TrafficSimulation.NUM_SLOW_CARS; i++) {
            
            // randomly choose the lane (unless the limit is reached)
            if (rightLane_dummyPosition>= TrafficSimulation.ROAD_SIZE)
                lane = LEFT_LANE;
            else if (leftLane_dummyPosition>= TrafficSimulation.ROAD_SIZE)
                lane = RIGHT_LANE;            
            else lane = r.nextInt(NUM_LANES) + 1;            
            
            // retrieve position for the (soon to be generated) car to be placed
            if (lane == RIGHT_LANE) dummyPosition = rightLane_dummyPosition;
            else dummyPosition = leftLane_dummyPosition;
            
            
            // randomly choose the type of car (unless the limit is reached)
            if (slow_generated == TrafficSimulation.NUM_SLOW_CARS)            // limit is reached
                type_of_car = TYPE_CAR_FAST;
            else if (fast_generated == TrafficSimulation.NUM_FAST_CARS)       // limit is reached
                type_of_car = TYPE_CAR_SLOW;
            else type_of_car = r.nextInt(NUM_TYPE_CAR) + 1; // randomly

            // generate the car and add it to the list of cars
            if (type_of_car == TYPE_CAR_SLOW) {                
                if (!createdBrokenCar && TrafficSimulation.NUM_SLOW_CARS <= 4*(slow_generated+1)){
                    createdBrokenCar = true;
                    tmpC = new BrokenCarNS(i, lane, dummyPosition, limitSpeed); // broken car
                    slow_generated++;
                } else {
                    tmpC = new SlowCarNS(i, lane, dummyPosition, limitSpeed);   // slow car
                    slow_generated++;                    
                }                
            } else {
                tmpC = new FastCarNS(i, lane, dummyPosition, limitSpeed);
                fast_generated++;
            }
            cars.add(tmpC);

            // save data to the road structure (lanes)
            if (lane == RIGHT_LANE) rightLane[dummyPosition] = tmpC.getSpeed();
            else leftLane[dummyPosition] = tmpC.getSpeed();

            // follow the 2-seconds rule 
            dummyPosition = dummyPosition + 2*tmpC.getSpeed();
            if (tmpC.getSpeed()==0) dummyPosition +=1;          // speed is 0, so gap should be 1

            // set the position for the next car
            if (lane == RIGHT_LANE) rightLane_dummyPosition = dummyPosition;
            else leftLane_dummyPosition = dummyPosition;
        }
        
        // debug
        if (TrafficSimulation.DEBUG){
            System.out.println("Lane 1\n" + Arrays.toString(rightLane) + "\n");
            System.out.println("Lane 2\n" + Arrays.toString(leftLane) + "\n");        
        }
    }

    public void nextState() {
        // CALCULATE NEW STATE /////////////////////////////////////////////////
        // clear helper lanes
        for (int i = 0; i < helperRight.length; i++) {
            helperRight[i] = -1;
            helperLeft[i] = -1;
        }

        // move cars (check rules on current road and save new positions in next road)
        for (CarNS car : cars) {
            moveCar(car);
        }

        // END OF CALCULATE NEW STATE //////////////////////////////////////////
        //
        // set new state
        rightLane = helperRight.clone();
        leftLane = helperLeft.clone();
    }

    /*
     * Prints the current state of the road to the console.
     */
    public void printTrafficSituation() {
        String traffic_rightLane = "|", traffic_leftLane = "|";

        for (int i = 0; i < rightLane.length; i++) {
            traffic_rightLane += toSymbol(rightLane[i]);
            traffic_leftLane += toSymbol(leftLane[i]);
        }

        traffic_rightLane += "|";
        traffic_leftLane += "|";

//        System.out.println(traffic_rightLane + "\n" + traffic_leftLane + "\n");
        System.out.println(traffic_leftLane + "\n" + traffic_rightLane + "\n");
    }

    /*
     * Calculates all the statistics for the simulation.
     * @param numIterations The number of states the simulation has.
     * @return The statistics of the simulation.
     */
    public String getStatistics(int numIterations) {
        //TODO implement this method! (calculate flows etc)
        return "not implemented yet";
    }

    // Will probably be deleted (and replaced with getStatistics()).
    public void printFlow(int numIterations) throws FileNotFoundException, IOException {
        int totalDistance = 0;
        for (CarNS c : cars) {
            totalDistance += c.getTraveledDistance();
        }
//        double flow = ((double) totalDistance) / (numIterations * TrafficSimulation.ROAD_SIZE * 2 * (TrafficSimulation.NUM_SLOW_CARS + TrafficSimulation.NUM_FAST_CARS));
        double flow = Math.round(totalDistance * 100/numIterations) /100.0;
        System.out.println("Flow: " + flow);        
        System.out.println("Number of cars passing the end of segment: " + numCarsPassingEnd + ", numIterations so far: " + numIterations + ", Flow = " + numCarsPassingEnd/numIterations);

//        if (TrafficSimulation.DEBUG){
        if (true){
            try(  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true))) ){
                out.println(numIterations + "," + flow + "," + numCarsPassingEnd);
            }        
        }
    }

    /*==========================================================================
    A measurement of flow by calculating the travelled cells over  
    a measure period (i.e. numIterations)
    ==========================================================================*/
    public double getFlow(int numIterations){
        int totalDistance = 0;
        for (CarNS c : cars) {
            totalDistance += c.getTraveledDistance();
        }
        return Math.round(totalDistance * 100/numIterations) /100.0;    // round up to 2 digit after zero        
    }

    
    /*==========================================================================
    Another measurement of flow by calculating the number of cars passing the 
    end of the road segment during a measure period (i.e. numIterations)
    ==========================================================================*/
    public double getFlow2(int numIterations){
        return Math.round(numCarsPassingEnd * 100/numIterations) /100.0;    // round up to 2 digit after zero
    }
    
    /*
     *
     * @return The list of cars in the road.
     */
    public ArrayList<CarNS> getCars() {
        return cars;
    }

    /*
     * Helper method for printTrafficSituation(). Converts speeds with more than
     * 2 digits to characters (Hex encoding). Does not change 1 digit speeds. If
     * the speed is -1 it returns an underscore.
     * @param speed An integer speed or -1 if no car is present.
     * @return A character that represents the input speed.
     */
    private char toSymbol(int speed) {
        if (speed >= 0 && speed <= 9) {
            return Character.forDigit(speed, 10);
        } else {
            switch (speed) {
                case -1:
                    return '_';
                case 10:
                    return 'A';
                case 11:
                    return 'B';
                case 12:
                    return 'C';

                case 13:
                    return 'D';
                case 14:
                    return 'E';
                case 15:
                    return 'F';
                default:
                    return '?';
            }
        }
    }

    private SpeedDistance getStatusWithFrontCar(int lane, int position) {
        int speed = 0, distance = 0;
        int[] arr;
        
        if (lane==RIGHT_LANE) arr = rightLane;
        else arr = leftLane;
            
        for (int i = position + 1; i < TrafficSimulation.ROAD_SIZE; i++) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = i - position - 1;
                return new SpeedDistance(speed, distance);
            }          
        }

        // the consideration car is at the end of the road, therefore continue searching from the beginning of the lane
        for (int i = 0; i < TrafficSimulation.ROAD_SIZE; i++) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = i + TrafficSimulation.ROAD_SIZE - position - 1;
                return new SpeedDistance(speed, distance);              
            }
        }
        
        return new SpeedDistance(TrafficSimulation.MAX_SPEED_FAST_CAR, Integer.MAX_VALUE);  // no car
    }

    private SpeedDistance getStatusWithBehindCar(int lane, int position) {
        int speed = 0, distance = 0;
        int[] arr;
        
        if (lane==RIGHT_LANE) arr = rightLane;
        else arr = leftLane;
            
        for (int i = position - 1; i >= 0; i--) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = position - i - 1;
                return new SpeedDistance(speed, distance);
            }          
        }

        // the consideration car is at the begining of the road, therefore continue searching from the end of the lane
        for (int i = TrafficSimulation.ROAD_SIZE-1; i >= 0; i--) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = position + TrafficSimulation.ROAD_SIZE - i - 1;
                return new SpeedDistance(speed, distance);              
            }
        }
        
        return new SpeedDistance(TrafficSimulation.MAX_SPEED_FAST_CAR, Integer.MAX_VALUE);  // no car
    }
    
    private void moveCar(CarNS car) {
        int lane = car.getLane();
        int position = car.getPosition();
        int speed = car.getSpeed();
        int otherLane = Math.floorMod(2*lane,3);

        SpeedDistance withCarFront = getStatusWithFrontCar(lane, position);
        SpeedDistance withCarFrontNextLane = getStatusWithFrontCar(otherLane, position-1);
        SpeedDistance withCarBehindNextLane = getStatusWithBehindCar(otherLane, position+1);
        
        int newSpeed = car.adaptSpeed(withCarFront, withCarFrontNextLane, withCarBehindNextLane);

        if (position + newSpeed >= TrafficSimulation.ROAD_SIZE) numCarsPassingEnd += 1;
        
        int newPosition = Math.floorMod(position + newSpeed, TrafficSimulation.ROAD_SIZE);
        int newLane = car.lane;
        
        // commit the changes
        car.setPosition(newPosition);
        //Setting the new speed, lane is already done during the call to adaptSpeed();

        if (TrafficSimulation.DEBUG) 
            System.out.println("Car " + car.getID() + " old speed " + speed + " new speed " + newSpeed + " old lane " + lane + " new lane " + newLane + "\n");
        
        if (newLane == LEFT_LANE) {
            helperLeft[newPosition] = newSpeed;
        } else {
            helperRight[newPosition] = newSpeed;
        }        
    }
    
}



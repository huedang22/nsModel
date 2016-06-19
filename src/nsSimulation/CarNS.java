package nsSimulation;

import java.awt.Color;
import java.util.Random;

/*==============================================================================
Implement Car class for NS model based on 
Nagel, K., Wolf, D. E., Wagner, P., & Simon, P. (1998). Two-lane traffic rules for cellular automata: A systematic approach. Physical Review E, 58(2), 1425–1437. http://doi.org/10.1103/PhysRevE.58.1425
===============================================================================*/

/*
 with cell length = 7.5m 
 speed          km/h
    1       	27
    2		54
    3		81
    4		108
    5		135
    6		162
 */

public class CarNS {

    protected int ID;
    protected int speed;
    protected int lane;
    protected int position;
    protected int maxSpeed;
    protected Color color;
    protected int traveledDistance;
    protected int maxReachedSpeed = -1;

    public CarNS(int ID, int lane, int position) {
        this.ID = ID;
        this.lane = lane;
        this.position = position;
        this.traveledDistance = 0;
    }
    
    public void clearTraveledDistance() {
        traveledDistance = 0;
    }

    public int getTraveledDistance() {
        return traveledDistance;
    }

    public int getID() {
        return ID;
    }

    public int getSpeed() {
        return speed;
    }

    public int getMaxReachedSpeed() {
        return maxReachedSpeed;
    }

    public void setMaxReachedSpeed(int newSpeed) {
        if (maxReachedSpeed < newSpeed) {
            maxReachedSpeed = newSpeed;
        }
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public int getLane() {
        return lane;
    }

    public void setPosition(int newPosition) {
        if (this.position + speed >= TrafficSimulation.ROAD_SIZE) {
            traveledDistance += TrafficSimulation.ROAD_SIZE - this.position;
            traveledDistance += newPosition;
        } else traveledDistance += newPosition - this.position;
        
        this.position = newPosition;
    }

    public int getPosition() {
        return position;
    }

    public Color getColor() {
        return color;
    }

    public String getType() {
        switch (this.getClass().toString()) {
            case "class nsSimulation.SlowCarNS":
                return "S";
            case "class nsSimulation.FastCarNS":
                return "F";
        }
        return "E";
    }

    @Override
    public String toString() {
        return "(" + getType() + " " + lane + "," + position + "," + speed + ") ";
    }

    /*
     * This method adjusts the speed of the car based on neighbour cars
     */
    public int adaptSpeed(SpeedDistance carFront, SpeedDistance carFrontNextLane, SpeedDistance carBehindNextLane){
            
//        if (TrafficSimulation.DEBUG)
//            System.out.print("Lane " + lane + " pos " + position + " speed " + speed + 
//                    ", sFront " + carFront.getSpeed() + ", dFront " + carFront.getDistance() + 
//                    ", sFrontNext " + carFrontNextLane.getSpeed() + ", dFrontNext " + carFrontNextLane.getDistance() + 
//                    ", sBehindNext " + carBehindNextLane.getSpeed() + " dBehindNext " + carBehindNextLane.getDistance() + ". ");

        boolean goodGapToChange = (carBehindNextLane.getDistance() >= TrafficSimulation.MAX_SPEED_FAST_CAR && carFrontNextLane.getDistance() >= speed);
        
        if (carFront.getDistance() <= TrafficSimulation.DISTANCE_TO_LOOK_AHEAD || carFrontNextLane.getDistance() <= TrafficSimulation.DISTANCE_TO_LOOK_AHEAD){            
            
            if (lane==RoadNS.RIGHT_LANE) {                
                if (TrafficSimulation.APPLY_SYMMETRIC_RULE && speed==0){
                    // symmetric rule - paper section VIII-B
                    if (goodGapToChange && carFrontNextLane.getDistance()>carFront.getDistance()){
//                    if (goodGapToChange){
                        lane = RoadNS.LEFT_LANE;     // switch lane
                        if (TrafficSimulation.DEBUG)
                            System.out.print("Change to left lane. ");                          
                    }
                } else if ((carFront.getSpeed() <= speed || carFrontNextLane.getSpeed() <= speed) && goodGapToChange) {
                    lane = RoadNS.LEFT_LANE;     // switch lane
                    if (TrafficSimulation.DEBUG)
                        System.out.print("Change to left lane. ");  
                }
            } else {                
                if (TrafficSimulation.APPLY_SYMMETRIC_RULE && speed==0){
                    // symmetric rule - paper section VIII-B
                    if (goodGapToChange && carFrontNextLane.getDistance()>carFront.getDistance()){
//                    if (goodGapToChange){
                        lane = RoadNS.RIGHT_LANE;     // switch lane
                        if (TrafficSimulation.DEBUG)
                            System.out.print("Change to left lane. ");                          
                    }
                } else if ((carFront.getSpeed() > (speed + TrafficSimulation.SLACK) && carFrontNextLane.getSpeed() > (speed + TrafficSimulation.SLACK)) && goodGapToChange) {
                    lane = RoadNS.RIGHT_LANE;     // switch lane
                    if (TrafficSimulation.DEBUG)
                        System.out.print("Change to right lane. ");                      
                }
            }            
        }
        
        // forward movement - paper section VI-B
        if (speed < maxSpeed) speed += TrafficSimulation.MAX_ACCELERATION;
        if (speed > carFront.getDistance()) speed = carFront.getDistance();
        if (speed >= 1){
            Random r = new Random();
            if (r.nextDouble() < TrafficSimulation.PROBABILITY_FLUCTUATION) speed--;
        }
        
        setMaxReachedSpeed(speed);
        return speed;
    }
    
}

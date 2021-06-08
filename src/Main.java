import dataModels.Element;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Elements describe a channel (same as the Element in the C++ code)
        ArrayList<Element> elements;

        // Random is a RNG
        Random random = new Random();

        // d0 is the reference distance (microcell)
        final int d0 = 100;
        // f is the transmission frequency
        final int f =  1900 * 1000000;
        // lambda is the wavelength.
        // 299792458 is the speed of light.
        final double lambda = 299792458./f;
        // n is the loss factor
        final int n = 4;
        // Ld0 is a constant, the path loss of the reference distance in dB
        final double Ld0 = 10*Math.log10(Math.pow(4*Math.PI*d0/lambda, 2));
        // The sum of Interference + Noise in Watts
        final double interPlusNoise = 1.12 * Math.pow(10,-11);

        //  7 symbols every 1 msec = 7000 symbols/sec
        final double symbolPerSec = 7000;

        // maxSinr is a variable that has the value of the maximum SINR
        double maxSinr = -1000;

        // Stats contains the number of occurrences a CQI had
        int[] stats = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

        /*
         * channels contains the number of channels user specified
         * acceptedCallsCounter is the number of calls that are accepted
         * blockedCallsCounter is the number of calls that are blocked. 15 consecutive blocked calls end the simulation
         * probCounter counts the number of consecutive failures.
         * incomingCallsCounter counts all of the accepted and blocked calls.
         */
        int channels, acceptedCallsCounter = 0, blockedCallsCounter = 0, probCounter = 0, incomingCallsCounter = 0;
        double callDuration, interarrival, totalTime, A, accepted;
        double previousTotalTime, nextArrivalTime = 0, tempNextArrivalTime, sumInterarrivalTimes = 0;
        // Theoretical blocking probability is calculated from Erlang-B
        double theoreticalBlockingProbability;
        double distance = 0, ld = 0, receivingPower = 0, sinr = 0;
        // CQI index
        int cqi = 0;
        // Modulation
        String modulation = "";
        // Bitrate of a call
        double bitrate = 0;

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the number of channels at the server");
        channels = scanner.nextInt();
        System.out.println("Enter the call duration (in seconds)");
        callDuration = scanner.nextDouble();
        System.out.println("Enter interarrival time");
        interarrival = scanner.nextDouble();

        // Kept it from original C++ code
        int noOfOngoingCalls = 0;

        // A = lambda (not wavelength) * H
        A = (1/interarrival) * callDuration;

        // Create channels
        elements = new ArrayList<>(channels);
        for (int i = 0; i < channels ; i++) {
            elements.add(new Element());
        }

        // Time of the simulation.
        totalTime = 0;
        // Time of an event, following the poisson process
        nextArrivalTime += AuxFunctions.poisson(interarrival);

        // Erlang-B
        theoreticalBlockingProbability = AuxFunctions.erlangB(channels, A);
//        theoreticalBlockingProbability = 0;

        
        //Original while condition - kept it to check the correctness of the code
//        while (incomingCallsCounter<50000){

        // Current blocking probability
        double blockingProbability = 0;

        // Calculate the difference
        while (Math.abs(blockingProbability-theoreticalBlockingProbability)*100>0.1 || probCounter <15){
            // Total time
            previousTotalTime = totalTime;
            totalTime = nextArrivalTime;
            // Reset variables
            // Distance of the last user, calculated using euclidean distance
            distance = 0;
            ld = 0;
            receivingPower = 0;
            sinr = 0;
            cqi = 0;
            modulation = "";
            bitrate = 0;
            incomingCallsCounter++;

            /*
             * Change the distances
             * There is a very slight chance that the person won't move.
             * Direction is random
             */

            // For every channel find the occupied ones
            for (Element element: elements) {
                if(element.occupied == 1)
                {
                    // Double produces numbers uniformly in (0,1). With round we can get the direction the user is moving.
                    int direction = (int) Math.round(random.nextDouble());
                    int flag = 0;
                    // 5km/h  is equal to about 1.39 m/sec
                    double meters = 1.39 * (totalTime - previousTotalTime);

                    // Change x direction.
                    if (direction == 0){
                        /*
                         * Calculate distances if user moves closer or further from the center of our cell
                         * (cell in this case is a circle)
                         * Then decide where the user goes.
                         * If both are valid (either closer or further) then where each user moves is random.
                         * If user some how can't move in the x axis then we try the y axis (which is highly unlikely)
                         * Each user is constrained in having a distance of at least 380m away from the center of the cell
                         * and at most 2000m away.
                         */
                        double distancePlus = element.x + meters;
                        double distanceMinus = element.x - meters;
                        if ((distancePlus>= 380 && distancePlus <= 2000) && (distanceMinus>= 380 && distanceMinus <= 2000)){
                            int sign = (int) Math.round(random.nextDouble());
                            if(sign == 0) {
                                element.x = distancePlus;
                            } else {
                                element.x = distanceMinus;
                            }
                        } else if(distancePlus>= 380 && distancePlus <= 2000) {
                            element.x = distancePlus;
                        } else if (distanceMinus>= 380 && distanceMinus <= 2000) {
                            element.x = distanceMinus;
                        } else {
                            flag = 1;
                        }
                    }
                    // Same as before but for the y axis.
                    if(direction == 1 || flag == 1){
                        double distancePlus = element.y + meters;
                        double distanceMinus = element.y - meters;
                        if ((distancePlus>= 380 && distancePlus <= 2000) && (distanceMinus>= 380 && distanceMinus <= 2000)){
                            int sign = (int) Math.round(random.nextDouble());
                            if(sign == 0) {
                                element.y = distancePlus;
                            } else {
                                element.y = distanceMinus;
                            }
                        } else if(distancePlus>= 380 && distancePlus <= 2000) {
                            element.y = distancePlus;
                        } else {
                            element.y = distanceMinus;
                        }
                    }

                }
            }

            //Equal to the Uncheck function in the original C++ code.
            for (Element element:elements) {
                if (element.timeout < totalTime && element.occupied == 1){
                    System.out.println("Call ended at " + element.timeout + " (Arrived at " + element.timein + ")."
                            + " \nFinal distance is: " + Math.sqrt(Math.pow(element.x, 2) + Math.pow(element.y, 2)) );
                    element.timein = 0;
                    element.timeout = 0;
                    element.occupied = 0;
                    element.x = 0;
                    element.y = 0;
                    noOfOngoingCalls--;
                }
            }

            System.out.println("------------Incoming Call---------------");
            // Accepted - equal to check function
            accepted = 0;
            for (Element element: elements) {
                // If a channel is available
                if (element.occupied == 0){
                    element.timein = nextArrivalTime;
                    // Random time of call ending.
                    element.timeout = nextArrivalTime + AuxFunctions.poisson(callDuration);
                    element.occupied = 1;

                    /*
                     * Distance is 0 in the first iteration
                     * Then produce random x, y until 380 < x^2 + y^2 < 2000
                     */

                    while (distance < 380 || distance > 2000) {
                        element.x = 4000 * random.nextDouble() - 2000;
                        element.y = 4000 * random.nextDouble() - 2000;
                        distance = Math.sqrt(Math.pow(element.x, 2) + Math.pow(element.y, 2));
                    }

                    // ld is the path loss in dB
                    ld = Ld0 + 10*Math.log10(Math.pow(distance/d0, n));

                    // Receiving power is the power of the signal (in dB)
                    receivingPower = 10*Math.log10(15) - ld;

                    /*
                     * Since P is in dBW we change it to watts.
                     * SINR = P / (N + I)
                     */
                    sinr = Math.pow(10,receivingPower/10) / ((interPlusNoise));

                    // SINR in dB
                    sinr = 10*Math.log10(sinr);

                    // If current sinr is the largest we've seen
                    if(sinr > maxSinr){
                        maxSinr = sinr;
                    }

                    // Channel Quality Indicator from the table.
                    cqi = AuxFunctions.findCQI(sinr);
                    // Modulation used is derived from the Adaptive Modulation and Coding table
                    modulation = AuxFunctions.findModulation(cqi);
                    // Bitrate is calculated from bits/symbol * symbols/sec
                    bitrate = AuxFunctions.findBitrate(cqi)*symbolPerSec;
                    // If user is out of range
                    if(cqi != -1)
                        stats[cqi]++;
                    noOfOngoingCalls++;
                    accepted = 1;
                    break;
                }
            }

            if(accepted == 1) {
                acceptedCallsCounter++;

                System.out.println("Call "+ acceptedCallsCounter +" accepted at " + totalTime + ". Distance from BS is: " + distance + "m.");
                System.out.println("Pathloss (in dB) is: " + ld);
                System.out.println("Receiving power (in dB) is: " + receivingPower);
                System.out.println("SINR (in dB) is: " + (sinr));
                System.out.println("CQI is equal to " + (cqi + 1) + ".");
                System.out.println( "Used modulation is: " + modulation + " with a bitrate of: " + bitrate + " bits/sec.");
            }
            else {
                blockedCallsCounter++;
                System.out.println("Call blocked at " + totalTime);
            }

            System.out.println("----------------------------------------");
            // Find the next arrival time
            tempNextArrivalTime = nextArrivalTime;
            nextArrivalTime += AuxFunctions.poisson(interarrival);
            sumInterarrivalTimes += nextArrivalTime - tempNextArrivalTime;

            // Calculate the blocking probability necessary to terminate the loop
            blockingProbability = (double) blockedCallsCounter/incomingCallsCounter;

            // The condition must be true for 15 iterations
            if((Math.abs(blockingProbability-theoreticalBlockingProbability)*100)<=0.1){
                probCounter++;
            } else{
                probCounter = 0;
            }
        }

        // Final statistics
        System.out.println("\n--------------------------------------------- \n");
        System.out.println("Τheoretical Blocking Probability = " + theoreticalBlockingProbability);
        System.out.println("Simulation's Blocking Probability = " + blockingProbability);
        System.out.println("Mean Interarrival = " + sumInterarrivalTimes/incomingCallsCounter);
        System.out.println("Total number of incoming calls = " + incomingCallsCounter);
        System.out.println("Accepted calls = " + acceptedCallsCounter);
        System.out.println("Percentage of accepted calls = " + (double) (100*acceptedCallsCounter)/incomingCallsCounter + "%");
        System.out.println("Blocked calls = " + blockedCallsCounter);
        System.out.println("Percentage of blocked calls = " + (double) (100*blockedCallsCounter)/incomingCallsCounter + "%");
        System.out.print("The CQI were: {");
        for (int i = 0; i <15 ; i++) {
            System.out.print(stats[i] + " ");
        }
        System.out.println("}\nMaximum value of SINR was " + maxSinr);
    }
}

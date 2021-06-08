import java.util.Random;

public class AuxFunctions {

    // Recursive Erlang-B
    public static double erlangB(int n, double A){
        if(n == 0){
            return 1;
        }
        double pbt = erlangB(n-1, A);
        return (A*pbt/n)/(1 + (A*pbt/n));
    }

    // Simulates a poisson event
    public static double poisson(double average){
        double delay, randomVariable;
        Random random = new Random();
        // random.nextDouble returns a uniformly distributed number in (0,1)
        randomVariable = random.nextDouble();
        // delay is an exponential random variable with lambda = average
        delay = -1 * average * Math.log(randomVariable);
        return delay;
    }

    // Simple function that matches an SINR to the given CQI
    public static int findCQI(double sinr){
        double[] CQI = {
                -9.478, -6.658, -4.098, -1.798, 0.399, 2.424, 4.489, 6.367, 8.456,
                10.266, 12.218, 14.122, 15.849, 17.786, 19.809
        };
        // if -1 is returned then user was out of range
        int index = -1;
        for (int i = 0; i <14 ; i++) {
            if(sinr >=  CQI[i] && sinr <CQI[i+1]) {
                index = i;
                break;
            }
        }
        if(sinr>CQI[14])
            index = 14;

        return index;
    }

    // Matches a modulation with an index
    public static String findModulation(int index){
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return "QPSK";
            case 6:
            case 7:
            case 8:
                return "16QAM";
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                return "64QAM";
            default:
                return "Error";
        }
    }

    // Matches a bitrate with an index
    public static double findBitrate(int index) {
        switch (index) {
            case 0:
                return 0.1523;
            case 1:
                return 0.2344;
            case 2:
                return 0.3770;
            case 3:
                return 0.6010;
            case 4:
                return 0.8770;
            case 5:
                return 1.1758;
            case 6:
                return 1.4766;
            case 7:
                return 1.9141;
            case 8:
                return 2.4063;
            case 9:
                return 2.7305;
            case 10:
                return 3.3223;
            case 11:
                return 3.9023;
            case 12:
                return 4.5234;
            case 13:
                return 5.1152;
            case 14:
                return 5.5547;
            default:
                return 0;
        }
    }

}

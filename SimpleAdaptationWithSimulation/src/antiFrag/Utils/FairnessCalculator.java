package antiFrag.Utils;

import domain.Mote;
import simulator.QoS;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Float.NaN;

public class FairnessCalculator {
    private double vMinBattery;
    private double vMaxBattery;
    private double vMinQueue;
    private double vMaxQueue;
    private double[] weights = new double[2]; // to apply on batteryRemaining e packetQueue.size()

    //calculate weights and ranges
    private void calculateWeightsAndRanges(List<Mote> motes) {
        double sumBattery = 0.0;
        double sumQueue = 0.0;
        double sumBatterySq = 0.0;
        double sumQueueSq = 0.0;

        vMinBattery = Double.MAX_VALUE;
        vMaxBattery = Double.MIN_VALUE;
        vMinQueue = Double.MAX_VALUE;
        vMaxQueue = Double.MIN_VALUE;

        int n = motes.size();

        for (Mote mote : motes) {
            double batteryRemaining = mote.getBatteryRemaining();
            double queueSize = mote.getQueueSize();

            // variance setup
            sumBattery += batteryRemaining;
            sumQueue += queueSize;
            sumBatterySq += batteryRemaining * batteryRemaining;
            sumQueueSq += queueSize * queueSize;

            // min max
            vMinBattery = Math.min(vMinBattery, batteryRemaining);
            vMaxBattery = Math.max(vMaxBattery, batteryRemaining);
            vMinQueue = Math.min(vMinQueue, queueSize);
            vMaxQueue = Math.max(vMaxQueue, queueSize);
        }

        // get variance
        double varianceBattery = (sumBatterySq / n) - Math.pow(sumBattery / n, 2);
        double varianceQueue = (sumQueueSq / n) - Math.pow(sumQueue / n, 2);

        // get weights
        double totalVariance = varianceBattery + varianceQueue;
        double weightBattery = (totalVariance - varianceBattery) / totalVariance;
        double weightQueue = (totalVariance - varianceQueue) / totalVariance;

        // we do not want min = 0.0 to avoid n/0
        double minWeight = 0.01;
        double maxWeight = 0.99;

        weightBattery = Math.max(minWeight, Math.min(weightBattery, maxWeight));
        weightQueue = Math.max(minWeight, Math.min(weightQueue, maxWeight));

        // normalize weights
        double weightSum = weightBattery + weightQueue;
        weights[0] = weightBattery / weightSum;
        weights[1] = weightQueue / weightSum;
    }


    // formula 3
    private double calculateQoSScore(Mote mote) {
        double batteryNorm = normalize(mote.getBatteryRemaining(), vMinBattery, vMaxBattery);


        double queueNorm = normalize(mote.getQueueSize(), vMinQueue, vMaxQueue);
        return weights[0] * batteryNorm + weights[1] * queueNorm;

    }


    private double normalize(double value, double min, double max) {
        return (max == min) ? 0.0 : (value - min) / (max - min);
    }



    // ζ_t with formula 5
    public double calculateFairnessIndex(List<Mote> motes) {
        if (motes.isEmpty()) return 0.0;


        calculateWeightsAndRanges(motes);

        double sumQoS = 0.0;
        double sumQoSSquared = 0.0;

        for (Mote mote : motes) {
            double score = calculateQoSScore(mote); // Q_t(S) for each Mote
            sumQoS += score;
            sumQoSSquared += score * score;
        }

        int n = motes.size();

        // get di ζ_t

        double res = (sumQoS * sumQoS) / (n * sumQoSSquared);
        if (Double.isNaN(res)){
            return 0;
        }else {

            return res;
        }
    }
}
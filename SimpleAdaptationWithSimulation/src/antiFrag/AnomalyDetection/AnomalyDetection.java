package antiFrag.AnomalyDetection;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class AnomalyDetection {

    /**
     * this class aims to both construct points to identify runs and build the 9-dimensional space
     * to get distances of runTimeData from standardData
     */
    public static class DataPoint {
        private int runId;
        private double[] points;

        public DataPoint(int runId, double[] points) {
            this.runId = runId;
            this.points = points;
        }

        public double[] getPoints() {
            return this.points;
        }

        public int getRunId() {
            return this.runId;
        }
    }


    private static Map<Integer, DataPoint> normalModel = new HashMap<>();
    private static int commonDimension = 9;
    private static String folder;

    public static void main(String[] args) throws IOException{
        // Directory standard files
        String baseDir = "AnomalyDetectionFiles";


        List<File> qosFiles = Files.list(Paths.get(baseDir))
                .filter(p -> p.toString().contains("qos_"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        List<File> stateFiles = Files.list(Paths.get(baseDir))
                .filter(p -> p.toString().contains("state"))
                .map(Path::toFile)
                .collect(Collectors.toList());


        buildNormalModel(qosFiles, stateFiles);




        //System.out.println("sizeNormalMOdel "+normalModel.size());


        // dummy experiment
        double[] newRuntimeData = {0.14, 24.82968, 11874.9, 7, 7, 0.513, 15, 100, 15}; // Dati simulati ricevuti a runtime
        int receivedRunId = 3;  // Supponiamo che abbiamo ricevuto dati per la Run 2



        checkForAnomaly(receivedRunId, newRuntimeData);
    }

    public void init(String baseDir){

        System.out.println("init anomalydetection from folder: " + baseDir);
        folder = baseDir;
        try {
            // = "AnomalyDetectionFiles";

            //qos_X.csv e stateY.txt
            Path dir = Paths.get(baseDir);
            System.out.println("path " + dir);
            List<File> qosFiles = Files.list(dir)
                    .filter(p -> p.toString().contains("qos_"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            List<File> stateFiles = Files.list(dir)
                    .filter(p -> p.toString().contains("state"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());


            buildNormalModel(qosFiles, stateFiles);
        }catch (Exception e){
            System.err.println("exception in init anomalyDetection");
            System.err.println(e.getMessage());
        }

    }

    // normal model constructor
    public static void buildNormalModel(List<File> qosFiles, List<File> stateFiles) throws IOException {
        // Process qos_X.csv
        int count = 0;

        for (File file : qosFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line[0].equals("Run")) continue; // Skip Header

                    int runId = Integer.parseInt(line[0]);
                    if (runId>95){
                        continue;
                    }
                    double packetLoss = Double.parseDouble(line[1]);
                    double energyConsumption = Double.parseDouble(line[2]);
                    double nodesExceedingEnergyUsage = Double.parseDouble(line[3]);
                    double nodesExceedingQueueSpace = Double.parseDouble(line[4]);
                    double fairnessIndex = Double.parseDouble(line[5]);

                    // builfing point with data from csv and txt
                    double[] point = new double[commonDimension];
                    point[0] = packetLoss;
                    point[1] = energyConsumption;
                    point[2] = nodesExceedingEnergyUsage;
                    point[3] = nodesExceedingQueueSpace;
                    point[4] = fairnessIndex;
                    double[] state = getState(stateFiles.get(Math.min(qosFiles.indexOf(file), stateFiles.size()-1)), count, runId+1);
                    point[5] = state[0];
                    point[6] = state[1];
                    point[7] = state[2];
                    point[8] = state[3];
                    normalModel.put(count, new DataPoint(runId, point));
                    count++;
                }
            }
        }
        //System.out.println("count after qos " + count);





        //writeNormalModelToCsv();

    }

    public static double[] getState(File file, int count, int correspondentId) {
        double[] ret = new double[4];
        double totalBattery = 0;
        double averagePower = 0;
        double totalDistribution = 0;
        int moteCount = 0;
        int linkCount = 0;

        // Regex to extract battery e link values
        Pattern batteryPattern = Pattern.compile("battery=([\\d.,]+)/");
        Pattern linkPattern = Pattern.compile("Link \\[.*power=(\\d+), distribution=(\\d+)\\]");

        boolean insideFirstTimestamp = false;
        String separator = "---------------------------------------------------";
        boolean validRun = false;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // get first timestamp
                if (line.startsWith("timestamp:")) {
                    int runId = Integer.parseInt(line.replace("timestamp:", "").trim());
                    // is current timestamp valid (= requested timestamp)
                    validRun = (runId == correspondentId);
                    // if not valid reset counts
                    if (!validRun) {
                        moteCount = 0;
                        linkCount = 0;
                        totalBattery = 0;
                        averagePower = 0;
                        totalDistribution = 0;
                    }
                    continue;
                }

                if (!validRun) {
                    continue; //skip non valid headers
                }

                // stop the count if reading separator
                if (line.equals(separator)) {
                    break;
                }

                // count motes
                if (line.startsWith("Mote")) {
                    moteCount++;
                }

                // get battery values
                Matcher batteryMatcher = batteryPattern.matcher(line);
                if (batteryMatcher.find()) {
                    double battery = Double.parseDouble(batteryMatcher.group(1).replace(',', '.'));
                    totalBattery += battery;
                }

                // get link values
                Matcher linkMatcher = linkPattern.matcher(line);
                while (linkMatcher.find()) {
                    double power = Double.parseDouble(linkMatcher.group(1));
                    double distribution = Double.parseDouble(linkMatcher.group(2));
                    averagePower += power;
                    totalDistribution += distribution;
                    linkCount++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // get actual values
        if (moteCount > 0 && linkCount > 0) {
            ret[0] = totalBattery / moteCount;       // avg battery
            ret[1] = averagePower / linkCount;      // avg pwr
            ret[2] = totalDistribution / linkCount; // avg distribution
            ret[3] = moteCount;                     // moteCount
            //System.out.println("Mote e link count: " + moteCount + ", " + linkCount);
        }

        return ret;
    }

    public static void writeNormalModelToCsv() {
        String folderPath = "AnomalyDetection";
        String filePath = folderPath + "/normalModel.csv";


        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }


        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // header
            String[] header = {"0", "1", "2", "3", "4", "5", "6", "7", "8"};
            writer.writeNext(header);

            // data
            for (Map.Entry<Integer, DataPoint> entry : normalModel.entrySet()) {
                double[] points = entry.getValue().getPoints();
                String[] row = Arrays.stream(points)
                        .mapToObj(Double::toString)
                        .toArray(String[]::new);
                writer.writeNext(row);
            }

            //System.out.println("File scritto con successo: " + filePath);

        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }






    public static boolean checkForAnomaly(int runId, double[] newRuntimeData) { //newRuntimeData = point


        // get distance between runtime and normal data
        double distance = getDistancepvt(runId, newRuntimeData);//distanceCalculator.compute(newRuntimeData, normalData);
        System.out.println("distance from normal data is: " + distance);
        System.out.println("Folder "+folder +" runId" + runId);

        double anomalyThreshold = 20.0;
        if (distance > anomalyThreshold) {
            System.out.println("Anomaly in Run ID " + runId);
            return true;
        } else {
            System.out.println("No anomapy in Run ID " + runId);
            return false;
        }
    }

    public double getDistance(int runId, double[] newRuntimeData) {


        // normal = no anomaly
        return realDistance(runId, newRuntimeData);
    }

    private static double getDistancepvt(int runId, double[] newRuntimeData) {


        // normal = no anomaly
        return realDistance(runId, newRuntimeData);
    }

    private static double realDistance(int runId, double[] newRuntimeData) {
        int stop = normalModel.size();
        stop = Math.min(stop, 9500); //my case limitation
        double distance = Double.MAX_VALUE;
        for(int i = 0; i<stop; i++) {
            DataPoint dataPoint = normalModel.get(i);
            double[] normalData = normalModel.get(i).getPoints();
            if(i<1) {
                System.out.println("timestamp " + runId + " folder " + folder);
                System.out.println("newRunTimeData " + Arrays.toString(newRuntimeData));
                System.out.println("normaldata " + Arrays.toString(normalData));
            }
            if(dataPoint.runId == runId){
                //System.out.println("size normal model: "+normalModel.size());

                EuclideanDistance distanceCalculator = new EuclideanDistance();

                // Calcola la distanza tra i dati runtime e i dati normali

                distance = Math.min(distanceCalculator.compute(newRuntimeData, normalData),distance);
                //System.out.println("distance to be discovered " + Arrays.toString(normalData));
            }
        }
        return distance;
    }


    public static Map<Integer, DataPoint> getNormalModel() {
        return normalModel;
    }

    public static void setNormalModel(Map<Integer, DataPoint> normalModel) {
        AnomalyDetection.normalModel = normalModel;
    }
}

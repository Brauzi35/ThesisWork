package antiFrag.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLister {

    public static List<String> getFileNamesFromDirectory(String directoryPath) {
        List<String> fileNames = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileNames.add(file.getName());
                    }
                }
            }
        } else {
            System.err.println("The specified path is not a directory: " + directoryPath);
        }

        // Ordina i nomi dei file
        Collections.sort(fileNames, (file1, file2) -> {
            if (file1.equals("recoveryFromAnomaly.json")) {
                return -1;
            }
            if (file2.equals("recoveryFromAnomaly.json")) {
                return 1;
            }
            // order other files as: CASE1, CASE2, ..., CASEN
            return file1.compareTo(file2);
        });

        return fileNames;
    }

    public static String addNewFolderToConfig(String configFilePath) {
        Properties properties = new Properties();
        File configFile = new File(configFilePath);

        try (FileReader reader = new FileReader(configFile)) {
            // load config files
            properties.load(reader);
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return null;
        }

        String foldersValue = properties.getProperty("folders", "");
        if (foldersValue.isEmpty()) {
            System.err.println("No folders property found or it is empty.");
            return null;
        }

        // get max number
        String[] folders = foldersValue.split(",");
        int maxNumber = 0;
        Pattern pattern = Pattern.compile("AnomalyDetectionFiles(\\d+)");

        for (String folder : folders) {
            Matcher matcher = pattern.matcher(folder.trim());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                if (number > maxNumber) {
                    maxNumber = number;
                }
            }
        }

        // get new number
        int newFolderNumber = maxNumber + 1;
        String newFolder = "AnomalyDetectionFiles" + newFolderNumber;

        // update config
        foldersValue += "," + newFolder;
        properties.setProperty("folders", foldersValue);

        // write config
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, null);
        } catch (IOException e) {
            System.err.println("Error writing to config file: " + e.getMessage());
            return null;
        }

        // create new folder
        File newFolderDir = new File(newFolder);
        if (!newFolderDir.exists()) {
            if (newFolderDir.mkdirs()) {
                System.out.println("Successfully created directory: " + newFolderDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create directory: " + newFolderDir.getAbsolutePath());
                return null;
            }
        }

        System.out.println("Added new folder: " + newFolder);
        return newFolder;
    }






}

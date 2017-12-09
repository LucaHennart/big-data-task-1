package Main;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class Main {

    /**
     * If you have different path for your emails datasets, enter them here.
     * queryFiles contains a custom set of emails you want to give in query
     * pathDq is a single query email
     */
    private final static String pathDataset = "./emails/1";
    private final static String pathDataset2 = "./emails/2";
    private final static String pathDataset3 = "./emails/3";
    private final static String pathDataset4 = "./emails/4";
    private final static String pathDataset5 = "./emails/5";
    private final static String pathDataset6 = "./emails/6";
    private final static String pathDq = "./dq/dq.txt";
    private final static String queryFiles = "./emails/QueryFiles";

    /**
     * verbose option enables more message display
     * debug option set to true only uses count(pathDataset) emails
     * debug option set to false uses emails from dataset 1 to dataset 5
     */
    private final static boolean debug = true;
    private final static boolean verbose = false;

    public static void main(String[] args) {

        Main main = new Main();
        Main.displayGeneralInformation();
        long startTimer = System.nanoTime();
        StringBuilder totalResultsLog = new StringBuilder();

        try {

            // Part 1
            //main.part1(pathDq, totalResultsLog);

            // Part 2
            /*
            List<HashFunction> hyperplanes = main.constructHyperplanes(4);
            Map<String,Set<String>> result = main.getHyperplanesSubset(hyperplanes);
            main.part2(pathDq, result, hyperplanes, totalResultsLog);
            */

            // Part 3
            //main.part3(queryFiles, 4, totalResultsLog);

            // Part 4
            //main.part4(pathDataset6);

            Main.displayTimeNeeded(startTimer);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* ******************************************* Part 1 Computation *************************************************/

    /**
     * Takes all the words in an email and return a map of the occurences
     * @param email path of the email
     * @return map of the occurences
     */
    public Map<String, Integer> convert(String email) {
        email = email.substring(9);
        String[] words = email.split(" ");
        Map<String, Integer> myMap = new HashMap<>();
        for (String word : words) {
            Integer currentValue = myMap.get(word);
            if(currentValue != null) {
                myMap.put(word, currentValue + 1);
            } else {
                myMap.put(word, 1);
            }
        }
        return myMap;
    }

    /**
     * Compute the distance between two vectors
     * @param vector1 A map of the occurences of every word in an email
     * @param vector2 A map of the occurences of every word in an email
     * @return the distance between the two vectors
     */
    private double computeDistance(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double distance = 0;
        if(vector1 != null && vector2 != null) {
            Integer finalDistance = 0;
            Integer normVector1 = 0;
            Integer normVector2 = 0;

            Set<String> vector1Keys = vector1.keySet();
            Set<String> vector2Keys = vector2.keySet();
            Integer vector1Value;
            Integer vector2Value;
            for(String key : vector1Keys) {
                vector1Value = vector1.get(key);
                if(vector2.containsKey(key)) {
                    finalDistance +=  vector1Value * vector2.get(key);
                }
                normVector1 += vector1Value * vector1Value;
            }
            for(String key : vector2Keys) {
                vector2Value = vector2.get(key);
                normVector2 += vector2Value * vector2Value ;
            }
            distance = finalDistance / (Math.sqrt(normVector1) * Math.sqrt(normVector2));
            distance = Math.acos(distance > 1 ? 1 : distance);
        } else {
            System.err.println("One vector is unitialized");
        }

        return distance;
    }

    /**
     * Compute the minimal distance between a vector query email and a list of email vectors
     * @param listvectors The vectors in the datasets
     * @param testedVector The query vector
     * @param log Information about the message and the distance will be stored there
     * @return The index of the email in the dataset with the minimal distance
     */
    private int computeMinDistance(List<Map<String, Integer>> listvectors, Map<String, Integer> testedVector,
                                   StringBuilder log) {
        double distance = Double.MAX_VALUE;
        int index = 0;
        int minIndex = Integer.MAX_VALUE;
        for(Map<String, Integer> vector : listvectors) {
            double tmpDistance = computeDistance(testedVector, vector);
            if(tmpDistance < distance) {
                distance = tmpDistance;
                minIndex = index;
            }
            index++;
        }
        log.append(distance);
        if(verbose) System.out.println("Minimum Distance is : " + distance);
        return minIndex;
    }


    /* ******************************************* Part 2 Computation *************************************************/

    /**
     * Checks if an email is under of above an hyperplane
     * @param h The hash function representing the hyperplane
     * @param email The tested email
     * @return False means under and True above
     */
    private boolean aboveUnderHyperplane(HashFunction h, String email) {
        BigInteger result = new BigInteger("0");
        email = email.substring(9);
        String[] words = email.split(" ");
        for (String word : words) {
            long index = (long)word.hashCode();
            long outcome = h.hashLong(index).asInt();

            result = result.add(BigInteger.valueOf(index).multiply(BigInteger.valueOf(outcome)));

        }

        return result.compareTo(BigInteger.valueOf(0)) >= 0;
    }

    /**
     * Add an email to the corresponding subset of emails
     * @param collection The subset collection
     * @param hyperplanes The hyperplanes creating the signature for the subsets
     * @param email The email to be placed in the subset collection
     * @throws IOException File issues
     */
    private void updateSubsetMessages(Map<String, Set<String>> collection, List<HashFunction> hyperplanes, File email)
            throws IOException{

        StringBuilder signature = new StringBuilder();

        String emailContent = Files.toString(email, Charsets.UTF_8);
        String emailPath = email.getAbsolutePath();
        for(HashFunction f : hyperplanes) {

            if(this.aboveUnderHyperplane(f, emailContent)) {
                signature.append(0);
            } else {
                signature.append(1);
            }
        }

        String strSignature = signature.toString();

        if(!collection.containsKey(strSignature)) {
            Set<String> tmpSet = new HashSet<>();
            tmpSet.add(emailPath);
            collection.put(strSignature, tmpSet);
        } else {
            collection.get(strSignature).add(emailPath);
        }
    }

    /**
     * Get the query email signature
     * @param hyperplanes The hyperplanes creating the signature for the subsets
     * @param emailContent The content of the email
     * @return The generated signature for the query email
     */
    private String getEmailSignature(List<HashFunction> hyperplanes, String emailContent) {

        StringBuilder signature = new StringBuilder();

        for(HashFunction f : hyperplanes) {
            if(this.aboveUnderHyperplane(f, emailContent)) {
                signature.append(0);
            } else {
                signature.append(1);
            }
        }
        return signature.toString();
    }

    /**
     * Construct numberHyperplanes hyperplanes giving a seed
     * @param numberHyperplanes How many hyperplanes you want to create. Must be a power of 2.
     * @param seed The generating seed for predictable results
     * @return A list of the generated murmur3 hash functions representing the hyperplanes
     */
    private List<HashFunction> constructHyperplanes(int numberHyperplanes, int seed) {
        return this.generatePredictableHashesFunction(numberHyperplanes, seed);
    }

    /**
     * Get the full subset collection of emails in the datasets
     * @param hyperplanes The hyperplanes created to generate the subsets
     * @return The full subset collection of emails
     */
    private Map<String,Set<String>> getHyperplanesSubset(List<HashFunction> hyperplanes) {

        Map<String, Set<String>> result = new HashMap<>();
        List<List<File>> files;
        try {

            if (debug) {
                files = new ArrayList<>();
                files.add(Main.getEmailsContent(pathDataset));
            } else
                files = Main.getAllFiles();

            for (List<File> folder : files) {
                for (File file : folder) {
                    this.updateSubsetMessages(result, hyperplanes, file);
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /* ***************************************** File handling functions **********************************************/

    /**
     * Get the List of File of a dataset folder
     * @param pathToFolder The path to the folder
     * @return the List of File
     * @throws Exception If the folder didn't exist
     */
    private static List<File> getEmailsContent(String pathToFolder) throws Exception {
        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        List<File> filesArray;

        if(files != null) {
            filesArray = Arrays.asList(files);
        } else {
            throw new Exception("No File Found");
        }

        return filesArray;
    }

    /**
     * Get the content of an email from its path, encoded in UTF-8
     * @param pathToFile The path to the file
     * @return The content of the email
     * @throws IOException If the file did not exist
     */
    private static String getEmailContent(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        return Files.toString(file, Charsets.UTF_8);
    }

    /* ******************************************* Utilitary functions ************************************************/

    /**
     * Construct numberHyperplanes hyperplanes giving a seed
     * @param numberHyperplanes How many hyperplanes you want to create. Must be a power of 2.
     * @param seed The generating seed for predictable results
     * @return A list of the generated murmur3 hash functions representing the hyperplanes
     */
    private List<HashFunction> generatePredictableHashesFunction(int numberHyperplanes, int seed) {
        List<HashFunction> hyperplanes = new ArrayList<>();
        for(int i = 1; i <= numberHyperplanes; i++) {
            hyperplanes.add(Hashing.murmur3_128((((Integer)(seed * i))).hashCode()));
        }

        return hyperplanes;
    }

    /**
     * Get all the files in all the datasets
     * @return A list of folder files, grouped by folder
     * @throws Exception If a folder was not found
     */
    private static List<List<File>> getAllFiles() throws Exception {
        List<List<File>> allFiles = new ArrayList<>();

        allFiles.add(Main.getEmailsContent(pathDataset));
        allFiles.add(Main.getEmailsContent(pathDataset2));
        allFiles.add(Main.getEmailsContent(pathDataset3));
        allFiles.add(Main.getEmailsContent(pathDataset4));
        allFiles.add(Main.getEmailsContent(pathDataset5));
        
        return allFiles;
    }

    /**
     * Displays general execution information
     */
    private static void displayGeneralInformation() {
        System.err.println("[INFO] Heap Size = " + (Runtime.getRuntime().totalMemory() / 1000) + " MB");
        //System.err.println("[INFO] Available Processors = " + Runtime.getRuntime().availableProcessors());
    }

    /**
     * Displays the time needed to execute something from the last date given in parameters. Unit given in ms.
     * @param startTime Last time
     */
    private static void displayTimeNeeded(long startTime) {
        System.err.println("[INFO] Time needed = " + ((System.nanoTime() - startTime) / 1000000)+ " ms");
    }

    /**
     * Calculates the average error from the exact distance and the approximate distance of numberFiles files
     * @param exactDistance The exact distance
     * @param approximateDistance The approximate distance
     * @param numberFiles The number of files used for these sums of distances
     * @return The average error, from 0 to 1.
     */
    private double calculateAverageError(double exactDistance, double approximateDistance, int numberFiles) {
        return (approximateDistance - exactDistance) / numberFiles;
    }


    /* ********************************************* Part Functions ***************************************************/

    /**
     * Execute Part 1 task
     * @param pathDq The path of the query message
     * @param resultsLog Logs of the results
     */
    public void part1(String pathDq, StringBuilder resultsLog) {
        try {
            List<List<File>> files = null;

            if(debug) {
                files = new ArrayList<>();
                files.add(Main.getEmailsContent(pathDataset));
            }
            else
                files = Main.getAllFiles();

            List<Map<String, Integer>> datasets = new ArrayList<>();
            List<List<String>> listFileNames = new ArrayList<>();

            Map<String, Integer> dqMail = null;

            dqMail = this.convert(Main.getEmailContent(pathDq));

            List<Integer> folderSizes = new ArrayList<>();
            for(List<File> folder : files) {

                int numberFiles = 0;
                List<String> listFolderFilesNames = new ArrayList<>();
                for(File file : folder) {

                    String fileContent = Files.toString(file, Charsets.UTF_8);
                    listFolderFilesNames.add(file.getAbsolutePath());
                    datasets.add(this.convert(fileContent));
                    numberFiles++;
                }
                folderSizes.add(numberFiles);
                listFileNames.add(listFolderFilesNames);
            }

            int neighbourMessageIndex = (this.computeMinDistance(datasets, dqMail, resultsLog));

            for(int it = 0; it < folderSizes.size() ; it++) {

                if((neighbourMessageIndex - folderSizes.get(it)) <= 0 ) {
                    String filePathResult = listFileNames.get(it).get(neighbourMessageIndex);
                    resultsLog.append(",").append("\n");
                    if(verbose) System.out.println("Minimum Distance message is : \n" + filePathResult);
                    break;
                } else {
                    neighbourMessageIndex -= folderSizes.get(it);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes part 2 task
     * @param pathDq The path of the query email
     * @param result The subsets of dataset email
     * @param hyperplanes The random hyperplanes
     * @param resultsLog Logs of the results
     * @throws IOException If some file was not to be found
     */
    public void part2(String pathDq, Map<String,Set<String>> result, List<HashFunction> hyperplanes,
                      StringBuilder resultsLog) throws IOException {
            try {

            String dqMail = Main.getEmailContent(pathDq);
            String dqSignature = this.getEmailSignature(hyperplanes, dqMail);

            if(verbose) System.out.println("Dq signature is : " + dqSignature);
            List<String> listFileNames = new ArrayList<>();
            List<Map<String, Integer>> datasets = new ArrayList<>();


            if(result.containsKey(dqSignature)) {
                for(String similarEmailPath : result.get(dqSignature)) {
                    String fileContent = Files.toString(new File(similarEmailPath), Charsets.UTF_8);
                    datasets.add(this.convert(fileContent));
                    listFileNames.add(similarEmailPath);

                    if(verbose) System.out.println(similarEmailPath);
                }

                int neighbourMessageIndex = (this.computeMinDistance(datasets, this.convert(dqMail), resultsLog));
                String finalPathResult = listFileNames.get(neighbourMessageIndex);

                resultsLog.append(",").append(finalPathResult).append("\n");

                if(verbose) System.out.println("Final result : "  + finalPathResult);
            } else {
                if(verbose) System.out.println("No Subset With Same Signature. Adding PI/2... ");
                resultsLog.append(Math.PI/2).append(",").append("No").append("\n");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes part 3 task
     * @param pathQueries A path to the folder containing all the queries (usually queryFiles)
     * @param numberHyperplanes The number of hyperplanes you want to generate
     * @param resultLog Logs of the results
     * @throws Exception If some file was not to be found
     */
    public void part3(String pathQueries, int numberHyperplanes, StringBuilder resultLog) throws Exception {

        long startTimer = System.nanoTime();

        List<File> listQueries = getEmailsContent(pathQueries);
        for(File query : listQueries) {
            this.part1(query.getAbsolutePath(), resultLog);
        }

        double totalDistance = 0;
        for(String distance : resultLog.toString().split(",\n")) {
            totalDistance += Double.parseDouble(distance);
        }
        Main.displayTimeNeeded(startTimer);
        System.err.println("[INFO] Total Exact Distance : " + totalDistance);

        while(numberHyperplanes >= 1) {

            System.err.println("[INFO] Results for " + numberHyperplanes + " hyperplanes");
            StringBuilder approximateResultsLog = new StringBuilder();
            List<HashFunction> hyperplanes = this.constructHyperplanes(numberHyperplanes, 365836470);
            Map<String,Set<String>> result = this.getHyperplanesSubset(hyperplanes);
            startTimer = System.nanoTime();
            for(File query : listQueries) {
                this.part2(query.getAbsolutePath(), result, hyperplanes, approximateResultsLog);
                if(verbose) System.out.println(approximateResultsLog);
            }

            double totalAppDistance = 0;

            for(String pairs : approximateResultsLog.toString().split("\n")) {
                String[] pair = pairs.split(",");
                totalAppDistance += Double.parseDouble(pair[0]);
            }



            Main.displayTimeNeeded(startTimer);
            System.err.println("[INFO] Total Approximate Distance with "+ numberHyperplanes + " hyperplanes : " + totalAppDistance);
            System.err.println("[INFO] Error with  "+ numberHyperplanes + " hyperplanes is : " + this.calculateAverageError(totalDistance, totalAppDistance, 100) + " %");

            if (numberHyperplanes == 1)
                numberHyperplanes = 0;
            else
                numberHyperplanes /= 2;

        }

    }

    /**
     * Executes part 4 task
     * @param pathQueries A path to the folder containing all the queries (usually pathDataset6)
     * @throws Exception If some file was not to be found
     */
    public void part4(String pathQueries) throws Exception {
        long startTimer = System.nanoTime();
        System.err.println("[INFO] Starting to build hyperplanes indexes...");
        List<File> listQueries = getEmailsContent(pathQueries);
        List<HashFunction> hyperplanes1 = this.constructHyperplanes(16, 264738391);
        List<HashFunction> hyperplanes2 = this.constructHyperplanes(16, 395847264);
        List<HashFunction> hyperplanes3 = this.constructHyperplanes(16, 950284883);
        List<HashFunction> hyperplanes4 = this.constructHyperplanes(16, 673753848);
        Main.displayTimeNeeded(startTimer);

        startTimer = System.nanoTime();
        System.err.println("[INFO] Starting to fulfill hyperplanes indexes...");
        Map<String,Set<String>> result1 = this.getHyperplanesSubset(hyperplanes1);
        Map<String,Set<String>> result2 = this.getHyperplanesSubset(hyperplanes2);
        Map<String,Set<String>> result3 = this.getHyperplanesSubset(hyperplanes3);
        Map<String,Set<String>> result4 = this.getHyperplanesSubset(hyperplanes4);
        Main.displayTimeNeeded(startTimer);

        // Spam As Genuine = Tagged as genuine but it actually was a spam message
        int spamAsSpam = 0;
        int genuineAsGenuine = 0;
        int spamAsGenuine = 0;
        int genuineAsSpam = 0;


        for(File query : listQueries) {
            startTimer = System.nanoTime();

            String path = query.getAbsolutePath();
            boolean isSpam = path.contains("spam");
            StringBuilder resultLog = new StringBuilder();
            double distance = Double.MAX_VALUE;
            String pathFile = "";

            this.part2(path, result1, hyperplanes1, resultLog);
            this.part2(path, result2, hyperplanes2, resultLog);
            this.part2(path, result3, hyperplanes3, resultLog);
            this.part2(path, result4, hyperplanes4, resultLog);

            for(String pairs : resultLog.toString().split("\n")) {

                String[] pair = pairs.split(",");

                double tmpDistance = Double.parseDouble(pair[0]);
                if(tmpDistance < distance) {
                    distance = tmpDistance;
                    pathFile = pair[1];
                }
            }
            boolean isSpamDetected = pathFile.contains("spam");

            if(isSpam) {
                if(isSpamDetected)
                    spamAsSpam++;
                else
                    spamAsGenuine++;
            } else {
                if(isSpamDetected)
                    genuineAsSpam++;
                else
                    genuineAsGenuine++;
            }

            if(verbose) System.out.println(resultLog);
            Main.displayTimeNeeded(startTimer);
        }

        int sumQueries = spamAsGenuine + spamAsSpam + genuineAsGenuine + genuineAsSpam;
        System.err.println("[INFO] 1-nearest Strategy Results - If no messages found, then tagged as Genuine :");
        System.err.println("------      Spam tagged as spam : " + ((double)spamAsSpam / sumQueries));
        System.err.println("------      Spam tagged as genuine : " + ((double)spamAsGenuine / sumQueries));
        System.err.println("------      Genuine tagged as genuine : " + ((double)genuineAsGenuine / sumQueries));
        System.err.println("------      Genuine tagged as spam : " + ((double)genuineAsSpam / sumQueries));

    }

}

package com.cnmpdemo.core;


import com.cnmpdemo.logging.CNMPDemoLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;

/**
 * Created by kiranya on 7/4/18.
 */

public class CNMPDemo {

    /********************************************************************************************
    * - Bootstrap CNMPDemo i.e CNMPDemoPatternChecker..                                         *
    * - One CNMPDemoPatternChecker, CNMPDemoSellChecker instance for every Price stream..       *
    *                                                                                           *
    * - CNMPDemoPatternChecker will actually assess the patterns and decide:                    *
    *   - Entry strategy                                                                        *
    *   - Stay in market strategy (with help from CNMPDemoSellChecker)                          *
    *   - Exit strategy (with help from CNMPDemoSellChecker)                                    *                                                                                          *
    * - CNMPDemoSellChecker will have the logic to decide the exit strategy                     *
    ********************************************************************************************/


    /********************************************************************************************
     *                                                                                           *
     * Jar based execution..                                                                     *
     *                                                                                           *
     * 1 - Common components are Logger and Properties                                           *
     *                                                                                           *
     ********************************************************************************************/

    public static CNMPDemoLogger cnmpLogger = new CNMPDemoLogger();
    public static Properties cnmpProperties = new Properties();
    public static String BTC_NAME = "BTC";


    // MongoDB connections..
    public static String mongoDBConnString = "mongodb://35.200.24.49:27017";
    public static MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoDBConnString));
    public static String mongoDBName = "trades";
    public static MongoDatabase database;
    public static MongoCollection<Document> dbCollection;
    public static final String collectionName = "15sec-Aggregates";

    // Initialise worker thread pool..
    private static List<Thread> workerThreads = new ArrayList<>();


    /********************************************************************************************
     *                                                                                           *
     * Locate and read App Properties..                                                          *
     *                                                                                           *
     ********************************************************************************************/
    {
        try {
            this.getCnmpProperties().load(getClass().getResourceAsStream("/META-INF/cnmp.properties"));
            //System.out.println("Property value is --> "+ cnmpProperties.getProperty("slidingWindowSize"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Issues reading properties file!!");
            throw new RuntimeException("Unable tor read properties file!!");
        }
    }


    /********************************************************************************************
     *                                                                                           *
     * Instantiate one thread per Price Stream..                                                 *
     *                                                                                           *
     ********************************************************************************************/

    public static void main(String args[]) throws Exception {

        try {
            CNMPDemo.cnmpLogger.setup();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Issues setting up the logger!!");
            throw new RuntimeException("Problems with creating the log files");
        }

        try {

            database = mongoClient.getDatabase(mongoDBName);
            dbCollection = database.getCollection(collectionName);
            MongoIterable <String> collections = database.listCollectionNames();
            for (String coinName: collections) {
                if (coinName.endsWith(BTC_NAME)) {
                    CNMPDemoPatternChecker patternChecker = new CNMPDemoPatternChecker(coinName);
                    CNMPDemo.cnmpLogger.logger.info(coinName + " - bootstrap..");
                    Thread patternCheckerThread = new Thread(patternChecker);
                    patternCheckerThread.start();
                    workerThreads.add(patternCheckerThread);
                }
            }

            for (Thread thread : workerThreads) {
                thread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mongoClient.close();
        }

        /*********************************************************************************************
         * 1 - For test purposes using Filestreaming......                                           *
         ********************************************************************************************/
        /*
        for(String coinName: args) {
            CNMPDemoPatternChecker patternChecker = new CNMPDemoPatternChecker(coinName,coinName+"_prices.txt");
            CNMPDemo.cnmpLogger.logger.info(coinName + " - bootstrap..");
            Thread patternCheckerThread = new Thread(patternChecker);
            patternCheckerThread.start();
        }
        */
    }


    public Properties getCnmpProperties() {
        return cnmpProperties;
    }

    public void setCnmpProperties(Properties cnmpProperties) {
        this.cnmpProperties = cnmpProperties;
    }

}


package com.cnmpdemo.core;

import com.cnmpdemo.logging.CNMPDemoLogger;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.bson.Document;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Created by kiranya on 7/4/18.
 */
public class CNMPDemoPatternChecker extends CNMPDemo implements Runnable {

    // Coin Id/Name is a reference for the respective Price Stream
    private String coinName;

    // Format for decimals.. Needs updating..
    private static final DecimalFormat df = new DecimalFormat("#.##");

    // Can be intelligent to set a dynamic window size ex: per coin (then make it instance level..)
    private static final int slidingWindowSize = 4;
    private static final int regressionWindowSize = 4;
    private static final int mongoWindowSize = 4;
    private static final int relativeDownTolerance = 4;
    private static final int ticksBeforeEntry = 4;
    private static MongoCollection<Document> dbCollection;

    // Thread specific query
    private BasicDBObject query = new BasicDBObject();

    // Each price in the Sliding Window can be for example - 15sec or 30sec average..
    private double[][] slidingWindow = new double [slidingWindowSize][2];

    // Hold slope values..
    private double[] regressionAggregates = new double [regressionWindowSize];
    private double[] mongoAggregates = new double [mongoWindowSize];
    private String[] mongoAggStrings = new String [mongoWindowSize];


    // Can be set not to compute intercept
    private SimpleRegression simpleRegression = new SimpleRegression(true);

    // This will be mod of Sliding Window size
    private int streamCounter = -1;

    // This will be mod Regression Window size
    private int regressionCounter = -1;

    // This will be mod Mongo Window size
    private int mongoCounter = -1;

    // Will be set by CalmnessChecker - Basically a Thread to indicate an entry price..
    private boolean calm = false;
    private double buyPrice;

    // Used as Trend Indicator.. Set when the coin is in Uptrend and reset when the coin is in DownTrend..
    private boolean isInUpTrend = false;

    // To indicate if a coin is purchased and we're in trade..Set after purchasing and Reset after selling..
    private boolean inTrade = false;

    // Up and Down counters are only tracked when we're in action..i.e Action Lifecycle is from "Determining to Buy till Exit.."
    private int downCounter;
    private int upCounter;

    // Will hold what ever the price was at the time of exit trigger..
    private double exitPrice;

    // Will be set by SellChecker - Basically a Thread to indicate an exit price..
    private boolean sellIndicator = false;

    // Yet to be used..
    private double stopLossPrice;

    public void run()
    {
        CNMPDemo.cnmpLogger.logger.info(this.coinName + " - Pattern checking for entry and once entered monitor till exit!!");


        /*********************************************************************************************
         * 1 - For test purposes using Filestreaming......                                           *
         ********************************************************************************************/
        /*
        // Stream prices
        try (Stream<String> stream = Files.lines(Paths.get(this.getPriceStreamName()))) {

            //stream.forEach(System.out::println);

            stream.forEach(this::compute);


        } catch (Exception e) {
            e.printStackTrace();
        }
        */


        try {

            dbCollection = database.getCollection(collectionName);

            while (true) {

                mongoCounter = ++mongoCounter;
                mongoCounter = mongoCounter % mongoWindowSize;

                //insert logic to read the value of the coin pair;
                mongoAggregates[mongoCounter] = dbCollection.find(query).first().getDouble("high");
                mongoAggStrings[mongoCounter] = dbCollection.find(query).first().getString("_id");

                if (mongoCounter == mongoWindowSize - 1) {
                        if ((mongoAggregates[0] >= 0 && mongoAggregates[1] >= 0 && mongoAggregates[2] >= 0 && mongoAggregates[3] >= 0)) {
                            if (
                                    ((mongoAggregates[0] <= mongoAggregates[1]) && (mongoAggregates[1] <= mongoAggregates[2]) && (mongoAggregates[2] <= mongoAggregates[3]))
                                            || ((mongoAggregates[0] > mongoAggregates[1]) && (mongoAggregates[0] <= mongoAggregates[2]) && (mongoAggregates[2] <= mongoAggregates[3]))
                                    ) {
                                CNMPDemo.cnmpLogger.logger.info(this.coinName + " is in Uptrend " + Arrays.toString(mongoAggregates) + Arrays.toString(mongoAggStrings));
                                this.setInUpTrend(true);
                                int tempUpCounter = this.getUpCounter();
                                this.setUpCounter(++tempUpCounter);
                                if (this.getUpCounter() >= ticksBeforeEntry && this.isInTrade() == false) {
                                    CNMPDemo.cnmpLogger.logger.info(this.coinName + " can now be bought..");
                                    this.setInTrade(true);
                                }
                            }
                             else {
                                CNMPDemo.cnmpLogger.logger.info(this.coinName + " seeing downticks..");
                                if (isInUpTrend()) {
                                    int tempDownCounter = this.getDownCounter();
                                    this.setDownCounter(++tempDownCounter);
                                    CNMPDemo.cnmpLogger.logger.info(this.coinName + " was in uptrend but seeing downticks..");
                                    if (this.getDownCounter() >= this.getUpCounter() / relativeDownTolerance) {
                                        /****************************
                                         * 1 - Reset the counters    *
                                         * 2 - Reset the trends      *
                                         ****************************/
                                        this.setInUpTrend(false);
                                        this.setUpCounter(0);
                                        this.setDownCounter(0);
                                        if (this.isInTrade() == true) {
                                            CNMPDemo.cnmpLogger.logger.info(this.coinName + " can be exited..");
                                            this.setInTrade(false);
                                        }
                                    }
                                } else if (!isInUpTrend()) {
                                    CNMPDemo.cnmpLogger.logger.info(this.coinName + " is in downtrend..");
                                    this.setUpCounter(0);
                                    this.setDownCounter(0);
                                }
                            }

                        }
                }
                Thread.sleep(15000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void computeUsingRegression(String price) {
        this.streamCounter = ++this.streamCounter;

        this.slidingWindow[streamCounter][0]=streamCounter;
        this.slidingWindow[streamCounter][1]=Double.parseDouble(price);

        streamCounter = streamCounter%slidingWindowSize;

        if (streamCounter == slidingWindowSize-1) {

            // Reset the stream counter
            streamCounter = -1;


            regressionCounter = ++regressionCounter;
            regressionCounter = regressionCounter%regressionWindowSize;
            regressionAggregates[regressionCounter] = this.computeSimpleRegression(this.getSlidingWindow());

            if (regressionCounter == regressionWindowSize-1) {

                CNMPDemo.cnmpLogger.logger.info(this.coinName + " - regressionAggregates --> "+ Arrays.toString(this.regressionAggregates));

                /***********************************
                 * 1 - Reset the stream counter    *
                 * 2 - Reset the regression        *
                 * **********************************/

                // Reset the regression counter
                regressionCounter = -1;

                // Reset the regression
                simpleRegression.clear();

                /*******************************************************************************************
                 * 1 - Regression of regressions (Not in use)......                                         *
                 ********************************************************************************************/

                /*
                double slidingWindowRegression[][] = {
                        {0, regressionAggregates[0]},
                        {1, regressionAggregates[1]},
                        {2, regressionAggregates[2]},
                        {3, regressionAggregates[3]}
                };

                CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " - Sliding window regression --> "+ this.computeSimpleRegression(slidingWindowRegression));

                // Reset the regression
                simpleRegression.clear();

                */

                /*******************************************************************************************
                 * 1 - Logic to determine and monitor trends                                               *
                 * 2 - Based on the trend:                                                                 *
                 *       For an Up Trending coin, do the following..                                       *
                 *          a - Keep incrementing upCounter for every uptick                               *
                 *          b - Generate buy signal when up ticks is beyond tolerance                      *
                 *          c - Keep incrementing downCounter for every downtick                           *
                 *          d - If num of down ticks is beyond tolerance then:                             *
                 *              1 - Reset the Trend and Counter                                            *
                 *              2 - Generate sell signals                                                  *
                 *       For an Coin that's not in Up Trend, do the following..                            *
                 *          a - Generate sell signals when number of downTicks > Threshold                 *
                 *          b - Just reset the Up and Down counters                                        *
                 *          c - Up and Down counters are of significance only when a coin is in UpTrend    *
                 * *****************************************************************************************/

                if ((regressionAggregates[0] >= 0 && regressionAggregates[1] >= 0 && regressionAggregates[2] >= 0 && regressionAggregates[3] >= 0)) {
                    if  (
                            ((regressionAggregates[0] <= regressionAggregates[1]) &&  (regressionAggregates[1] <= regressionAggregates[2]) &&  (regressionAggregates[2] <= regressionAggregates[3]))
                                    || ((regressionAggregates[0] > regressionAggregates[1]) &&  (regressionAggregates[0] <= regressionAggregates[2]) &&  (regressionAggregates[2] <= regressionAggregates[3]))
                            ) {
                        CNMPDemo.cnmpLogger.logger.info(this.coinName + " is in Uptrend");
                        this.setInUpTrend(true);
                        int tempUpCounter = this.getUpCounter();
                        this.setUpCounter(++tempUpCounter);
                        if (this.getUpCounter() >= ticksBeforeEntry && this.isInTrade() == false) {
                            CNMPDemo.cnmpLogger.logger.info(this.coinName + " can now be bought..");
                            this.setInTrade(true);
                        }
                    }
                } else {
                    if(isInUpTrend()) {
                        int tempUpCounter = this.getUpCounter();
                        this.setDownCounter(++tempUpCounter);
                        if(this.getDownCounter() >= tempUpCounter/relativeDownTolerance) {
                            /****************************
                             * 1 - Reset the counters    *
                             * 2 - Reset the trends      *
                             ****************************/
                            this.setInUpTrend(false);
                            this.setUpCounter(0);
                            this.setDownCounter(0);
                            if (this.isInTrade() == true) {
                                CNMPDemo.cnmpLogger.logger.info(this.coinName + " can be exited..");
                                this.setInTrade(false);
                            }
                        }
                    } else if(!isInUpTrend()) {
                        this.setUpCounter(0);
                        this.setDownCounter(0);
                    }
                }
            }

        }
    }


    public double computeSimpleRegression(double[][] data) {

        simpleRegression.addData(data);

        /*
        CNMPDemo.cnmpLogger.logger.finest(this.getCoinName() +
                        " For " + Arrays.deepToString(data) + " -->"
                        +  " slope = " + Double.valueOf(df.format(simpleRegression.getSlope()))
                        + ", intercept = " + Double.valueOf(df.format(simpleRegression.getIntercept()))
                        );
        */
        return Double.parseDouble(df.format(simpleRegression.getSlope()));
    }


    public CNMPDemoPatternChecker(String coinName) {
        this.coinName = coinName;
        query.put("_id", coinName);
    }


    public double[][] getSlidingWindow() {
        return slidingWindow;
    }


    public void setSlidingWindow(double[][] slidingWindow) {
        this.slidingWindow = slidingWindow;
    }

       /*
    Getters and Setters from here on..
     */

    public boolean isSellIndicator() {
        return sellIndicator;
    }

    public void setSellIndicator(boolean sellIndicator) {
        this.sellIndicator = sellIndicator;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public boolean isCalm() {
        return calm;
    }

    public void setCalm(boolean calm) {
        this.calm = calm;
    }

    public double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(float stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public boolean isInUpTrend() {
        return isInUpTrend;
    }

    public void setInUpTrend(boolean inUpTrend) {
        isInUpTrend = inUpTrend;
    }

    public int getDownCounter() {
        return downCounter;
    }

    public void setDownCounter(int downCounter) {
        this.downCounter = downCounter;
    }

    public int getUpCounter() {
        return upCounter;
    }

    public void setUpCounter(int upCounter) {
        this.upCounter = upCounter;
    }

    public static CNMPDemoLogger getCnmpLogger() {
        return cnmpLogger;
    }

    public static void setCnmpLogger(CNMPDemoLogger cnmpLogger) {
        CNMPDemo.cnmpLogger = cnmpLogger;
    }

    public boolean isInTrade() {
        return inTrade;
    }

    public void setInTrade(boolean inTrade) {
        this.inTrade = inTrade;
    }





}

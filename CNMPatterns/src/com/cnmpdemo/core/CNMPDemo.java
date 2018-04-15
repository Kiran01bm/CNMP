package com.cnmpdemo.core;


import com.cnmpdemo.logging.CNMPDemoLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

        for(String coinName: args) {
            CNMPDemoPatternChecker patternChecker = new CNMPDemoPatternChecker(coinName,coinName+"_prices.txt");
            CNMPDemo.cnmpLogger.logger.info(coinName + " - bootstrap..");
            Thread patternCheckerThread = new Thread(patternChecker);
            patternCheckerThread.start();
        }
    }

    // Will be set by CalmnessChecker - Basically a Thread to indicate an entry price..
    private boolean calm = false;
    private double buyPrice;

    // Coin Id/Name is a reference for the respective Price Stream
    private String coinName;
    private String priceStreamName;

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

    public String getCoinName() {
        return coinName;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
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


    public Properties getCnmpProperties() {
        return cnmpProperties;
    }

    public void setCnmpProperties(Properties cnmpProperties) {
        this.cnmpProperties = cnmpProperties;
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

    public String getPriceStreamName() {
        return priceStreamName;
    }

    public void setPriceStreamName(String priceStreamName) {
        this.priceStreamName = priceStreamName;
    }

}


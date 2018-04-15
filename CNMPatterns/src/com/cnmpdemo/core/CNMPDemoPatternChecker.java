package com.cnmpdemo.core;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by kiranya on 7/4/18.
 */
public class CNMPDemoPatternChecker extends CNMPDemo implements Runnable {

    // Format for decimals.. Needs updating..
    private static final DecimalFormat df = new DecimalFormat("#.##");

    // Can be intelligent to set a dynamic window size ex: per coin (then make it instance level..)
    private static final int slidingWindowSize = 4;
    private static final int regressionWindowSize = 4;
    private static final int relativeDownTolerance = 4;
    private static final int ticksBeforeEntry = 2;

    // Each price in the Sliding Window can be for example - 15sec or 30sec average..
    private double[][] slidingWindow = new double [slidingWindowSize][2];

    // Hold slope values..
    private double[] regressionAggregates = new double [regressionWindowSize];

    // Can be set not to compute intercept
    private SimpleRegression simpleRegression = new SimpleRegression(true);

    // This will be mod of Sliding Window size
    private int streamCounter = -1;

    // This will be mod Regression Window size
    private int regressionCounter = -1;

    public void run()
    {
        CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " - Pattern checking for entry and once entered monitor till exit!!");

        // Stream prices
        try (Stream<String> stream = Files.lines(Paths.get(this.getPriceStreamName()))) {

            //stream.forEach(System.out::println);

            stream.forEach(this::compute);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void compute(String price) {
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

                CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " - regressionAggregates --> "+ Arrays.toString(this.regressionAggregates));

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
                        CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " is in Uptrend");
                        this.setInUpTrend(true);
                        int tempUpCounter = this.getUpCounter();
                        this.setUpCounter(++tempUpCounter);
                        if (this.getUpCounter() >= ticksBeforeEntry && this.isInTrade() == false) {
                            CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " can now be bought..");
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
                                CNMPDemo.cnmpLogger.logger.info(this.getCoinName() + " can be exited..");
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



    public CNMPDemoPatternChecker(String coinName, String priceStreamName) {
        this.setCoinName(coinName);
        this.setPriceStreamName(priceStreamName);
    }



    public double[][] getSlidingWindow() {
        return slidingWindow;
    }

    public void setSlidingWindow(double[][] slidingWindow) {
        this.slidingWindow = slidingWindow;
    }



}

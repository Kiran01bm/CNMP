package com.cnmpdemo.misc;

/**
 * Created by kiranya on 8/4/18.
 */

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.text.DecimalFormat;

public class TestRegression {

    // creating regression object, passing true to have intercept term

    // time = intercept + slope * price
    private static SimpleRegression simpleRegression = new SimpleRegression(true);
    private static SimpleRegression simpleRegressionWithoutIntercept = new SimpleRegression(false);
    private static DecimalFormat df = new DecimalFormat("#.##");

    public static void main(String[] args) {

        // passing data to the model
        // model will be fitted automatically by the class

        double[][] data = new double[][] {
                {1, 2},
                {2, 3},
                {3, 4},
                {4, 5},
                {5, 6}
        };

        computeSimpleRegression(data, "23456");

        data = new double[][] {
                {1, 1},
                {2, 2},
                {3, 3},
                {4, 4},
                {5, 5}
        };

        computeSimpleRegression(data, "12345");

        data = new double[][] {
                {1, 4},
                {2, 3},
                {3, 2},
                {4, 1},
                {5, 0}
        };

        computeSimpleRegression(data, "43210");

        data = new double[][] {
                {1, 2},
                {2, 1},
                {3, 3},
                {4, 4},
                {5, 5}
        };

        computeSimpleRegression(data, "21345");

    }


    public static void computeSimpleRegression(double[][] data, String pattern) {

        simpleRegression.addData(data);

        System.out.println("");
        // get model parameters
        System.out.println("For " + pattern + " -->"
                +  " slope = " + Double.valueOf(df.format(simpleRegression.getSlope()))
                + ", intercept = " + Double.valueOf(df.format(simpleRegression.getIntercept()))
                + ", prediction for 1.5 = " + Double.valueOf(df.format(simpleRegression.predict(1.5))));

    }
}

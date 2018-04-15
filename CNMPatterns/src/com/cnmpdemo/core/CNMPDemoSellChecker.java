package com.cnmpdemo.core;

/**
 * Created by kiranya on 7/4/18.
 */
public class CNMPDemoSellChecker implements Runnable {

    public CNMPDemoSellChecker(CNMPDemoPatternChecker patternChecker) {
        this.patternChecker = patternChecker;
    }

    private CNMPDemoPatternChecker patternChecker;

    public void run()
    {
        System.out.println("Checking for signals to sell!!");
        System.out.println("TBC: Implement ways for Predicting dump/vertical falls");
    }

}
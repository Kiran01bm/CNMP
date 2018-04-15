package com.cnmpdemo.misc;

/**
 * Created by kiranya on 7/4/18.
 */

public class StaticInitializationCheck {

    static {
        step(1);
    }

    public static int step_1 = step(2);
    public int step_6 = step(6);

    public StaticInitializationCheck() {
        step(8);
    }

    {
        step(7);
    }

    // Just for demonstration purposes:
    public static int step(int step) {
        System.out.println("Step " + step);
        return step;
    }
}

class ExampleSubclass extends StaticInitializationCheck {

    {
        step(9);
    }

    public static int step_3 = step(3);
    public int step_10 = step(10);

    static {
        step(4);
    }

    public ExampleSubclass() {
        step(11);
    }

    public static void main(String[] args) {
        step(5);
        new ExampleSubclass();
        step(12);
    }
}
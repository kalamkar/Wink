package com.ojogaze.wink;

/**
 * Created by abhi on 6/19/17.
 */

public class CustomFilter {

    private final int window[] = new int[20];

    float step(float value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = (int) value;
        return Stats.calculateMedian(window, 0, window.length);
    }
}

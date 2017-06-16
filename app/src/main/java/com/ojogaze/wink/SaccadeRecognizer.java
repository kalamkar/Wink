package com.ojogaze.wink;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;

/**
 * Created by abhi on 6/15/17.
 */

public class SaccadeRecognizer {
    public static final int GRAPH_LENGTH = 1024;

    public final int window[] = new int[GRAPH_LENGTH];
    public final int feature1[] = new int[GRAPH_LENGTH];

    private final int threshold;

    private int prevValue = 0;
    private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
    private int countSinceLatestDirectionChange = 0;

    public int saccadeLength = 0;
    public int saccadeAmplitude = 0;

    private IirFilter filter;

    public SaccadeRecognizer(float samplingFreq, int threshold) {
        this.threshold = threshold;
        filter = new IirFilter(IirFilterDesignExstrom.design(
                FilterPassType.bandpass, 1, 1.024 / samplingFreq, 2.56 / samplingFreq));
    }

    public void update(int value) {
//        value = (int) filter.step(value);

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        int newDirection = value - prevValue;
        newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

        if (currentDirection != newDirection && newDirection != 0) {
            currentDirection = newDirection;

            // Calculate median baseline instead of using zero, Needed during bootup time
            // when filters are adjusting for the drift and baseline is away from zero.
            int median = Stats.calculateMedian(window, 0, window.length);
            saccadeLength = countSinceLatestDirectionChange;
            saccadeAmplitude = prevValue - median;

            countSinceLatestDirectionChange = 0;
        } else {
            countSinceLatestDirectionChange++;
            saccadeLength = 0;
            saccadeAmplitude = 0;
        }

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = hasSaccade() ? saccadeAmplitude : 0;

        prevValue = value;
    }

    public boolean hasSaccade() {
        return saccadeLength > 0 && Math.abs(saccadeAmplitude) >= threshold ;
    }
}

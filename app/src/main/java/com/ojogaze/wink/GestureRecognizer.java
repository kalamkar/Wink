package com.ojogaze.wink;

/**
 * Created by abhi on 6/15/17.
 */

public class GestureRecognizer {
    public static final int GRAPH_LENGTH = 1024;

    public final int window[] = new int[GRAPH_LENGTH];
    public final int feature1[] = new int[GRAPH_LENGTH];
    public final int feature2[] = new int[GRAPH_LENGTH];

    private final float samplingFreq;
    private final int saccadeAmplitudeThreshold;
    private final int fixationThreshold;

    private int prevValue = 0;
    private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
    private int countSinceLatestDirectionChange = 0;

    public int saccadeLength = 0;
    public int saccadeAmplitude = 0;
    public int fixationSamples;
    public int lastFixationSamplesAgo = Integer.MAX_VALUE;

    // private IirFilter filter;
    private CustomFilter filter = new CustomFilter();

    public GestureRecognizer(float samplingFreq, int saccadeAmplitudeThreshold,
                             long fixationThresholdMillis) {
        this.samplingFreq = samplingFreq;
        this.saccadeAmplitudeThreshold = saccadeAmplitudeThreshold;
        this.fixationThreshold = (int) (fixationThresholdMillis / (1000 / samplingFreq));
//        filter = new IirFilter(IirFilterDesignExstrom.design(
//                FilterPassType.bandpass, 1, 1.024 / samplingFreq, 2.56 / samplingFreq));
    }

    public void update(int value) {
        value = (int) filter.step(value);

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        Stats stats = new Stats(window, 0, window.length);

        int newDirection = value - prevValue;
        newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

        float fixationAmplitudeThreshold = 3 * stats.stdDev;
        fixationSamples = Math.abs(stats.median - value) < fixationAmplitudeThreshold
                ? fixationSamples + 1 : 1;
        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        if (fixationSamples >= fixationThreshold) {
            feature2[Math.max(0, feature2.length - fixationSamples)] = stats.median;
            feature2[feature2.length - 1] = stats.median;
            lastFixationSamplesAgo = 0;
        } else {
            feature2[feature2.length - 1] = 0;
            lastFixationSamplesAgo++;
        }

        if (currentDirection != newDirection && newDirection != 0) {
            currentDirection = newDirection;

            // Calculate median baseline instead of using zero, Needed during bootup time
            // when filters are adjusting for the drift and baseline is away from zero.
//            int median = Stats.calculateMedian(window, 0, window.length);
            saccadeLength = countSinceLatestDirectionChange;
            saccadeAmplitude = prevValue - stats.median;

            countSinceLatestDirectionChange = 0;
        } else {
            countSinceLatestDirectionChange++;
            saccadeLength = 0;
            saccadeAmplitude = 0;
        }

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = hasGesture() ? prevValue : 0;

        prevValue = value;
    }

    public boolean hasGesture() {
        return (saccadeLength > 0 && Math.abs(saccadeAmplitude) >= saccadeAmplitudeThreshold)
                && (lastFixationSamplesAgo < (200 / (1000 / samplingFreq)));
    }

    public void resetGesture() {
        lastFixationSamplesAgo = Integer.MAX_VALUE;
        fixationSamples = 1;
    }

//    public boolean hasSaccade() {
//        return saccadeLength > 0 && Math.abs(saccadeAmplitude) >= saccadeAmplitudeThreshold;
//    }
//
//    public boolean hasFixation() {
//        return lastFixationSamplesAgo < (200 / (1000 / samplingFreq));
//    }
}

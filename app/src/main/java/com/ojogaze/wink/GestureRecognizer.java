package com.ojogaze.wink;

/**
 * Created by abhi on 6/15/17.
 */

public class GestureRecognizer {
    public static final int GRAPH_LENGTH = 1024;

    public final int window[] = new int[GRAPH_LENGTH];
    public final int feature1[] = new int[GRAPH_LENGTH];
    public final int feature2[] = new int[GRAPH_LENGTH];

    private final SaccadeRecognizer saccades;
    private final FixationRecognizer fixations;

    private long updateCount = 0;

    // private IirFilter filter;
    private CustomFilter filter = new CustomFilter();

    public GestureRecognizer(float samplingFreq, int saccadeAmplitudeThreshold,
                             long fixationThresholdMillis) {
        saccades = new SaccadeRecognizer(saccadeAmplitudeThreshold);
        fixations = new FixationRecognizer(samplingFreq, fixationThresholdMillis);
//        filter = new IirFilter(IirFilterDesignExstrom.design(
//                FilterPassType.bandpass, 1, 1.024 / samplingFreq, 2.56 / samplingFreq));
    }

    public void update(int value) {
        updateCount++;
        value = (int) filter.step(value);

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        Stats stats = new Stats(window, 0, window.length);

        saccades.update(value, stats.median);
        fixations.update(value, stats.median, 3 * stats.stdDev);

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = hasGesture() ? saccades.prevValue : 0;

        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        if (fixations.isTriggered()) {
            feature2[Math.max(0, feature2.length - 1 - fixations.fixationLength)] = stats.median;
            feature2[feature2.length - 1] = stats.median;
        } else {
            feature2[feature2.length - 1] = 0;
        }
    }

    public boolean hasGesture() {
        return updateCount > window.length && saccades.isTriggered() && fixations.isTriggered();
    }

    public void resetGesture() {
        fixations.lastFixationSamplesAgo = Integer.MAX_VALUE;
        fixations.fixationLength = 0;
    }

    private static class FixationRecognizer {
        private final int lengthThreshold;
        private final int lengthTolerance;

        public int fixationLength;
        public int lastFixationSamplesAgo = Integer.MAX_VALUE;

        FixationRecognizer(float samplingFreq, long fixationThresholdMillis) {
            lengthThreshold = (int) (fixationThresholdMillis / (1000 / samplingFreq));
            lengthTolerance = (int) (200 / (1000 / samplingFreq));
        }

        public void update(int value, int base, int amplitudeThreshold) {
            fixationLength = Math.abs(value - base) < amplitudeThreshold ? fixationLength + 1 : 0;
            if (fixationLength >= lengthThreshold) {
                lastFixationSamplesAgo = 0;
            } else {
                lastFixationSamplesAgo++;
            }
        }

        private boolean isTriggered() {
            return lastFixationSamplesAgo < lengthTolerance;
        }
    }

    private static class SaccadeRecognizer {
        private final int amplitudeThreshold;

        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestDirectionChange = 0;

        public int saccadeLength = 0;
        public int saccadeAmplitude = 0;

        SaccadeRecognizer(int amplitudeThreshold) {
            this.amplitudeThreshold = amplitudeThreshold;
        }

        private void update(int value, int base) {
            int newDirection = value - prevValue;
            newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

            if (currentDirection != newDirection && newDirection != 0) {
                currentDirection = newDirection;

                saccadeLength = countSinceLatestDirectionChange;
                saccadeAmplitude = prevValue - base;

                countSinceLatestDirectionChange = 0;
            } else {
                countSinceLatestDirectionChange++;
                saccadeLength = 0;
                saccadeAmplitude = 0;
            }

            prevValue = value;
        }

        private boolean isTriggered() {
            return saccadeLength > 0 && Math.abs(saccadeAmplitude) >= amplitudeThreshold;
        }
    }
}

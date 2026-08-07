package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

/**
 * Loop detector based on autocorrelation of the sustain region.
 *
 * <p>The sustain of a musical note is quasi periodic: the waveform repeats itself
 * with a fundamental period {@code P}. A loop is valid when the seam between the
 * end and the beginning of the repeated segment is continuous, i.e. when
 * {@code loopEnd - loopStart} is an integer multiple of {@code P}.
 *
 * <p>Algorithm:
 * <ol>
 *     <li>Estimate the fundamental period {@code P} of the sustain region (the last
 *         second before the note off marker) via normalized autocorrelation.</li>
 *     <li>Pick the loop start such that {@code loopEnd - loopStart} is a multiple of
 *         {@code P} (one period here).</li>
 * </ol>
 */
public class AutocorrelationLoopDetector implements LoopDetector {
    /**
     * Minimal loop duration, in seconds
     */
    public static final float MIN_LOOP_DURATION_IN_SEC = 0.05f;
    /**
     * Duration of the sustain analysis window, in seconds. Must be at least as large
     * as the longest loop we want to detect (a loop can last up to 4 seconds).
     */
    public static final float ANALYSIS_WINDOW_IN_SEC = 8.0f;

    @Override
    public LoopSetting detectLoop(LoopDetectionContext context) {
        float[] samples = WavSampleReader.readMonoSamples(context.getWavFile());
        int sampleRate = context.getSampleRate();
        long loopEnd = context.getNoteOffSampleMarker();
        int period = detectPeriod(samples, sampleRate, (int) loopEnd);
        if (period <= 0) {
            return null;
        }
        long loopStart = loopEnd - period;
        if (loopStart < 0) {
            return null;
        }
        LoopSetting loopSetting = new LoopSetting();
        loopSetting.setSampleStart(loopStart);
        loopSetting.setSampleEnd(loopEnd);
        return loopSetting;
    }

    /**
     * Estimate the fundamental period of the sustain region ending at {@code loopEnd}.
     *
     * @param samples    mono samples of the whole note
     * @param sampleRate sample rate in Hz
     * @param loopEnd    end of the sustain, in samples
     * @return the estimated period in samples, or {@code -1} if it cannot be found
     */
    private int detectPeriod(float[] samples, int sampleRate, int loopEnd) {
        int minPeriod = Math.max(1, (int) (sampleRate * MIN_LOOP_DURATION_IN_SEC));
        int maxPeriod = (int) (sampleRate * ANALYSIS_WINDOW_IN_SEC);
        int windowLength = Math.min(loopEnd, (int) (sampleRate * ANALYSIS_WINDOW_IN_SEC));
        int windowStart = Math.max(0, loopEnd - windowLength);
        int nbSamples = loopEnd - windowStart;
        if (nbSamples <= 2 * minPeriod) {
            return -1;
        }
        double bestScore = -1;
        int bestPeriod = -1;
        for (int period = minPeriod; period <= Math.min(maxPeriod, nbSamples / 2); period++) {
            double score = autocorrelation(samples, windowStart, period, nbSamples - period);
            if (score > bestScore) {
                bestScore = score;
                bestPeriod = period;
            }
        }
        return bestPeriod;
    }

    /**
     * Normalized autocorrelation over the analysis window: how well the waveform
     * shifted by {@code period} samples correlates with itself.
     */
    private double autocorrelation(float[] samples, int windowStart, int period, int nbSamples) {
        if (nbSamples <= 0) {
            return -1;
        }
        double dot = 0;
        double energyWin = 0;
        double energyShifted = 0;
        for (int i = 0; i < nbSamples; i++) {
            double a = samples[windowStart + i];
            double b = samples[windowStart + i + period];
            dot += a * b;
            energyWin += a * a;
            energyShifted += b * b;
        }
        double denom = Math.sqrt(energyWin * energyShifted);
        return denom == 0 ? -1 : dot / denom;
    }
}
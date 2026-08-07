package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

    private final boolean writeCsv;

    /**
     * @param writeCsv if {@code true}, dump the autocorrelation scores to a CSV file
     *                 next to the wav (debug/test purpose only, default {@code false})
     */
    public AutocorrelationLoopDetector(boolean writeCsv) {
        this.writeCsv = writeCsv;
    }

    public AutocorrelationLoopDetector() {
        this(false);
    }

    @Override
    public LoopSetting detectLoop(LoopDetectionContext context) {
        float[] samples = WavSampleReader.readMonoSamples(context.getWavFile());
        int sampleRate = context.getSampleRate();
        long loopEnd = context.getNoteOffSampleMarker();
        int period = detectPeriod(samples, sampleRate, (int) loopEnd, context.getWavFile());
        if (period <= 0) {
            return null;
        }
        return LoopSeamPlacer.findLoop(samples, period, loopEnd, sampleRate);
    }

    /**
     * Estimate the period of the sustain region ending at {@code loopEnd}.
     *
     * @param samples    mono samples of the whole note
     * @param sampleRate sample rate in Hz
     * @param loopEnd    end of the sustain, in samples
     * @return the estimated period in samples, or {@code -1} if it cannot be found
     */
    private int detectPeriod(float[] samples, int sampleRate, int loopEnd, File wavFile) {
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
        int maxPeriodToScan = Math.min(maxPeriod, nbSamples / 2);
        double[] scores = new double[maxPeriodToScan - minPeriod + 1];
        for (int period = minPeriod; period <= maxPeriodToScan; period++) {
            double score = autocorrelation(samples, windowStart, period, nbSamples - period);
            scores[period - minPeriod] = score;
            if (score > bestScore) {
                bestScore = score;
                bestPeriod = period;
            }
        }
        if (writeCsv) {
            writeScoresCsv(wavFile, minPeriod, scores);
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

    /**
     * Debug helper: write the autocorrelation scores (one row per period) as a CSV
     * file next to the wav, so peaks can be inspected externally (e.g. in Excel).
     *
     * @param wavFile    source wav (used to derive the output file name)
     * @param minPeriod  period of the first score
     * @param scores     scores indexed by {@code period - minPeriod}
     */
    private void writeScoresCsv(File wavFile, int minPeriod, double[] scores) {
        File csvFile = new File(wavFile.getParentFile(),
                wavFile.getName().replaceAll("(?i)\\.wav$", "") + "-autocorrelation.csv");
        try (PrintWriter writer = new PrintWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.println("period;sampleStartFromLoopEnd;score");
            for (int i = 0; i < scores.length; i++) {
                int period = minPeriod + i;
                writer.printf(Locale.US, "%d;%d;%.6f%n", period, period - minPeriod, scores[i]);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write autocorrelation CSV " + csvFile, e);
        }
    }
}
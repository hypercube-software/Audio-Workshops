package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.insights.dft.DFTResult;
import com.hypercube.workshop.audioworkshop.api.insights.dft.fast.FFTCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.windows.BlackmanHarris;
import com.hypercube.workshop.audioworkshop.api.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMEncoding;
import com.hypercube.workshop.synthripper.model.LoopSetting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Loop detector based on the frequency domain: the loop is seamless when the
 * spectrum of the very end of the sustain (the window ending at the note off
 * marker) matches the spectrum of a window ending {@code P} samples earlier.
 *
 * <p>A candidate loop of period {@code P} is scored by the mean absolute
 * difference (in dB) between the short term spectra at the seam. The correct
 * loop period minimizes this distance, because the seam then repeats the same
 * spectral content.
 *
 * <p>Unlike the time domain autocorrelation, this only compares magnitudes
 * (phases are discarded), which is robust to the small amplitude variations of
 * looped samples such as water or rhythmic loops.
 *
 * <p>Algorithm:
 * <ol>
 *     <li>Compute the reference spectra of the last windows ending at the
 *         note off marker (loop end).</li>
 *     <li>Coarse search: scan the period range by steps of {@link #COARSE_STEP}
 *         and keep the best candidate.</li>
 *     <li>Fine search: scan every single sample around the coarse best and pick
 *         the minimum of the spectral distance.</li>
 *     <li>Return the loop {@code [loopEnd - P, loopEnd]}.</li>
 * </ol>
 */
public class FrequencyDomainLoopDetector implements LoopDetector {
    /**
     * Minimal loop duration, in seconds
     */
    public static final float MIN_LOOP_DURATION_IN_SEC = 0.05f;
    /**
     * Maximal loop duration, in seconds. A loop can last up to 4 seconds.
     */
    public static final float MAX_LOOP_DURATION_IN_SEC = 8.0f;
    /**
     * Size of the short term FFT windows used to compare the seam, in ms
     */
    public static final int FFT_WINDOW_IN_MS = 12;
    /**
     * Number of adjacent windows compared on each side of the seam
     */
    public static final int NB_COMPARED_FRAMES = 3;
    /**
     * Step (in samples) of the coarse search
     */
    public static final int COARSE_STEP = 128;

    private final boolean writeCsv;

    /**
     * @param writeCsv if {@code true}, dump the spectral distance scores to a CSV
     *                 file next to the wav (debug/test purpose only, default {@code false})
     */
    public FrequencyDomainLoopDetector(boolean writeCsv) {
        this.writeCsv = writeCsv;
    }

    public FrequencyDomainLoopDetector() {
        this(false);
    }

    @Override
    public LoopSetting detectLoop(LoopDetectionContext context) {
        float[] samples = WavSampleReader.readMonoSamples(context.getWavFile());
        int sampleRate = context.getSampleRate();
        long loopEnd = context.getNoteOffSampleMarker();

        PCMBufferFormat format = new PCMBufferFormat(FFT_WINDOW_IN_MS, sampleRate,
                BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        int windowSize = format.getSampleBufferSize();
        int hop = windowSize / 2;

        int minPeriod = Math.max(1, (int) (sampleRate * MIN_LOOP_DURATION_IN_SEC));
        int maxPeriod = (int) (sampleRate * MAX_LOOP_DURATION_IN_SEC);
        maxPeriod = Math.min(maxPeriod, (int) loopEnd - windowSize - (NB_COMPARED_FRAMES - 1) * hop);
        if (maxPeriod < minPeriod) {
            return null;
        }

        // Reference spectra: windows ending at (loopEnd - k * hop)
        double[][] reference = spectraAt(samples, format, windowSize, hop, loopEnd);
        if (reference == null) {
            return null;
        }

        // 1) coarse search
        int bestCoarse = -1;
        double bestScore = Double.MAX_VALUE;
        double[] coarseScores = new double[(maxPeriod - minPeriod) / COARSE_STEP + 1];
        int coarseIdx = 0;
        for (int period = minPeriod; period <= maxPeriod; period += COARSE_STEP, coarseIdx++) {
            double score = score(samples, format, windowSize, hop, loopEnd, period, reference);
            coarseScores[coarseIdx] = score;
            if (score < bestScore) {
                bestScore = score;
                bestCoarse = period;
            }
        }
        if (bestCoarse < 0) {
            return null;
        }

        // 2) fine search around the coarse best
        int from = Math.max(minPeriod, bestCoarse - COARSE_STEP);
        int to = Math.min(maxPeriod, bestCoarse + COARSE_STEP);
        int bestPeriod = bestCoarse;
        for (int period = from; period <= to; period++) {
            double score = score(samples, format, windowSize, hop, loopEnd, period, reference);
            if (score < bestScore) {
                bestScore = score;
                bestPeriod = period;
            }
        }

        if (writeCsv) {
            writeScoresCsv(context.getWavFile(), minPeriod, COARSE_STEP, coarseScores, bestPeriod, bestScore);
        }

        return LoopSeamPlacer.findLoop(samples, bestPeriod, loopEnd, sampleRate);
    }

    /**
     * Mean absolute spectral distance (in dB) between the reference windows
     * ending at {@code loopEnd} and the windows ending {@code period} samples earlier.
     */
    private double score(float[] samples, PCMBufferFormat format, int windowSize, int hop,
                         long loopEnd, int period, double[][] reference) {
        double[][] candidate = spectraAt(samples, format, windowSize, hop, loopEnd - period);
        if (candidate == null) {
            return Double.MAX_VALUE;
        }
        double total = 0;
        int nbFrames = Math.min(reference.length, candidate.length);
        for (int f = 0; f < nbFrames; f++) {
            total += spectralDistance(reference[f], candidate[f]);
        }
        return total / nbFrames;
    }

    /**
     * Compute the spectra of {@link #NB_COMPARED_FRAMES} consecutive windows
     * ending at {@code endSample} (hop by {@code windowSize / 2}).
     *
     * @return the list of magnitude spectra in dB, or {@code null} if the windows
     *         fall before the beginning of the signal
     */
    private double[][] spectraAt(float[] samples, PCMBufferFormat format, int windowSize, int hop, long endSample) {
        int nbFrames = NB_COMPARED_FRAMES;
        if (endSample - windowSize - (nbFrames - 1) * hop < 0) {
            return null;
        }
        double[][] result = new double[nbFrames][];
        for (int f = 0; f < nbFrames; f++) {
            long windowEnd = endSample - f * hop;
            result[f] = spectrumAt(samples, format, windowSize, windowEnd);
        }
        return result;
    }

    /**
     * Compute the magnitude spectrum (in dB) of the window ending at {@code windowEnd}.
     */
    private double[] spectrumAt(float[] samples, PCMBufferFormat format, int windowSize, long windowEnd) {
        long windowStart = windowEnd - windowSize;
        double[][] raw = new double[1][windowSize];
        for (int i = 0; i < windowSize; i++) {
            raw[0][i] = samples[(int) (windowStart + i)];
        }
        try (FFTCalculator fft = new FFTCalculator(format, new BlackmanHarris())) {
            fft.onBuffer(new SampleBuffer(raw, 0, windowSize, 1));
            DFTResult result = fft.getMagnitudes()[0].get(0);
            return result.getMagnitudes();
        }
    }

    /**
     * Mean absolute difference of two magnitude spectra (in dB), excluding the DC bin.
     */
    private double spectralDistance(double[] a, double[] b) {
        int nb = Math.min(a.length, b.length);
        double sum = 0;
        int count = 0;
        for (int i = 1; i < nb; i++) {
            sum += Math.abs(a[i] - b[i]);
            count++;
        }
        return count == 0 ? Double.MAX_VALUE : sum / count;
    }

    /**
     * Debug helper: write the coarse search scores (one row per tested period)
     * as a CSV file next to the wav, so the spectral distance curve can be
     * inspected externally (e.g. in Excel).
     */
    private void writeScoresCsv(File wavFile, int minPeriod, int step, double[] scores,
                                int bestPeriod, double bestScore) {
        File csvFile = new File(wavFile.getParentFile(),
                wavFile.getName().replaceAll("(?i)\\.wav$", "") + "-frequency-distance.csv");
        try (PrintWriter writer = new PrintWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.println("period;score");
            for (int i = 0; i < scores.length; i++) {
                int period = minPeriod + i * step;
                writer.printf(Locale.US, "%d;%.6f%n", period, scores[i]);
            }
            writer.printf(Locale.US, "#best;%d;%.6f%n", bestPeriod, bestScore);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write frequency distance CSV " + csvFile, e);
        }
    }
}

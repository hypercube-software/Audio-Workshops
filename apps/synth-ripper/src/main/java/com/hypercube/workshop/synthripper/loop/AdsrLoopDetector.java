package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * Loop detector based on the global amplitude envelope (ADSR) of the note.
 *
 * <p>Instead of searching for a periodic seam, this detector reads the overall
 * dynamics of the sample:
 * <ol>
 *     <li>Compute the (smoothed) amplitude envelope of the whole note.</li>
 *     <li>Locate the attack peak (end of the Attack phase, start of the Decay).</li>
 *     <li>Estimate the sustain level (the plateau the envelope falls back to)
 *         and the start of the sustain (end of the Decay phase).</li>
 *     <li>The loop is {@code [start of Decay, end of Sustain]}: the loop begins
 *         at the attack peak and ends at the note off marker (where the Release
 *         phase would start).</li>
 * </ol>
 *
 * <p>This is useful for samples where the sustain is not strictly periodic (e.g.
 * slowly evolving pads or heavily filtered sounds): the loop only needs the
 * envelope to be flat, not the waveform itself to repeat exactly.
 */
@Slf4j
public class AdsrLoopDetector implements LoopDetector {
    /**
     * Size of the envelope analysis window, in seconds
     */
    public static final float ENVELOPE_WINDOW_IN_SEC = 0.01f;
    /**
     * Size of the smoothing window applied on the envelope, in seconds
     */
    public static final float ENVELOPE_SMOOTH_IN_SEC = 0.05f;
    /**
     * The sustain is considered reached when the envelope falls below
     * {@code sustainLevel * (1 + SUSTAIN_TOLERANCE)}.
     */
    public static final double SUSTAIN_TOLERANCE = 0.10;
    /**
     * Minimum duration (in seconds) the envelope must stay at the sustain level
     * to confirm the sustain phase was actually reached
     */
    public static final float MIN_SUSTAIN_DURATION_IN_SEC = 0.3f;
    /**
     * Fraction of the sustain windows that must be at or below the sustain level.
     * Kept below 1 so isolated transients (very common on evolving/streaming
     * sounds) do not prevent the sustain region from being recognised.
     */
    public static final double SUSTAIN_STABLE_RATIO = 0.8;
    /**
     * Minimal loop duration, in seconds
     */
    public static final float MIN_LOOP_DURATION_IN_SEC = 0.05f;

    private final boolean writeCsv;

    /**
     * @param writeCsv if {@code true}, dump the amplitude envelope to a CSV file
     *                 next to the wav (debug/test purpose only, default {@code false})
     */
    public AdsrLoopDetector(boolean writeCsv) {
        this.writeCsv = writeCsv;
    }

    public AdsrLoopDetector() {
        this(false);
    }

    @Override
    public LoopSetting detectLoop(LoopDetectionContext context) {
        float[] samples = WavSampleReader.readMonoSamples(context.getWavFile());
        int sampleRate = context.getSampleRate();
        long release = context.getNoteOffSampleMarker();
        if (release <= 0) {
            return null;
        }

        int windowSize = Math.max(1, (int) (sampleRate * ENVELOPE_WINDOW_IN_SEC));
        int nbWindows = (int) (release / windowSize);
        if (nbWindows < 4) {
            return null;
        }
        float[] envelope = computeEnvelope(samples, windowSize, nbWindows);
        envelope = smoothEnvelope(envelope, Math.max(1, (int) (ENVELOPE_SMOOTH_IN_SEC / ENVELOPE_WINDOW_IN_SEC)));

        int attackPeakWindow = findAttackPeak(envelope);
        int sustainStartWindow = -1;
        if (attackPeakWindow >= 0) {
            double sustainLevel = estimateSustainLevel(envelope, attackPeakWindow);
            if (sustainLevel > 0) {
                sustainStartWindow = findSustainStart(envelope, attackPeakWindow, sustainLevel);
            }
        }

        if (writeCsv) {
            writeEnvelopeCsv(context.getWavFile(), envelope, windowSize, attackPeakWindow, sustainStartWindow);
        }

        if (attackPeakWindow < 0) {
            log.warn("ADSR: envelope is silent, no attack peak found");
            return null;
        }
        double sustainLevel = estimateSustainLevel(envelope, attackPeakWindow);
        if (sustainLevel <= 0) {
            log.warn("ADSR: no sustain level found (peak window {})", attackPeakWindow);
            return null;
        }
        if (sustainStartWindow < 0) {
            log.warn("ADSR: sustain never reached (peak window {}, sustain level {})",
                    attackPeakWindow, sustainLevel);
            return null;
        }

        long loopStart = (long) attackPeakWindow * windowSize;
        long loopEnd = release;
        if (loopEnd - loopStart < (long) (sampleRate * MIN_LOOP_DURATION_IN_SEC)) {
            log.warn("ADSR: loop too short (start {} end {}, min {})",
                    loopStart, loopEnd, (long) (sampleRate * MIN_LOOP_DURATION_IN_SEC));
            return null;
        }

        log.debug("ADSR: peak={} (sample {}), sustain level={}, sustain start={} (sample {}), loop=[{}..{}]",
                attackPeakWindow, loopStart, sustainLevel, sustainStartWindow,
                (long) sustainStartWindow * windowSize, loopStart, loopEnd);

        LoopSetting loopSetting = new LoopSetting();
        loopSetting.setSampleStart(loopStart);
        loopSetting.setSampleEnd(loopEnd);
        // Envelope based: the loop is a single pass over the decay + sustain region
        loopSetting.setLoopLength(loopEnd - loopStart);
        return loopSetting;
    }

    /**
     * Peak amplitude of each window of {@code windowSize} samples, as a float in [0,1].
     */
    private float[] computeEnvelope(float[] samples, int windowSize, int nbWindows) {
        float[] envelope = new float[nbWindows];
        for (int w = 0; w < nbWindows; w++) {
            float max = 0;
            int base = w * windowSize;
            for (int i = 0; i < windowSize; i++) {
                float v = Math.abs(samples[base + i]);
                if (v > max) {
                    max = v;
                }
            }
            envelope[w] = max;
        }
        return envelope;
    }

    /**
     * Moving average smoothing of the envelope.
     */
    private float[] smoothEnvelope(float[] envelope, int smoothSize) {
        float[] out = new float[envelope.length];
        for (int i = 0; i < envelope.length; i++) {
            int from = Math.max(0, i - smoothSize / 2);
            int to = Math.min(envelope.length - 1, i + smoothSize / 2);
            float sum = 0;
            for (int j = from; j <= to; j++) {
                sum += envelope[j];
            }
            out[i] = sum / (to - from + 1);
        }
        return out;
    }

    /**
     * Window where the envelope is maximal: the attack peak (start of the Decay).
     *
     * @return window index, or -1 if the envelope is entirely silent
     */
    private int findAttackPeak(float[] envelope) {
        int best = -1;
        float bestValue = 0;
        for (int w = 0; w < envelope.length; w++) {
            if (envelope[w] > bestValue) {
                bestValue = envelope[w];
                best = w;
            }
        }
        return best;
    }

    /**
     * Robust estimate of the sustain level: the median of the envelope over the
     * windows strictly after the attack peak. This is the plateau the envelope
     * falls back to once the initial transient (attack + decay) is over.
     */
    private double estimateSustainLevel(float[] envelope, int attackPeakWindow) {
        int from = Math.min(envelope.length - 1, attackPeakWindow + 1);
        if (from >= envelope.length) {
            return -1;
        }
        float[] tail = Arrays.copyOfRange(envelope, from, envelope.length);
        Arrays.sort(tail);
        return tail[tail.length / 2];
    }

    /**
     * First window after the attack peak where the envelope has settled to the
     * sustain level, i.e. start of the Sustain phase. Over a window of
     * {@code MIN_SUSTAIN_DURATION_IN_SEC}, at least {@code SUSTAIN_STABLE_RATIO} of
     * the samples must be at or below the sustain threshold, so isolated
     * transients do not falsely break the plateau.
     *
     * @return window index, or -1 if the sustain is never reached
     */
    private int findSustainStart(float[] envelope, int attackPeakWindow, double sustainLevel) {
        double threshold = sustainLevel * (1 + SUSTAIN_TOLERANCE);
        int sustainLen = Math.max(1, (int) (MIN_SUSTAIN_DURATION_IN_SEC / ENVELOPE_WINDOW_IN_SEC));
        int required = Math.max(1, (int) (sustainLen * SUSTAIN_STABLE_RATIO));
        for (int w = attackPeakWindow + 1; w + sustainLen <= envelope.length; w++) {
            int count = 0;
            for (int j = w; j < w + sustainLen; j++) {
                if (envelope[j] <= threshold) {
                    count++;
                }
            }
            if (count >= required) {
                return w;
            }
        }
        return -1;
    }

    /**
     * Debug helper: dump the (smoothed) envelope as CSV next to the wav, with the
     * detected attack peak and sustain start annotated.
     */
    private void writeEnvelopeCsv(File wavFile, float[] envelope, int windowSize,
                                  int attackPeakWindow, int sustainStartWindow) {
        File csvFile = new File(wavFile.getParentFile(),
                wavFile.getName().replaceAll("(?i)\\.wav$", "") + "-adsr-envelope.csv");
        try (PrintWriter writer = new PrintWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.println("sample;amplitude");
            for (int w = 0; w < envelope.length; w++) {
                long sample = (long) w * windowSize;
                writer.printf(Locale.US, "%d;%.6f%n", sample, envelope[w]);
            }
            writer.printf(Locale.US, "#attackPeak;%d;%.6f%n",
                    (long) attackPeakWindow * windowSize,
                    attackPeakWindow >= 0 ? envelope[attackPeakWindow] : Double.NaN);
            writer.printf(Locale.US, "#sustainStart;%d;%.6f%n",
                    (long) sustainStartWindow * windowSize,
                    sustainStartWindow >= 0 ? envelope[sustainStartWindow] : Double.NaN);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write ADSR envelope CSV " + csvFile, e);
        }
    }
}

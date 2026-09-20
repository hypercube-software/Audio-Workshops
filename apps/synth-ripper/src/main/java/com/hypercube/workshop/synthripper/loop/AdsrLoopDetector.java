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
import java.util.Arrays;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * Loop detector based on the ADSR envelope of the note, refined with a spectral
 * stability check to locate the true start of the Sustain phase.
 *
 * <p>Instead of searching for a periodic seam, this detector reads the overall
 * dynamics of the sample:
 * <ol>
 *     <li>Compute the (smoothed) amplitude envelope of the whole note.</li>
 *     <li>Locate the attack peak (end of the Attack phase, start of the Decay).</li>
 *     <li>Estimate the sustain level (the plateau the envelope falls back to).</li>
 *     <li>Detect the end of the Decay (start of Sustain) by requiring the
 *         spectrum to be stable: the short-term FFT signature of the note must
 *         not lose or gain energy in a significant way. A moving spectral
 *         distance to the tail of the note is used, so isolated transients do not
 *         falsely start the sustain.</li>
 *     <li>The loop is {@code [start of Sustain, note off marker]}.</li>
 * </ol>
 *
 * <p>The sustain start is the first window where <em>both</em> the amplitude has
 * settled to the sustain level <em>and</em> the spectrum is stable. Requiring both
 * guarantees the loop does not start while the sound is still decaying.
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
     * Time constant of the one-pole low-pass (envelope follower) applied on the
     * smoothed envelope before computing the slope. A short moving-average is not
     * enough to discriminate the Decay from the Sustain on evolving/streaming
     * sounds, whose envelope keeps oscillating at the beat rate (measured ~2% of
     * the peak on this project's pads). The follower integrates those ripples away
     * so the macro-slope of the plateau can be measured.
     */
    public static final float ENVELOPE_FOLLOWER_TAU_IN_SEC = 0.3f;
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
     * Tolerance on the macro-slope of the smoothed envelope, expressed as the
     * absolute envelope change allowed over {@link #MIN_SUSTAIN_DURATION_IN_SEC}
     * (a windowed peak envelope in [0,1]). Integrated over the sustain duration,
     * the trend of a decayed note stays below this value, while the fall of the
     * Decay and the final Release fade-out exceed it. This is the "slope / level
     * plateau" criterion of the classic ADSR segmentation.
     */
    public static final double SUSTAIN_SLOPE_TOLERANCE = 0.002;
    /**
     * Size of the (Hamming-windowed) FFT used to compute the spectral signature of
     * each analysis window, in seconds
     */
    public static final float FFT_WINDOW_IN_SEC = 0.03f;
    /**
     * Lag (in seconds) between the two spectra compared to compute the local
     * spectral flux. Short enough to catch the transition from a fast evolving
     * decay to a slow, stable sustain.
     */
    public static final float SPECTRAL_FLUX_LAG_IN_SEC = 0.05f;
    /**
     * Percentile (0..1) of the spectral-flux distribution (after the attack peak)
     * used as the spectral stability gate. The gate is intentionally permissive:
     * it only excludes the still-fast-evolving early Decay, while the sustain start
     * itself is decided by the envelope slope plateau (see {@link #findSustainStart}).
     * A high percentile keeps the gate from overruling the slope criterion on
     * sounds whose spectrum keeps evolving through the sustain (streaming pads).
     */
    public static final double SPECTRAL_STABILITY_PERCENTILE = 0.95;
    /**
     * Spectral distance (in dB) below which two spectra are considered identical.
     */
    public static final double SPECTRAL_DISTANCE_FLOOR_DB = 0.2;
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
        double[] flux = null;
        if (attackPeakWindow >= 0) {
            // The sustain starts only where the amplitude has settled AND the spectrum is stable.
            flux = computeSpectralDistance(samples, sampleRate, windowSize, attackPeakWindow, (int) release);
            double threshold = spectralDistanceFloor(flux);
            float[] follower = smoothEnvelopeFollower(envelope);
            sustainStartWindow = findSustainStart(envelope, follower, attackPeakWindow, threshold, flux);
        }

        if (writeCsv) {
            writeEnvelopeCsv(context.getWavFile(), envelope, flux, windowSize, attackPeakWindow, sustainStartWindow);
        }

        if (attackPeakWindow < 0) {
            log.warn("ADSR: envelope is silent, no attack peak found");
            return null;
        }
        if (sustainStartWindow < 0) {
            log.warn("ADSR: sustain never reached (peak window {}, release {})", attackPeakWindow, release);
            return null;
        }

        long loopStart = (long) sustainStartWindow * windowSize;
        long loopEnd = release;
        if (loopEnd - loopStart < (long) (sampleRate * MIN_LOOP_DURATION_IN_SEC)) {
            log.warn("ADSR: loop too short (start {} end {}, min {})",
                    loopStart, loopEnd, (long) (sampleRate * MIN_LOOP_DURATION_IN_SEC));
            return null;
        }

        log.debug("ADSR: peak={} (sample {}), sustain start={} (sample {}), loop=[{}..{}]",
                attackPeakWindow, (long) attackPeakWindow * windowSize,
                sustainStartWindow, loopStart, loopStart, loopEnd);

        LoopSetting loopSetting = new LoopSetting();
        loopSetting.setSampleStart(loopStart);
        loopSetting.setSampleEnd(loopEnd);
        // Envelope + spectral based: the loop is a single pass over the stable sustain region
        loopSetting.setLoopLength(loopEnd - loopStart);
        // Expose the ADSR phase boundaries so they can be embedded as markers in the wav.
        loopSetting.setAttackPeakSample((long) attackPeakWindow * windowSize);
        loopSetting.setSustainStartSample(loopStart);
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
     * One-pole low-pass (envelope follower) applied on the smoothed envelope:
     * {@code follower[i] = follower[i-1] + alpha * (envelope[i] - follower[i-1])}
     * with {@code alpha = 1 - exp(-dt/tau)}. This integrates the beat-rate
     * oscillations that remain after the moving-average, so the macro-slope of the
     * plateau is measurable, while the initial transients are preserved enough to
     * keep the Decay visually distinct on the follower.
     */
    private float[] smoothEnvelopeFollower(float[] envelope) {
        float alpha = (float) (1 - Math.exp(-ENVELOPE_WINDOW_IN_SEC / ENVELOPE_FOLLOWER_TAU_IN_SEC));
        float[] out = new float[envelope.length];
        float acc = envelope[0];
        out[0] = acc;
        for (int i = 1; i < envelope.length; i++) {
            acc += alpha * (envelope[i] - acc);
            out[i] = acc;
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
     * Spectral flux (in dB) of every analysis window: the mean absolute spectral
     * difference between a window's spectrum and the spectrum {@code lag} windows
     * earlier. This is a <em>local</em> stability measure: during the Decay the
     * spectrum changes fast (high flux), once the Sustain is reached it becomes
     * slow and stays low. Using a short lag (instead of comparing to a distant
     * tail reference) lets slowly evolving pad sustains be detected early.
     *
     * @return an array aligned with the envelope windows, {@code flux[w]} = mean abs
     *         spectral difference (dB) between window {@code w} and a nearby window
     */
    private double[] computeSpectralDistance(float[] samples, int sampleRate, int windowSize,
                                             int attackPeakWindow, int release) {
        PCMBufferFormat format = new PCMBufferFormat((int) (FFT_WINDOW_IN_SEC * 1000), sampleRate,
                BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        int fftSize = format.getSampleBufferSize();

        int fluxLag = Math.max(1, (int) (SPECTRAL_FLUX_LAG_IN_SEC / ENVELOPE_WINDOW_IN_SEC));
        int nbWindows = release / windowSize;

        // Pre-compute one spectrum per window so the local differences are cheap.
        double[][] spectra = new double[nbWindows][];
        for (int w = 0; w < nbWindows; w++) {
            spectra[w] = spectrumAt(samples, format, fftSize, (long) (w + 1) * windowSize);
        }

        double[] flux = new double[nbWindows];
        for (int w = 0; w < nbWindows; w++) {
            if (spectra[w] == null || w - fluxLag < 0 || spectra[w - fluxLag] == null) {
                flux[w] = Double.MAX_VALUE;
                continue;
            }
            flux[w] = spectralDistance(spectra[w], spectra[w - fluxLag]);
        }
        return flux;
    }

    /**
     * Stable baseline of the spectral flux: the median flux over the tail of the
     * note (the already sustained region). In the sustain the flux is a small,
     * roughly constant value; a window whose flux is close to this baseline is
     * considered spectrally stable. The result is floored at
     * {@link #SPECTRAL_DISTANCE_FLOOR_DB}.
     */
    /**
     * Stable baseline of the spectral flux, used as a permissive gate: the flux of
     * a sustained note is below it. It is taken as the {@code 75th} percentile of
     * the flux observed after the attack peak, so only windows still exhibiting an
     * unusually high spectral evolution (the early Decay) are excluded, everything
     * else counts as stable. This keeps the Sustain start driven by where the
     * amplitude settles, without letting a permanently low-but-nonzero flux of an
     * evolving pad push the sustain to the very end of the note. The result is
     * floored at {@link #SPECTRAL_DISTANCE_FLOOR_DB}.
     */
    private double spectralDistanceFloor(double[] flux) {
        if (flux == null) {
            return SPECTRAL_DISTANCE_FLOOR_DB;
        }
        double[] values = new double[flux.length];
        int count = 0;
        for (double v : flux) {
            if (v != Double.MAX_VALUE) {
                values[count++] = v;
            }
        }
        if (count == 0) {
            return SPECTRAL_DISTANCE_FLOOR_DB;
        }
        double[] copy = Arrays.copyOf(values, count);
        Arrays.sort(copy);
        int index = Math.min(copy.length - 1, (int) (copy.length * SPECTRAL_STABILITY_PERCENTILE));
        return Math.max(SPECTRAL_DISTANCE_FLOOR_DB, copy[index]);
    }

    /**
     * Magnitude spectrum (in dB) of the {@code fftSize}-samples window ending at
     * {@code windowEnd}, or {@code null} if the window falls before the start of
     * the signal.
     */
    private double[] spectrumAt(float[] samples, PCMBufferFormat format, int fftSize, long windowEnd) {
        long windowStart = windowEnd - fftSize;
        if (windowStart < 0) {
            return null;
        }
        double[][] raw = new double[1][fftSize];
        for (int i = 0; i < fftSize; i++) {
            raw[0][i] = samples[(int) (windowStart + i)];
        }
        try (FFTCalculator fft = new FFTCalculator(format, new BlackmanHarris())) {
            fft.onBuffer(new SampleBuffer(raw, 0, fftSize, 1));
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
     * First window after the attack peak where the sound has <em>both</em> settled
     * in amplitude and stabilised spectrally:
     * <ul>
     *     <li><b>slope</b> — the macro-slope of the <em>follower</em> envelope
     *     integrated over {@link #MIN_SUSTAIN_DURATION_IN_SEC} must stay within
     *     {@link #SUSTAIN_SLOPE_TOLERANCE}, i.e. the envelope no longer falls
     *     noticeably: this is the classic "level plateau" of the ADSR Decay end.
     *     The follower filters the beat-rate ripples so the plateau is actually
     *     measurable on evolving/streaming sounds.</li>
     *     <li><b>amplitude</b> — the envelope is at or below the sustain level
     *     (a loose upper bound, only rejects still-loud early windows).</li>
     *     <li><b>spectral</b> — the local spectral flux is below the stability
     *     floor (permissive gate, keeps isolating transients from starting the
     *     sustain).</li>
     * </ul>
     * Over {@code MIN_SUSTAIN_DURATION_IN_SEC}, at least {@code SUSTAIN_STABLE_RATIO}
     * of the windows must satisfy <em>all</em> conditions, so isolated transients
     * do not falsely break the plateau.
     *
     * @param follower        one-pole low-pass of the smoothed envelope, slope is
     *                        measured on it
     * @return window index, or -1 if the sustain is never reached
     */
    private int findSustainStart(float[] envelope, float[] follower, int attackPeakWindow,
                                 double spectralFloor,
                                 double[] spectralDistance) {
        if (spectralDistance == null) {
            return -1;
        }
        double sustainLevel = estimateSustainLevel(envelope, attackPeakWindow);
        if (sustainLevel <= 0) {
            return -1;
        }
        double amplitudeThreshold = sustainLevel * (1 + SUSTAIN_TOLERANCE);
        double spectralThreshold = spectralFloor * (1 + SUSTAIN_TOLERANCE);
        int sustainLen = Math.max(1, (int) (MIN_SUSTAIN_DURATION_IN_SEC / ENVELOPE_WINDOW_IN_SEC));
        int required = Math.max(1, (int) (sustainLen * SUSTAIN_STABLE_RATIO));
        for (int w = attackPeakWindow + 1; w + sustainLen <= envelope.length; w++) {
            int count = 0;
            for (int j = w; j < w + sustainLen; j++) {
                boolean slopeStable = Math.abs(integratedSlope(follower, j, sustainLen)) <= SUSTAIN_SLOPE_TOLERANCE;
                boolean amplitudeStable = envelope[j] <= amplitudeThreshold;
                boolean spectralStable = spectralDistance[j] <= spectralThreshold;
                if (slopeStable && amplitudeStable && spectralStable) {
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
     * Macro-slope of the smoothed envelope integrated over the next window of
     * {@code span} analysis windows: the net envelope change over
     * {@code span * ENVELOPE_WINDOW_IN_SEC} seconds. A decayed note has a
     * clearly negative value during the Decay and a value near zero through the
     * Sustain.
     */
    private double integratedSlope(float[] envelope, int window, int span) {
        int to = Math.min(envelope.length - 1, window + span);
        if (to <= window) {
            return 0;
        }
        return envelope[to] - envelope[window];
    }

    /**
     * Debug helper: dump the (smoothed) envelope as CSV next to the wav, with the
     * detected attack peak and sustain start annotated.
     */
    private void writeEnvelopeCsv(File wavFile, float[] envelope, double[] flux, int windowSize,
                                  int attackPeakWindow, int sustainStartWindow) {
        File csvFile = new File(wavFile.getParentFile(),
                wavFile.getName().replaceAll("(?i)\\.wav$", "") + "-adsr-envelope.csv");
        int sustainLen = Math.max(1, (int) (MIN_SUSTAIN_DURATION_IN_SEC / ENVELOPE_WINDOW_IN_SEC));
        try (PrintWriter writer = new PrintWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.println("sample;amplitude;flux;slope");
            for (int w = 0; w < envelope.length; w++) {
                long sample = (long) w * windowSize;
                double slope = integratedSlope(envelope, w, sustainLen);
                if (flux != null && w < flux.length && flux[w] != Double.MAX_VALUE) {
                    writer.printf(Locale.US, "%d;%.6f;%.6f;%.6f%n", sample, envelope[w], flux[w], slope);
                } else {
                    writer.printf(Locale.US, "%d;%.6f;;%.6f%n", sample, envelope[w], slope);
                }
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

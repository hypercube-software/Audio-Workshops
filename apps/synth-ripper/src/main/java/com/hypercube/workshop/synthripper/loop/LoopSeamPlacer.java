package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

/**
 * Places a loop of a given period onto the signal so that the seam is seamless.
 *
 * <p>Given a period {@code P} and a sustain region ending at the note off marker
 * (the release), this searches for the best loop start {@code S} such that the
 * first samples of the loop best match the samples exactly one period after them:
 * <pre>S(i) == S(i + P)</pre> for the very beginning of the loop. Minimizing the
 * mismatch at this seam makes {@code [S, S + P]} seamless when repeated.
 *
 * <p>This is the crucial piece the simpler detectors missed: the note off (release)
 * marker is <em>not</em> necessarily aligned with a boundary of the loop, so the
 * loop end must be {@code S + P} and not the release marker.
 */
public class LoopSeamPlacer {
    /**
     * Duration of the seam comparison window, in seconds
     */
    public static final float SEAM_WINDOW_IN_SEC = 0.02f;
    /**
     * Step of the coarse seam search, in samples
     */
    public static final int SEAM_COARSE_STEP = 4;
    /**
     * A candidate extension of the loop by an extra period is kept when its seam
     * score is not worse than this factor times the score of the single-period loop.
     * This keeps the loop as long as the natural repetitions in the sample allow.
     */
    public static final double EXTENSION_TOLERANCE = 2.5;

    private LoopSeamPlacer() {
    }

    /**
     * Place a loop of the given period in the sustain region.
     *
     * @param samples    mono samples of the whole note
     * @param period     the loop period {@code P}, in samples
     * @param release    the note off (release) marker, in samples: end of the sustain
     * @param sampleRate sample rate in Hz
     * @return a {@link LoopSetting} with {@code sampleEnd - sampleStart} a multiple
     *         of {@code loopLength} and {@code sampleEnd <= release}, or {@code null}
     *         if nothing valuable is found
     */
    public static LoopSetting findLoop(float[] samples, int period, long release, int sampleRate) {
        if (period <= 0) {
            return null;
        }
        int seamWindow = Math.min((int) (sampleRate * SEAM_WINDOW_IN_SEC), Math.max(1, period));
        // The loop must lie inside the sustain. The loop end must be close to the
        // release, so we only look for the seam in a window of one period before it:
        // the loop should not drift into the middle of the sustain.
        long maxStart = release - Math.max(period, 1);
        if (maxStart < 0) {
            return null;
        }
        long minStart = Math.max(0, release - 2L * period);

        // 1) coarse search for the best alias of the loop (best seam)
        long bestStart = -1;
        double bestScore = Double.MAX_VALUE;
        for (long s = minStart; s <= maxStart; s += SEAM_COARSE_STEP) {
            double score = seamScore(samples, s, period, seamWindow);
            if (score < bestScore) {
                bestScore = score;
                bestStart = s;
            }
        }
        if (bestStart < 0) {
            return null;
        }

        // 2) fine search around the coarse best
        long from = Math.max(minStart, bestStart - SEAM_COARSE_STEP);
        long to = Math.min(maxStart, bestStart + SEAM_COARSE_STEP);
        for (long s = from; s <= to; s++) {
            double score = seamScore(samples, s, period, seamWindow);
            if (score < bestScore) {
                bestScore = score;
                bestStart = s;
            }
        }

        long loopStart = bestStart;
        long loopEnd = bestStart + period;
        if (loopEnd > release) {
            loopEnd = release;
            loopStart = loopEnd - period;
            if (loopStart < 0) {
                return null;
            }
        }

        // 3) extension: if the sample contains several natural repetitions of the
        //    period, pull the loop start back by extra periods (still seamlessly)
        loopStart = extendLoopBack(samples, loopStart, loopEnd, period, seamWindow, bestScore);

        LoopSetting loopSetting = new LoopSetting();
        loopSetting.setSampleStart(loopStart);
        loopSetting.setSampleEnd(loopEnd);
        // Period P as detected, i.e. the length of a single repetition. The final
        // loop [sampleStart, sampleEnd] may span several periods when extended.
        loopSetting.setLoopLength(period);
        return loopSetting;
    }

    /**
     * Try to extend the loop backwards: replacing the loop start by
     * {@code loopStart - P} increases the loop length by one period while the seam
     * (now between {@code loopStart} and {@code loopEnd}) must stay acceptable.
     *
     * <p>Looping content is seamless when the sample at the loop start equals the
     * sample at the loop end, i.e. {@code loopStart[i] == loopStart[i + loopLength]}.
     */
    private static long extendLoopBack(float[] samples, long loopStart, long loopEnd, int period,
                                       int seamWindow, double bestScore) {
        long current = loopStart;
        long length = loopEnd - current;
        double threshold = bestScore * EXTENSION_TOLERANCE;
        while (current - period >= 0) {
            long candidate = current - period;
            long candidateLength = length + period;
            int i0 = (int) candidate;
            int i1 = (int) (candidate + candidateLength);
            if (i1 + seamWindow > samples.length) {
                break;
            }
            double score = seamScore(samples, candidate, (int) candidateLength, seamWindow);
            if (score > threshold) {
                break;
            }
            current = candidate;
            length = candidateLength;
        }
        return current;
    }

    /**
     * Mean absolute difference between the first {@code seamWindow} samples of the loop
     * and the samples exactly one period later. The smaller the score the more seamless
     * the loop: {@code S[i] must equal S[i + P]}.
     */
    private static double seamScore(float[] samples, long start, int period, int seamWindow) {
        int i0 = (int) start;
        int i1 = i0 + period;
        double sum = 0;
        for (int i = 0; i < seamWindow; i++) {
            double d = samples[i0 + i] - samples[i1 + i];
            sum += d * d;
        }
        return sum;
    }
}
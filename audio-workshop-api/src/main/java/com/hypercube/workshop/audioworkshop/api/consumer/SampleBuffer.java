package com.hypercube.workshop.audioworkshop.api.consumer;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This class act as "window" or a "view" upon a buffer of samples
 * <ul>
 *     <li>{@link #sampleStart} and {@link #sampleEnd} delimit the view</li>
 *     <li>{@link #getRawSampleIndex(int)} is essential to get the right sample index in the buffer {@link #samples}</li>
 * </ul>
 * <p>
 * The internal buffer of samples is not really private on purpose,
 * it can be shared among many SampleBuffers using {@link #split(int, int)} or {@link #getRawSampleIndex(int)}.
 */
@Accessors(fluent = true)
public class SampleBuffer {
    private final double[][] samples;
    private final int sampleStart;
    private final int sampleEnd;
    @Getter
    private final int nbSamples;
    @Getter
    private final int nbChannels;

    public SampleBuffer(double[][] samples, int sampleStart, int sampleEnd, int nbChannels) {
        this.samples = samples;
        this.sampleStart = sampleStart;
        this.sampleEnd = sampleEnd;
        this.nbChannels = nbChannels;
        this.nbSamples = sampleEnd - sampleStart;
    }

    /**
     * return the right sample in the sample window
     *
     * @param channel Audio channel
     * @param index   Position in the window
     * @return the sample value
     */
    public double sample(int channel, int index) {
        return samples[channel][getRawSampleIndex(index)];
    }

    /**
     * Return the real position in the sample buffer given {@link #sampleStart}
     *
     * @param index input index in the window
     * @return real index in the buffer {@link #samples}
     */
    private int getRawSampleIndex(int index) {
        return index + sampleStart;
    }

    /**
     * Give access to the internal buffer in case we need it
     *
     * @param channel Audio channel
     * @return buffer of samples which is potentially bigger than the sample window defined by {@link #sampleStart} and {@link #sampleEnd}
     */
    public double[] getRawBuffer(int channel) {
        return samples[channel];
    }

    /**
     * Give access to the internal buffers in case we need it
     *
     * @return buffer of samples which is potentially bigger than the sample window defined by {@link #sampleStart} and {@link #sampleEnd}
     */
    public double[][] getRawBuffers() {
        return samples;
    }

    /**
     * Modify the internal sample buffer
     *
     * @param channel audio channel
     * @param index   position the window
     * @param sample  value to set
     */
    public void setSample(int channel, int index, double sample) {
        samples[channel][getRawSampleIndex(index)] = sample;
    }

    /**
     * Create a new sample window from the actual sample window
     *
     * @param from      index in the current window
     * @param nbSamples width of the new window
     * @return
     */
    public SampleBuffer split(int from, int nbSamples) {
        return new SampleBuffer(samples, getRawSampleIndex(from), getRawSampleIndex(from + nbSamples), nbChannels);
    }

    /**
     * Apply a linear fade in to the windowed samples
     */
    public void fadeIn() {
        double ratio = 1.0 / nbSamples;
        for (int ch = 0; ch < nbChannels; ch++) {
            for (int s = 0; s < nbSamples; s++) {
                double scale = s * ratio;
                double sample = sample(ch, s) * scale;
                setSample(ch, s, sample);
            }
        }
    }

    /**
     * Apply a linear fade out to the windowed samples
     */
    public void fadeOut() {
        double ratio = 1.0 / nbSamples;
        for (int ch = 0; ch < nbChannels; ch++) {
            for (int s = 0; s < nbSamples; s++) {
                double scale = (nbSamples - 1 - s) * ratio;
                double sample = sample(ch, s) * scale;
                setSample(ch, s, sample);
            }
        }
    }
}

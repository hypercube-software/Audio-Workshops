package com.hypercube.workshop.audioworkshop.common.consumer;

/**
 * Transform a stream of buffers with given size in another stream with different samples size
 * <p>sampleIncrement != windowSizeInSamples can be used to reuse previous samples (very usefull for FFT)</p>
 */
public class WindowedSampleBufferConsumer implements SampleBufferConsumer {
    protected final int windowSizeInSamples;
    protected final int nbChannels;
    protected final int sampleIncrement;
    protected final double[][] samples;
    protected final SampleBufferConsumer consumer;
    protected int nbSamplesRead;

    public WindowedSampleBufferConsumer(int windowSizeInSamples, int nbChannels, SampleBufferConsumer consumer) {
        this.windowSizeInSamples = windowSizeInSamples;
        this.sampleIncrement = windowSizeInSamples;
        this.nbChannels = nbChannels;
        this.samples = new double[nbChannels][windowSizeInSamples];
        this.consumer = consumer;
        reset();
    }

    public WindowedSampleBufferConsumer(int windowSizeInSamples, int sampleIncrement, int nbChannels, SampleBufferConsumer consumer) {
        if (sampleIncrement == 0 || sampleIncrement > windowSizeInSamples) {
            throw new IllegalArgumentException("Illegal value for sampleIncrement:" + sampleIncrement + ". Should be 0 or > " + windowSizeInSamples);
        }
        this.windowSizeInSamples = windowSizeInSamples;
        this.sampleIncrement = sampleIncrement;
        this.nbChannels = nbChannels;
        this.samples = new double[nbChannels][windowSizeInSamples];
        this.consumer = consumer;
        reset();
    }

    @Override
    public void reset() {
        // we need to slide the window
        // yyyyyyyyzzzzzzzzzzzzzzzzz : sampleIncrement is samples (y) in the entire window
        // zzzzzzzzzzzzzzzzz         : overlap is the amount of samples we reuse (z) and slide the the begining
        int overlap = windowSizeInSamples - sampleIncrement;
        for (int s = 0; s < overlap; s++) {
            {
                for (int ch = 0; ch < nbChannels; ch++) {
                    samples[ch][s] = samples[ch][s + sampleIncrement];
                }
            }
        }
        // we put the write head just after the slided samples
        nbSamplesRead = overlap;
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        for (int s = 0; s < buffer.nbSamples(); s++) {
            for (int ch = 0; ch < nbChannels; ch++) {
                samples[ch][nbSamplesRead] = buffer.sample(ch, s);
            }
            nbSamplesRead++;
            if (nbSamplesRead == windowSizeInSamples) {
                consumer.onBuffer(new SampleBuffer(samples, 0, windowSizeInSamples, buffer.nbChannels()));
                reset();
            }
        }
    }
}

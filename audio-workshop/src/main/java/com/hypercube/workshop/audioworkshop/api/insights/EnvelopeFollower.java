package com.hypercube.workshop.audioworkshop.api.insights;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleConsumer;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import lombok.Getter;

public class EnvelopeFollower implements SampleBufferConsumer {
    private final EnvelopeState state;
    private final SampleConsumer sampleConsumer;
    private long samplePos = 0;
    private long noiseFloorAcquisitionTimeInSamples;
    @Getter
    private double noiseFloor;
    @Getter
    private long sampleStart = -1;

    public long getSampleEnd() {
        return sampleEnd != -1 ? sampleEnd : samplePos;
    }

    private long sampleEnd = -1;

    public EnvelopeFollower(PCMFormat format, double releaseTimeMs, double windowSizeInMs, SampleConsumer sampleConsumer) {
        this.state = new EnvelopeState(format, releaseTimeMs, windowSizeInMs);
        this.sampleConsumer = sampleConsumer;
        noiseFloorAcquisitionTimeInSamples = format.millisecondsToSamples(250);
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        for (int s = 0; s < buffer.nbSamples(); s++) {
            for (int c = 0; c < buffer.nbChannels(); c++) {
                state.onNewSample(buffer.sample(c, s), c);
            }
            state.nextSample();
            double rms = state.getRMS(0);
            double sample = buffer.sample(0, s);
            sampleConsumer.onSample(sample, rms, state.getCorrelation(), 0);
            samplePos++;
            if (samplePos < noiseFloorAcquisitionTimeInSamples) {
                noiseFloor = Math.max(noiseFloor, rms);
            } else {
                if (rms > noiseFloor && sampleStart == -1) {
                    sampleStart = samplePos;
                }
                if (rms <= noiseFloor && sampleEnd != -1) {
                    sampleEnd = samplePos;
                }
            }
        }
    }
}

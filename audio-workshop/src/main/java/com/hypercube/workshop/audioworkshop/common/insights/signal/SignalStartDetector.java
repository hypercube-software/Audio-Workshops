package com.hypercube.workshop.audioworkshop.common.insights.signal;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.common.consumer.WindowedSampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.common.insights.peak.PeakCalculator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignalStartDetector extends WindowedSampleBufferConsumer {
    public static final int WINDOW_SIZE_MS = 1;
    private final PeakCalculator peakCalculator;
    private final PCMFormat format;
    long positionInSamples = 0;
    boolean inSignal;
    long signalStart = 0;

    public SignalStartDetector(PCMFormat format) {
        super(format.millisecondsToSamples(WINDOW_SIZE_MS), format.getNbChannels(), new PeakCalculator());
        this.peakCalculator = (PeakCalculator) consumer;
        this.format = format;
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        peakCalculator.reset();
        peakCalculator.onBuffer(buffer);
        long cutPoint = positionInSamples + peakCalculator.getFirstZeroCrossingPosInSample();
        positionInSamples += buffer.nbSamples();
        double rmsDb = peakCalculator.getSamplePeakDb(2);
        boolean cross = (rmsDb > -40);
        if (!inSignal && cross) {
            inSignal = true;
            if (signalStart == 0) {
                signalStart = cutPoint;
                log.info("IN  " + getMsg(signalStart, rmsDb));
            }

        }
    }

    private String getMsg(long cutPoint, double rmsDb) {
        long positionInMs = (long) format.samplesToMilliseconds(cutPoint);
        long positionInSeconds = positionInMs / 1000;
        int hours = (int) (positionInSeconds / 3600);
        int minutes = (int) ((positionInSeconds % 3600) / 60);
        int seconds = (int) (positionInSeconds % 60);
        int ms = (int) (positionInMs % 1000);
        String msg = String.format("%d %02d:%02d:%02d.%03d %.2f dB", cutPoint, hours, minutes, seconds, ms, rmsDb);
        return msg;
    }
}

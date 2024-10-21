package com.hypercube.workshop.audioworkshop.common.insights.signal;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.common.consumer.WindowedSampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.common.insights.peak.PeakCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class SignalSegmentsDetector extends WindowedSampleBufferConsumer {
    public static final int WINDOW_SIZE_MS = 50;
    private final PeakCalculator peakCalculator;
    private final PCMFormat format;
    private final double noiseFloor;
    long positionInSamples = 0;
    SignalSegment firstSegment;
    SignalSegment lastSegment;

    public SignalSegmentsDetector(PCMFormat format, double noiseFloor) {
        super(format.millisecondsToSamples(WINDOW_SIZE_MS), format.getNbChannels(), new PeakCalculator());
        this.peakCalculator = (PeakCalculator) consumer;
        this.format = format;
        this.noiseFloor = noiseFloor;
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        peakCalculator.reset();
        peakCalculator.onBuffer(buffer);
        long cutPoint = positionInSamples + peakCalculator.getFirstZeroCrossingPosInSample();
        double rmsDb = peakCalculator.getSamplePeakDb(2);
        SignalSegmentType type = rmsDb > noiseFloor ? SignalSegmentType.SIGNAL : SignalSegmentType.SILENT;
        SignalSegment signalSegment = new SignalSegment(positionInSamples, positionInSamples + buffer.nbSamples(), type);
        positionInSamples += buffer.nbSamples();
        if (lastSegment == null) {
            firstSegment = lastSegment = signalSegment;
        } else {
            lastSegment.link(signalSegment);
            lastSegment = lastSegment.tryToMerge();
        }
    }

    private String getMsg(long cutPoint, double rmsDb) {
        long positionInMs = (long) (cutPoint * 1000 / format.getSampleRate());
        long positionInSeconds = positionInMs / 1000;
        int hours = (int) (positionInSeconds / 3600);
        int minutes = (int) ((positionInSeconds % 3600) / 60);
        int seconds = (int) (positionInSeconds % 60);
        int ms = (int) (positionInMs % 1000);
        String msg = String.format("%d %02d:%02d:%02d.%03d %.2f dB", cutPoint, hours, minutes, seconds, ms, rmsDb);
        return msg;
    }

    public List<SignalSegment> getSegments() {
        List<SignalSegment> result = new ArrayList<>();
        SignalSegment s = firstSegment;
        while (s != null) {
            result.add(s);
            s = s.getNext();
        }
        return result;
    }
}

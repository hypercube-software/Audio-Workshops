package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.api.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.api.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.api.record.WavRecordListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MonitorRecorder extends WavRecordListener {
    private final int nbChannels;
    private final double[] loudness;
    private final long startTime = System.currentTimeMillis();
    private long lastTime = 0;

    public MonitorRecorder(File output, PCMFormat format, int maxDuration, TimeUnit maxDurationUnit) throws IOException {
        super(output, format, maxDuration, maxDurationUnit);
        nbChannels = format.getNbChannels();
        loudness = new double[nbChannels];
    }

    public void recordWithMonitoring(AudioInputDevice inputDevice, AudioOutputDevice outputDevice, PCMBufferFormat format) {
        try (AudioInputLine line = new AudioInputLine(inputDevice, format)) {
            try (AudioOutputLine outLine = new AudioOutputLine(outputDevice, format)) {
                outLine.start();
                line.record(this, outLine);
            }
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        }
    }

    @Override
    public boolean onNewBuffer(SampleBuffer buffer, byte[] pcmBuffer, int pcmSize) {
        computeLoudness(buffer);
        return super.onNewBuffer(buffer, pcmBuffer, pcmSize);
    }

    private void computeLoudness(SampleBuffer buffer) {
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < buffer.nbSamples(); s++) {
                double sample = Math.abs(buffer.sample(c, s));
                loudness[c] += sample;
            }
        }
        for (int c = 0; c < nbChannels; c++) {
            loudness[c] = loudness[c] / buffer.nbSamples();
        }
        logLoudness();
    }

    private void logLoudness() {
        long now = System.currentTimeMillis();
        long elapsed = (now - lastTime) / 1000;
        long recordDuration = (now - startTime) / 1000;
        if (elapsed > 1) {
            long durationInSec = (long) format.samplesToMilliseconds(currentDurationInSamples) / 1000;
            lastTime = now;
            double dB = 20 * Math.log10(loudness[0]); // look first channel for now
            log.info("{} secs {} sec Volume: {} dB", recordDuration, durationInSec, dB);
        }
    }
}

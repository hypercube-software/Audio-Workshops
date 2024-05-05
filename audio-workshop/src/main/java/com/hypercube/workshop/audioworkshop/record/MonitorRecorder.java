package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.record.WavRecordListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MonitorRecorder extends WavRecordListener {
    private final int nbChannels;
    private final float[] loudness;
    private final long startTime = System.currentTimeMillis();
    private long lastTime = 0;

    public MonitorRecorder(File output, AudioLineFormat format, int maxDuration, TimeUnit maxDurationUnit) throws IOException {
        super(output, format, maxDuration, maxDurationUnit);
        nbChannels = format.getNbChannels();
        loudness = new float[nbChannels];
    }

    public void recordWithMonitoring(AudioInputDevice inputDevice, AudioOutputDevice outputDevice, AudioLineFormat format) {
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
    public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
        computeLoudness(sampleBuffer, nbSamples);
        return super.onNewBuffer(sampleBuffer, nbSamples, pcmBuffer, pcmSize);
    }

    private void computeLoudness(float[][] sampleBuffer, int nbSamples) {
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < nbSamples; s++) {
                float sample = Math.abs(sampleBuffer[c][s]);
                loudness[c] += sample;
            }
        }
        for (int c = 0; c < nbChannels; c++) {
            loudness[c] = loudness[c] / nbSamples;
        }
        logLoudness();
    }

    private void logLoudness() {
        long now = System.currentTimeMillis();
        long elapsed = (now - lastTime) / 1000;
        long recordDuration = (now - startTime) / 1000;
        if (elapsed > 1) {
            long durationInSec = (long) (currentDurationInSamples / format.getSampleRate());
            lastTime = now;
            double dB = 20 * Math.log10(loudness[0]); // look first channel for now
            log.info("{} secs {} sec Volume: {} dB", recordDuration, durationInSec, dB);
        }
    }
}

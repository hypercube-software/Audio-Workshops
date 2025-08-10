package com.hypercube.workshop.audioworkshop.files.riff.insights;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@Getter
public class RiffInspectorState implements SampleBufferConsumer {
    private final Color waveformColor = Color.decode("#06A885");
    private final double[] max;
    private final BufferedImage image;
    private final int samplesPerPixel;
    private final PCMFormat format;
    private final Graphics graphics;
    private final int waveHeightInPixel;
    private final double scale;
    private int samplePositionInWindow;
    private int x;

    public RiffInspectorState(BufferedImage image, int samplesPerPixel, PCMFormat format) {
        if (samplesPerPixel <= 0)
            throw new IllegalArgumentException("samplesPerPixel must be positive: " + samplesPerPixel);
        this.image = image;
        this.samplesPerPixel = samplesPerPixel;
        this.format = format;
        max = new double[format.getNbChannels()];
        graphics = image.getGraphics();
        waveHeightInPixel = image.getHeight() / format.getNbChannels();
        if (waveHeightInPixel < 10) {
            throw new AudioError("Image height %d is too small for %d channels".formatted(image.getHeight(), format.getNbChannels()));
        }
        double maxAmplitudeInPixel = waveHeightInPixel / 2.0f;
        scale = maxAmplitudeInPixel / 0.9f;
    }

    @Override
    public void reset() {
        for (int c = 0; c < format.getNbChannels(); c++) {
            max[c] = 0;
        }
        x = 0;
        samplePositionInWindow = 0;
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        for (int s = 0; s < buffer.nbSamples(); s++) {
            for (int c = 0; c < buffer.nbChannels(); c++) {
                max[c] = Math.max(max[c], Math.abs(buffer.sample(c, s)));
            }
            samplePositionInWindow++;
            if (samplePositionInWindow == samplesPerPixel && x < image.getWidth()) {
                String logMsg = "%d".formatted(x);
                for (int c = 0; c < buffer.nbChannels(); c++) {
                    logMsg += " | %6.6f".formatted(max[c]);
                    double amplitude = max[c] * scale;
                    int middleY = waveHeightInPixel / 2 + waveHeightInPixel * c;
                    graphics.setColor(waveformColor);
                    graphics.drawLine(x, (int) (middleY - amplitude), x, (int) (middleY + amplitude));
                    max[c] = 0;
                }
                log.info(logMsg);
                x++;
                samplePositionInWindow = 0;
            }
        }

    }
}

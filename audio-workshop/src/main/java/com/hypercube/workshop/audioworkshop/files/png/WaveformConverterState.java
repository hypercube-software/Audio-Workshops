package com.hypercube.workshop.audioworkshop.files.png;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.files.riff.RiffAudioInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@Getter
public class WaveformConverterState {
    private final float[] max;
    private final BufferedImage image;
    private final int samplesPerPixel;
    private final RiffAudioInfo format;
    private final Graphics graphics;
    private final int waveHeightInPixel;
    private final float scale;
    private int samplePositionInWindow;
    private int x;
    private Color waveformColor = Color.decode("#06A885");

    public WaveformConverterState(BufferedImage image, int samplesPerPixel, RiffAudioInfo format) {
        if (samplesPerPixel <= 0)
            throw new IllegalArgumentException("samplesPerPixel must be positive: " + samplesPerPixel);
        this.image = image;
        this.samplesPerPixel = samplesPerPixel;
        this.format = format;
        max = new float[format.getNbChannels()];
        graphics = image.getGraphics();
        waveHeightInPixel = image.getHeight() / format.getNbChannels();
        if (waveHeightInPixel < 10) {
            throw new AudioError("Image height %d is too small for %d channels".formatted(image.getHeight(), format.getNbChannels()));
        }
        float maxAmplitudeInPixel = waveHeightInPixel / 2.0f;
        scale = maxAmplitudeInPixel / 0.9f;
    }

    public void updateImage(float[][] samples, int nbSamples) {
        int nbChannels = format.getNbChannels();
        for (int s = 0; s < nbSamples; s++) {
            for (int c = 0; c < nbChannels; c++) {
                max[c] = Math.max(max[c], Math.abs(samples[c][s]));
            }
            samplePositionInWindow++;
            if (samplePositionInWindow == samplesPerPixel && x < image.getWidth()) {
                String logMsg = "%d".formatted(x);
                for (int c = 0; c < nbChannels; c++) {
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

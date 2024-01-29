package com.hypercube.workshop.audioworkshop.common;

import lombok.Getter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteOrder;

@SuppressWarnings({"FieldCanBeLocal", "java:S1068"})
public class AudioOutputLine implements AutoCloseable {
    private final SourceDataLine line;

    private final AudioOutputDevice device;

    private final int sampleRate;

    private final int bitDepth;

    @Getter
    private final int sampleBufferSize;

    private final int byteBufferSize;

    private static final ByteOrder order = ByteOrder.BIG_ENDIAN;

    public AudioOutputLine(AudioOutputDevice device, int sampleRate, int bitDepth, int bufferDurationMs) throws LineUnavailableException {
        this.device = device;
        this.sampleRate = sampleRate;
        this.bitDepth = bitDepth;
        this.sampleBufferSize = (bufferDurationMs * sampleRate) / 1000;
        final int bytesPerSamples = bitDepth / 8;
        this.byteBufferSize = sampleBufferSize * bytesPerSamples;
        AudioFormat format = new AudioFormat(sampleRate, bitDepth, 1, true, order == ByteOrder.BIG_ENDIAN);
        this.line = AudioSystem.getSourceDataLine(format, device.mixerInfo);
        line.open(format, byteBufferSize);
    }

    public void sendBuffer(byte[] rawData) {
        int nbWritten = 0;
        while (nbWritten != rawData.length) {
            nbWritten += line.write(rawData, nbWritten, rawData.length - nbWritten);
        }
    }

    public void start() {
        line.start();
    }

    public void close() {
        stop();
        line.close();
    }

    public void pause() {
        line.stop();
    }

    public void stop() {
        line.drain();
        line.stop();
    }

}

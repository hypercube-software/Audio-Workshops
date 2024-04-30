package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.AudioOutputDevice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteOrder;

@SuppressWarnings({"FieldCanBeLocal", "java:S1068"})
public class AudioOutputLine extends AudioLine implements AutoCloseable {
    private final SourceDataLine line;

    private final AudioOutputDevice device;

    public AudioOutputLine(AudioOutputDevice device, AudioFormat format, int bufferDurationMs) throws LineUnavailableException {
        super(format, bufferDurationMs);
        this.device = device;
        this.line = AudioSystem.getSourceDataLine(format, device.getMixerInfo());
        line.open(format, byteBufferSize);
    }

    public AudioOutputLine(AudioOutputDevice device, int sampleRate, int bitDepth, ByteOrder byteOrder, int bufferDurationMs) throws LineUnavailableException {
        super(new AudioFormat(sampleRate, bitDepth, 1, true, byteOrder == ByteOrder.BIG_ENDIAN), bufferDurationMs);
        this.device = device;
        this.line = AudioSystem.getSourceDataLine(format, device.getMixerInfo());
        line.open(format, byteBufferSize);
    }

    public void sendBuffer(byte[] pcmBuffer, int pcmSize) {
        int nbWritten = 0;
        while (nbWritten != pcmSize) {
            nbWritten += line.write(pcmBuffer, nbWritten, pcmSize - nbWritten);
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

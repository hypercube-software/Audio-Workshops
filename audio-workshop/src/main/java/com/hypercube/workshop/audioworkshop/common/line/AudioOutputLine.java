package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

@SuppressWarnings({"FieldCanBeLocal", "java:S1068"})
public class AudioOutputLine extends AudioLine implements AutoCloseable {
    private final SourceDataLine line;
    private final AudioOutputDevice device;

    public AudioOutputLine(AudioOutputDevice device, PCMBufferFormat format) throws LineUnavailableException {
        super(format);
        this.device = device;
        AudioFormat audioFormat = format.getAudioFormat();
        this.line = AudioSystem.getSourceDataLine(audioFormat, device.getMixerInfo());
        line.open(audioFormat, format.getByteBufferSize());
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

package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.RecordListener;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMconverter;
import com.hypercube.workshop.audioworkshop.common.wav.WavRecordListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AudioInputLine extends AudioLine implements Closeable {
    private static final ByteOrder order = ByteOrder.LITTLE_ENDIAN;
    private final TargetDataLine line;
    private final AudioInputDevice device;

    public AudioInputLine(AudioInputDevice device, AudioFormat format, int bufferDurationMs) throws LineUnavailableException {
        super(format, bufferDurationMs);
        this.device = device;
        this.line = AudioSystem.getTargetDataLine(format, device.getMixerInfo());
        line.open(format, byteBufferSize);
    }

    public AudioInputLine(AudioInputDevice device, int sampleRate, int nbChannels, BitDepth bitDepth, ByteOrder byteOrder, int bufferDurationMs) throws LineUnavailableException {
        super(new AudioFormat(sampleRate, bitDepth.getBits(), nbChannels, true, byteOrder == ByteOrder.BIG_ENDIAN), bufferDurationMs);
        this.device = device;
        this.line = AudioSystem.getTargetDataLine(format, device.getMixerInfo());
        line.open(format, byteBufferSize);
    }

    @Override
    public void close() throws IOException {
        line.close();
    }

    public void record(RecordListener listener) {
        record(listener, null);
    }

    public void record(RecordListener listener, AudioOutputLine outputLine) {
        line.start();
        final byte[] pcmData = new byte[byteBufferSize];
        final float[][] normalizedData = new float[nbChannels][sampleBufferSize];
        boolean bigEndian = order == ByteOrder.BIG_ENDIAN;
        int frameSize = bytesPerSamples * nbChannels;
        for (; ; ) {
            int nbRead = line.read(pcmData, 0, pcmData.length);
            if (nbRead <= 0)
                break;
            if (outputLine != null) {
                outputLine.sendBuffer(pcmData, nbRead);
            }
            int nbSampleRead = nbRead / frameSize;
            PCMconverter.convert(pcmData, normalizedData, nbSampleRead, nbChannels, bitDepth, bigEndian, true);
            if (!listener.onNewBuffer(normalizedData, nbSampleRead, pcmData, nbRead))
                break;
        }
    }

    public void recordWAV(int duration, TimeUnit timeUnit, File output) {
        try (WavRecordListener out = new WavRecordListener(output, format, duration, timeUnit)) {
            record(out);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}

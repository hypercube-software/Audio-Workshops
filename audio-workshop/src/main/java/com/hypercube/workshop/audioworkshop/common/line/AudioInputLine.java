package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMConverter;
import com.hypercube.workshop.audioworkshop.common.record.RecordListener;
import com.hypercube.workshop.audioworkshop.common.record.WavRecordListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AudioInputLine extends AudioLine implements Closeable {
    private final TargetDataLine line;
    private final AudioInputDevice device;

    public AudioInputLine(AudioInputDevice device, PCMBufferFormat format) throws LineUnavailableException {
        super(format);
        this.device = device;
        AudioFormat audioFormat = format.getAudioFormat();
        this.line = AudioSystem.getTargetDataLine(audioFormat, device.getMixerInfo());
        line.open(audioFormat, format.getByteBufferSize());
    }

    @Override
    public void close() throws IOException {
        line.close();
    }

    public void record(RecordListener listener) {
        record(listener, null);
    }

    public void record(RecordListener listener, AudioOutputLine outputLine) {
        int nbChannels = format.getNbChannels();
        int frameSize = format.getBytesPerSamples() * nbChannels;
        var converter = PCMConverter.getPCMtoSampleFunction(format);
        byte[] pcmData = format.allocatePcmBuffer();
        double[][] normalizedData = format.allocateSampleBuffer();
        ByteBuffer pcmBuffer = format.wrapPCMBuffer(pcmData);
        line.start();
        for (; ; ) {
            int nbRead = line.read(pcmData, 0, pcmData.length);
            if (nbRead <= 0)
                break;
            if (outputLine != null) {
                outputLine.sendBuffer(pcmData, nbRead);
            }
            int nbSampleRead = nbRead / frameSize;
            converter.convert(pcmBuffer, normalizedData, nbSampleRead, nbChannels);
            if (!listener.onNewBuffer(new SampleBuffer(normalizedData, 0, nbSampleRead, nbChannels), pcmData, nbRead))
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

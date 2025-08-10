package com.hypercube.workshop.audioworkshop.api.line;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMConverter;
import com.hypercube.workshop.audioworkshop.api.record.RecordListener;
import com.hypercube.workshop.audioworkshop.api.record.WavRecordListener;
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
        line = AudioSystem.getTargetDataLine(audioFormat, device.getMixerInfo());
        line.open(audioFormat, format.getByteBufferSize());
    }

    @Override
    public void close() throws IOException {
        line.stop();
        line.close();
    }

    /**
     * Record without monitoring
     */
    public void record(RecordListener listener) {
        record(listener, null);
    }

    /**
     * Convert incoming PCM buffers into a {@link SampleBuffer} and pass them to a {@link RecordListener}
     * <p>This method record until {@link RecordListener#onNewBuffer} return false</p>
     *
     * @param listener   will receive samples
     * @param outputLine used to monitor what is recorded
     */
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
            double durationInNano = format.samplesToMilliseconds(nbSampleRead) * 1000000L;
            converter.convert(pcmBuffer, normalizedData, nbSampleRead, nbChannels);
            if (listener != null) {
                long start = System.nanoTime();
                boolean continueRecord = listener.onNewBuffer(new SampleBuffer(normalizedData, 0, nbSampleRead, nbChannels), pcmData, nbRead);
                double elapsedTime = System.nanoTime() - start;
                if (elapsedTime > durationInNano) {
                    log.error("ERROR: listener take too much time to process the buffer: " + (elapsedTime / 1000000f) + " ms");
                    //continueRecord = false;
                }
                if (!continueRecord) {
                    break;
                }
            }
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

package com.hypercube.workshop.audioworkshop.common.record;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.WaveGUIDCodecs;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffFmtChunk;
import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioFormat;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class WavRecordListener implements RecordListener, Closeable {
    public static final int INFINITE_DURATION = -1;
    private final RiffWriter out;
    private final RiffChunk dataChunk;
    private final long maxDurationInSamples;
    protected long currentDurationInSamples = 0;
    protected final AudioFormat format;

    public WavRecordListener(File output, AudioFormat format) throws IOException {
        this.format = checkFormat(format);
        this.out = new RiffWriter(output);
        this.dataChunk = new RiffChunk(null, Chunks.DATA, (int) out.getPosition(), 0);
        this.maxDurationInSamples = INFINITE_DURATION;
        createChunks(format);
    }

    private AudioFormat checkFormat(AudioFormat format) {
        if (format.isBigEndian()) {
            throw new AudioError("BigEndian is not supported by WAV format");
        }
        return format;
    }

    public WavRecordListener(File output, AudioFormat format, int maxDuration, TimeUnit maxDurationUnit) throws IOException {
        this.format = checkFormat(format);
        this.out = new RiffWriter(output);
        this.dataChunk = new RiffChunk(null, Chunks.DATA, (int) out.getPosition(), 0);
        this.maxDurationInSamples = computeMaxDurationInSamples(format, maxDuration, maxDurationUnit);
        createChunks(format);
    }

    private long computeMaxDurationInSamples(AudioFormat format, int maxDuration, TimeUnit maxDurationUnit) {
        long maxDurationInSeconds = TimeUnit.SECONDS.convert(maxDuration, maxDurationUnit);
        return (long) (format.getSampleRate() * maxDurationInSeconds);
    }

    private void createChunks(AudioFormat format) throws IOException {
        RiffFmtChunk fmt = new RiffFmtChunk(format.getChannels(), (int) format.getSampleRate(), (int) format.getSampleSizeInBits(), WaveGUIDCodecs.WMMEDIASUBTYPE_PCM);
        out.writeChunk(fmt, fmt.getBytes());
        out.writeChunk(dataChunk);
    }

    @Override
    public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
        try {
            out.write(pcmBuffer, pcmSize);
            currentDurationInSamples += nbSamples;
            if (maxDurationInSamples != INFINITE_DURATION) {
                return currentDurationInSamples < maxDurationInSamples;
            } else {
                return true;
            }
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    @Override
    public void close() throws IOException {
        out.closeChunk(dataChunk);
        out.setSize();
        out.close();
    }
}

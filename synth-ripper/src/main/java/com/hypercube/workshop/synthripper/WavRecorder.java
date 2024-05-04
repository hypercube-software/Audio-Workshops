package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMconverter;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.WaveGUIDCodecs;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffFmtChunk;
import com.hypercube.workshop.synthripper.config.ChannelMap;
import com.hypercube.workshop.synthripper.config.ChannelMapping;
import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioFormat;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class WavRecorder implements Closeable {
    public static final int INFINITE_DURATION = -1;
    private final RiffWriter out;
    private final RiffChunk dataChunk;
    private final long maxDurationInSamples;
    protected long currentDurationInSamples = 0;
    protected final AudioFormat format;
    private final float[][] outSampleBuffer;
    private final byte[] pcmBuffer;

    public WavRecorder(File output, AudioFormat format) throws IOException {
        this.format = checkFormat(format);
        this.out = new RiffWriter(output);
        this.dataChunk = new RiffChunk(null, Chunks.DATA, (int) out.getPosition(), 0);
        this.maxDurationInSamples = INFINITE_DURATION;
        this.outSampleBuffer = new float[format.getChannels()][(int) (format.getSampleRate() * 2)];
        this.pcmBuffer = new byte[(int) (format.getFrameSize() * format.getSampleRate() * 2)];
        createChunks(format);
    }

    private AudioFormat checkFormat(AudioFormat format) {
        if (format.isBigEndian()) {
            throw new AudioError("BigEndian is not supported by WAV format");
        }
        return format;
    }

    private void createChunks(AudioFormat format) throws IOException {
        RiffFmtChunk fmt = new RiffFmtChunk(format.getChannels(), (int) format.getSampleRate(), (int) format.getSampleSizeInBits(), WaveGUIDCodecs.WMMEDIASUBTYPE_PCM);
        out.writeChunk(fmt, fmt.getBytes());
        out.writeChunk(dataChunk);
    }

    public boolean write(float[][] sampleBuffer, int nbSamples, ChannelMap channelMap) {
        try {
            for (int c = 0; c < sampleBuffer.length; c++) {
                ChannelMapping dstChannel = channelMap.get(c);
                if (dstChannel != null && dstChannel.dst() < format.getChannels()) {
                    outSampleBuffer[dstChannel.dst()] = sampleBuffer[c];
                }
            }
            for (int c = 0; c < format.getChannels(); c++) {
                int pcmSize = nbSamples * format.getFrameSize();
                PCMconverter.convert(outSampleBuffer, pcmBuffer, nbSamples, format.getChannels(), BitDepth.valueOf(format.getSampleSizeInBits()), format.isBigEndian(), true);
                out.write(pcmBuffer, pcmSize);
            }
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


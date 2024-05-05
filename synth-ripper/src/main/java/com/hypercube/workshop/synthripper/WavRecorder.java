package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMconverter;
import com.hypercube.workshop.audioworkshop.common.pcm.SampleToPCMFunction;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.WaveGUIDCodecs;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffFmtChunk;
import com.hypercube.workshop.synthripper.config.ChannelMap;
import com.hypercube.workshop.synthripper.config.ChannelMapping;
import lombok.RequiredArgsConstructor;
import org.jline.utils.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class WavRecorder implements Closeable {
    public static final int INFINITE_DURATION = -1;
    private final RiffWriter out;
    private final RiffChunk dataChunk;
    private final long maxDurationInSamples;
    private final SampleToPCMFunction converter;
    protected long currentDurationInSamples = 0;
    protected final AudioLineFormat format;
    private final float[][] outSampleBuffer;
    private final byte[] pcmBuffer;
    private final ByteBuffer pcmByteBuffer;

    public WavRecorder(File output, AudioLineFormat format) throws IOException {
        this.format = checkFormat(format);
        this.out = new RiffWriter(output);
        this.dataChunk = new RiffChunk(null, Chunks.DATA, (int) out.getPosition(), 0);
        this.maxDurationInSamples = INFINITE_DURATION;
        this.outSampleBuffer = format.allocateSampleBuffer();
        this.pcmBuffer = format.allocatePcmBuffer();
        this.pcmByteBuffer = format.wrapPCMBuffer(this.pcmBuffer);
        this.converter = PCMconverter.getSampleToPCMFunction(format);
        createChunks(format);
    }

    private AudioLineFormat checkFormat(AudioLineFormat format) {
        if (format.isBigEndian()) {
            throw new AudioError("BigEndian is not supported by WAV format");
        }
        return format;
    }

    private void createChunks(AudioLineFormat format) throws IOException {
        RiffFmtChunk fmt = new RiffFmtChunk(format.getNbChannels(), format.getSampleRate(), format.getSampleSizeInBits(), WaveGUIDCodecs.WMMEDIASUBTYPE_PCM);
        out.writeChunk(fmt, fmt.getBytes());
        out.writeChunk(dataChunk);
    }

    public boolean write(float[][] sampleBuffer, int startPosInSamples, int nbSamples, ChannelMap channelMap) {
        try {
            for (int c = 0; c < sampleBuffer.length; c++) {
                ChannelMapping dstChannel = channelMap.get(c);
                if (dstChannel != null && dstChannel.dst() < format.getNbChannels()) {
                    outSampleBuffer[dstChannel.dst()] = sampleBuffer[c];
                }
            }
            for (int c = 0; c < format.getNbChannels(); c++) {
                int pcmSize = nbSamples * format.getBytesPerSamples();
                converter.convert(outSampleBuffer, pcmByteBuffer, nbSamples, format.getNbChannels());
                int offset = startPosInSamples * format.getBytesPerSamples() * format.getNbChannels();
                if (offset != 0) {
                    Log.info("Skip " + startPosInSamples + " samples , " + offset + " bytes");
                }
                out.write(pcmBuffer, offset, pcmSize - offset);
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


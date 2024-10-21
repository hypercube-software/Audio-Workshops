package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMConverter;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMMarker;
import com.hypercube.workshop.audioworkshop.common.pcm.SampleToPCMFunction;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.synthripper.config.ChannelMap;
import com.hypercube.workshop.synthripper.config.ChannelMapping;
import lombok.RequiredArgsConstructor;
import org.jline.utils.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

@RequiredArgsConstructor
public class WavRecorder implements Closeable {
    public static final int INFINITE_DURATION = -1;
    private final RiffWriter out;
    private final RiffChunk dataChunk;
    private final long maxDurationInSamples;
    private final SampleToPCMFunction converter;
    protected long currentDurationInSamples = 0;
    protected final PCMFormat format;
    private final double[][] outSampleBuffer;
    private final byte[] pcmBuffer;
    private final ByteBuffer pcmByteBuffer;
    private boolean dataChunkClosed;

    public WavRecorder(File output, PCMBufferFormat format) throws IOException {
        this.format = checkFormat(format);
        this.out = new RiffWriter(output);
        this.dataChunk = new RiffChunk(null, Chunks.DATA, (int) out.getPosition(), 0);
        this.maxDurationInSamples = INFINITE_DURATION;
        this.outSampleBuffer = format.allocateSampleBuffer();
        this.pcmBuffer = format.allocatePcmBuffer();
        this.pcmByteBuffer = format.wrapPCMBuffer(this.pcmBuffer);
        this.converter = PCMConverter.getSampleToPCMFunction(format);
        out.writeFmtChunk(format);
        out.beginChunk(Chunks.DATA);
        dataChunkClosed = false;
    }

    public void writeMarkers(List<PCMMarker> pcmMarkers) throws IOException {
        if (!dataChunkClosed) {
            throw new IllegalArgumentException("call endWrite() first");
        }
        out.writeMarkers(pcmMarkers);
    }

    private PCMFormat checkFormat(PCMFormat format) {
        if (format.isBigEndian()) {
            throw new AudioError("BigEndian is not supported by WAV format");
        }
        return format;
    }

    public boolean write(SampleBuffer buffer, int startPosInSamples, ChannelMap channelMap) {
        try {
            for (int c = 0; c < buffer.nbChannels(); c++) {
                ChannelMapping dstChannel = channelMap.get(c);
                if (dstChannel != null && dstChannel.dst() < format.getNbChannels()) {
                    outSampleBuffer[dstChannel.dst()] = buffer.getRawBuffer(c);
                }
            }
            for (int c = 0; c < format.getNbChannels(); c++) {
                int pcmSize = buffer.nbSamples() * format.getBytesPerSamples();
                converter.convert(outSampleBuffer, pcmByteBuffer, buffer.nbSamples(), format.getNbChannels());
                int offset = startPosInSamples * format.getBytesPerSamples() * format.getNbChannels();
                if (offset != 0) {
                    Log.info("Skip " + startPosInSamples + " samples , " + offset + " bytes");
                }
                out.write(pcmBuffer, offset, pcmSize - offset);
            }
            currentDurationInSamples += buffer.nbSamples() - startPosInSamples;
            if (maxDurationInSamples != INFINITE_DURATION) {
                return currentDurationInSamples < maxDurationInSamples;
            } else {
                return true;
            }
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    public void endWrite() throws IOException {
        out.endChunk(); // Close DATA Chunk
        dataChunkClosed = true;
    }

    @Override
    public void close() throws IOException {
        if (!dataChunkClosed) {
            endWrite();
        }
        out.close();
    }
}


package com.hypercube.workshop.audioworkshop.api.format;

import com.hypercube.workshop.audioworkshop.api.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMEncoding;
import lombok.Getter;

import java.nio.ByteOrder;

/**
 * This class is all about the specification of audio buffers
 */
@Getter
public class PCMBufferFormat extends PCMFormat {
    private final int bufferDurationMs;
    private final int sampleBufferSize;
    private final int byteBufferSize;

    public PCMBufferFormat(int bufferDurationMs, int sampleRate, BitDepth bitDepth, int nbChannels, PCMEncoding encoding, ByteOrder byteOrder) {
        super(sampleRate, bitDepth, nbChannels, encoding, byteOrder);
        this.bufferDurationMs = bufferDurationMs;
        this.sampleBufferSize = millisecondsToSamples(bufferDurationMs);
        this.byteBufferSize = sampleBufferSize * getFrameSizeInBytes();
    }

    public PCMBufferFormat(int bufferDurationMs, PCMFormat format) {
        super(format.sampleRate, format.bitDepth, format.nbChannels, format.encoding, format.byteOrder);
        this.bufferDurationMs = bufferDurationMs;
        this.sampleBufferSize = millisecondsToSamples(bufferDurationMs);
        this.byteBufferSize = sampleBufferSize * getFrameSizeInBytes();
    }

    public PCMBufferFormat withDuration(int bufferDurationMs) {
        return new PCMBufferFormat(bufferDurationMs, sampleRate, bitDepth, nbChannels, encoding, byteOrder);
    }

    public byte[] allocatePcmBuffer() {
        return new byte[bytesPerSamples * sampleBufferSize * nbChannels];
    }

    public double[][] allocateSampleBuffer() {
        return new double[nbChannels][sampleBufferSize];
    }
}

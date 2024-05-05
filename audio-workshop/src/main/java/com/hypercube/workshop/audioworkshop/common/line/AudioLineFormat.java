package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import lombok.Getter;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
public class AudioLineFormat {
    private final int bufferDurationMs;
    private final int sampleRate;
    private final BitDepth bitDepth;
    private final int nbChannels;
    private final PCMEncoding encoding;
    private final ByteOrder byteOrder;
    private final int sampleBufferSize;
    private final int byteBufferSize;
    private final int bytesPerSamples;

    public AudioLineFormat(int bufferDurationMs, int sampleRate, BitDepth bitDepth, int nbChannels, PCMEncoding encoding, ByteOrder byteOrder) {
        this.bufferDurationMs = bufferDurationMs;
        this.sampleRate = sampleRate;
        this.bitDepth = bitDepth;
        this.nbChannels = nbChannels;
        this.encoding = encoding;
        this.byteOrder = byteOrder;
        this.sampleBufferSize = (bufferDurationMs * sampleRate) / 1000;
        this.bytesPerSamples = bitDepth.getBytes();
        this.byteBufferSize = sampleBufferSize * bytesPerSamples * nbChannels;

    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, bitDepth.getBits(), nbChannels, encoding == PCMEncoding.SIGNED, byteOrder == ByteOrder.BIG_ENDIAN);
    }

    public boolean isBigEndian() {
        return byteOrder == ByteOrder.BIG_ENDIAN;
    }

    public int getSampleSizeInBits() {
        return bitDepth.getBits();
    }

    public ByteBuffer wrapPCMBuffer(byte[] buffer) {
        return ByteBuffer.wrap(buffer)
                .order(byteOrder);
    }

    public byte[] allocatePcmBuffer() {
        return new byte[bytesPerSamples * sampleBufferSize * nbChannels];
    }

    public float[][] allocateSampleBuffer() {
        return new float[nbChannels][sampleBufferSize];
    }
}
